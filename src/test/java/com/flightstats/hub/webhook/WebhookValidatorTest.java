package com.flightstats.hub.webhook;

import com.flightstats.hub.config.AppProperty;
import com.flightstats.hub.config.PropertyLoader;
import com.flightstats.hub.config.WebhookProperty;
import com.flightstats.hub.exception.InvalidRequestException;
import com.google.common.base.Strings;
import org.junit.Before;
import org.junit.Test;

public class WebhookValidatorTest {

    private static final int CALLBACK_TIMEOUT_DEFAULT_IN_SEC = 120;
    private WebhookValidator webhookValidator;
    private Webhook webhook;

    private AppProperty appProperty = new AppProperty(PropertyLoader.getInstance());;
    private WebhookProperty webhookProperty = new WebhookProperty(PropertyLoader.getInstance());;

    @Before
    public void setUp() {
        webhookValidator = new WebhookValidator(appProperty, webhookProperty);
        webhook = Webhook.builder()
                .callbackUrl("http://client/url")
                .channelUrl("http://hub/channel/channelName")
                .parallelCalls(1)
                .build();
    }

    @Test
    public void testName() {
        webhook = webhook.withDefaults(CALLBACK_TIMEOUT_DEFAULT_IN_SEC);
        webhookValidator.validate(webhook.withName("aA9_-"));
    }

    @Test
    public void testNameLarge() {
        webhook = webhook.withDefaults(CALLBACK_TIMEOUT_DEFAULT_IN_SEC);
        webhookValidator.validate(webhook.withName(Strings.repeat("B", 128)));
    }

    @Test(expected = InvalidRequestException.class)
    public void testNameSizeTooBig() {
        webhookValidator.validate(webhook.withName(Strings.repeat("B", 129)));
    }

    @Test(expected = InvalidRequestException.class)
    public void testZeroCalls() {
        webhookValidator.validate(webhook.withParallelCalls(0));
    }

    @Test(expected = InvalidRequestException.class)
    public void testNameChars() {
        webhookValidator.validate(webhook.withName("aA9:"));
    }

    @Test(expected = InvalidRequestException.class)
    public void testNonChannelUrl() {
        webhookValidator.validate(Webhook.builder()
                .callbackUrl("http:/client/url")
                .channelUrl("http:\\hub/channel/channelName")
                .parallelCalls(1)
                .name("nothing")
                .build());
    }

    @Test(expected = InvalidRequestException.class)
    public void testInvalidCallbackUrl() throws Exception {
        webhookValidator.validate(Webhook.builder()
                .callbackUrl("not a url")
                .channelUrl("http://hub/channel/channelName")
                .parallelCalls(1)
                .name("nothing")
                .build());
    }

    @Test(expected = InvalidRequestException.class)
    public void testInvalidChannelUrl() throws Exception {
        webhookValidator.validate(Webhook.builder()
                .callbackUrl("http:/client/url")
                .channelUrl("http://hub/channe/channelName")
                .parallelCalls(1)
                .name("testInvalidChannelUrl")
                .build());
    }

    @Test(expected = InvalidRequestException.class)
    public void testInvalidBatch() throws Exception {
        webhook = webhook.withBatch("non").withName("blah");
        webhookValidator.validate(webhook);
    }

    @Test
    public void testBatchLowerCase() throws Exception {
        webhook = webhook.withBatch("single").withCallbackTimeoutSeconds(10).withName("blah");
        webhookValidator.validate(webhook);
    }

    @Test()
    public void testValidCallbackTimeout() throws Exception {
        webhook = webhook.withCallbackTimeoutSeconds(1000).withBatch("SINGLE").withName("blah");
        webhookValidator.validate(webhook);
    }

    @Test(expected = InvalidRequestException.class)
    public void testInvalidSingleHeartbeat() {
        webhook = webhook.withBatch("SINGLE").withHeartbeat(true).withName("blah").withCallbackTimeoutSeconds(10);
        webhookValidator.validate(webhook);
    }

    @Test(expected = InvalidRequestException.class)
    public void testInvalidCallbackTimeout() {
        webhook = webhook.withCallbackTimeoutSeconds(10 * 1000).withBatch("SINGLE").withName("blah");
        webhookValidator.validate(webhook);
    }

    @Test(expected = InvalidRequestException.class)
    public void testInvalidCallbackTimeoutZero() {
        webhook = webhook.withCallbackTimeoutSeconds(0).withBatch("SINGLE").withName("blah");
        webhookValidator.validate(webhook);
    }

    @Test(expected = InvalidRequestException.class)
    public void testInvalidLocalhost() {
        PropertyLoader.getInstance().setProperty("hub.type", "aws");
        webhook = Webhook.builder()
                .callbackUrl("http:/localhost:8080/url")
                .channelUrl("http://hub/channel/channelName")
                .parallelCalls(1)
                .name("testInvalidChannelUrl")
                .batch("SINGLE")
                .callbackTimeoutSeconds(1)
                .build();

        webhookValidator.validate(webhook);
    }

}