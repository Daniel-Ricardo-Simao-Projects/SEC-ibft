package pt.ulisboa.tecnico.hdsledger.service.state;

import pt.ulisboa.tecnico.hdsledger.service.models.Transaction;

import java.io.Serializable;
import java.util.Base64;

public class Block implements Serializable {

	private Transaction transaction;

    public Block(Transaction transaction) {
		this.transaction = transaction;
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
