package pt.ulisboa.tecnico.hdsledger.service.models;

import java.security.PublicKey;
import java.util.Base64;

public class Transaction {

    private String sourcePubKey;

    private String sourceClientId;

    private String destPubKey;

    private String destClientId;

    private int amount;

    private int fee;

    private byte[] signature;

    public Transaction(PublicKey sourcePubKey, String sourceClientId, PublicKey destPubKey, String destClientId,
                       int amount, byte[] signature) {
        this.sourcePubKey = Base64.getEncoder().encodeToString(sourcePubKey.getEncoded());
        this.sourceClientId = sourceClientId;
        this.destPubKey = Base64.getEncoder().encodeToString(destPubKey.getEncoded());
        this.destClientId = destClientId;
        this.amount = amount;
        this.fee = amount / 10; // 10% fee
        this.signature = signature;
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

    public int getFee() { return fee; }

    public void setFee(int fee) { this.fee = fee; }

    @Override
    public String toString() {
        return "Transaction{" +
                // "sourcePubKey='" + sourcePubKey + '\'' +
                ", sourceClientId='" + sourceClientId + '\'' +
                // ", destPubKey='" + destPubKey + '\'' +
                ", destClientId='" + destClientId + '\'' +
                ", amount=" + amount +
                ", fee=" + fee +
                ", signature=" + "byte[" + signature.length + "]" +
                '}';
    }
}
