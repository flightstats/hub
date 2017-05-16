package com.flightstats.hub.util;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import java.math.BigInteger;
import java.nio.charset.Charset;

public class Hash {

    //This assumes we are using a full 64 bit range.
    private static final BigInteger TOTAL_RANGE =
            BigInteger.valueOf(Long.MAX_VALUE)
                    .subtract(BigInteger.valueOf(Long.MIN_VALUE))
                    .add(BigInteger.ONE);

    private static final HashFunction hashFunction = Hashing.farmHashFingerprint64();

    public static long hash(String key) {
        return hashFunction.hashString(key, Charset.defaultCharset()).asLong();
    }

    public static long getRangeSize(int nodes) {
        return TOTAL_RANGE.divide(BigInteger.valueOf(nodes)).longValue();
    }
}
