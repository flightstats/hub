package com.flightstats.hub.testcase;

import com.flightstats.hub.BaseTest;
import com.flightstats.hub.model.Webhook;
import javax.inject.Inject;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.flightstats.hub.model.ChannelContentStorageType.SINGLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class WebhookErrorTest extends BaseTest {
    @Inject
    private HubHelper hubHelper;
    @Inject
    private CallbackServerHelper callbackServerHelper;
    private Webhook webhook;
    private String channelName;
    private String webhookName;

    @BeforeEach
    public void before() {
        super.before();
        callbackServerHelper.startCallbackServer();

        this.channelName = generateRandomString();
        this.webhookName = generateRandomString();

        initChannelAndWebhook();
    }

    private Webhook buildWebhook() {
        return Webhook.builder()
                .name(webhookName)
                .channelUrl(hubHelper.getHubClientBaseUrl() + "channel/" + channelName)
                .callbackUrl(callbackServerHelper.getCallbackClientBaseUrl() + "callback/")
                .batch(SINGLE.toString())
                .build();
    }

    private void initChannelAndWebhook() {
        hubHelper.createChannel(channelName);

        createWebhook();
    }

    private void createWebhook() {
        webhook = buildWebhook().withParallelCalls(1).withMaxAttempts(0);
        hubHelper.insertAndVerifyWebhook(webhook);
    }

    private void verifyHasReceivedErrorForItem(String firstUrl) {
        hubHelper.awaitHubHasCallbackErrorForItemPath(webhookName, firstUrl);
        assertTrue(hubHelper.hasCallbackErrorInHub(webhookName, firstUrl));
        log.info("Found callback error for first item {} {}", webhookName, firstUrl);
    }

    @Test
    @SneakyThrows
    void testThatNewlyCreatedWebhookDoesntReceiveStaleErrors() {
        // verify that errors are created for the first item
        String firstUrl = hubHelper.addItemToChannel(channelName, "{ name:\"item1\" }");
        callbackServerHelper.errorOnCreate(
                (callback) -> callback.getUris().stream().anyMatch(
                        (uri) -> uri.contains(firstUrl)));
        verifyHasReceivedErrorForItem(firstUrl);

        // adding second item to channel
        hubHelper.addItemToChannel(channelName, "{ name:\"item2\" }");

        // delete webhook
        log.info("Deleting webhook {}", webhookName);
        hubHelper.deleteWebhook(webhookName);

        // re-add webhook
        log.info("Re-creating webhook {}", webhookName);
        hubHelper.insertAndVerifyWebhook(webhook);

        // add new item and wait to hear about it
        String thirdUrl = hubHelper.addItemToChannel(channelName, "{ name:\"item3\" }");
        log.info("Adding new item to channel {}", thirdUrl);
        callbackServerHelper.awaitItemCountSentToWebhook(webhookName, Optional.of(thirdUrl), 1);

        // assert has no errors at all
        assertFalse(hubHelper.hasCallbackErrorInHub(webhookName, firstUrl));
        log.info("Verified no errors exist for callback in hub.");
    }

    @Test
    void testSettingCursorBeyondErrorClearsErrorStateAndContinues() {
        // verify that errors are created for the first item
        String firstUrl = hubHelper.addItemToChannel(channelName, "{ name:\"item1\" }");
        callbackServerHelper.errorOnCreate(
                (callback) -> callback.getUris().stream().anyMatch(
                        (uri) -> uri.contains(firstUrl)));
        verifyHasReceivedErrorForItem(firstUrl);

        // add new item
        String secondUrl = hubHelper.addItemToChannel(channelName, "{ name:\"item2\" }");
        log.info("Adding new item to channel {}", secondUrl);

        // move cursor to firstUrl so it skips over the error
        log.info("Updating webhook to startItem {}", firstUrl);
        webhook = webhook.withStartItem(firstUrl);
        hubHelper.updateAndVerifyWebhook(webhook);

        // verify that you get the second item's data
        log.info("Verifying that data for 2nd item was sent {}", secondUrl);
        Optional<String> opt = callbackServerHelper.awaitItemCountSentToWebhook(webhookName, Optional.of(secondUrl), 1).stream().findFirst();
        assertTrue(opt.isPresent());
        assertEquals(secondUrl, opt.get());

        // verify that no errors exist on the hub
        log.info("Verifying that no errors exist on the hub for webhook {}", webhookName);
        assertFalse(hubHelper.hasCallbackErrorInHub(webhookName, firstUrl));
    }

    @Test
    @SneakyThrows
    void testWebhookCursorUpdateLoopDoesntCorruptState() {
        for (int i = 1; i <= 3; i++) {
            log.info("Iteration {}", i);
            this.testSettingCursorBeyondErrorClearsErrorStateAndContinues();
            hubHelper.deleteWebhook(webhookName);
            this.createWebhook();
            log.info("Completed iteration {}", i);
        }
    }

    @Test
    @SneakyThrows
    void testWebhookRecreationLoopDoesntCorruptState() {
        for (int i = 1; i <= 3; i++) {
            log.info("Iteration {}", i);
            this.testThatNewlyCreatedWebhookDoesntReceiveStaleErrors();
            log.info("Completed iteration {}", i);
        }
    }

    @AfterEach
    void after() {
        hubHelper.deleteChannelAndWebhook(channelName, webhookName);
        callbackServerHelper.stopCallbackServer();
    }

}