package com.flightstats.cryptoproxy.security;

import com.google.inject.Inject;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import java.security.InvalidKeyException;

public class AESDecryptionCipher {
    private final Cipher cipher;

    @Inject
    public AESDecryptionCipher(SecretKey secretKey, Cipher cipher) throws InvalidKeyException {
        this.cipher = cipher;
        this.cipher.init(Cipher.DECRYPT_MODE, secretKey);
    }

    public byte[] decrypt(byte[] data) throws BadPaddingException, IllegalBlockSizeException {
        return cipher.doFinal(data);
    }

}
