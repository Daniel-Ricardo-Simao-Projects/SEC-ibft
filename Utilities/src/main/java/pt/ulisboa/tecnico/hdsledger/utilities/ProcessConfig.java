package pt.ulisboa.tecnico.hdsledger.utilities;

public class ProcessConfig {
    public ProcessConfig() {
    }

    private boolean isLeader;

    private String hostname;

    private String id;

    private int port;

    private int clientPort;

    private ByzantineType byzantineType;

    private int messageDelay;

    public boolean isLeader() {
        return isLeader;
    }

    public void setLeader(boolean leader) {
        isLeader = leader;
    }
    
    public void setPort(int port) {
        this.port = port;
    }
    
    public int getPort() {
        return port;
    }
    
    public String getId() {
        return id;
    }
    
    public String getHostname() {
        return hostname;
    }
    
    public int getClientPort() {
        return clientPort;
    }
    
    public enum ByzantineType {
        NONE,
        SILENT,                 // drop messages
        DELAY,                  // delay messages                  
        FAKE_LEADER,            // pretend to be the leader
        EQUIVOCATION,           // send different messages to different nodes
        SILENT_COMMIT,          // do not broadcast commit message
        FAKE_SIGNATURE,         // use a not recongized signature
        BYZANTINE_BROADCAST,    // broadcast different messages
        FAKE_BALANCE,           // send a fake balance
        DOUBLE_SPEND,           // double spend
        OVER_ACCESS,            // access another client account
        OVER_SPEND,             // spend more than the balance
    }

    public ByzantineType getByzantineType() {
        return byzantineType;
    }
    
    public void setByzantineType(ByzantineType byzantineType) {
        this.byzantineType = byzantineType;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setClientPort(int clientPort) {
        this.clientPort = clientPort;
    }

    public int getMessageDelay() {
        return messageDelay;
    }

    public void setMessageDelay(int messageDelay) {
        this.messageDelay = messageDelay;
    }
    
}
