package com.flightstats.hub.webhook;

import com.flightstats.hub.config.properties.AppProperties;
import com.flightstats.hub.config.properties.WebhookProperties;
import com.flightstats.hub.exception.InvalidRequestException;
import com.flightstats.hub.model.WebhookType;
import com.google.common.base.Strings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebhookValidatorTest {

    private static final int CALLBACK_TIMEOUT_DEFAULT_IN_SEC = 120;
    private static final int MINIMUM_CALLBACK_TIMEOUT_IN_SEC = 1;
    private static final int MAXIMUM_CALLBACK_TIMEOUT_IN_SEC = 1800;

    @Test
    void testName() {
        WebhookValidator webhookValidator = new WebhookValidator(getDefaultAppProperties(), getDefaultWebhookProperties());
        Webhook webhook = getWebhook(builder -> builder.name("aA9_-"));
        assertDoesNotThrow(() -> webhookValidator.validate(webhook));
    }

    @Test
    void testNameLarge() {
        WebhookValidator webhookValidator = new WebhookValidator(getDefaultAppProperties(), getDefaultWebhookProperties());
        String almostTooLongName = Strings.repeat("B", 128);
        Webhook webhook = getWebhook(builder -> builder.name(almostTooLongName));
        assertDoesNotThrow(() -> webhookValidator.validate(webhook));
    }

    @Test
    void testNameSizeTooBig() {
        WebhookValidator webhookValidator = new WebhookValidator(getDefaultAppProperties(), getDefaultWebhookProperties());
        String tooLongName = Strings.repeat("B", 129);
        Webhook webhook = getWebhook(builder -> builder.name(tooLongName));
        assertThrows(InvalidRequestException.class, () -> webhookValidator.validate(webhook));
    }

    @Test
    void testZeroCalls() {
        WebhookValidator webhookValidator = new WebhookValidator(getDefaultAppProperties(), getDefaultWebhookProperties());
        Webhook webhook = getWebhook(builder -> builder.parallelCalls(0));
        assertThrows(InvalidRequestException.class, () -> webhookValidator.validate(webhook));
    }

    @Test
    void testNameChars() {
        WebhookValidator webhookValidator = new WebhookValidator(getDefaultAppProperties(), getDefaultWebhookProperties());
        Webhook webhook = getWebhook(builder -> builder.name("aA9:"));
        assertThrows(InvalidRequestException.class, () -> webhookValidator.validate(webhook));
    }

    @Test
    void testNonChannelUrl() {
        WebhookValidator webhookValidator = new WebhookValidator(getDefaultAppProperties(), getDefaultWebhookProperties());
        Webhook webhook = getWebhook(builder -> builder.channelUrl("http:\\hub/channel/channelName"));
        assertThrows(InvalidRequestException.class, () -> webhookValidator.validate(webhook));
    }

    @Test
    void testInvalidCallbackUrl() {
        WebhookValidator webhookValidator = new WebhookValidator(getDefaultAppProperties(), getDefaultWebhookProperties());
        Webhook webhook = getWebhook(builder -> builder.callbackUrl("not a url"));
        assertThrows(InvalidRequestException.class, () -> webhookValidator.validate(webhook));
    }

    @Test
    void testInvalidChannelUrl() {
        WebhookValidator webhookValidator = new WebhookValidator(getDefaultAppProperties(), getDefaultWebhookProperties());
        Webhook webhook = getWebhook(builder -> builder.channelUrl("http://hub/chanel/channelName"));
        assertThrows(InvalidRequestException.class, () -> webhookValidator.validate(webhook));
    }

    @Test
    void testInvalidBatch() {
        WebhookValidator webhookValidator = new WebhookValidator(getDefaultAppProperties(), getDefaultWebhookProperties());
        Webhook webhook = getWebhook(builder -> builder.batch("non"));
        assertThrows(InvalidRequestException.class, () -> webhookValidator.validate(webhook));
    }

    @Test
    void testBatchLowerCase() {
        WebhookValidator webhookValidator = new WebhookValidator(getDefaultAppProperties(), getDefaultWebhookProperties());
        Webhook webhook = getWebhook(builder -> builder.batch("single"));
        assertDoesNotThrow(() -> webhookValidator.validate(webhook));
    }

    @Test
    void testInvalidSingleHeartbeat() {
        WebhookValidator webhookValidator = new WebhookValidator(getDefaultAppProperties(), getDefaultWebhookProperties());
        Webhook webhook = getWebhook(builder -> builder.batch("SINGLE").heartbeat(true));
        assertThrows(InvalidRequestException.class, () -> webhookValidator.validate(webhook));
    }

    @Test
    void testInvalidCallbackTimeout() {
        WebhookValidator webhookValidator = new WebhookValidator(getDefaultAppProperties(), getDefaultWebhookProperties());
        Webhook webhook = getWebhook(builder -> builder.callbackTimeoutSeconds(MAXIMUM_CALLBACK_TIMEOUT_IN_SEC + 1));
        assertThrows(InvalidRequestException.class, () -> webhookValidator.validate(webhook));
    }

    @Test
    void testInvalidCallbackTimeoutZero() {
        WebhookValidator webhookValidator = new WebhookValidator(getDefaultAppProperties(), getDefaultWebhookProperties());
        Webhook webhook = getWebhook(builder -> builder.callbackTimeoutSeconds(0));
        assertThrows(InvalidRequestException.class, () -> webhookValidator.validate(webhook));
    }

    @Test
    void testInvalidLocalhost() {
        WebhookValidator webhookValidator = new WebhookValidator(getDefaultAppProperties(), getDefaultWebhookProperties());
        Webhook webhook = getWebhook(builder -> builder.callbackUrl("http://localhost:8080/url"));
        assertThrows(InvalidRequestException.class, () -> webhookValidator.validate(webhook));
    }

    private Webhook getWebhook(Function<Webhook.WebhookBuilder, Webhook.WebhookBuilder> customizations) {
        Webhook.WebhookBuilder webhookBuilder = Webhook.builder()
                .name("someName")
                .callbackUrl("http://client/url")
                .channelUrl("http://hub/channel/channelName")
                .parallelCalls(1)
                .batch(WebhookType.SINGLE.name())
                .callbackTimeoutSeconds(CALLBACK_TIMEOUT_DEFAULT_IN_SEC)
                .heartbeat(false);
        return customizations.apply(webhookBuilder).build();
    }

    private AppProperties getDefaultAppProperties() {
        AppProperties appProperties = mock(AppProperties.class);
        when(appProperties.getHubType()).thenReturn("aws");
        return appProperties;
    }

    private WebhookProperties getDefaultWebhookProperties() {
        WebhookProperties webhookProperties = mock(WebhookProperties.class);
        when(webhookProperties.getCallbackTimeoutMinimum()).thenReturn(MINIMUM_CALLBACK_TIMEOUT_IN_SEC);
        when(webhookProperties.getCallbackTimeoutMaximum()).thenReturn(MAXIMUM_CALLBACK_TIMEOUT_IN_SEC);
        return webhookProperties;
    }
}