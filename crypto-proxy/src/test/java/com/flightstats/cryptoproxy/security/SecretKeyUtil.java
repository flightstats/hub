package com.flightstats.cryptoproxy.security;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class SecretKeyUtil {
    public static SecretKey createKey(String key, String keyAlgorithm) throws NoSuchAlgorithmException {
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        sr.setSeed(key.getBytes());
        KeyGenerator kgen = KeyGenerator.getInstance(keyAlgorithm);
        int keyLength = 128;
        kgen.init(keyLength, sr);
        SecretKey skey = kgen.generateKey();
        return skey;
    }
}