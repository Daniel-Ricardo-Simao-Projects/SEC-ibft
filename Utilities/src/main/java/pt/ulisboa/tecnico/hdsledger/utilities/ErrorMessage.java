package pt.ulisboa.tecnico.hdsledger.utilities;

public enum ErrorMessage {
    ConfigFileNotFound("The configuration file is not available at the path supplied"),
    ConfigFileFormat("The configuration file has wrong syntax"),
    NoSuchNode("Can't send a message to a non existing node"),
    SocketSendingError("Error while sending message"),
    CannotOpenSocket("Error while opening socket"),

    // transfer errors
    SourceAccountNotFound("Source account not found"),
    DestinationAccountNotFound("Destination account not found"),
    InvalidSignature("Invalid signature"),
    SameSourceAndDestination("Source and destination accounts are the same, not allowed"),
    InvalidAmount("Invalid amount"),
    PublicKeyMismatch("The public key does not match the sender's public key"),
    InsufficientFunds("Insufficient funds");

    private final String message;

    ErrorMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
