package pt.ulisboa.tecnico.hdsledger.communication;

import java.io.Serializable;

public class Message implements Serializable {

    // Sender identifier
    private String senderId;
    // Message identifier
    private int messageId;
    // Message type
    private Type type;
    // MAC of the message
    private byte[] mac = null;

    public enum Type {
        APPEND, PRE_PREPARE, PREPARE, COMMIT, ROUND_CHANGE, CATCHUP, ACK, IGNORE, APPEND_RESPONSE, TRANSFER, BALANCE;
    }

    public Message(String senderId, Type type) {
        this.senderId = senderId;
        this.type = type;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public byte[] getMac() {
        return mac;
    }

    public byte[] popMac() {
        byte[] mac = this.mac;
        this.mac = null;
        return mac;
    }

    public void setMac(byte[] mac) {
        this.mac = mac;
    }
}
