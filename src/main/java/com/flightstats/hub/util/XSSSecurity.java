package com.flightstats.hub.util;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.owasp.esapi.ESAPI;

public class XSSSecurity {
    private final String value;

    public XSSSecurity(String value) {
        this.value = value;
    }

    public String strip() {
        if (value == null)
            return null;

        // Use the ESAPI library to avoid encoded attacks.
        String newValue = ESAPI.encoder().canonicalize(value);

        // Avoid null characters
        newValue = newValue.replaceAll("\0", "");

        // Clean out HTML
        return Jsoup.clean(newValue, Safelist.none());
    }
}
