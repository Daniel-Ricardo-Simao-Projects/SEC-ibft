package pt.ulisboa.tecnico.hdsledger.service.services;

import pt.ulisboa.tecnico.hdsledger.communication.AppendRequest;
import pt.ulisboa.tecnico.hdsledger.communication.AppendResponse;
import pt.ulisboa.tecnico.hdsledger.communication.Message;
import pt.ulisboa.tecnico.hdsledger.communication.TransferRequest;
import pt.ulisboa.tecnico.hdsledger.communication.Link;
import pt.ulisboa.tecnico.hdsledger.service.models.Transaction;
import pt.ulisboa.tecnico.hdsledger.service.models.TransactionQueue;
import pt.ulisboa.tecnico.hdsledger.utilities.Authenticate;
import pt.ulisboa.tecnico.hdsledger.utilities.CustomLogger;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;
import pt.ulisboa.tecnico.hdsledger.service.models.Account;
import pt.ulisboa.tecnico.hdsledger.service.state.Block;

import java.io.IOException;
import java.security.PublicKey;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

import javax.swing.text.html.Option;

import com.google.gson.Gson;

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

    // Transaction queue
    private TransactionQueue transactionQueue;

    public SerenityLedgerService(String nodeId, ProcessConfig[] clientConfigs, NodeService service, Link link,
                                 TransactionQueue transactionQueue) {
        this.clientConfigs = clientConfigs;
        this.nodeId = nodeId;
        this.service = service;
        this.link = link;
        this.transactionQueue = transactionQueue;
    }

    public CompletableFuture<AppendResponse> callConsensusInstance(AppendRequest request) {
        CompletableFuture<AppendResponse> future = new CompletableFuture<>();

        TransferRequest transferRequest = request.deserializeTransferMessage();

        Transaction transaction = new Transaction(Authenticate.getPublicKeyFromString(transferRequest.getSourcePubKey()),
                request.getSenderId(), Authenticate.getPublicKeyFromString(transferRequest.getDestPubKey()),
                transferRequest.getDestClientId(), transferRequest.getAmount(), request.getStringToAppend(), request.getSignature());

        transactionQueue.addTransaction(transaction);

        if(transactionQueue.haveNecessaryTransactions()) {
            Block block = new Block(transactionQueue.getBlockSize());
            block.setTransactions(transactionQueue.getTransactions());
            String blockSerialized = new Gson().toJson(block);
            LOGGER.log(Level.INFO, MessageFormat.format("{0} - Starting consensus instance for block: {1}", nodeId, block.toString()));
            service.startConsensus(blockSerialized);
        }

        // Use a separate thread to check for consensus completion
        new Thread(() -> {
            try {
                /*while (service.getLedger().getLedgerList().size() < service.getConsensusInstance()) {
                    Thread.sleep(500);
                }*/

                AppendResponse response = new AppendResponse(Message.Type.APPEND_RESPONSE, nodeId,
                        request.getRequestId(), "SUCCESS");

                future.complete(response);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        }).start();

        return future;
    }

    public boolean verifyTransfer(AppendRequest request) {

        // Deserialize transfer request
        TransferRequest transferRequest = request.deserializeTransferMessage();

        PublicKey sourcePubKey = Authenticate.getPublicKeyFromString(transferRequest.getSourcePubKey());

        Account sourceAccount = service.getLedger().getAccounts().get(request.getSenderId());
        if (sourceAccount == null) {
            LOGGER.log(Level.WARNING, "Source account not found");
            return false;
        }
        Account destAccount = service.getLedger().getAccounts().get(transferRequest.getDestClientId());
        if (destAccount == null) {
            LOGGER.log(Level.WARNING, "Destination account not found");
            return false;
        }

        // Check if the signature is valid
        if (!Authenticate.verifyDigitalSignature(request.getStringToAppend(), request.getSignature(), sourcePubKey)) {
            LOGGER.log(Level.WARNING, "Invalid signature");
            return false;
        }

        if (transferRequest.getSourcePubKey().equals(transferRequest.getDestPubKey())) {
            LOGGER.log(Level.WARNING, "Source and destination accounts are the same, not allowed");
            return false;
        }

        if (transferRequest.getAmount() <= 0) {
            LOGGER.log(Level.WARNING, "Invalid amount");
            return false;
        }

        if (!sourcePubKey.equals(Authenticate.readPublicKey("../Utilities/keys/" + request.getSenderId() + "Pub.key"))) {
            LOGGER.log(Level.WARNING, "The public key does not match the sender's public key");
            return false;
        }

        if (sourceAccount.getBalance() < transferRequest.getAmount() + transferRequest.getAmount() / 10) {
            LOGGER.log(Level.WARNING, "Insufficient funds");
            return false;
        }
        
        return true;

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
                                case TRANSFER -> {
                                    AppendRequest request = (AppendRequest) message;

                                    LOGGER.log(Level.INFO,
                                            MessageFormat.format("{0} - Received TRANSFER message: {1} - from {2}",
                                                    nodeId, request.getStringToAppend(),
                                                    message.getSenderId()));

                                    if (!verifyTransfer(request)) {
                                        link.send(message.getSenderId(), new AppendResponse(Message.Type.APPEND_RESPONSE, nodeId, request.getRequestId(), ""));
                                        return;
                                    }
                                    CompletableFuture<AppendResponse> responseFuture = callConsensusInstance(request);

                                    try {
                                        AppendResponse response = responseFuture.get();
                                        link.send(message.getSenderId(), response);
                                    } catch (InterruptedException | ExecutionException e) {
                                        throw new RuntimeException(e);
                                    }

                                }
                                case BALANCE -> {
                                    AppendRequest request = (AppendRequest) message;

                                    LOGGER.log(Level.INFO,
                                            MessageFormat.format("{0} - Received BALANCE message: {1} - from {2}",
                                                    nodeId, request.getStringToAppend(),
                                                    message.getSenderId()));

                                    var balance = service.getLedger().getAccounts().get(request.getSenderId()).getBalance();
                                    try {
                                        link.send(message.getSenderId(), new AppendResponse(Message.Type.APPEND_RESPONSE, nodeId, request.getRequestId(), String.valueOf(balance)));
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    // verify balance request
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
