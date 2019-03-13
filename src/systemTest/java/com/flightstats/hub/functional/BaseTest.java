package com.flightstats.hub.functional;

import com.flightstats.hub.functional.config.GuiceModule;
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
public class BaseTest {

    protected Injector injector = Guice.createInjector(new GuiceModule());

    private Retrofit retrofitHub =
            injector.getInstance(Key.get(Retrofit.class, Names.named("hub")));
    private Retrofit retrofitCallback =
            injector.getInstance(Key.get(Retrofit.class, Names.named("callback")));

    @Before
    public void setup () {
        injector.injectMembers(this);
    }

    protected String generateRandomString() {
        return randomAlphaNumeric(10);
    }

    protected <T> T getHubClient(Class<T> serviceClass) {
        return retrofitHub.create(serviceClass);
    }

    protected <T> T getCallbackClient(Class<T> serviceClass) {
        return retrofitCallback.create(serviceClass);
    }

    protected HttpUrl getHubClientBaseUrl() {
        return retrofitHub.baseUrl();
    }

    protected HttpUrl getCallbackClientBaseUrl() {
        return retrofitCallback.baseUrl();
    }

}
