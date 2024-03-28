package pt.ulisboa.tecnico.hdsledger.communication;

import java.security.PublicKey;
import java.util.Base64;

public class TransferRequest {

    private String sourcePubKey;

    private String destPubKey;

    private String destClientId;

    private int amount;

    public TransferRequest(PublicKey sourcePubKey, PublicKey destPubKey, String destClientId, int amount) {
        this.sourcePubKey = Base64.getEncoder().encodeToString(sourcePubKey.getEncoded());
        this.destPubKey = Base64.getEncoder().encodeToString(destPubKey.getEncoded());
        this.destClientId = destClientId;
        this.amount = amount;
    }

    public String getSourcePubKey() {
        return sourcePubKey;
    }

    public String getDestPubKey() {
        return destPubKey;
    }

    public int getAmount() {
        return amount;
    }

    public String getDestClientId() {
        return destClientId;
    }

}