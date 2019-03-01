package com.flightstats.hub;

import com.flightstats.hub.callback.CallbackServer;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static com.flightstats.hub.util.StringUtils.randomAlphaNumeric;

@Slf4j
public class BaseTest {

    private static final String PROPERTY_FILE_NAME = "integration-hub.properties";

    protected Injector injector = Guice.createInjector(new AbstractModule() {
        @Override
        protected void configure() {
            Names.bindProperties(binder(), loadProperties());
            bind(CallbackServer.class).asEagerSingleton();
        }

        @Singleton
        @Provides
        public Retrofit retrofit(@Named("base.url") String hubBaseUrl) {
            return new Retrofit.Builder()
                    .baseUrl(hubBaseUrl)
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(new OkHttpClient.Builder().build())
                    .build();
        }
    });

    protected Retrofit retrofit = injector.getInstance(Retrofit.class);

    private Properties loadProperties() {
        final Properties properties = new Properties();
        try (final InputStream inputStream =
                     this.getClass().getClassLoader().getResourceAsStream(PROPERTY_FILE_NAME)) {
            properties.load(inputStream);

        } catch (IOException e) {
            log.error("Property file {} not found", PROPERTY_FILE_NAME, e);
        }
        return properties;
    }

    protected String generateRandomString() {
        return randomAlphaNumeric(10);
    }

    protected <T> T getHttpClient(Class<T> serviceClass) {
        return retrofit.create(serviceClass);
    }

}
