package pt.ulisboa.tecnico.hdsledger.client.services;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Scanner;

import pt.ulisboa.tecnico.hdsledger.communication.Message.Type;
import pt.ulisboa.tecnico.hdsledger.utilities.Authenticate;

public class ClientParser {

    public static void Parse(String clientId, ClientService clientService) {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("--------- MENU ----------");
            //System.out.println("1 - append <string>");
            System.out.println("1 - transfer <dest> <amount>");
            System.out.println("2 - balance");
            System.out.println("3 - exit");
            System.out.print("Enter a command: ");
            String input = scanner.nextLine();

            String[] commandAndArgument = input.split(" ", 3);
            String command = commandAndArgument[0].toLowerCase();

            switch (command) {
                // case "append":
                //     if (commandAndArgument.length == 2) {
                //         String appendString = commandAndArgument[1];
                //         String response = clientService.requestAppend(appendString, Type.APPEND);
                //         System.out.println("String appended: " + String.join("", response));
                //     } else {
                //         System.out.println("Invalid append command. Usage: append <string>");
                //     }
                //     break;
                case "transfer":
                    if (commandAndArgument.length == 3) {
                        String destination = commandAndArgument[1];
                        int amount = Integer.parseInt(commandAndArgument[2]);
                        try {
                            PublicKey source = Authenticate
                                    .readPublicKey("../Utilities/keys/" + clientId + "Pub.key");
                            PublicKey dest = Authenticate
                                    .readPublicKey("../Utilities/keys/" + destination + "Pub.key");
                            String returnMessage = clientService.transfer(source, dest, destination, amount);
                            System.out.println(returnMessage);
                        } catch (Exception e) {
                            System.out.println("Error reading keys: " + e);
                        }
                    } else {
                        System.out.println("Invalid transfer command. Usage: transfer <dest> <amount>");
                    }
                    break;
                case "balance":
                    try {
                        PublicKey source = Authenticate.readPublicKey("../Utilities/keys/" + clientId + "Pub.key");
                        int balance = clientService.checkBalance(source);
                        System.out.println("Your balance: " + balance);
                    } catch (Exception e) {
                        System.out.println("Error reading keys");
                    }
                    break;
                case "exit":
                    System.out.println("Exiting the loop.");
                    scanner.close();
                    System.exit(0);
                default:
                    System.out.println("Invalid command. Available commands: append, transfer, balance, exit");
            }
        }
    }

}
