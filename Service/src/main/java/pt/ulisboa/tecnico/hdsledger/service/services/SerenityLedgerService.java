package pt.ulisboa.tecnico.hdsledger.service.services;

import pt.ulisboa.tecnico.hdsledger.communication.AppendRequest;
import pt.ulisboa.tecnico.hdsledger.communication.AppendResponse;
import pt.ulisboa.tecnico.hdsledger.communication.Message;
import pt.ulisboa.tecnico.hdsledger.communication.Link;
import pt.ulisboa.tecnico.hdsledger.utilities.CustomLogger;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

public class SerenityLedgerService implements UDPService {
    private static final CustomLogger LOGGER = new CustomLogger(SerenityLedgerService.class.getName());

    // Clients configurations
    private final ProcessConfig[] clientConfigs;

    // Node identifier
    private final String nodeId;

    // Node service
    private final NodeService service;

    // Link to communicate with blockchain nodes
    private final Link link;

    public SerenityLedgerService(String nodeId, ProcessConfig[] clientConfigs, NodeService service, Link link) {
        this.clientConfigs = clientConfigs;
        this.nodeId = nodeId;
        this.service = service;
        this.link = link;
    }

    public CompletableFuture<AppendResponse> callConsensusInstance(AppendRequest request) {
        CompletableFuture<AppendResponse> future = new CompletableFuture<>();

        service.startConsensus(request.getStringToAppend());

        // Use a separate thread to check for consensus completion
        new Thread(() -> {
            try {
                while (service.getLedger().size() < service.getConsensusInstance()) {
                    Thread.sleep(500);
                }

                AppendResponse response = new AppendResponse(Message.Type.APPEND_RESPONSE, nodeId,
                        request.getRequestId(), service.getLedger());

                future.complete(response);
            } catch (InterruptedException e) {
                future.completeExceptionally(e);
            }
        }).start();

        return future;
    }

    @Override
    public void listen() {
        try {
            // Thread to listen on every client request
            new Thread(() -> {
                try {
                    while (true) {
                        Message message = link.receive();

                        // Separate thread to handle each message
                        new Thread(() -> {
                            switch (message.getType()) {
                                case APPEND -> {
                                    AppendRequest request = (AppendRequest) message;

                                    LOGGER.log(Level.INFO,
                                            MessageFormat.format("{0} - Received message: {1} - from {2}",
                                                    nodeId, request.getStringToAppend(),
                                                    message.getSenderId()));

                                    CompletableFuture<AppendResponse> responseFuture = callConsensusInstance(request);

                                    try {
                                        AppendResponse response = responseFuture.get();
                                        link.send(message.getSenderId(), response);
                                    } catch (InterruptedException | ExecutionException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                                default -> {
                                    LOGGER.log(Level.INFO,
                                            MessageFormat.format("{0} - Received unknown message from {1}",
                                                    nodeId, message.getSenderId()));
                                }
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