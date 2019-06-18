package com.flightstats.hub.system.config;

import com.amazonaws.services.s3.AmazonS3;
import com.flightstats.hub.clients.callback.CallbackClientFactory;
import com.flightstats.hub.clients.hub.HubClientFactory;
import com.flightstats.hub.clients.s3.S3ClientFactory;
import com.flightstats.hub.system.service.CallbackService;
import com.flightstats.hub.system.service.ChannelService;
import com.flightstats.hub.system.service.WebhookService;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Names;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Properties;

@Slf4j
@AllArgsConstructor
public class GuiceModule extends AbstractModule {
    private final Properties properties;

    @Override
    protected void configure() {
        bind(S3ClientFactory.class).asEagerSingleton();
        Names.bindProperties(binder(), properties);

        bind(HubClientFactory.class);
        bind(CallbackClientFactory.class);
        bind(ChannelService.class);
        bind(WebhookService.class);
        bind(CallbackService.class);
    }

    @Singleton
    @Named("hub")
    @Provides
    public Retrofit retrofitHub(ServiceProperties serviceProperties) {
        return new Retrofit.Builder()
                .baseUrl(serviceProperties.getHubUrl())
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(new OkHttpClient.Builder().build())
                .build();
    }

    @Singleton
    @Named("callback")
    @Provides
    public Retrofit retrofitCallback(ServiceProperties serviceProperties) {
        return new Retrofit.Builder()
                .baseUrl(serviceProperties.getCallbackUrl())
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
}
