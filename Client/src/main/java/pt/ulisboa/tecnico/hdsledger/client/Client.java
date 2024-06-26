package pt.ulisboa.tecnico.hdsledger.client;

import pt.ulisboa.tecnico.hdsledger.client.services.ClientParser;
import pt.ulisboa.tecnico.hdsledger.client.services.ClientService;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfigBuilder;

public class Client {

    private static String clientsConfigPath = "src/main/resources/";
    private static String nodesConfigPath = "../Service/src/main/resources/";

    public static void main(String[] args) {
        try {
            String id = args[0];
            clientsConfigPath += args[1];
            nodesConfigPath += args[2];

            ProcessConfig[] clientConfigs = loadProcessConfigs(clientsConfigPath);
            ProcessConfig[] nodeConfigs = loadProcessConfigs(nodesConfigPath);

            // Get the client config
            ProcessConfig clientConfig = getClientConfigById(clientConfigs, id);

            // Adjust node configurations for correct port
            setNodePorts(nodeConfigs);

            ClientService clientService = new ClientService(clientConfig, nodeConfigs);
            clientService.listen();

            System.out.println("--- HDS Serenity Ledger Client ---");
            System.out.println("Client with id " + id);
            System.out.println("Connected on " + clientConfig.getHostname() + ":" + clientConfig.getPort());

            ClientParser.Parse(clientConfig.getId(), clientService);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Function to load process configurations from a file
    private static ProcessConfig[] loadProcessConfigs(String filePath) {
        return new ProcessConfigBuilder().fromFile(filePath);
    }

    // Function to get client config by ID
    private static ProcessConfig getClientConfigById(ProcessConfig[] clientConfigs, String clientId) {
        for (ProcessConfig config : clientConfigs) {
            if (config.getId().equals(clientId)) {
                return config;
            }
        }
        return null; // or throw an exception, depending on your error-handling strategy
    }

    // Function to set node ports
    private static void setNodePorts(ProcessConfig[] nodeConfigs) {
        for (ProcessConfig nodeConfig : nodeConfigs) {
            nodeConfig.setPort(nodeConfig.getClientPort());
        }
    }
}
