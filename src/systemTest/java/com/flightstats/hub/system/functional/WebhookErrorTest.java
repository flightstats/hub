package com.flightstats.hub.system.functional;

import com.flightstats.hub.kubernetes.HubLifecycle;
import com.flightstats.hub.model.Webhook;
import javax.inject.Inject;

import com.flightstats.hub.model.WebhookCallbackSetting;
import com.flightstats.hub.system.ModelBuilder;
import com.flightstats.hub.system.config.DependencyInjector;
import com.flightstats.hub.system.service.CallbackService;
import com.flightstats.hub.system.service.ChannelService;
import com.flightstats.hub.system.service.WebhookService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;

import java.util.Comparator;

import static com.flightstats.hub.util.StringUtils.randomAlphaNumeric;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WebhookErrorTest extends DependencyInjector {
    @Inject
    private ChannelService channelService;
    @Inject
    private WebhookService webhookService;
    @Inject
    private CallbackService callbackService;
    @Inject
    private HubLifecycle hubLifecycle;
    @Inject
    private ModelBuilder modelBuilder;

    private Webhook webhook;
    private String channelName;
    private String webhookName;

    @BeforeAll
    void hubSetup() {
        hubLifecycle.setup();
    }

    @BeforeEach
    void before() {
        this.channelName = randomAlphaNumeric(10);
        this.webhookName = randomAlphaNumeric(10);

        initChannelAndWebhook();
    }

    private Webhook buildWebhook() {
        return modelBuilder.webhookBuilder()
                .channelName(channelName)
                .webhookName(webhookName)
                .build();
    }

    private void initChannelAndWebhook() {
        channelService.create(channelName);

        createWebhook();
    }

    private void createWebhook() {
        webhook = buildWebhook().withParallelCalls(1).withMaxAttempts(0);
        webhookService.insertAndVerify(webhook);
    }

    private void verifyHasReceivedErrorForItem(String firstUrl) {
        callbackService.awaitHubHasCallbackErrorForItemPath(webhookName, firstUrl);
        assertTrue(callbackService.hasCallbackErrorInHub(webhookName, firstUrl));
        log.info("Found callback error for first item {} {}", webhookName, firstUrl);
    }

    @RepeatedTest(3)
    void testThatNewlyCreatedWebhookDoesntReceiveStaleErrors() {
        // verify that errors are created for the first item
        WebhookCallbackSetting item = WebhookCallbackSetting.builder()
                .failureStatusCode(500)
                .build();
        String firstUrl = channelService.addItem(channelName, item);

        verifyHasReceivedErrorForItem(firstUrl);

        // adding second item to channel
        channelService.addItem(channelName, "{ name:\"item2\" }");

        // delete webhook
        log.info("Deleting webhook {}", webhookName);
        webhookService.delete(webhookName);

        // re-add webhook
        log.info("Re-creating webhook {}", webhookName);
        webhookService.insertAndVerify(webhook);
        assertTrue(callbackService.isErrorListEventuallyCleared(webhookName));

        // add new item and wait to hear about it
        String thirdUrl = channelService.addItem(channelName, "{ name:\"item3\" }");
        log.info("Adding new item to channel {}", thirdUrl);
        callbackService.awaitItemCountSentToWebhook(webhookName, 1);

        // assert has no errors at all
        assertTrue(callbackService.getCallbackErrorsInHub(webhookName).isEmpty());
        log.info("Verified no errors exist for callback in hub.");
    }

    @RepeatedTest(3)
    void testSettingCursorBeyondErrorClearsErrorStateAndContinues() {
        // verify that errors are created for the first item
        WebhookCallbackSetting item = WebhookCallbackSetting.builder()
                .failureStatusCode(500)
                .build();
        String firstUrl = channelService.addItem(channelName, item);

        verifyHasReceivedErrorForItem(firstUrl);

        // add new item
        String secondUrl = channelService.addItem(channelName, "{ name:\"item2\" }");
        log.info("Adding new item to channel {}", secondUrl);

        // move cursor to firstUrl so it skips over the error
        log.info("Updating webhook to startItem {}", firstUrl);
        webhook = webhook.withStartItem(firstUrl);
        webhookService.updateAndVerify(webhook);

        // verify that you get the second item's data
        log.info("Verifying that data for 2nd item was sent {}", secondUrl);
        String opt = callbackService.awaitItemCountSentToWebhook(webhookName, 2).stream()
                .min(Comparator.reverseOrder())
                .orElseThrow(AssertionError::new);
        assertEquals(secondUrl, opt);

        // verify that no errors exist on the hub
        log.info("Verifying that no errors exist on the hub for webhook {}", webhookName);
        assertTrue(callbackService.isErrorListEventuallyCleared(webhookName));
    }

    @AfterEach
    void after() {
        this.channelService.delete(channelName);
        this.webhookService.delete(webhookName);
    }

    @AfterAll
    void hubCleanup() {
        hubLifecycle.cleanup();
    }

}
