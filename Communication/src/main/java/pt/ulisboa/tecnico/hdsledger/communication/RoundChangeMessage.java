package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

import java.util.List;

public class RoundChangeMessage {
    // Value
    private String preparedValue;

    private int preparedRound;

    // List of piggybacked prepare messages
    private List<ConsensusMessage> prepareMessages;

    public RoundChangeMessage() {
    }

    public RoundChangeMessage(String preparedValue, int preparedRound) {
        this.preparedValue = preparedValue;
        this.preparedRound = preparedRound;
    }

    public String getPreparedValue() {
        return preparedValue;
    }

    public void setPreparedValue(String preparedValue) {
        this.preparedValue = preparedValue;
    }

    public int getPreparedRound() { return preparedRound; }

    public void setPreparedRound(int preparedRound) {
        this.preparedRound = preparedRound;
    }

    public List<ConsensusMessage> getPrepareMessages() {
        return prepareMessages;
    }

    public void setPrepareMessages(List<ConsensusMessage> prepareMessages) {
        this.prepareMessages = prepareMessages;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}
