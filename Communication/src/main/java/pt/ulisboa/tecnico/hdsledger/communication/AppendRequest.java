package pt.ulisboa.tecnico.hdsledger.communication;

public class AppendRequest extends Message {

    private int requestId;

    private String stringToAppend;

    public AppendRequest(Type type, String senderId, int requestId, String stringToAppend) {
        super(senderId, type);
        this.stringToAppend = stringToAppend;
        this.requestId = requestId;
    }

    public String getStringToAppend() {
        return stringToAppend;
    }

    public void setStringToAppend(String stringToAppend) {
        this.stringToAppend = stringToAppend;
    }
}
