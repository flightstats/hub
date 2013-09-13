package com.flightstats.cryptoproxy.security;

import com.flightstats.cryptoproxy.app.config.GuiceContextListenerFactory;
import org.junit.Before;
import org.junit.Test;

import javax.crypto.*;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;

import static org.junit.Assert.assertNotEquals;

public class AESEncryptionCipherTest {

    @Test
    public void testCiphers() throws InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidParameterSpecException, UnsupportedEncodingException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException {
        // GIVEN
        String data = "Hello, World!";

        SecretKey secretKey = GuiceContextListenerFactory.CryptoProxyModule.buildSecretKey();
        Cipher cipher = GuiceContextListenerFactory.CryptoProxyModule.buildAESCipher();
        AESEncryptionCipher encryptionCipher = new AESEncryptionCipher(secretKey, cipher);

        // WHEN
        byte[] cipherData = encryptionCipher.encrypt(data.getBytes());

        // THEN
        assertNotEquals(data, new String(cipherData));
    }
}
