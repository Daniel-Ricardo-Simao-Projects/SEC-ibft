package pt.ulisboa.tecnico.hdsledger.client.services;

import pt.ulisboa.tecnico.hdsledger.communication.*;
import pt.ulisboa.tecnico.hdsledger.utilities.*;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.concurrent.atomic.AtomicInteger;
public class ClientService {

    private static final CustomLogger LOGGER = new CustomLogger(ClientService.class.getName());

    // Client Config
    private final ProcessConfig config;

    // Link to communicate with nodes
    private final Link link;

    private final Map<Integer, AppendResponse> responses = new HashMap<>();

    // Current request ID
    private AtomicInteger requestIdCounter = new AtomicInteger(0);

    public ClientService(ProcessConfig clientConfig, ProcessConfig[] nodeConfigs) {
        this.config = clientConfig;

        this.link = new Link(clientConfig, clientConfig.getPort(), nodeConfigs, AppendResponse.class);
    }

    public ArrayList<String> requestAppend(String stringToAppend) {
        int requestId = this.requestIdCounter.getAndIncrement();

        AppendRequest request = new AppendRequest(AppendRequest.Type.APPEND, this.config.getId(), requestId, stringToAppend);

        this.link.broadcast(request);

        // Use CountDownLatch to block until the response is received
        CountDownLatch latch = new CountDownLatch(1);

        // Separate thread to handle the response
        new Thread(() -> {
            try {
                while (true) {
                    AppendResponse appendResponse = responses.get(requestId);
                    if (appendResponse != null) {
                        latch.countDown(); // Release the latch when the response is received
                        break;
                    }
                    Thread.sleep(50);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        try {
            // Block until the latch is released or until a timeout occurs (adjust timeout as needed)
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        AppendResponse appendResponse = responses.get(requestId);
        if (appendResponse != null) {
            return appendResponse.getCurrentBlockchain();
        } else {
            // Handle timeout or other scenarios
            return new ArrayList<>();
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
                                    responses.put(response.getRequestId(), response);
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
