package com.flightstats.cryptoproxy.security;

import com.flightstats.cryptoproxy.app.config.GuiceContextListenerFactory;
import org.junit.Test;

import javax.crypto.*;
import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class AESCipherTest {
    @Test
    public void testCiphers() throws InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidParameterSpecException, UnsupportedEncodingException, BadPaddingException, IllegalBlockSizeException, InvalidAlgorithmParameterException {
        String data = "Hello, World!";

        byte[] cipherdata = buildCipher().encrypt(data.getBytes());

        assertNotEquals(data, new String(cipherdata));

        String plaintext = new String(buildCipher().decrypt(cipherdata));

        assertEquals(data, plaintext);
    }

    private AESCipher buildCipher() throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException {
        GuiceContextListenerFactory.CryptoProxyModule module = new GuiceContextListenerFactory.CryptoProxyModule();
        javax.crypto.Cipher encryptCipher = module.buildCipher();
        javax.crypto.Cipher decryptCipher = module.buildCipher();
        SecretKey secretKey = module.buildSecretKey();

        AESCipher cipher = new AESCipher(secretKey, encryptCipher, decryptCipher);
        return cipher;
    }
}
