package pt.ulisboa.tecnico.hdsledger.service.state;

import java.io.Serializable;
import java.util.Base64;

public class Block implements Serializable {
    private String value;

    private String clientID;

    private String signature;

    public Block(String value, String clientID, byte[] signature) {
        this.value = value;
        this.clientID = clientID;
        this.signature = Base64.getEncoder().encodeToString(signature);
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

}
