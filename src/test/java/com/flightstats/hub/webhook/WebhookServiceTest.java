package com.flightstats.hub.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.app.HubModule;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.model.MinutePath;
import com.google.gson.Gson;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WebhookServiceTest {

    @Test
    public void testJsonSerialization() {
        Dao<Webhook> webhookDao = mock(Dao.class);
        WebhookValidator webhookValidator = mock(WebhookValidator.class);
        WebhookManager webhookManager = mock(WebhookManager.class);
        LastContentPath lastContentPath = mock(LastContentPath.class);
        ChannelService channelService = mock(ChannelService.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        HubProperties hubProperties = mock(HubProperties.class);
        WebhookService webhookService = new WebhookService(webhookDao, webhookValidator, webhookManager, lastContentPath, channelService, objectMapper, hubProperties);

        HubModule hubModule = new HubModule(new Properties());
        Gson gson = hubModule.provideGson();

        ContentKey contentKey = new ContentKey();
        Webhook webhook = Webhook.builder()
                .channelUrl("test")
                .callbackUrl("http://test.callback.url/")
                .tagUrl("http://hub/tag/allTheThings")
                .build();

        Webhook marshalledWebhook = webhookService.fromJson(gson.toJson(webhook));

        assertEquals(webhook, marshalledWebhook);
    }

    @Test
    public void testFromJsonStartItemIsHashPath() {
        Dao<Webhook> webhookDao = mock(Dao.class);
        WebhookValidator webhookValidator = mock(WebhookValidator.class);
        WebhookManager webhookManager = mock(WebhookManager.class);
        LastContentPath lastContentPath = mock(LastContentPath.class);
        ChannelService channelService = mock(ChannelService.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        HubProperties hubProperties = mock(HubProperties.class);
        WebhookService webhookService = new WebhookService(webhookDao, webhookValidator, webhookManager, lastContentPath, channelService, objectMapper, hubProperties);

        ContentKey contentKey = new ContentKey();
        String startItem = "http://hub/channel/test/" + contentKey.toUrl();
        Map<String, String> data = new HashMap<>();
        data.put("startItem", startItem);
        String json = constructJson(data);
        Webhook webhook = webhookService.fromJson(json);
        assertEquals(contentKey, webhook.getStartingKey());
    }

    @Test
    public void testFromJsonStartItemIsTimePath() {
        Dao<Webhook> webhookDao = mock(Dao.class);
        WebhookValidator webhookValidator = mock(WebhookValidator.class);
        WebhookManager webhookManager = mock(WebhookManager.class);
        LastContentPath lastContentPath = mock(LastContentPath.class);
        ChannelService channelService = mock(ChannelService.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        HubProperties hubProperties = mock(HubProperties.class);
        WebhookService webhookService = new WebhookService(webhookDao, webhookValidator, webhookManager, lastContentPath, channelService, objectMapper, hubProperties);

        MinutePath minutePath = new MinutePath();
        String startItem = "http://hub/channel/test/" + minutePath.toUrl();
        Map<String, String> data = new HashMap<>();
        data.put("startItem", startItem);
        String json = constructJson(data);
        Webhook webhook = webhookService.fromJson(json);
        assertEquals(minutePath, webhook.getStartingKey());
    }

    @Test
    public void testSingleBatchWithDefaults() {
        Dao<Webhook> webhookDao = mock(Dao.class);
        WebhookValidator webhookValidator = mock(WebhookValidator.class);
        WebhookManager webhookManager = mock(WebhookManager.class);
        LastContentPath lastContentPath = mock(LastContentPath.class);
        ChannelService channelService = mock(ChannelService.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        HubProperties hubProperties = mock(HubProperties.class);
        when(hubProperties.getCallbackTimeoutDefault()).thenReturn(420);
        WebhookService webhookService = new WebhookService(webhookDao, webhookValidator, webhookManager, lastContentPath, channelService, objectMapper, hubProperties);

        Webhook webhook = Webhook.builder().build();

        assertNull(webhook.getParallelCalls());
        assertNull(webhook.getBatch());
        assertFalse(webhook.isHeartbeat());
        assertNull(webhook.getTtlMinutes());
        assertNull(webhook.getMaxWaitMinutes());
        assertNull(webhook.getCallbackTimeoutSeconds());
        assertNull(webhook.getMaxAttempts());

        webhook = webhookService.withDefaults(webhook);

        assertEquals(1L, (long) webhook.getParallelCalls());
        assertEquals("SINGLE", webhook.getBatch());
        assertFalse(webhook.isHeartbeat());
        assertEquals(Integer.valueOf(0), webhook.getTtlMinutes());
        assertEquals(Integer.valueOf(1), webhook.getMaxWaitMinutes());
        assertEquals(Integer.valueOf(420), webhook.getCallbackTimeoutSeconds());
        assertEquals(Integer.valueOf(0), webhook.getMaxAttempts());
    }

    @Test
    public void testMinuteBatchWithDefaults() {
        Dao<Webhook> webhookDao = mock(Dao.class);
        WebhookValidator webhookValidator = mock(WebhookValidator.class);
        WebhookManager webhookManager = mock(WebhookManager.class);
        LastContentPath lastContentPath = mock(LastContentPath.class);
        ChannelService channelService = mock(ChannelService.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        HubProperties hubProperties = mock(HubProperties.class);
        WebhookService webhookService = new WebhookService(webhookDao, webhookValidator, webhookManager, lastContentPath, channelService, objectMapper, hubProperties);

        Webhook webhook = Webhook.builder()
                .batch(Webhook.MINUTE)
                .build();

        assertFalse(webhook.isHeartbeat());

        webhook = webhookService.withDefaults(webhook);

        assertTrue(webhook.isHeartbeat());
    }

    @Test
    public void testSecondBatchWithDefaults() {
        Dao<Webhook> webhookDao = mock(Dao.class);
        WebhookValidator webhookValidator = mock(WebhookValidator.class);
        WebhookManager webhookManager = mock(WebhookManager.class);
        LastContentPath lastContentPath = mock(LastContentPath.class);
        ChannelService channelService = mock(ChannelService.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        HubProperties hubProperties = mock(HubProperties.class);
        WebhookService webhookService = new WebhookService(webhookDao, webhookValidator, webhookManager, lastContentPath, channelService, objectMapper, hubProperties);

        Webhook webhook = Webhook.builder()
                .batch(Webhook.SECOND)
                .build();

        assertFalse(webhook.isHeartbeat());

        webhook = webhookService.withDefaults(webhook);

        assertTrue(webhook.isHeartbeat());
    }

    private String constructJson(Map<String, String> data) {
        return String.format("{%s}", data.entrySet().stream()
                .map((entry) -> String.format("\"%s\": \"%s\"", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(",")));
    }

}
