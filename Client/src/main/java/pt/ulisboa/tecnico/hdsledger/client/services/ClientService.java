package pt.ulisboa.tecnico.hdsledger.client.services;

import pt.ulisboa.tecnico.hdsledger.communication.ConsensusMessage;
import pt.ulisboa.tecnico.hdsledger.communication.Message;
import pt.ulisboa.tecnico.hdsledger.communication.Link;
import pt.ulisboa.tecnico.hdsledger.communication.AppendRequest;
import pt.ulisboa.tecnico.hdsledger.utilities.*;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.concurrent.atomic.AtomicInteger;
public class ClientService {

    private static final CustomLogger LOGGER = new CustomLogger(ClientService.class.getName());

    // Client Config
    private final ProcessConfig config;

    // Link to communicate with nodes
    private final Link link;

    // Current request ID
    private AtomicInteger requestIdCounter = new AtomicInteger(0);

    public ClientService(ProcessConfig clientConfig, ProcessConfig[] nodeConfigs) {
        this.config = clientConfig;

        this.link = new Link(config, config.getPort(), nodeConfigs, ConsensusMessage.class);
    }

    public String requestAppend(String stringToAppend) {
        int requestId = this.requestIdCounter.getAndIncrement();

        AppendRequest request = new AppendRequest(AppendRequest.Type.APPEND, this.config.getId(), requestId, stringToAppend);

        this.link.broadcast(request);

        return stringToAppend;
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
