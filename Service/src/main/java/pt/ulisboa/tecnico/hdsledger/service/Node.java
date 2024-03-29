package pt.ulisboa.tecnico.hdsledger.service;

import pt.ulisboa.tecnico.hdsledger.communication.AppendRequest;
import pt.ulisboa.tecnico.hdsledger.communication.ConsensusMessage;
import pt.ulisboa.tecnico.hdsledger.communication.Link;
import pt.ulisboa.tecnico.hdsledger.service.models.TransactionQueue;
import pt.ulisboa.tecnico.hdsledger.service.services.NodeService;
import pt.ulisboa.tecnico.hdsledger.service.services.SerenityLedgerService;
import pt.ulisboa.tecnico.hdsledger.utilities.CustomLogger;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfigBuilder;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig.ByzantineType;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.logging.Level;

public class Node {

    private static final CustomLogger LOGGER = new CustomLogger(Node.class.getName());
    // Hardcoded path to files
    private static String nodesConfigPath = "src/main/resources/";

    private static String clientsConfigPath = "../Client/src/main/resources/";

    public static void main(String[] args) {

        try {
            // Command line arguments
            String id = args[0];
            nodesConfigPath += args[1];
            clientsConfigPath += args[2];

            // Create configuration instances
            ProcessConfig[] nodeConfigs = new ProcessConfigBuilder().fromFile(nodesConfigPath);
            ProcessConfig leaderConfig = Arrays.stream(nodeConfigs).filter(ProcessConfig::isLeader).findAny().get();
            ProcessConfig nodeConfig = Arrays.stream(nodeConfigs).filter(c -> c.getId().equals(id)).findAny().get();
            ProcessConfig[] clients = new ProcessConfigBuilder().fromFile(clientsConfigPath);

            LOGGER.log(Level.INFO, MessageFormat.format("{0} - Running at {1}:{2,number,#}; is leader: {3}; latency: {4,number,#}ms\n" + 
                                                        "BYZANTINE TYPE: {5}",
                    nodeConfig.getId(), nodeConfig.getHostname(), nodeConfig.getPort(),
                    nodeConfig.isLeader(), nodeConfig.getMessageDelay(), nodeConfig.getByzantineType()));
            
            // byzantine test
            if (nodeConfig.getByzantineType() == ByzantineType.FAKE_LEADER) {
                nodeConfig.setLeader(true);
                // Set the leader to itself inside node configs
                for (var node : nodeConfigs) {
                    if (node.getId().equals(nodeConfig.getId())) {
                        node.setLeader(true);
                    } else if (node.getId().equals(leaderConfig.getId())) {
                        node.setLeader(false);
                    }
                }
                leaderConfig = nodeConfig;
            }

            // Abstraction to send and receive messages
            Link linkToNodes = new Link(nodeConfig, nodeConfig.getPort(), nodeConfigs,
                    ConsensusMessage.class);

            Link linkToClients = new Link(nodeConfig, nodeConfig.getClientPort(), clients, AppendRequest.class);

            TransactionQueue transactionQueue = new TransactionQueue();

            // Services that implement listen from UDPService
            NodeService nodeService = new NodeService(linkToNodes, nodeConfig, clients, leaderConfig,
                    nodeConfigs, transactionQueue);

            SerenityLedgerService serenityLedgerService = new SerenityLedgerService(id, clients, nodeService,
                    linkToClients, transactionQueue);

            nodeService.listen();
            serenityLedgerService.listen();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
