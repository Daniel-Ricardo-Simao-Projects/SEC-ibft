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

    private String valueSignature;

    private byte[] signature;

    public Transaction(PublicKey sourcePubKey, String sourceClientId, PublicKey destPubKey, String destClientId,
                       int amount, String valueSignature, byte[] signature) {
        this.sourcePubKey = Base64.getEncoder().encodeToString(sourcePubKey.getEncoded());
        this.sourceClientId = sourceClientId;
        this.destPubKey = Base64.getEncoder().encodeToString(destPubKey.getEncoded());
        this.destClientId = destClientId;
        this.amount = amount;
        this.fee = amount / 20; // 5% fee
        this.valueSignature = valueSignature;
        this.signature = signature;
    }

    public String getSourcePubKey() {
        return sourcePubKey;
    }

    public String getSourceClientId() {
        return sourceClientId;
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

    public byte[] getSignature() {
        return signature;
    }

    public String getValueSignature() {
        return valueSignature;
    }

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

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Transaction)) {
            return false;
        }
        Transaction transaction = (Transaction) obj;
        return transaction.sourceClientId.equals(sourceClientId) && transaction.destClientId.equals(destClientId) && transaction.amount == amount &&
                transaction.fee == fee && transaction.valueSignature.equals(valueSignature);
    }
}
