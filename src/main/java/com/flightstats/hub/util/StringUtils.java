package com.flightstats.hub.util;


import org.apache.commons.text.RandomStringGenerator;

public class StringUtils {
    static final RandomStringGenerator generator = new RandomStringGenerator.Builder()
            .withinRange('0', 'z')
            .filteredBy(Character::isLetter, Character::isDigit)
            .build();

    static public String randomAlphaNumeric(int length) {
        return generator.generate(length);
    }
}
