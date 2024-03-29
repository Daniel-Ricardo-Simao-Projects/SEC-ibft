package pt.ulisboa.tecnico.hdsledger.service.state;

import pt.ulisboa.tecnico.hdsledger.service.models.Transaction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Base64;

public class Block implements Serializable {

	// Transactions list in the block with fixed size
	private ArrayList<Transaction> transactions = new ArrayList<>(1);

	public Block() {
		// Empty constructor
	}

	public ArrayList<Transaction> getTransactions() {
		return transactions;
	}

	public void addTransaction(Transaction transaction) {
		transactions.add(transaction);
	}

	public void setTransactions(ArrayList<Transaction> transactions) {
		this.transactions = transactions;
	}

	@Override
	public String toString() {
		return "Block{" +
				"transactions=" + transactions.toString() +
				'}';
	}
}
