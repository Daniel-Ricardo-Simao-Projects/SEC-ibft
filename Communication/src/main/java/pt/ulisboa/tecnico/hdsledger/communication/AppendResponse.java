package pt.ulisboa.tecnico.hdsledger.communication;

import java.util.ArrayList;

public class AppendResponse extends Message {

    private int requestId;

    private ArrayList<String> currentBlockchain = new ArrayList<String>();

    public AppendResponse(Type type, String senderId, int requestId, ArrayList<String> currentBlockchain) {
        super(senderId, type);
        this.requestId = requestId;
        this.currentBlockchain = currentBlockchain;
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public ArrayList<String> getCurrentBlockchain() {
        return currentBlockchain;
    }

    public void setCurrentBlockchain(ArrayList<String> currentBlockchain) {
        this.currentBlockchain = currentBlockchain;
    }

}
