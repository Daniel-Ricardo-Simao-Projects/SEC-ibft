package pt.ulisboa.tecnico.hdsledger.utilities;

public class ProcessConfig {
    public ProcessConfig() {}

    private boolean isLeader;

    private String hostname;

    private String id;

    private int port;

    private int clientPort;

    public boolean isLeader() {
        return isLeader;
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

}
