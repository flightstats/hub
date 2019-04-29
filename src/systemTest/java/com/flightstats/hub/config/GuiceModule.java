package com.flightstats.hub.config;

import com.flightstats.hub.callback.CallbackResource;
import com.flightstats.hub.callback.CallbackServer;
import com.flightstats.hub.testcase.CallbackServerHelper;
import com.flightstats.hub.testcase.HubHelper;
import com.google.inject.AbstractModule;
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

@Slf4j
public class GuiceModule extends AbstractModule {
    private static final String PROPERTY_FILE_NAME = "integration-hub.properties";

    @Override
    protected void configure() {
        Names.bindProperties(binder(), loadProperties());
        bind(CallbackResource.class).asEagerSingleton();
        bind(CallbackServer.class).asEagerSingleton();
        bind(HubHelper.class);
        bind(CallbackServerHelper.class);
    }

    @Singleton
    @Named("hub")
    @Provides
    public Retrofit retrofitHub(@Named("base.url") String hubBaseUrl) {
        return new Retrofit.Builder()
                .baseUrl(hubBaseUrl)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(new OkHttpClient.Builder().build())
                .build();
    }

    @Singleton
    @Named("callback")
    @Provides
    public Retrofit retrofitCallback(CallbackServer callbackServer) {
        return new Retrofit.Builder()
                .baseUrl(callbackServer.getBaseUrl())
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(new OkHttpClient.Builder().build())
                .build();
    }

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

}
