package com.flightstats.hub.system.functional;

import com.flightstats.hub.kubernetes.HubLifecycle;
import com.flightstats.hub.kubernetes.HubLifecycleSuiteExtension;
import com.flightstats.hub.model.Webhook;
import javax.inject.Inject;

import com.flightstats.hub.model.WebhookCallbackSetting;
import com.flightstats.hub.system.ModelBuilder;
import com.flightstats.hub.system.config.DependencyInjector;
import com.flightstats.hub.system.config.GuiceInjectionExtension;
import com.flightstats.hub.system.service.CallbackService;
import com.flightstats.hub.system.service.ChannelService;
import com.flightstats.hub.system.service.WebhookService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;

import static com.flightstats.hub.util.StringUtils.randomAlphaNumeric;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith({ GuiceInjectionExtension.class, HubLifecycleSuiteExtension.class})
class WebhookErrorTest extends DependencyInjector {
    private final ChannelService channelService;
    private final WebhookService webhookService;
    private final CallbackService callbackService;
    private final ModelBuilder modelBuilder;

    private String nameSeed;
    private Webhook webhook;
    private String channelName;
    private String webhookName;

    WebhookErrorTest(ChannelService channelService, WebhookService webhookService, CallbackService callbackService, ModelBuilder modelBuilder) {
        this.channelService = channelService;
        this.webhookService = webhookService;
        this.callbackService = callbackService;
        this.modelBuilder = modelBuilder;
    }

    @BeforeAll
    void hubSetup() {
        nameSeed = randomAlphaNumeric(5);
    }

    @BeforeEach
    void beforeEach(TestInfo testInfo, RepetitionInfo repetitionInfo) {
        String methodName = testInfo.getTestMethod().get().getName();
        this.channelName = (nameSeed + "channel" + methodName).substring(0, 30);
        this.webhookName = (nameSeed + "webhook" + methodName).substring(0, 30);

        if (repetitionInfo.getCurrentRepetition() == 1) {
            log.info("creating " + channelName + " and " + webhookName);
            initChannelAndWebhook();
        }
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
        fail();
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
        assertTrue(callbackService.areItemsEventuallySentToWebhook(webhookName, Collections.singletonList(thirdUrl)));

        // assert has no errors at all
        assertTrue(callbackService.getCallbackErrorsInHub(webhookName).isEmpty());
        log.info("Verified no errors exist for callback in hub.");
    }

    @RepeatedTest(3)
    void testSettingCursorBeyondErrorClearsErrorStateAndContinues() {
        fail();
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
        assertTrue(callbackService.areItemsEventuallySentToWebhook(webhookName, Collections.singletonList(secondUrl)));

        // verify that no errors exist on the hub
        log.info("Verifying that no errors exist on the hub for webhook {}", webhookName);
        assertTrue(callbackService.isErrorListEventuallyCleared(webhookName));

        log.info("deleting " + channelName + " and " + webhookName);
        this.channelService.delete(channelName);
        this.webhookService.delete(webhookName);

        log.info("creating " + channelName + " and " + webhookName);
        initChannelAndWebhook();
    }

    @AfterEach
    void after(TestInfo testInfo, RepetitionInfo repetitionInfo) {
        if (repetitionInfo != null && repetitionInfo.getCurrentRepetition() == repetitionInfo.getTotalRepetitions()) {
            log.info("deleting " + channelName + " and " + webhookName);
            this.channelService.delete(channelName);
            this.webhookService.delete(webhookName);
        }
    }
}
