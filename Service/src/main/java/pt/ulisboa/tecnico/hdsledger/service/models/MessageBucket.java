package pt.ulisboa.tecnico.hdsledger.service.models;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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

    // justifyPrePrepare

    /*  return true if it has 2f+1 valid round change messages
    to be valid it should have valid signature <- TODO */
    public boolean hasValidRoundChangeQuorum(String nodeId, int instance, int round) {

        Map<Integer, Map<String, ConsensusMessage>> roundMap = bucket.get(instance);
        if (roundMap == null) {
            return false; // or handle the case where roundMap is null
        }

        Map<String, ConsensusMessage> senderMap = roundMap.get(round);
        if (senderMap == null) {
            return false; // or handle the case where senderMap is null
        }

        // Check for the highest round change message and return the value, using a normal for loop (without lambdas)
        int roundChangeCount = 0;

        for (Map.Entry<String, ConsensusMessage> entry : senderMap.entrySet()) {
            RoundChangeMessage roundChangeMessage = entry.getValue().deserializeRoundChangeMessage();
            if (roundChangeMessage.getPreparedRound() >= -1) {
                roundChangeCount++;
            }
        }

        return roundChangeCount >= quorumSize;
    }

    /* returns true if all the round change messages haven't a prepared round neither a prepared value
        returns true if it has a valid quorum of prepares with pr and pv == highestPrepare(Quorum of RoundChange)
        returns false otherwise */
    public boolean justifyRoundChange(String nodeId, int instance, int round) {

        // Get RoundChange messages
        Map<String, ConsensusMessage> roundChangeMessages = bucket.get(instance).get(round);

        // If all the round change messages haven't a prepared round neither a prepared value return true
        if (roundChangeMessages.values().stream().allMatch((message) -> {
            RoundChangeMessage roundChangeMessage = message.deserializeRoundChangeMessage();
            return roundChangeMessage.getPreparedRound() == -1 && roundChangeMessage.getPreparedValue().equals("");
        })) {

            return true;
        }

        // Get the highest prepared round and value
        Optional<Map<Integer, String>> highestPrepared = highestPrepared(nodeId, instance, round);

        // If there is no highest prepared round and value return false
        if (!highestPrepared.isPresent()) {
            return false;
        }

        // Get the highest prepared round and value
        Map<Integer, String> highestPreparedMap = highestPrepared.get();
        int highestPreparedRound = highestPreparedMap.keySet().iterator().next();

        // Iterate over round change messages, check if any of them has a valid quorum of prepares with pr and pv == highestPrepare(Quorum of RoundChange)
        for (ConsensusMessage message : roundChangeMessages.values()) {
            RoundChangeMessage roundChangeMessage = message.deserializeRoundChangeMessage();
            List<ConsensusMessage> prepareMessages = roundChangeMessage.getPrepareMessages();

            // Check if a valid quorum of prepares with pr == highestPreparedRound and pv == highestPreparedValue
            if (prepareMessages.stream().filter((prepareMessage) -> {
                PrepareMessage prepare = prepareMessage.deserializePrepareMessage();

                return prepareMessage.getRound() == highestPreparedRound && prepare.getValue().equals(highestPreparedMap.get(highestPreparedRound));
            }).count() >= quorumSize) {

                return true;
            }
        }


        return false;
    }

    // returns prepared round and prepared value (an integer and a string) of the RoundChange message with the highest pr
    public Optional<Map<Integer, String>> highestPrepared(String nodeId, int instance, int round) {

        // Get RoundChange messages
        Map<String, ConsensusMessage> roundChangeMessages = bucket.get(instance).get(round);

        int highestPreparedRound = -1;
        String highestPreparedValue = "";

        // Iterate over round change messages, get the highest prepared round and value
        for (ConsensusMessage message : roundChangeMessages.values()) {
            RoundChangeMessage roundChangeMessage = message.deserializeRoundChangeMessage();
            if (roundChangeMessage.getPreparedRound() > highestPreparedRound) {
                highestPreparedRound = roundChangeMessage.getPreparedRound();
                highestPreparedValue = roundChangeMessage.getPreparedValue();
            }
        }

        if (highestPreparedRound != -1) {
            Map<Integer, String> highestPrepared = new HashMap<>();
            highestPrepared.put(highestPreparedRound, highestPreparedValue);


            return Optional.of(highestPrepared);
        }


        return Optional.empty();
    }

    /*public Optional<String> hasValidRoundChangeQuorum(String nodeId, int instance, int round) {
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
    }*/


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

    // Given an instance and a round, returns list of messages
    public List<ConsensusMessage> getPrepareMessages(int instance, int round) {
        // get all messages from the bucket with the given instance and round
        Map<String, ConsensusMessage> roundMessages = bucket.get(instance).get(round);

        // return the list of consensus messages
        return roundMessages.values().stream().collect(Collectors.toList());
    }


    public Map<String, ConsensusMessage> getMessages(int instance, int round) {
        return bucket.get(instance).get(round);
    }
}