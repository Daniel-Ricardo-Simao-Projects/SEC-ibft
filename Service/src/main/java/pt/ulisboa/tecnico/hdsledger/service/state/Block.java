package pt.ulisboa.tecnico.hdsledger.service.state;

import pt.ulisboa.tecnico.hdsledger.service.models.Transaction;

import java.io.Serializable;
import java.util.Base64;

public class Block implements Serializable {
    private String value;

    private String clientID;

    private String signature;

	private Transaction transaction;

    public Block(String value, String clientID, byte[] signature, Transaction transaction) {
        this.value = value;
        this.clientID = clientID;
        this.signature = Base64.getEncoder().encodeToString(signature);
		this.transaction = transaction;
    }

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getClientID() {
		return clientID;
	}

	public void setClientID(String clientID) {
		this.clientID = clientID;
	}

	public byte[] getSignature() {
		return Base64.getDecoder().decode(signature);
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}

	public Transaction getTransaction() {
		return transaction;
	}

	public void setTransaction(Transaction transaction) {
		this.transaction = transaction;
	}

	@Override
	public String toString() {
		return "Block{" +
				// "value='" + value + '\'' +
				// ", clientID='" + clientID + '\'' +
				// ", signature='" + signature + '\'' +
				", transaction=" + transaction.toString() +
				'}';
	}
}
