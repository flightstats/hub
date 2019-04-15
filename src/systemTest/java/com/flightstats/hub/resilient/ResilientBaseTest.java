package com.flightstats.hub.resilient;

import com.flightstats.hub.resilient.config.ConfigModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;

import static com.flightstats.hub.util.StringUtils.randomAlphaNumeric;

@Slf4j
public class ResilientBaseTest {

    protected Injector injector = Guice.createInjector(new ConfigModule());

    @Before
    public void setup() {
        injector.injectMembers(this);
    }

    protected String generateRandomString() {
        return randomAlphaNumeric(10).toLowerCase();
    }
}
