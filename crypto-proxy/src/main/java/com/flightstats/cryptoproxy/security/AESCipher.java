package com.flightstats.cryptoproxy.security;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import java.security.InvalidKeyException;

@Singleton
public class AESCipher {
    private final javax.crypto.Cipher encryptCipher;
    private final javax.crypto.Cipher decryptCipher;

    @Inject
    public AESCipher(SecretKey secretKey,
                     Cipher encryptCipher,
                     Cipher decryptCipher) throws InvalidKeyException {
        this.encryptCipher = encryptCipher;
        this.encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey);
        this.decryptCipher = decryptCipher;
        this.decryptCipher.init(Cipher.DECRYPT_MODE, secretKey);
    }

    public byte[] encrypt(byte[] data) throws BadPaddingException, IllegalBlockSizeException {
        return encryptCipher.doFinal(data);
    }

    public byte[] decrypt(byte[] data) throws BadPaddingException, IllegalBlockSizeException {
        return decryptCipher.doFinal(data);
    }

}
