package pt.ulisboa.tecnico.hdsledger.client;

import pt.ulisboa.tecnico.hdsledger.client.services.ClientService;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfig;
import pt.ulisboa.tecnico.hdsledger.utilities.ProcessConfigBuilder;

import java.util.Scanner;

public class Client {

    private static String clientsConfigPath = "src/main/resources/client_config.json";
    private static String nodesConfigPath = "../Service/src/main/resources/";

    public static void main(String[] args) {
        try {
            String id = args[0];
            nodesConfigPath += args[1];

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

            Scanner scanner = new Scanner(System.in);

            while (true) {
                System.out.print("Enter command (append <string> or exit): ");
                String input = scanner.nextLine();

                // Splitting the input to get the command and argument (if any)
                String[] commandAndArgument = input.split(" ", 2);
                String command = commandAndArgument[0].toLowerCase();

                switch (command) {
                    case "append":
                        if (commandAndArgument.length == 2) {
                            String appendString = commandAndArgument[1];
                            String response = clientService.requestAppend(appendString);
                            System.out.println("String appended: " + response);
                        } else {
                            System.out.println("Invalid append command. Usage: append <string>");
                        }
                        break;
                    case "exit":
                        System.out.println("Exiting the loop.");
                        scanner.close();
                        System.exit(0);
                    default:
                        System.out.println("Invalid command. Available commands: append, exit");
                }
            }

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