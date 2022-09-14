package com.flightstats.hub.system.service;

import com.flightstats.hub.clients.hub.HubClientFactory;
import com.flightstats.hub.clients.hub.channel.ChannelResourceClient;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ChannelConfigExpirationSettings;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import retrofit2.Call;
import retrofit2.Response;

import javax.inject.Inject;

import java.util.Arrays;
import java.util.Collections;

import static javax.ws.rs.core.Response.Status.CREATED;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class ChannelConfigService {

    private static final String CHANNEL_OWNER = "system-tests";
    private final ChannelResourceClient channelResourceClient;
    private final DynamoDbService dynamoService;
    private final HttpUrl hubBaseUrl;

    @Inject
    public ChannelConfigService(HubClientFactory hubClientFactory, DynamoDbService dynamoService) {
        this.dynamoService = dynamoService;
        this.channelResourceClient = hubClientFactory.getHubClient(ChannelResourceClient.class);
        this.hubBaseUrl = hubClientFactory.getHubBaseUrl();
    }

    public String getChannelUrl(String channelName) {
        return hubBaseUrl + "channel/" + channelName;
    }

    @SneakyThrows
    public void createWithDefaults(String channelName) {
        log.info("Create channel name {} ", channelName);
        create(ChannelConfig.builder().name(channelName).owner(CHANNEL_OWNER).build());
    }

    @SneakyThrows
    public void create(ChannelConfig channel) {
        Call<Object> call = channelResourceClient.create(configWithOwner(channel));
        Response<Object> response = call.execute();
        log.debug("channel creation response {}, channelName, {}", response, channel.getName());
        assertEquals(CREATED.getStatusCode(), response.code());
    }

    @SneakyThrows
    public ChannelConfig getUncached(String channelName) {
        Call<ChannelConfig> call = channelResourceClient.getUncachedConfig(channelName);
        Response<ChannelConfig> response = call.execute();
        return response.body();
    }

    @SneakyThrows
    public void update(ChannelConfig channel) {
        Call<Object> call = channelResourceClient.update(channel.getName(), configWithOwner(channel));
        Response<Object> response = call.execute();
        log.debug("channel update response {}, channelName, {}", response, channel.getName());
        assertEquals(CREATED.getStatusCode(), response.code());
    }

    public void updateExpirationSettings(ChannelConfig channel) {
        ChannelConfigExpirationSettings expirationSettings = ChannelConfigExpirationSettings.builder()
                .channelName(channel.getName())
                .keepForever(channel.getKeepForever())
                .maxItems(channel.getMaxItems())
                .ttlDays(channel.getTtlDays())
                .mutableTime(channel.getMutableTime())
                .build();

        dynamoService.updateChannelConfig(expirationSettings);
        ChannelConfig updatedConfig = getUncached(channel.getName()).toBuilder()
                .tags(Arrays.asList("updated-expiration-settings", "historical"))
                .build();
        update(updatedConfig);
    }

    @SneakyThrows
    public void delete(String channelName) {
        this.channelResourceClient.delete(channelName).execute();
    }

    private ChannelConfig configWithOwner(ChannelConfig config) {
        return config.toBuilder()
                .owner(config.getOwner().isEmpty() ? CHANNEL_OWNER : config.getOwner())
                .build();
    }
}