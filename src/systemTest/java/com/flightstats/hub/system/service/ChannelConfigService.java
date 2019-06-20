package com.flightstats.hub.system.service;

import com.flightstats.hub.clients.hub.HubClientFactory;
import com.flightstats.hub.clients.hub.channel.ChannelItemResourceClient;
import com.flightstats.hub.clients.hub.channel.ChannelResourceClient;
import com.flightstats.hub.model.ChannelConfig;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import retrofit2.Call;
import retrofit2.Response;

import javax.inject.Inject;

import static javax.ws.rs.core.Response.Status.CREATED;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class ChannelConfigService {

    private static final String CHANNEL_OWNER = "system-tests";
    private ChannelResourceClient channelResourceClient;
    private HttpUrl hubBaseUrl;

    @Inject
    public ChannelConfigService(HubClientFactory hubClientFactory) {
        this.channelResourceClient = hubClientFactory.getHubClient(ChannelResourceClient.class);
        this.hubBaseUrl = hubClientFactory.getHubBaseUrl();
    }

    public String getChannelUrl(String channelName) {
        return getHubBaseUrl() + "channel/" + channelName;
    }

    private HttpUrl getHubBaseUrl() {
        return hubBaseUrl;
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
        log.info("channel creation response {}, channelName, {}", response, channel.getName());
        assertEquals(CREATED.getStatusCode(), response.code());
    }

    @SneakyThrows
    public void update(ChannelConfig channel) {
        Call<Object> call = channelResourceClient.update(channel.getName(), configWithOwner(channel));
        Response<Object> response = call.execute();
        log.info("channel update response {}, channelName, {}", response, channel.getName());
        assertEquals(CREATED.getStatusCode(), response.code());
    }

    @SneakyThrows
    public void delete(String channelName) {
        this.channelResourceClient.delete(channelName).execute();
    }

    private ChannelConfig configWithOwner(ChannelConfig config) {
        ChannelConfig.ChannelConfigBuilder builder = config.toBuilder();
        if (config.getOwner().isEmpty()) {
            builder.owner(CHANNEL_OWNER);
        }
        return builder.build();
    }
}