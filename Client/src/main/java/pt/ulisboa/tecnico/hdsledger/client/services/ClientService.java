package pt.ulisboa.tecnico.hdsledger.client.services;

import pt.ulisboa.tecnico.hdsledger.communication.*;
import pt.ulisboa.tecnico.hdsledger.communication.Message.Type;
import pt.ulisboa.tecnico.hdsledger.utilities.*;

import java.io.IOException;
import java.security.PublicKey;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import com.google.gson.Gson;

import java.util.concurrent.atomic.AtomicInteger;
public class ClientService {

    private static final CustomLogger LOGGER = new CustomLogger(ClientService.class.getName());

    // Client Config
    private final ProcessConfig config;

    // Link to communicate with nodes
    private final Link link;

    // private final Map<Integer, AppendResponse> responses = new HashMap<>();

    private final Map<Integer, List<AppendResponse>> responses = new HashMap<>(); // Use List to store multiple responses

    private final int quorumSize;

    private final int f;

    // Current request ID
    private AtomicInteger requestIdCounter = new AtomicInteger(0);

    public ClientService(ProcessConfig clientConfig, ProcessConfig[] nodeConfigs) {
        this.config = clientConfig;

        this.link = new Link(clientConfig, clientConfig.getPort(), nodeConfigs, AppendResponse.class);

        int nodeCount = nodeConfigs.length;

        f = Math.floorDiv(nodeCount - 1, 3);
        quorumSize = Math.floorDiv(nodeCount + f, 2) + 1;
    }

    public boolean transfer(PublicKey source, PublicKey destination, String destId, int amount) {
        System.out.println("Transferring amount: " + amount + "...");

        TransferRequest transferRequest = new TransferRequest(source, destination, destId, amount);
        
        String requestTransferSerialized = new Gson().toJson(transferRequest);
        
        if (requestAppend(requestTransferSerialized, Type.TRANSFER).equals("")) {
            return false;
        }

        return true;
    }

    public int checkBalance(PublicKey publicKey) {
        System.out.println("Checking balance...");
        
        BalanceRequest balanceRequest = new BalanceRequest(publicKey);

        String requestBalanceSerialized = new Gson().toJson(balanceRequest);

        String balance = requestAppend(requestBalanceSerialized, Type.BALANCE);

        try {
            return Integer.parseInt(balance);
        } catch (Exception e) {
            System.out.println("Error: " + e);
            return 0;
        }
    }

    public String requestAppend(String stringToAppend, Type type) {
        int requestId = this.requestIdCounter.getAndIncrement();

        AppendRequest request = new AppendRequest(type, this.config.getId(), requestId, stringToAppend);

        // Sign data
        byte[] signature = Authenticate.signData(stringToAppend, "../Utilities/keys/" + this.config.getId() + "Priv.key");
        request.setSignature(signature);

        this.link.broadcast(request);

        // Use CountDownLatch to block until the response is received
        CountDownLatch latch = new CountDownLatch(1);

        // Separate thread to handle the response
        new Thread(() -> {
            try {
                while (true) {
                    List<AppendResponse> appendResponses = responses.get(requestId);
                    if (appendResponses != null && appendResponses.size() >= quorumSize) { // Check if 3 responses are received
                        Map<String, Integer> responseCounts = new HashMap<>();
                        for (AppendResponse response : appendResponses) {
                            responseCounts.put(response.getResponse(), responseCounts.getOrDefault(response.getResponse(), 0) + 1);
                        }
                        for (int count : responseCounts.values()) {
                            if (count >= quorumSize) {
                                latch.countDown(); // Release the latch when a quorum is achieved
                                break;
                            }
                        }
                    }
                    Thread.sleep(50);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        try {
            // Block until the latch is released or until a timeout occurs
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        List<AppendResponse> appendResponses = responses.get(requestId);
        if (appendResponses != null && appendResponses.size() >= quorumSize) { // Check if 3 responses are received
            // return most common response
            Map<String, Integer> responseCounts = new HashMap<>();
            for (AppendResponse response : appendResponses) {
                responseCounts.put(response.getResponse(), responseCounts.getOrDefault(response.getResponse(), 0) + 1);
            }

            String mostCommonResponse = "";

            for (Map.Entry<String, Integer> entry : responseCounts.entrySet()) {
                if (entry.getValue() > responseCounts.getOrDefault(mostCommonResponse, 0)) {
                    mostCommonResponse = entry.getKey();
                }
            }

            return mostCommonResponse;
        } else {
            // Handle timeout or other scenarios
            return "";
        }
    }


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

                                case APPEND_RESPONSE -> {
                                    LOGGER.log(Level.INFO,
                                            MessageFormat.format("{0} - Received APPEND_RESPONSE message from {1}",
                                                    config.getId(), message.getSenderId()));

                                    AppendResponse response = (AppendResponse) message;
                                    responses.computeIfAbsent(response.getRequestId(), k -> new ArrayList<>()).add(response); // Store responses in the map
                                }

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
