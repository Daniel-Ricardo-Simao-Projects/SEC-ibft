package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

public class RoundChangeMessage {
    // Value
    private String preparedValue;

    private int preparedRound;

    public RoundChangeMessage(String preparedValue, int preparedRound) {
        this.preparedValue = preparedValue;
        this.preparedRound = preparedRound;
    }

    public String getPreparedValue() {
        return preparedValue;
    }

    public int getPreparedRound() { return preparedRound; }

    public String toJson() {
        return new Gson().toJson(this);
    }
}
