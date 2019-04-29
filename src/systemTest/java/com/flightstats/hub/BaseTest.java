package com.flightstats.hub;

import com.flightstats.hub.config.GuiceModule;
import com.google.inject.Guice;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;

import static com.flightstats.hub.util.StringUtils.randomAlphaNumeric;

@Slf4j
public class BaseTest {

    @Before
    public void before () {
        Guice.createInjector(new GuiceModule()).injectMembers(this);
    }

    public String generateRandomString() {
        return randomAlphaNumeric(10);
    }

}
