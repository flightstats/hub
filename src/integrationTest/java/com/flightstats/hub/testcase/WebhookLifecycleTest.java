package com.flightstats.hub.testcase;

import com.flightstats.hub.BaseTest;
import com.flightstats.hub.client.ChannelItemResourceClient;
import com.flightstats.hub.client.ChannelResourceClient;
import com.flightstats.hub.client.WebhookResourceClient;
import com.flightstats.hub.model.Channel;
import com.flightstats.hub.model.ChannelItem;
import com.flightstats.hub.model.Webhook;
import com.flightstats.hub.server.CallbackServer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import retrofit2.Call;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;

import static javax.ws.rs.core.Response.Status.CREATED;
import static org.testng.AssertJUnit.assertEquals;

@Slf4j
public class WebhookLifecycleTest extends BaseTest {

    private static final String CHANNEL_ITEM_VALUE = "{\"first\": \"fn\", \"last\":\"ln\"}";

    private ChannelItemResourceClient channelItemResourceClient;
    private ChannelResourceClient channelResourceClient;
    private WebhookResourceClient webhookResourceClient;
    private CallbackServer callbackServer;
    private String channelName;
    private String webhookName;

    @Before
    public void setup() {

        this.channelItemResourceClient = getHttpClient(ChannelItemResourceClient.class);
        this.channelResourceClient = getHttpClient(ChannelResourceClient.class);
        this.webhookResourceClient = getHttpClient(WebhookResourceClient.class);
        this.callbackServer = injector.getInstance(CallbackServer.class);

        this.channelName = generateRandomString();
        this.webhookName = generateRandomString();

        this.callbackServer.start();
    }

    @Test
    public void test() {
        List<String> channelItems = new ArrayList<>();

        createChannel();
        for (int i = 0; i < 10; i++) {
            channelItems.add(addItemToChannel(CHANNEL_ITEM_VALUE));
            log.info(channelItems.get(i));
        }

        createWebhook(channelItems.get(5));
    }


    @SneakyThrows
    public void createChannel() {
        log.info("Channel name {} ", channelName);

        Call<Object> call = channelResourceClient.create(Channel.builder().name(channelName).build());
        Response<Object> response = call.execute();

        assertEquals(CREATED.getStatusCode(), response.code());
    }

    @SneakyThrows
    public String addItemToChannel(Object data) {

        Call<ChannelItem> call = channelItemResourceClient.add(channelName, data);
        Response<ChannelItem> response = call.execute();

        assertEquals(CREATED.getStatusCode(), response.code());

        return response.body().get_links().getSelf().getHref();
    }

    @SneakyThrows
    public void createWebhook(String startItem) {
        log.info("Webhook name {} ", webhookName);

        Call<com.flightstats.hub.model.Webhook> webhook = webhookResourceClient.create(webhookName, buildWebhook(startItem));
        Response<com.flightstats.hub.model.Webhook> response = webhook.execute();

        assertEquals(CREATED.getStatusCode(), response.code());
    }

    private Webhook buildWebhook(String startItem) {
        return Webhook.builder()
                .channelUrl(retrofit.baseUrl() + "channel/" + channelName)
                .callbackUrl(callbackServer.getUrl() + "callback")
                .parallelCalls(2)
                .startItem(startItem)
                .batch("single")
                .ttlMinutes(0)
                .build();
    }

    @After
    public void destroy() {
        this.channelResourceClient.delete(channelName);
        this.callbackServer.stop();
    }
}
