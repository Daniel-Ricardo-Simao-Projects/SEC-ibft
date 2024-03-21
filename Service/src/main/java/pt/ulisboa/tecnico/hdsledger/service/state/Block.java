package pt.ulisboa.tecnico.hdsledger.service.state;

import java.io.Serializable;

public class Block implements Serializable {
    private String value;

    private String clientID;

    private byte[] signature;

    public Block(String value, String clientID, byte[] signature) {
        this.value = value;
        this.clientID = clientID;
        this.signature = signature;
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
		return signature;
	}

	public void setSignature(byte[] signature) {
		this.signature = signature;
	}

}
