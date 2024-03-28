package pt.ulisboa.tecnico.hdsledger.utilities;

import javax.crypto.Mac;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

public class Authenticate {

    private static final String MAC_ALGO = "HmacSHA256";

    public static byte[] signData(String data, String privateKeyPath) {
        try {
            PrivateKey privateKey = readPrivateKey(privateKeyPath);
            return createDigitalSignature(data, privateKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean verifySignature(String data, byte[] signatureBytes, String publicKeyPath) {
        try {
            PublicKey publicKey = readPublicKey(publicKeyPath);
            return verifyDigitalSignature(data, signatureBytes, publicKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static byte[] createDigitalSignature(String data, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(data.getBytes());
        byte[] signatureBytes = signature.sign();
        return signatureBytes;
    }

    public static boolean verifyDigitalSignature(String data, byte[] signatureBytes, PublicKey publicKey) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(data.getBytes());
            return signature.verify(signatureBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        
    }

    public static byte[] createMAC(byte[] bytes, Key key) throws Exception {
        Mac mac = Mac.getInstance(MAC_ALGO);
        mac.init(key);
        byte[] macBytes = mac.doFinal(bytes);
        return macBytes;
    }

    public static boolean verifyMAC(byte[] receivedMacBytes, byte[] bytes, Key key) throws Exception {
        Mac mac = Mac.getInstance(MAC_ALGO);
        mac.init(key);
        byte[] recomputedMacBytes = mac.doFinal(bytes);
        return Arrays.equals(receivedMacBytes, recomputedMacBytes);
    }

    public static PrivateKey readPrivateKey(String privateKeyPath) throws Exception {
        byte[] privEncoded = Files.readAllBytes(Paths.get(privateKeyPath));
        PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(privEncoded);
        KeyFactory keyFacPriv = KeyFactory.getInstance("RSA");
        return keyFacPriv.generatePrivate(privSpec);
    }

    public static PublicKey readPublicKey(String publicKeyPath) {
        try {
            byte[] pubEncoded = Files.readAllBytes(Paths.get(publicKeyPath));
            X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(pubEncoded);
            KeyFactory keyFacPub = KeyFactory.getInstance("RSA");
            return keyFacPub.generatePublic(pubSpec);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        
    }

    public static PublicKey getPublicKeyFromString(String publicKeyString) {
        try {
            byte[] pubEncoded = Base64.getDecoder().decode(publicKeyString);
            X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(pubEncoded);
            KeyFactory keyFacPub = KeyFactory.getInstance("RSA");
            return keyFacPub.generatePublic(pubSpec);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
