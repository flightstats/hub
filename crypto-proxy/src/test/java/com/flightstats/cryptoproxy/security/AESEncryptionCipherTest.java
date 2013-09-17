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

import static org.junit.Assert.assertNotEquals;

public class AESEncryptionCipherTest {

    @Test
    public void testEncryption() throws InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidParameterSpecException, IOException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException {
        // GIVEN
        String data = "Hello, World!";
        String keyAlgorithm = "AES";
        String cipherAlgorithm = "AES";
        SecretKey key = SecretKeyUtil.createKey("testKey", keyAlgorithm);

        SecretKey secretKey = GuiceContextListenerFactory.CryptoProxyModule.buildSecretKey(key.getEncoded(), keyAlgorithm);

        Cipher cipher = GuiceContextListenerFactory.CryptoProxyModule.buildAESCipher(cipherAlgorithm);
        AESEncryptionCipher encryptionCipher = new AESEncryptionCipher(secretKey, cipher);

        // WHEN
        byte[] cipherData = encryptionCipher.encrypt(data.getBytes());

        // THEN
        assertNotEquals(data, new String(cipherData));
    }

}
