package pt.ulisboa.tecnico.hdsledger.communication;

public class AppendRequest extends Message {

    private int requestId;

    private String stringToAppend;

    private byte[] signature;

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

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }
}
