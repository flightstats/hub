package com.flightstats.cryptoproxy.security;

import com.flightstats.cryptoproxy.app.config.GuiceContextListenerFactory;
import org.junit.Test;

import javax.crypto.*;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;

import static org.junit.Assert.assertEquals;

public class AESDecryptionCipherTest {

    @Test
    public void testDecryption() throws InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidParameterSpecException, IOException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException {
        // GIVEN
        String data = "Hello, World!";
        String keyAlgorithm = "AES";
        String cipherAlgorithm = "AES";
        SecretKey key = SecretKeyUtil.createKey("testKey", keyAlgorithm);

        SecretKey encryptSecretKey = GuiceContextListenerFactory.CryptoProxyModule.buildSecretKey(key.getEncoded(), keyAlgorithm);
        Cipher encryptAesCipher = GuiceContextListenerFactory.CryptoProxyModule.buildAESCipher(cipherAlgorithm);
        AESEncryptionCipher encryptionCipher = new AESEncryptionCipher(encryptSecretKey, encryptAesCipher);
        byte[] cipherData = encryptionCipher.encrypt(data.getBytes());

        SecretKey decryptSecretKey = GuiceContextListenerFactory.CryptoProxyModule.buildSecretKey(key.getEncoded(), keyAlgorithm);
        Cipher decryptAesCipher = GuiceContextListenerFactory.CryptoProxyModule.buildAESCipher(cipherAlgorithm);
        AESDecryptionCipher decryptionCipher = new AESDecryptionCipher(decryptSecretKey, decryptAesCipher);

        // WHEN
        byte[] resultData = decryptionCipher.decrypt(cipherData);

        // THEN
        assertEquals(data, new String(resultData));
    }
}
