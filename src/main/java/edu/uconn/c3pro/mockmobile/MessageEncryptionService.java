package edu.uconn.c3pro.mockmobile;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.PublicKey;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bch.c3pro.server.config.AppConfig;
import org.bch.c3pro.server.exception.C3PROException;

/**
 * Implements the access to a Amazon SQS queue
 * @author CHIP-IHL
 */
public class MessageEncryptionService {

    Log log = LogFactory.getLog(MessageEncryptionService.class);

    /**
     * Sends an encrypted message to the SQS (See documentation)
     * @param resource  The resource
     * @param publicKey The public key used to encrypt the symetric key
     * @param UUIDKey   the if of the key
     * @param version   The version
     * @throws C3PROException In case access to SQS is not possible
     */
    public EncryptedMessage encryptMessage(String resource, PublicKey publicKey, String UUIDKey, String version)
            throws C3PROException {

        // Generate the symetric private key to encrypt the message
        SecretKey symetricKey = generateSecretKey();

        byte []encKeyToSend = null;
        byte []encResource = null;
        Cipher cipher;
        try {
            // We encrypt the symetric key using the public available key
            int size = Integer.parseInt(AppConfig.getProp(AppConfig.SECURITY_PRIVATEKEY_SIZE, AppConfig.SECURITY_PRIVATEKEY_SIZE_DEFAULT));
            //SecureRandom random = new SecureRandom();
            //IvParameterSpec iv = new IvParameterSpec(random.generateSeed(16));
            encKeyToSend = encryptRSA(publicKey, symetricKey.getEncoded());

            // We encrypt the message
            cipher = Cipher.getInstance(AppConfig.getProp(AppConfig.SECURITY_PRIVATEKEY_ALG, AppConfig.SECURITY_PRIVATEKEY_ALG_DEFAULT));

            //cipher.init(Cipher.ENCRYPT_MODE, symmetricKey, iv);
            cipher.init(Cipher.ENCRYPT_MODE, symetricKey, new IvParameterSpec(new byte[size]));
            encResource = cipher.doFinal(resource.getBytes(AppConfig.UTF));
        } catch (UnsupportedEncodingException e) {
            throw new C3PROException(e.getMessage(), e);
        } catch (InvalidKeyException e) {
                throw new C3PROException(e.getMessage(), e);
        } catch (Exception e) {
            throw new C3PROException(e.getMessage(), e);
        }

        EncryptedMessage message = new EncryptedMessage(Base64.encodeBase64String(encResource), Base64.encodeBase64String(encKeyToSend), UUIDKey, version);
        return message;
    }

    /**
     * Generates a secret symmetric key
     * @return The generated key
     * @throws C3PROException In case an error occurs during the generation
     */
    public SecretKey generateSecretKey() throws C3PROException {
        SecretKey key = null;

        try {
            String baseAlg = AppConfig.getProp(AppConfig.SECURITY_PRIVATEKEY_BASEALG,AppConfig.SECURITY_PRIVATEKEY_BASEALG_DEFAULT);
			KeyGenerator generator = KeyGenerator.getInstance(baseAlg);
            int size = Integer.parseInt(AppConfig.getProp(AppConfig.SECURITY_PRIVATEKEY_SIZE,AppConfig.SECURITY_PRIVATEKEY_SIZE_DEFAULT));
            SecureRandom random = new SecureRandom();
            generator.init(size*8, random);
            key = generator.generateKey();
        } catch (Exception e) {
        		e.printStackTrace();
            throw new C3PROException(e.getMessage(), e);
        }
        return key;
    }

    /**
     * Encrypts the given byte array using the provided public key using RSA
     * @param key   The public key
     * @param text  The message to encrypt
     * @return      The encrypted message
     * @throws C3PROException In case an error occurs during the encryption
     */
    public byte[] encryptRSA(PublicKey key, byte[] text) throws C3PROException {
        Cipher cipher = null;
        byte [] out = null;
        try {
            cipher = Cipher.getInstance(AppConfig.getProp(AppConfig.SECURITY_PUBLICKEY_ALG, AppConfig.SECURITY_PUBLICKEY_ALG_DEFAULT));
            cipher.init(Cipher.ENCRYPT_MODE, key);
            out = cipher.doFinal(text);
        } catch (Exception e) {
            throw new C3PROException(e.getMessage(), e);
        }
        return out;
    }

}
