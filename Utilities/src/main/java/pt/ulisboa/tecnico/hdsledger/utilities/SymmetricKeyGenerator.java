package pt.ulisboa.tecnico.hdsledger.utilities;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.Key;

import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;

/**
 * This class is inspired from the Network and Computer Security (SIRS) 2023/2024 course classes,
 * accessible at: https://github.com/tecnico-sec/Java-Crypto-Details.
 * Its sole purpose is to generate AES keys for each execution of the system.
 */
public class SymmetricKeyGenerator {

    private static final String SYM_ALGO = "AES";

    private static final int SYM_KEY_SIZE = 128;

    public static void main(String[] args) throws Exception {

        if (args.length < 1) {
            System.err.println("args: keyPath");
            return;
        }
        final String keyPath = args[0];

        write(keyPath);

        System.out.println("Done.");
    }

    public static void write(String keyPath) throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance(SYM_ALGO);
        keyGen.init(SYM_KEY_SIZE);
        Key key = keyGen.generateKey();
        byte[] encoded = key.getEncoded();

        FileOutputStream fos = new FileOutputStream(keyPath);
        fos.write(encoded);
        fos.close();
    }

    public static Key read(String keyPath) throws Exception {
        FileInputStream fis = new FileInputStream(keyPath);
        byte[] encoded = new byte[fis.available()];
        fis.read(encoded);
        fis.close();

        SecretKeySpec keySpec = new SecretKeySpec(encoded, SYM_ALGO);
        return keySpec;
    }
}
