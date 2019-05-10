package com.flightstats.hub.webhook;

import com.flightstats.hub.config.AppProperties;
import com.flightstats.hub.config.PropertiesLoader;
import com.flightstats.hub.config.WebhookProperties;
import com.flightstats.hub.exception.InvalidRequestException;
import com.google.common.base.Strings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class WebhookValidatorTest {

    private AppProperties appProperties = new AppProperties(PropertiesLoader.getInstance());
    private WebhookProperties webhookProperties = new WebhookProperties(PropertiesLoader.getInstance());
    private static final int CALLBACK_TIMEOUT_DEFAULT_IN_SEC = 120;
    private WebhookValidator webhookValidator;
    private Webhook webhook;

    @BeforeEach
    void setUp() {
        webhookValidator = new WebhookValidator(appProperties, webhookProperties);
        webhook = Webhook.builder()
                .callbackUrl("http://client/url")
                .channelUrl("http://hub/channel/channelName")
                .parallelCalls(1)
                .build();
    }

    @Test
    void testName() {
        webhook = webhook.withDefaults(CALLBACK_TIMEOUT_DEFAULT_IN_SEC);
        webhookValidator.validate(webhook.withName("aA9_-"));
    }

    @Test
    void testNameLarge() {
        webhook = webhook.withDefaults(CALLBACK_TIMEOUT_DEFAULT_IN_SEC);
        webhookValidator.validate(webhook.withName(Strings.repeat("B", 128)));
    }

    @Test
    void testNameSizeTooBig() {
        assertThrows(InvalidRequestException.class, () -> webhookValidator.validate(webhook.withName(Strings.repeat("B", 129))));
    }

    @Test
    void testZeroCalls() {
        assertThrows(InvalidRequestException.class, () -> webhookValidator.validate(webhook.withParallelCalls(0)));
    }

    @Test
    void testNameChars() {
        assertThrows(InvalidRequestException.class, () -> webhookValidator.validate(webhook.withName("aA9:")));
    }

    @Test
    void testNonChannelUrl() {
        assertThrows(InvalidRequestException.class, () -> webhookValidator.validate(Webhook.builder()
                .callbackUrl("http:/client/url")
                .channelUrl("http:\\hub/channel/channelName")
                .parallelCalls(1)
                .name("nothing")
                .build()));
    }

    @Test
    void testInvalidCallbackUrl() {
        assertThrows(InvalidRequestException.class, () -> webhookValidator.validate(Webhook.builder()
                .callbackUrl("not a url")
                .channelUrl("http://hub/channel/channelName")
                .parallelCalls(1)
                .name("nothing")
                .build()));
    }

    @Test
    void testInvalidChannelUrl() {
        assertThrows(InvalidRequestException.class, () -> webhookValidator.validate(Webhook.builder()
                .callbackUrl("http:/client/url")
                .channelUrl("http://hub/channe/channelName")
                .parallelCalls(1)
                .name("testInvalidChannelUrl")
                .build()));
    }

    @Test
    void testInvalidBatch() {
        webhook = webhook.withBatch("non").withName("blah");
        assertThrows(InvalidRequestException.class, () -> webhookValidator.validate(webhook));
    }

    @Test
    void testBatchLowerCase() {
        webhook = webhook.withBatch("single").withCallbackTimeoutSeconds(10).withName("blah");
        webhookValidator.validate(webhook);
    }

    @Test
    void testValidCallbackTimeout() {
        webhook = webhook.withCallbackTimeoutSeconds(1000).withBatch("SINGLE").withName("blah");
        webhookValidator.validate(webhook);
    }

    @Test
    void testInvalidSingleHeartbeat() {
        webhook = webhook.withBatch("SINGLE").withHeartbeat(true).withName("blah").withCallbackTimeoutSeconds(10);
        assertThrows(InvalidRequestException.class, () -> webhookValidator.validate(webhook));
    }

    @Test
    void testInvalidCallbackTimeout() {
        webhook = webhook.withCallbackTimeoutSeconds(10 * 1000).withBatch("SINGLE").withName("blah");
        assertThrows(InvalidRequestException.class, () -> webhookValidator.validate(webhook));
    }

    @Test
    void testInvalidCallbackTimeoutZero() {
        webhook = webhook.withCallbackTimeoutSeconds(0).withBatch("SINGLE").withName("blah");
        assertThrows(InvalidRequestException.class, () -> webhookValidator.validate(webhook));
    }

    @Test
    void testInvalidLocalhost() {
        PropertiesLoader.getInstance().setProperty("hub.type", "aws");
        webhook = Webhook.builder()
                .callbackUrl("http:/localhost:8080/url")
                .channelUrl("http://hub/channel/channelName")
                .parallelCalls(1)
                .name("testInvalidChannelUrl")
                .batch("SINGLE")
                .callbackTimeoutSeconds(1)
                .build();
        assertThrows(InvalidRequestException.class, () -> webhookValidator.validate(webhook));
    }

}