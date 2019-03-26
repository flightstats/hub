package com.flightstats.hub.functional.config;

import com.flightstats.hub.functional.callback.CallbackServer;
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

import static com.flightstats.hub.PropertyLoader.load;

@Slf4j
public class GuiceModule extends AbstractModule {

    private static final String PROPERTY_FILE_NAME = "functional-hub.properties";

    @Override
    protected void configure() {
        Names.bindProperties(binder(), load(PROPERTY_FILE_NAME));
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
    public Retrofit retrofitCallback() {
        return new Retrofit.Builder()
                .baseUrl(callbackServer().getBaseUrl())
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(new OkHttpClient.Builder().build())
                .build();
    }

    @Singleton
    @Provides
    public CallbackServer callbackServer() {
        return new CallbackServer();
    }

}
