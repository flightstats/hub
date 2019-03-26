package com.flightstats.hub.resilient;

import com.flightstats.hub.functional.config.GuiceModule;
import com.flightstats.hub.resilient.config.ConfigModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import org.junit.Before;
import retrofit2.Retrofit;

import static com.flightstats.hub.util.StringUtils.randomAlphaNumeric;

@Slf4j
public class ResilientBaseTest {

    protected Injector injector = Guice.createInjector(new ConfigModule());

    @Before
    public void setup () {
        injector.injectMembers(this);
    }

    protected String generateRandomString() {
        return randomAlphaNumeric(10).toLowerCase();
    }
}
