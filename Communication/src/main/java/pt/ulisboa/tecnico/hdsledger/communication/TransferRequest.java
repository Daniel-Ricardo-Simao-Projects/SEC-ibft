package pt.ulisboa.tecnico.hdsledger.communication;

import java.security.PublicKey;
import java.util.Base64;

public class TransferRequest {

    private String sourcePubKey;

    private String destPubKey;

    private int amount;

    public TransferRequest(PublicKey sourcePubKey, PublicKey destPubKey, int amount) {
        this.sourcePubKey = Base64.getEncoder().encodeToString(sourcePubKey.getEncoded());
        this.destPubKey = Base64.getEncoder().encodeToString(destPubKey.getEncoded());
        this.amount = amount;
    }

    public String getSourcePubKey() {
        return sourcePubKey;
    }

    public void setSourcePubKey(String sourcePubKey) {
        this.sourcePubKey = sourcePubKey;
    }

    public String getDestPubKey() {
        return destPubKey;
    }

    public void setDestPubKey(String destPubKey) {
        this.destPubKey = destPubKey;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

}
