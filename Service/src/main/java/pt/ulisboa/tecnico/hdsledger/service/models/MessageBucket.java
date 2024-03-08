package pt.ulisboa.tecnico.hdsledger.service.models;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import pt.ulisboa.tecnico.hdsledger.communication.CommitMessage;
import pt.ulisboa.tecnico.hdsledger.communication.ConsensusMessage;
import pt.ulisboa.tecnico.hdsledger.communication.PrepareMessage;
import pt.ulisboa.tecnico.hdsledger.communication.RoundChangeMessage;
import pt.ulisboa.tecnico.hdsledger.utilities.CustomLogger;

public class MessageBucket {

    private static final CustomLogger LOGGER = new CustomLogger(MessageBucket.class.getName());
    // Number of support faulty nodes
    private final int f;
    // Quorum size
    private final int quorumSize;
    // Instance -> Round -> Sender ID -> Consensus message
    private final Map<Integer, Map<Integer, Map<String, ConsensusMessage>>> bucket = new ConcurrentHashMap<>();

    public MessageBucket(int nodeCount) {
        f = Math.floorDiv(nodeCount - 1, 3);
        quorumSize = Math.floorDiv(nodeCount + f, 2) + 1;
    }

    /*
     * Add a message to the bucket
     * 
     * @param consensusInstance
     * 
     * @param message
     */
    public void addMessage(ConsensusMessage message) {
        int consensusInstance = message.getConsensusInstance();
        int round = message.getRound();

        bucket.putIfAbsent(consensusInstance, new ConcurrentHashMap<>());
        bucket.get(consensusInstance).putIfAbsent(round, new ConcurrentHashMap<>());
        bucket.get(consensusInstance).get(round).put(message.getSenderId(), message);
    }

    public Optional<String> hasValidPrepareQuorum(String nodeId, int instance, int round) {
        // Create mapping of value to frequency
        HashMap<String, Integer> frequency = new HashMap<>();
        bucket.get(instance).get(round).values().forEach((message) -> {
            PrepareMessage prepareMessage = message.deserializePrepareMessage();
            String value = prepareMessage.getValue();
            frequency.put(value, frequency.getOrDefault(value, 0) + 1);
        });

        // Only one value (if any, thus the optional) will have a frequency
        // greater than or equal to the quorum size
        return frequency.entrySet().stream().filter((Map.Entry<String, Integer> entry) -> {
            return entry.getValue() >= quorumSize;
        }).map((Map.Entry<String, Integer> entry) -> {
            return entry.getKey();
        }).findFirst();
    }

    public Optional<String> hasValidCommitQuorum(String nodeId, int instance, int round) {
        // Create mapping of value to frequency
        HashMap<String, Integer> frequency = new HashMap<>();
        bucket.get(instance).get(round).values().forEach((message) -> {
            CommitMessage commitMessage = message.deserializeCommitMessage();
            String value = commitMessage.getValue();
            frequency.put(value, frequency.getOrDefault(value, 0) + 1);
        });

        // Only one value (if any, thus the optional) will have a frequency
        // greater than or equal to the quorum size
        return frequency.entrySet().stream().filter((Map.Entry<String, Integer> entry) -> {
            return entry.getValue() >= quorumSize;
        }).map((Map.Entry<String, Integer> entry) -> {
            return entry.getKey();
        }).findFirst();
    }

    public Optional<String> hasValidRoundChangeQuorum(String nodeId, int instance, int round) {
        Map<Integer, Map<String, ConsensusMessage>> roundMap = bucket.get(instance);
        if (roundMap == null) {
            return Optional.empty(); // or handle the case where roundMap is null
        }

        Map<String, ConsensusMessage> senderMap = roundMap.get(round);
        if (senderMap == null) {
            return Optional.empty(); // or handle the case where senderMap is null
        }

        // Check for the highest round change message and return the value, using a normal for loop (without lambdas)
        int roundBegin = -1;
        String valueToReturn = "";

        for (Map.Entry<String, ConsensusMessage> entry : senderMap.entrySet()) {
            RoundChangeMessage roundChangeMessage = entry.getValue().deserializeRoundChangeMessage();
            if (roundChangeMessage.getPreparedRound() > roundBegin) {
                roundBegin = roundChangeMessage.getPreparedRound();
                valueToReturn = roundChangeMessage.getPreparedValue();
            }
        }

        return Optional.of(valueToReturn);
    }


    public Optional<Integer> findSmallestValidRoundChange(String nodeId, int instance, int currentRound) {
        // Create mapping of round to frequency
        HashMap<Integer, Integer> frequency = new HashMap<>();

        bucket.get(instance).forEach((round, roundMessages) -> {
            if (round > currentRound) {
                roundMessages.values().forEach((message) -> {
                    RoundChangeMessage roundChangeMessage = message.deserializeRoundChangeMessage();
                    int receivedRound = roundChangeMessage.getPreparedRound();
                    frequency.put(receivedRound, frequency.getOrDefault(receivedRound, 0) + 1);
                });
            }
        });

        // Find the smallest round
        return frequency.keySet().stream().min(Integer::compareTo);
    }


    public Map<String, ConsensusMessage> getMessages(int instance, int round) {
        return bucket.get(instance).get(round);
    }
}