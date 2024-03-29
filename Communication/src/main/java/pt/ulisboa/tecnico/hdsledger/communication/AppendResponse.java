package pt.ulisboa.tecnico.hdsledger.communication;

public class AppendResponse extends Message {

    private int requestId;

    //private ArrayList<String> currentBlockchain = new ArrayList<String>();
    private String response;

    public AppendResponse(Type type, String senderId, int requestId, String response) {
        super(senderId, type);
        this.requestId = requestId;
        //this.currentBlockchain = currentBlockchain;
        this.response = response;
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    // public ArrayList<String> getCurrentBlockchain() {
    //     return currentBlockchain;
    // }

    // public void setCurrentBlockchain(ArrayList<String> currentBlockchain) {
    //     this.currentBlockchain = currentBlockchain;
    // }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

}
