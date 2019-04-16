package com.flightstats.hub.system.config;

import com.flightstats.hub.helm.ReleaseDelete;
import com.flightstats.hub.helm.ReleaseInstall;
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

import java.util.HashMap;
import java.util.Map;

import static com.flightstats.hub.PropertyLoader.load;

@Slf4j
public class ConfigModule extends AbstractModule {

    private static final String PROPERTY_FILE_NAME = "test-hub.properties";
    private final String releaseName = "ddt-" + com.flightstats.hub.util.StringUtils.randomAlphaNumeric(10).toLowerCase();

    @Override
    protected void configure() {

        Names.bindProperties(binder(), load(PROPERTY_FILE_NAME));
        final Map<String, String> releaseNameProperty = new HashMap<>();
        releaseNameProperty.put("helm.release.name", releaseName);
        Names.bindProperties(binder(), releaseNameProperty);

        bind(ReleaseInstall.class);
        bind(ReleaseDelete.class);
    }

    @Singleton
    @Named("hub")
    @Provides
    public Retrofit retrofitHub(@Named("hub.url") String hubBaseUrl) {
        return new Retrofit.Builder()
                .baseUrl(String.format(hubBaseUrl, releaseName))
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(new OkHttpClient.Builder().build())
                .build();
    }

    @Singleton
    @Named("callback")
    @Provides
    public Retrofit retrofitCallback(@Named("callback.url") String callbackUrl) {
        return new Retrofit.Builder()
                .baseUrl(String.format(callbackUrl, releaseName))
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(new OkHttpClient.Builder().build())
                .build();
    }
}
