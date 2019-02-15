package com.flightstats.hub.util;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class StringUtilsTest {
    @Test
    public void testRandomAlphaNumeric() throws Exception {
        Pattern p = Pattern.compile("^\\w+$");
        String ranStr = StringUtils.randomAlphaNumeric(1000);
        Matcher m = p.matcher(ranStr);
        System.out.println("str =  " + ranStr);
        assertTrue(m.matches());
    }

}