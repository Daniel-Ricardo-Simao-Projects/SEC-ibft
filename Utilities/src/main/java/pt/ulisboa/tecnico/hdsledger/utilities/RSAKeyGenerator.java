package pt.ulisboa.tecnico.hdsledger.utilities;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * This class is sourced from the Network and Computer Security (SIRS) 2023/2024 course,
 * accessible at: https://github.com/tecnico-sec/Java-Crypto-Details.
 * Its sole purpose is to generate RSA key pairs for each execution of the system.
 */
public class RSAKeyGenerator {

    public static void main(String[] args) throws Exception {

        // check args
        if (args.length < 2) {
            System.err.println("Usage: rsa-key-gen <priv-key-file> <pub-key-file>");
            return;
        }

        final String privkeyPath = args[0];
        final String pubkeyPath = args[1];

        write(pubkeyPath, privkeyPath);

        // System.out.println("Key pair generated successfully.");
    }

    public static void write(String publicKeyPath, String privateKeyPath) throws GeneralSecurityException, IOException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(1024);
        KeyPair keys = keyGen.generateKeyPair();

        PrivateKey privKey = keys.getPrivate();
        byte[] privKeyEncoded = privKey.getEncoded();
        PublicKey pubKey = keys.getPublic();
        byte[] pubKeyEncoded = pubKey.getEncoded();

        FileOutputStream privFos = new FileOutputStream(privateKeyPath);
        privFos.write(privKeyEncoded);
        privFos.close();
        FileOutputStream pubFos = new FileOutputStream(publicKeyPath);
        pubFos.write(pubKeyEncoded);
        pubFos.close();
    }

    public static KeyPair read(String publicKeyPath, String privateKeyPath)
            throws GeneralSecurityException, IOException {
        FileInputStream pubFis = new FileInputStream(publicKeyPath);
        byte[] pubEncoded = new byte[pubFis.available()];
        pubFis.read(pubEncoded);
        pubFis.close();

        X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(pubEncoded);
        KeyFactory keyFacPub = KeyFactory.getInstance("RSA");
        PublicKey pub = keyFacPub.generatePublic(pubSpec);

        FileInputStream privFis = new FileInputStream(privateKeyPath);
        byte[] privEncoded = new byte[privFis.available()];
        privFis.read(privEncoded);
        privFis.close();

        PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(privEncoded);
        KeyFactory keyFacPriv = KeyFactory.getInstance("RSA");
        PrivateKey priv = keyFacPriv.generatePrivate(privSpec);

        KeyPair keys = new KeyPair(pub, priv);
        return keys;
    }
}
