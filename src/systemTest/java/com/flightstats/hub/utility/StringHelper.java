package com.flightstats.hub.utility;

import org.apache.commons.text.RandomStringGenerator;

public class StringHelper {

    private static final RandomStringGenerator generator = new RandomStringGenerator.Builder()
            .withinRange('0', 'z')
            .filteredBy(Character::isLetter, Character::isDigit)
            .build();

    public String randomAlphaNumeric(int length) {
        return generator.generate(length);
    }

}
