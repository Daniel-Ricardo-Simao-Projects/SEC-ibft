package pt.ulisboa.tecnico.hdsledger.communication;

import java.security.PublicKey;
import java.util.Base64;

public class BalanceRequest {
    
    private String pubKey;
    
    public BalanceRequest(PublicKey pubKey) {
        this.pubKey = Base64.getEncoder().encodeToString(pubKey.getEncoded());
    }
    
    public String getPubKey() {
        return pubKey;
    }

    public void setPubKey(String pubKey) {
        this.pubKey = pubKey;
    }

}
