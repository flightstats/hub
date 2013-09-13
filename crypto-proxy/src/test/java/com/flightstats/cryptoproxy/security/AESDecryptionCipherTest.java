package com.flightstats.cryptoproxy.security;

import com.flightstats.cryptoproxy.app.config.GuiceContextListenerFactory;
import org.junit.Test;

import javax.crypto.*;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;

import static org.junit.Assert.assertEquals;

public class AESDecryptionCipherTest {

    @Test
    public void testCiphers() throws InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidParameterSpecException, IOException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException {
        // GIVEN
        String data = "Hello, World!";
        String key = "testKey";

        SecretKey encryptSecretKey = GuiceContextListenerFactory.CryptoProxyModule.buildSecretKey(key.getBytes());
        Cipher encryptAesCipher = GuiceContextListenerFactory.CryptoProxyModule.buildAESCipher();
        AESEncryptionCipher encryptionCipher = new AESEncryptionCipher(encryptSecretKey, encryptAesCipher);
        byte[] cipherData = encryptionCipher.encrypt(data.getBytes());

        SecretKey decryptSecretKey = GuiceContextListenerFactory.CryptoProxyModule.buildSecretKey(key.getBytes());
        Cipher decryptAesCipher = GuiceContextListenerFactory.CryptoProxyModule.buildAESCipher();
        AESDecryptionCipher decryptionCipher = new AESDecryptionCipher(decryptSecretKey, decryptAesCipher);

        // WHEN
        byte[] resultData = decryptionCipher.decrypt(cipherData);

        // THEN
        assertEquals(data, new String(resultData));
    }
}
