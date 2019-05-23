package com.flightstats.hub.system.config;

import com.amazonaws.services.s3.AmazonS3;
import com.flightstats.hub.clients.callback.CallbackClientFactory;
import com.flightstats.hub.clients.hub.HubClientFactory;
import com.flightstats.hub.clients.s3.S3ClientFactory;
import com.flightstats.hub.system.service.CallbackService;
import com.flightstats.hub.system.service.ChannelService;
import com.flightstats.hub.system.service.WebhookService;
import com.flightstats.hub.utility.StringHelper;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


@Slf4j
public class GuiceModule extends AbstractModule {
    private final StringHelper stringHelper = new StringHelper();
    private final String releaseName = "ddt-" + stringHelper.randomAlphaNumeric(10).toLowerCase();
    private static final String PROPERTY_FILE_NAME = "system-test-hub.properties";

    @Override
    protected void configure() {
        Names.bindProperties(binder(), loadProperties());
        final Map<String, String> releaseNameProperty = new HashMap<>();
        releaseNameProperty.put("helm.release.name", releaseName);

        Names.bindProperties(binder(), releaseNameProperty);
        bind(StringHelper.class).toInstance(stringHelper);
        bind(S3ClientFactory.class).asEagerSingleton();
        bind(HubClientFactory.class);
        bind(CallbackClientFactory.class);
        bind(ChannelService.class);
        bind(WebhookService.class);
        bind(CallbackService.class);
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

    @Singleton
    @Provides
    public AmazonS3 s3Client(S3ClientFactory s3ClientFactory) {
        return s3ClientFactory.getS3Client();
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
