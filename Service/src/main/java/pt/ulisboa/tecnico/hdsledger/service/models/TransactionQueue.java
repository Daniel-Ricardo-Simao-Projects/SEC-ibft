package pt.ulisboa.tecnico.hdsledger.service.models;

import java.util.ArrayList;

public class TransactionQueue {

    private final int blockSize = 5;

    private final int maxSize = 10;

    private ArrayList<Transaction> transactions = new ArrayList<>(maxSize);

    public TransactionQueue() {
        // Empty constructor
    }

    public int getBlockSize() {
        return blockSize;
    }

    public ArrayList<Transaction> getTransactions() {
        return transactions;
    }

    public void addTransaction(Transaction transaction) {
        synchronized (transactions) {
            transactions.add(transaction);
        }
    }

    public void setTransactions(ArrayList<Transaction> transactions) {
        this.transactions = transactions;
    }

    public boolean haveNecessaryTransactions() {
        return transactions.size() == blockSize;
    }

    public boolean isFull() {
        return transactions.size() == maxSize;
    }

    public void removeTransaction(Transaction transaction) {
        synchronized (transactions) {
            transactions.remove(transaction);
        }
    }

    public void clear() {
        transactions.clear();
    }

    @Override
    public String toString() {
        return "TransactionQueue{" +
                "transactions=" + transactions.toString() +
                '}';
    }

}
