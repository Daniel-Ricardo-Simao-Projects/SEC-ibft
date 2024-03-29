package pt.ulisboa.tecnico.hdsledger.service.services;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import com.google.gson.Gson;

import pt.ulisboa.tecnico.hdsledger.communication.*;
import pt.ulisboa.tecnico.hdsledger.communication.builder.ConsensusMessageBuilder;
import pt.ulisboa.tecnico.hdsledger.service.models.*;
import pt.ulisboa.tecnico.hdsledger.service.state.Block;
import pt.ulisboa.tecnico.hdsledger.utilities.CustomLogger;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig.ByzantineType;
import pt.ulisboa.tecnico.hdsledger.utilities.Authenticate;

public class NodeService implements UDPService {

    private final ScheduledExecutorService timerExecutor = Executors.newScheduledThreadPool(1);

    //private ScheduledFuture<?> timerFuture;

    // Map to store the timer for each consensus instance
    private final Map<Integer, ScheduledFuture<?>> timers = new ConcurrentHashMap<>();

    private boolean isRoundChanging = false;

    private static final CustomLogger LOGGER = new CustomLogger(NodeService.class.getName());
    // Nodes configurations
    private final ProcessConfig[] nodesConfig;

    // Current node is leader
    private final ProcessConfig config;
    // Leader configuration
    private ProcessConfig leaderConfig;

    // Link to communicate with nodes
    private final Link link;

    // Consensus instance -> Round -> List of prepare messages
    private final MessageBucket prepareMessages;
    // Consensus instance -> Round -> List of commit messages
    private final MessageBucket commitMessages;

    private final MessageBucket roundChangeMessages;

    // Store if already received pre-prepare for a given <consensus, round>
    private final Map<Integer, Map<Integer, Boolean>> receivedPrePrepare = new ConcurrentHashMap<>();

    private final Map<Integer, Map<Integer, Boolean>> receivedRoundChange = new ConcurrentHashMap<>();

    // Consensus instance information per consensus instance
    private final Map<Integer, InstanceInfo> instanceInfo = new ConcurrentHashMap<>();
    // Current consensus instance
    private final AtomicInteger consensusInstance = new AtomicInteger(0);
    // Last decided consensus instance
    private final AtomicInteger lastDecidedConsensusInstance = new AtomicInteger(0);

    // Ledger
    private Ledger ledger;    

    private final Map<Integer, Map<Integer, Boolean>> sentRoundChange = new ConcurrentHashMap<>();

    public NodeService(Link link, ProcessConfig config, ProcessConfig[] clientConfigs,
            ProcessConfig leaderConfig, ProcessConfig[] nodesConfig) {

        this.link = link;
        this.config = config;
        this.leaderConfig = leaderConfig;
        this.nodesConfig = nodesConfig;

        this.prepareMessages = new MessageBucket(nodesConfig.length);
        this.commitMessages = new MessageBucket(nodesConfig.length);
        this.roundChangeMessages = new MessageBucket(nodesConfig.length);
    
        this.ledger = new Ledger(clientConfigs);
    }

    public ProcessConfig getConfig() {
        return this.config;
    }

    public int getConsensusInstance() {
        return this.consensusInstance.get();
    }

    public Ledger getLedger() {
        return this.ledger;
    }

    private boolean isLeader(String id) {
        return this.leaderConfig.getId().equals(id);
    }

    public ConsensusMessage createConsensusMessage(String value, int instance, int round) {
        PrePrepareMessage prePrepareMessage = new PrePrepareMessage(value);

        ConsensusMessage consensusMessage = new ConsensusMessageBuilder(config.getId(), Message.Type.PRE_PREPARE)
                .setConsensusInstance(instance)
                .setRound(round)
                .setMessage(prePrepareMessage.toJson())
                .build();

        return consensusMessage;
    }

    /*
     * Start an instance of consensus for a value
     * Only the current leader will start a consensus instance
     * the remaining nodes only update values.
     *
     * @param inputValue Value to value agreed upon
     */
    public void startConsensus(String blockSerialized) {

        Block block = new Gson().fromJson(blockSerialized, Block.class);
        Transaction transaction = block.getTransaction();
        String value = transaction.getValueSignature();
        String clientId = transaction.getSourceClientId();
        byte[] clientSignature = transaction.getSignature();

        // Set initial consensus values
        int localConsensusInstance = this.consensusInstance.incrementAndGet();
        InstanceInfo existingConsensus = this.instanceInfo.put(localConsensusInstance, new InstanceInfo(blockSerialized));

        LOGGER.log(Level.INFO,
                MessageFormat.format("{0} - Starting consensus for instance {1}", config.getId(), localConsensusInstance));

        this.leaderConfig = nodesConfig[0];

        // If startConsensus was already called for a given round
        if (existingConsensus != null) {
            LOGGER.log(Level.INFO, MessageFormat.format("{0} - Node already started consensus for instance {1}",
                    config.getId(), localConsensusInstance));
            return;
        }

        // Leader broadcasts PRE-PREPARE message
        if (this.config.isLeader()) {
            
            // Verify client signature
            if (!Authenticate.verifySignature(value, clientSignature, "../Utilities/keys/" + clientId + "Pub.key")) {
                LOGGER.log(Level.WARNING,
                        MessageFormat.format("{0} - Invalid client signature for value {1}", config.getId(), value));
                return;
            } else {
                LOGGER.log(Level.INFO,
                        MessageFormat.format("{0} - Valid client signature for value {1}", config.getId(), value));
            }
            
            InstanceInfo instance = this.instanceInfo.get(localConsensusInstance);
            LOGGER.log(Level.INFO,
                    MessageFormat.format("{0} - Node is leader, sending PRE-PREPARE message", config.getId()));

            ConsensusMessage consensusMessage = this.createConsensusMessage(blockSerialized, localConsensusInstance, instance.getCurrentRound());
            // Sign message
            byte[] signature = Authenticate.signData(consensusMessage.toString(), "../Utilities/keys/" + config.getId() + "Priv.key");
            consensusMessage.setDigitalSignature(signature);
            this.link.broadcast(consensusMessage);
        } else {
            LOGGER.log(Level.INFO,
                    MessageFormat.format("{0} - Node is not leader, waiting for PRE-PREPARE message", config.getId()));
        }

        LOGGER.log(Level.INFO,
                MessageFormat.format("{0} - Node timer started for instance {1} round {2}", config.getId(),
                        localConsensusInstance, this.instanceInfo.get(localConsensusInstance).getCurrentRound()));

        ScheduledFuture<?> timerFuture = timerExecutor.schedule(() -> {

            LOGGER.log(Level.INFO,
                    MessageFormat.format("{0} - Node timer expired for instance {1} round {2}", config.getId(),
                            localConsensusInstance, this.instanceInfo.get(localConsensusInstance).getCurrentRound()));

            timerExpiredNewRound(localConsensusInstance);

        }, getRoundTimer(localConsensusInstance), TimeUnit.MILLISECONDS);

        timers.put(localConsensusInstance, timerFuture);
    }

    /*
     * Handle pre prepare messages and if the message
     * came from leader and is justified them broadcast prepare
     *
     * @param message Message to be handled
     */
    public void uponPrePrepare(ConsensusMessage message) {

        int consensusInstance = message.getConsensusInstance();
        int round = message.getRound();
        String senderId = message.getSenderId();
        int senderMessageId = message.getMessageId();

        if(round < instanceInfo.get(consensusInstance).getCurrentRound()) {
            LOGGER.log(Level.INFO,
                    MessageFormat.format("{0} - Received PRE-PREPARE message for old round {1}, ignoring",
                            config.getId(), round));
            return;
        }

        PrePrepareMessage prePrepareMessage = message.deserializePrePrepareMessage();

        String blockSerialized = prePrepareMessage.getValue();

        LOGGER.log(Level.INFO,
                MessageFormat.format(
                        "{0} - Received PRE-PREPARE message from {1} Consensus Instance {2}, Round {3}",
                        config.getId(), senderId, consensusInstance, round));

        // Verify if pre-prepare was sent by leader
        if (!isLeader(senderId)){
            LOGGER.log(Level.WARNING,
                    MessageFormat.format(
                            "{0} - Received PRE-PREPARE message from non-leader {1}",
                            config.getId(), senderId));
            return;
        }

        // Verify client signature
        Block block = new Gson().fromJson(blockSerialized, Block.class);
        Transaction transaction = block.getTransaction();
        String value = transaction.getValueSignature();
        String clientId = transaction.getSourceClientId();
        byte[] clientSignature = transaction.getSignature();

        if (!Authenticate.verifySignature(value, clientSignature, "../Utilities/keys/" + clientId + "Pub.key")) {
            LOGGER.log(Level.WARNING,
                MessageFormat.format("{0} - Invalid client signature for value {1}", config.getId(), value));
            return;
        } else {
            LOGGER.log(Level.INFO,
                MessageFormat.format("{0} - Valid client signature for value {1}", config.getId(), value));
        }

        if (roundChangeMessages.justifyPrePrepare(config.getId(), consensusInstance, round, blockSerialized)) {
            LOGGER.log(Level.INFO,
                    MessageFormat.format("{0} - Received justified PRE-PREPARE message from leader {1} for Consensus Instance {2}, Round {3}",
                            config.getId(), senderId, consensusInstance, round));
        } else {
            LOGGER.log(Level.WARNING,
                    MessageFormat.format("{0} - Received unjustified PRE-PREPARE message from leader {1} for Consensus Instance {2}, Round {3}",
                            config.getId(), senderId, consensusInstance, round));
            return;
        }

        // Set instance value
        this.instanceInfo.putIfAbsent(consensusInstance, new InstanceInfo(blockSerialized));

        // Within an instance of the algorithm, each upon rule is triggered at most once
        // for any round r
        receivedPrePrepare.putIfAbsent(consensusInstance, new ConcurrentHashMap<>());
        if (receivedPrePrepare.get(consensusInstance).put(round, true) != null) {
            LOGGER.log(Level.INFO,
                    MessageFormat.format(
                            "{0} - Already received PRE-PREPARE message for Consensus Instance {1}, Round {2}, "
                                    + "replying again to make sure it reaches the initial sender",
                            config.getId(), consensusInstance, round));
            PrepareMessage prepareMessage = new PrepareMessage(prePrepareMessage.getValue());

            ConsensusMessage consensusMessage = new ConsensusMessageBuilder(config.getId(), Message.Type.PREPARE)
                    .setConsensusInstance(consensusInstance)
                    .setRound(round)
                    .setMessage(prepareMessage.toJson())
                    .setReplyTo(senderId)
                    .setReplyToMessageId(senderMessageId)
                    .build();
            
            // Sign message
            byte[] signature = Authenticate.signData(consensusMessage.toString(), "../Utilities/keys/" + config.getId() + "Priv.key");
            consensusMessage.setDigitalSignature(signature);
            this.link.broadcast(consensusMessage);

            return;
        }

        resetTimer(consensusInstance);

        PrepareMessage prepareMessage = new PrepareMessage(prePrepareMessage.getValue());

        ConsensusMessage consensusMessage = new ConsensusMessageBuilder(config.getId(), Message.Type.PREPARE)
                .setConsensusInstance(consensusInstance)
                .setRound(round)
                .setMessage(prepareMessage.toJson())
                .setReplyTo(senderId)
                .setReplyToMessageId(senderMessageId)
                .build();

        // Sign message
        byte[] signature = Authenticate.signData(consensusMessage.toString(), "../Utilities/keys/" + config.getId() + "Priv.key");
        consensusMessage.setDigitalSignature(signature);
        this.link.broadcast(consensusMessage);
    }

    /*
     * Handle prepare messages and if there is a valid quorum broadcast commit
     *
     * @param message Message to be handled
     */
    public synchronized void uponPrepare(ConsensusMessage message) {

        int consensusInstance = message.getConsensusInstance();
        int round = message.getRound();
        String senderId = message.getSenderId();

        if(round < instanceInfo.get(consensusInstance).getCurrentRound()) {
            LOGGER.log(Level.INFO,
                    MessageFormat.format("{0} - Received PREPARE message for old round {1}, ignoring",
                            config.getId(), round));
            return;
        }

        PrepareMessage prepareMessage = message.deserializePrepareMessage();

        String value = prepareMessage.getValue();

        LOGGER.log(Level.INFO,
                MessageFormat.format(
                        "{0} - Received PREPARE message from {1}: Consensus Instance {2}, Round {3}",
                        config.getId(), senderId, consensusInstance, round));

        // Doesn't add duplicate messages
        prepareMessages.addMessage(message);

        // Set instance values
        this.instanceInfo.putIfAbsent(consensusInstance, new InstanceInfo(value));
        InstanceInfo instance = this.instanceInfo.get(consensusInstance);

        // Within an instance of the algorithm, each upon rule is triggered at most once
        // for any round r
        // Late prepare (consensus already ended for other nodes) only reply to him (as
        // an ACK)
        if (instance.getPreparedRound() >= round) {
            LOGGER.log(Level.INFO,
                    MessageFormat.format(
                            "{0} - Already received PREPARE message for Consensus Instance {1}, Round {2}, "
                                    + "replying again to make sure it reaches the initial sender",
                            config.getId(), consensusInstance, round));
            
            // drop commit message
            if (config.getByzantineType() == ByzantineType.SILENT_COMMIT && round == 1) {
                LOGGER.log(Level.WARNING,
                        MessageFormat.format(
                                "{0} - Not broadcasting COMMIT message in round 1 to himself to force round change",
                                config.getId()));
                return;
            }

            ConsensusMessage m = new ConsensusMessageBuilder(config.getId(), Message.Type.COMMIT)
                    .setConsensusInstance(consensusInstance)
                    .setRound(round)
                    .setReplyTo(senderId)
                    .setReplyToMessageId(message.getMessageId())
                    .setMessage(instance.getCommitMessage().toJson())
                    .build();

            // Sign message
            byte[] signature = Authenticate.signData(m.toString(), "../Utilities/keys/" + config.getId() + "Priv.key");
            m.setDigitalSignature(signature);
            link.send(senderId, m);
            return;
        }

        // Find value with valid quorum
        Optional<String> preparedValue = prepareMessages.hasValidPrepareQuorum(config.getId(), consensusInstance, round);
        if (preparedValue.isPresent() && instance.getPreparedRound() < round) {
            instance.setPreparedValue(preparedValue.get());
            instance.setPreparedRound(round);

            // Must reply to prepare message senders
            Collection<ConsensusMessage> sendersMessage = prepareMessages.getMessages(consensusInstance, round)
                    .values();

            CommitMessage c = new CommitMessage(preparedValue.get());
            instance.setCommitMessage(c);

            // drop commit message
            if (config.getByzantineType() == ByzantineType.SILENT_COMMIT && round == 1) {
                LOGGER.log(Level.WARNING,
                        MessageFormat.format(
                                "{0} - Not broadcasting COMMIT message in round 1 to force round change",
                                config.getId()));
                return;
            }

            sendersMessage.forEach(senderMessage -> {
                ConsensusMessage m = new ConsensusMessageBuilder(config.getId(), Message.Type.COMMIT)
                        .setConsensusInstance(consensusInstance)
                        .setRound(round)
                        .setReplyTo(senderMessage.getSenderId())
                        .setReplyToMessageId(senderMessage.getMessageId())
                        .setMessage(c.toJson())
                        .build();

                // Sign message
                byte[] signature = Authenticate.signData(m.toString(), "../Utilities/keys/" + config.getId() + "Priv.key");
                m.setDigitalSignature(signature);
                link.send(senderMessage.getSenderId(), m);
            });
        }
    }

    /*
     * Handle commit messages and decide if there is a valid quorum
     *
     * @param message Message to be handled
     */
    public synchronized void uponCommit(ConsensusMessage message) {

        int consensusInstance = message.getConsensusInstance();
        int round = message.getRound();

        if (round < instanceInfo.get(consensusInstance).getCurrentRound()) {
            LOGGER.log(Level.INFO,
                    MessageFormat.format("{0} - Received COMMIT message for old round {1}, ignoring",
                            config.getId(), round));
            return;
        }

        LOGGER.log(Level.INFO,
                MessageFormat.format("{0} - Received COMMIT message from {1}: Consensus Instance {2}, Round {3}",
                        config.getId(), message.getSenderId(), consensusInstance, round));

        commitMessages.addMessage(message);

        InstanceInfo instance = this.instanceInfo.get(consensusInstance);

        if (instance == null) {
            // Should never happen because only receives commit as a response to a prepare message
            LOGGER.log(Level.WARNING, MessageFormat.format(
                    "{0} - CRITICAL: Received COMMIT message from {1}: Consensus Instance {2}, Round {3} BUT NO INSTANCE INFO",
                    config.getId(), message.getSenderId(), consensusInstance, round));
            return;
        }

        // Within an instance of the algorithm, each upon rule is triggered at most once
        // for any round r
        if (instance.getCommittedRound() >= round) {
            LOGGER.log(Level.INFO,
                    MessageFormat.format(
                            "{0} - Already received COMMIT message for Consensus Instance {1}, Round {2}, ignoring",
                            config.getId(), consensusInstance, round));
            return;
        }

        Optional<String> commitValue = commitMessages.hasValidCommitQuorum(config.getId(),
                consensusInstance, round);

        if (commitValue.isPresent() && instance.getCommittedRound() < round) {

            cancelTimer(consensusInstance);

            instance = this.instanceInfo.get(consensusInstance);
            instance.setCommittedRound(round);

            String value = commitValue.get();

            // Only finish a consensus instance if the last one was decided
            // We need to be sure that the previous value has been decided
            while (lastDecidedConsensusInstance.get() < consensusInstance - 1) {
                LOGGER.log(Level.INFO,
                        MessageFormat.format("{0} - Waiting for last consensus instance to be decided", config.getId()));
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }


            // Append value to the ledger (must be synchronized to be thread-safe)
            synchronized(ledger) {

                // Increment size of ledger to accommodate current instance
                ArrayList<String> ledgerList = ledger.getLedgerList();
                ledgerList.ensureCapacity(consensusInstance);
                while (ledgerList.size() < consensusInstance - 1) {
                    ledgerList.add("");
                }

                ledgerList.add(consensusInstance - 1, value);

                // assuming all transactions are transfers, change account balances
                Map<String, Account> accounts = ledger.getAccounts();

                Block block = new Gson().fromJson(value, Block.class);

                Transaction transaction = block.getTransaction();

                accounts.get(transaction.getSourceClientId()).subtractBalance(transaction.getAmount() + transaction.getFee());
                accounts.get(transaction.getDestClientId()).addBalance(transaction.getAmount());

                LOGGER.log(Level.INFO,
                        MessageFormat.format(
                                "{0} - Current Ledger: {1}",
                                config.getId(), String.join("", ledgerList)));
            }

            lastDecidedConsensusInstance.getAndIncrement();

            LOGGER.log(Level.INFO,
                    MessageFormat.format(
                            "{0} - Decided on Consensus Instance {1}, Round {2}, Successful? {3}",
                            config.getId(), consensusInstance, round, true));
        }
    }

    public synchronized void uponRoundChange(ConsensusMessage message) {

        int consensusInstance = message.getConsensusInstance();
        int round = message.getRound();

        LOGGER.log(Level.INFO,
                MessageFormat.format("{0} - Received ROUND CHANGE message from {1}: Consensus Instance {2}, Round {3}",
                        config.getId(), message.getSenderId(), consensusInstance, round));

        roundChangeMessages.addMessage(message);

        InstanceInfo instance = this.instanceInfo.get(consensusInstance);

        if(config.getId() == leaderConfig.getId() &&
                roundChangeMessages.hasValidRoundChangeQuorum(config.getId(), consensusInstance, round) &&
                roundChangeMessages.justifyRoundChange(config.getId(), consensusInstance, round)) {

            LOGGER.log(Level.INFO,
                    MessageFormat.format("{0} - Received valid  and justified ROUND CHANGE quorum for Consensus Instance {1}, Round {2}",
                            config.getId(), consensusInstance, round));

            // highestPrepared returns Optional<Map<Integer, String>>. If it's present, it means that there's a prepared value
            Optional<Map<Integer, String>> highestPrepared = roundChangeMessages.highestPrepared(config.getId(), consensusInstance, round);


            int newRound = 0;
            String value = "";

            if (highestPrepared.isPresent()) {
                Map<Integer, String> foundHighestPrepare = highestPrepared.get();
                newRound = foundHighestPrepare.keySet().iterator().next();
                value = foundHighestPrepare.get(newRound);
                if(newRound < instance.getCurrentRound())
                    newRound = instance.getCurrentRound();
            } else  {
                newRound = instance.getCurrentRound();
                value = instance.getInputValue();
            }

            LOGGER.log(Level.SEVERE,
                    MessageFormat.format("{0} - Broadcasting PRE-PREPARE message with valid prepared value: {1} for Consensus Instance {2}, Round {3}",
                            config.getId(), value, consensusInstance, newRound));

            ConsensusMessage consensusMessage = this.createConsensusMessage(value, consensusInstance, newRound);
            // Sign message
            byte[] signature = Authenticate.signData(consensusMessage.toString(), "../Utilities/keys/" + config.getId() + "Priv.key");
            consensusMessage.setDigitalSignature(signature);
            this.link.broadcast(consensusMessage);
            isRoundChanging = false;
        } else if (roundChangeMessages.hasValidRoundChangeQuorum(config.getId(), consensusInstance, round)) {
            LOGGER.log(Level.INFO,
                    MessageFormat.format("{0} - Received valid ROUND CHANGE quorum for Consensus Instance {1}, Round {2}",
                            config.getId(), consensusInstance, round));
            isRoundChanging = false;
        }

        if(roundChangeMessages.hasValidRoundChangeSet(config.getId(), consensusInstance, round, instance.getCurrentRound()) &&
                !alreadySentRoundChangeMessage(consensusInstance, round)) {

            int minimumRound = roundChangeMessages.getMinimumRoundChangeRound(config.getId(), consensusInstance, round, instance.getCurrentRound());

            if (minimumRound < instance.getCurrentRound()) {
                LOGGER.log(Level.INFO,
                        MessageFormat.format("{0} - Received ROUND CHANGE message for old round {1}, ignoring",
                                config.getId(), minimumRound));
                return;
            } else {
                instance.setCurrentRound(minimumRound);
            }

            this.leaderConfig = nodesConfig[(instance.getCurrentRound() % nodesConfig.length) - 1];

            resetTimer(consensusInstance);

            LOGGER.log(Level.INFO,
                    MessageFormat.format("{0} - Broadcasting ROUND CHANGE message for Consensus Instance {1}, Round {2}",
                            config.getId(), consensusInstance, minimumRound));

            // broadcast round change message
            RoundChangeMessage roundChangeMessage = new RoundChangeMessage();

            int localConsensusInstance = this.consensusInstance.get();

            if (instance.getPreparedRound() > 0) {
                roundChangeMessage.setPreparedRound(instance.getPreparedRound());
                roundChangeMessage.setPreparedValue(instance.getPreparedValue());
                roundChangeMessage.setPrepareMessages(prepareMessages.getPrepareMessages(localConsensusInstance, instance.getPreparedRound()));

                LOGGER.log(Level.INFO,
                        MessageFormat.format("{0} - Broadcasting ROUND CHANGE message with prepared value: {1} for Consensus Instance {2}, Round {3}",
                                config.getId(), instance.getPreparedValue(), localConsensusInstance, instance.getCurrentRound()));
            } else {
                roundChangeMessage.setPreparedRound(-1);
                roundChangeMessage.setPreparedValue("");
                roundChangeMessage.setPrepareMessages(new ArrayList<>());

                LOGGER.log(Level.INFO,
                        MessageFormat.format("{0} - Broadcasting ROUND CHANGE message with no prepared value for Consensus Instance {1}, Round {2}",
                                config.getId(), localConsensusInstance, instance.getCurrentRound()));
            }

            ConsensusMessage consensusMessage = new ConsensusMessageBuilder(config.getId(), Message.Type.ROUND_CHANGE)
                    .setConsensusInstance(localConsensusInstance)
                    .setRound(instance.getCurrentRound())
                    .setMessage(roundChangeMessage.toJson())
                    .build();

            // Sign message
            byte[] signature = Authenticate.signData(consensusMessage.toString(), "../Utilities/keys/" + config.getId() + "Priv.key");
            consensusMessage.setDigitalSignature(signature);
            this.link.broadcast(consensusMessage);

            sentRoundChange.putIfAbsent(localConsensusInstance, new ConcurrentHashMap<>());
            sentRoundChange.get(localConsensusInstance).put(instance.getCurrentRound(), true);
        }
    }

    private boolean alreadySentRoundChangeMessage(int consensusInstance, int round) {
        return sentRoundChange.get(consensusInstance) != null && sentRoundChange.get(consensusInstance).get(round) != null;
    }

    // Method to reset the timer
    private void resetTimer(int consensusInstance) {

        // Cancel the existing timer if it's still running
        if (timers.get(consensusInstance) != null && !timers.get(consensusInstance).isDone()) {
            timers.get(consensusInstance).cancel(true);
        }

        LOGGER.log(Level.INFO,
                MessageFormat.format("{0} - Node timer restarted for instance {1} round {2}", config.getId(),
                        consensusInstance, this.instanceInfo.get(consensusInstance).getCurrentRound()));

        // Schedule a new task with the desired delay
        ScheduledFuture<?> timerFuture = timerExecutor.schedule(() -> {

            LOGGER.log(Level.INFO,
                    MessageFormat.format("{0} - Node timer expired for instance {1} round {2}", config.getId(),
                            this.consensusInstance.get(), this.instanceInfo.get(this.consensusInstance.get()).getCurrentRound()));

            timerExpiredNewRound(consensusInstance);

        }, getRoundTimer(consensusInstance), TimeUnit.MILLISECONDS);

        // Replace the old timer with the new one
        timers.put(consensusInstance, timerFuture);
    }

    private int getRoundTimer(int consensusInstance) {
        int round = this.instanceInfo.get(consensusInstance).getCurrentRound();
        LOGGER.log(Level.SEVERE,
                MessageFormat.format("{0} - Timer for instance {1} round {2} is {3}ms", config.getId(),
                        consensusInstance, round, Math.pow(2, round) * 1000 + 2000));
        return (int) Math.pow(2, round) * 1000 + 2000;
    }

    private void cancelTimer(int consensusInstance) {
        if (timers.get(consensusInstance) != null && !timers.get(consensusInstance).isDone()) {
            timers.get(consensusInstance).cancel(true);
        }

        LOGGER.log(Level.INFO,
                MessageFormat.format("{0} - Node timer stopped for instance {1}", config.getId(), consensusInstance));
    }

    private void timerExpiredNewRound(int localConsensusInstance) {
        // Increment round number
        InstanceInfo instance = this.instanceInfo.get(localConsensusInstance);

        LOGGER.log(Level.INFO,
                MessageFormat.format("{0} - Timer expired for Consensus Instance {1}, Round {2}",
                        config.getId(), localConsensusInstance, instance.getCurrentRound()));

        instance.setCurrentRound(instance.getCurrentRound() + 1);
        isRoundChanging = true;

        // Set new leader for new round
        this.leaderConfig = nodesConfig[(instance.getCurrentRound() % nodesConfig.length) - 1];
        LOGGER.log(Level.SEVERE,
                MessageFormat.format("{0} - New leader for instance {1} round {2} is {3}", config.getId(),
                        localConsensusInstance, instance.getCurrentRound(), leaderConfig.getId()));

        // Set timer to running
        resetTimer(localConsensusInstance);

        // Broadcast ROUND_CHANGE message
        RoundChangeMessage roundChangeMessage = new RoundChangeMessage();

        if (instance.getPreparedRound() > 0) {
            roundChangeMessage.setPreparedRound(instance.getPreparedRound());
            roundChangeMessage.setPreparedValue(instance.getPreparedValue());
            roundChangeMessage.setPrepareMessages(prepareMessages.getPrepareMessages(localConsensusInstance, instance.getPreparedRound()));

            LOGGER.log(Level.INFO,
                    MessageFormat.format("{0} - Broadcasting ROUND CHANGE message with prepared value: {1} for Consensus Instance {2}, Round {3}",
                            config.getId(), instance.getPreparedValue(), localConsensusInstance, instance.getCurrentRound()));
        } else {
            roundChangeMessage.setPreparedRound(-1);
            roundChangeMessage.setPreparedValue("");
            roundChangeMessage.setPrepareMessages(new ArrayList<>());

            LOGGER.log(Level.INFO,
                    MessageFormat.format("{0} - Broadcasting ROUND CHANGE message with no prepared value for Consensus Instance {1}, Round {2}",
                            config.getId(), localConsensusInstance, instance.getCurrentRound()));
        }

        ConsensusMessage consensusMessage = new ConsensusMessageBuilder(config.getId(), Message.Type.ROUND_CHANGE)
                .setConsensusInstance(localConsensusInstance)
                .setRound(instance.getCurrentRound())
                .setMessage(roundChangeMessage.toJson())
                .build();

        // Sign message
        byte[] signature = Authenticate.signData(consensusMessage.toString(), "../Utilities/keys/" + config.getId() + "Priv.key");
        consensusMessage.setDigitalSignature(signature);
        this.link.broadcast(consensusMessage);

        sentRoundChange.putIfAbsent(localConsensusInstance, new ConcurrentHashMap<>());
        sentRoundChange.get(localConsensusInstance).put(instance.getCurrentRound(), true);
    }

    @Override
    public void listen() {
        try {
            // Thread to listen on every request
            new Thread(() -> {
                try {
                    while (true) {
                        Message message = link.receive();

                        // Separate thread to handle each message
                        new Thread(() -> {

                            switch (message.getType()) {

                                case PRE_PREPARE ->
                                    uponPrePrepare((ConsensusMessage) message);

                                case PREPARE ->
                                    uponPrepare((ConsensusMessage) message);

                                case COMMIT ->
                                    uponCommit((ConsensusMessage) message);

                                case ROUND_CHANGE ->
                                    uponRoundChange((ConsensusMessage) message);

                                case ACK ->
                                    LOGGER.log(Level.INFO, MessageFormat.format("{0} - Received ACK message from {1}",
                                            config.getId(), message.getSenderId()));

                                case IGNORE ->
                                    LOGGER.log(Level.INFO,
                                            MessageFormat.format("{0} - Received IGNORE message from {1}",
                                                    config.getId(), message.getSenderId()));

                                default ->
                                    LOGGER.log(Level.INFO,
                                            MessageFormat.format("{0} - Received unknown message from {1}",
                                                    config.getId(), message.getSenderId()));

                            }

                        }).start();
                    }
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
