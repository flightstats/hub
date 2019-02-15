package com.flightstats.hub.webhook;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.exception.InvalidRequestException;
import com.google.common.base.Strings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class WebhookValidatorTest {

    private WebhookValidator webhookValidator;
    private Webhook webhook;

    @BeforeEach
    void setUp() {
        webhookValidator = new WebhookValidator();
        webhook = Webhook.builder()
                .callbackUrl("http://client/url")
                .channelUrl("http://hub/channel/channelName")
                .parallelCalls(1)
                .build();
    }

    @Test
    void testName() {
        webhook = webhook.withDefaults();
        webhookValidator.validate(webhook.withName("aA9_-"));
    }

    @Test
    void testNameLarge() {
        webhook = webhook.withDefaults();
        webhookValidator.validate(webhook.withName(Strings.repeat("B", 128)));
    }

    @Test
    void testNameSizeTooBig() {
        try {
            webhookValidator.validate(webhook.withName(Strings.repeat("B", 129)));
        } catch (Exception ex) {
            assertEquals(ex.getClass(), InvalidRequestException.class);
        }
    }

    @Test
    void testZeroCalls() {
        try {
            webhookValidator.validate(webhook.withParallelCalls(0));
        } catch (Exception ex) {
            assertEquals(ex.getClass(), InvalidRequestException.class);
        }
    }

    @Test
    void testNameChars() {
        try {
            webhookValidator.validate(webhook.withName("aA9:"));
        } catch (Exception ex) {
            assertEquals(ex.getClass(), InvalidRequestException.class);
        }
    }

    @Test
    void testNonChannelUrl() {
        try {
            webhookValidator.validate(Webhook.builder()
                    .callbackUrl("http:/client/url")
                    .channelUrl("http:\\hub/channel/channelName")
                    .parallelCalls(1)
                    .name("nothing")
                    .build());
        } catch (Exception ex) {
            assertEquals(ex.getClass(), InvalidRequestException.class);
        }
    }

    @Test
    void testInvalidCallbackUrl() {
        try {
            webhookValidator.validate(Webhook.builder()
                    .callbackUrl("not a url")
                    .channelUrl("http://hub/channel/channelName")
                    .parallelCalls(1)
                    .name("nothing")
                    .build());
        } catch (Exception ex) {
            assertEquals(ex.getClass(), InvalidRequestException.class);
        }
    }

    @Test
    void testInvalidChannelUrl() {
        try {
            webhookValidator.validate(Webhook.builder()
                    .callbackUrl("http:/client/url")
                    .channelUrl("http://hub/channe/channelName")
                    .parallelCalls(1)
                    .name("testInvalidChannelUrl")
                    .build());
        } catch (Exception ex) {
            assertEquals(ex.getClass(), InvalidRequestException.class);
        }
    }

    @Test
    void testInvalidBatch() {
        try {
            webhook = webhook.withBatch("non").withName("blah");
            webhookValidator.validate(webhook);
        } catch (Exception ex) {
            assertEquals(ex.getClass(), InvalidRequestException.class);
        }
    }

    @Test
    void testBatchLowerCase() {
        webhook = webhook.withBatch("single").withCallbackTimeoutSeconds(10).withName("blah");
        webhookValidator.validate(webhook);
    }

    @Test()
    void testValidCallbackTimeout() {
        webhook = webhook.withCallbackTimeoutSeconds(1000).withBatch("SINGLE").withName("blah");
        webhookValidator.validate(webhook);
    }

    @Test
    void testInvalidSingleHeartbeat() {
        try {
            webhook = webhook.withBatch("SINGLE").withHeartbeat(true).withName("blah").withCallbackTimeoutSeconds(10);
            webhookValidator.validate(webhook);
        } catch (Exception ex) {
            assertEquals(ex.getClass(), InvalidRequestException.class);
        }
    }

    @Test
    void testInvalidCallbackTimeout() {
        try {
            webhook = webhook.withCallbackTimeoutSeconds(10 * 1000).withBatch("SINGLE").withName("blah");
            webhookValidator.validate(webhook);
        } catch (Exception ex) {
            assertEquals(ex.getClass(), InvalidRequestException.class);
        }
    }

    @Test
    void testInvalidCallbackTimeoutZero() {
        try {
            webhook = webhook.withCallbackTimeoutSeconds(0).withBatch("SINGLE").withName("blah");
            webhookValidator.validate(webhook);
        } catch (Exception ex) {
            assertEquals(ex.getClass(), InvalidRequestException.class);
        }
    }

    @Test
    void testInvalidLocalhost() {
        try {
            HubProperties.setProperty("hub.type", "aws");
            webhook = Webhook.builder()
                    .callbackUrl("http:/localhost:8080/url")
                    .channelUrl("http://hub/channel/channelName")
                    .parallelCalls(1)
                    .name("testInvalidChannelUrl")
                    .batch("SINGLE")
                    .callbackTimeoutSeconds(1)
                    .build();

            webhookValidator.validate(webhook);
        } catch (Exception ex) {
            assertEquals(ex.getClass(), InvalidRequestException.class);
        }
    }

}