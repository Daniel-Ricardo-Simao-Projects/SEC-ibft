package pt.ulisboa.tecnico.hdsledger.service.models;

import java.security.PublicKey;

public class Account {
    
    private String clientId;

    private int balance;

    private PublicKey publicKey;

    private final int initialBalance = 100;

    public Account(String clientId, PublicKey publicKey) {
        this.clientId = clientId;
        this.balance = initialBalance;
        this.publicKey = publicKey;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public int getBalance() {
        return balance;
    }

    public void addBalance(int amount) {
        this.balance += amount;
    }

    public void subtractBalance(int amount) {
        this.balance -= amount;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

}
