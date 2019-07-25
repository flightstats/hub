package com.flightstats.hub.system.config;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.s3.AmazonS3;
import com.flightstats.hub.clients.aws.AwsClientFactory;
import com.flightstats.hub.clients.callback.CallbackClientFactory;
import com.flightstats.hub.clients.hub.HubClientFactory;
import com.flightstats.hub.model.adapters.HubDateTimeTypeAdapter;
import com.flightstats.hub.model.adapters.HubDateTypeAdapter;
import com.flightstats.hub.system.service.CallbackService;
import com.flightstats.hub.system.service.ChannelConfigService;
import com.flightstats.hub.system.service.WebhookService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Names;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.joda.time.DateTime;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Date;
import java.util.Properties;

@Slf4j
@AllArgsConstructor
public class GuiceModule extends AbstractModule {
    private final Properties properties;

    @Override
    protected void configure() {
        bind(AwsClientFactory.class).asEagerSingleton();
        Names.bindProperties(binder(), properties);

        bind(HubClientFactory.class);
        bind(CallbackClientFactory.class);
        bind(ChannelConfigService.class);
        bind(WebhookService.class);
        bind(CallbackService.class);
    }

    @Singleton
    @Named("hub")
    @Provides
    public Retrofit retrofitHub(ServiceProperties serviceProperties) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Date.class, new HubDateTypeAdapter())
                .registerTypeAdapter(DateTime.class, new HubDateTimeTypeAdapter())
                .create();
        return new Retrofit.Builder()
                .baseUrl(serviceProperties.getHubUrl())
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
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
    public AmazonS3 s3Client(AwsClientFactory awsClientFactory) {
        return awsClientFactory.getS3Client();
    }

    @Singleton
    @Provides
    public AmazonDynamoDB dynamoDbClient(AwsClientFactory awsClientFactory) {
        return awsClientFactory.getDynamoDbClient();
    }
}
