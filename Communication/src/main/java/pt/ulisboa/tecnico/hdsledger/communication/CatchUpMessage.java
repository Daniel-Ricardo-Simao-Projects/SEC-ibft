package pt.ulisboa.tecnico.hdsledger.communication;

import com.google.gson.Gson;

import java.util.List;

public class CatchUpMessage {

    private List<ConsensusMessage> commitMessages;

    public CatchUpMessage(List<ConsensusMessage> commitMessages) {
        this.commitMessages = commitMessages;
    }

    public List<ConsensusMessage> getCommitMessages() {
        return commitMessages;
    }

    public void setCommitMessages(List<ConsensusMessage> commitMessages) {
        this.commitMessages = commitMessages;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}
