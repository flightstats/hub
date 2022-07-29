package com.flightstats.hub.system.functional;

import com.flightstats.hub.model.Webhook;
import com.flightstats.hub.model.WebhookCallbackSetting;
import com.flightstats.hub.system.ModelBuilder;
import com.flightstats.hub.system.extension.TestSuiteClassWrapper;
import com.flightstats.hub.system.service.CallbackService;
import com.flightstats.hub.system.service.ChannelConfigService;
import com.flightstats.hub.system.service.ChannelItemCreator;
import com.flightstats.hub.system.service.WebhookService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import javax.inject.Inject;
import java.util.Collections;

import static com.flightstats.hub.system.SystemTestUtils.randomChannelName;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@Execution(ExecutionMode.SAME_THREAD)
class WebhookErrorTest extends TestSuiteClassWrapper {
    @Inject
    private ChannelConfigService channelConfigService;
    @Inject
    private ChannelItemCreator itemCreator;
    @Inject
    private WebhookService webhookService;
    @Inject
    private CallbackService callbackService;
    @Inject
    private ModelBuilder modelBuilder;

    private String nameSeed;
    private Webhook webhook;
    private String channelName;
    private String webhookName;

    @BeforeAll
    void hubSetup() {
        nameSeed = randomChannelName(5);
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
        channelConfigService.createWithDefaults(channelName);

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
        String firstUrl = itemCreator.addItem(channelName, item).getItemUrl();

        verifyHasReceivedErrorForItem(firstUrl);

        // adding second item to channel
        itemCreator.addItem(channelName, "{ name:\"item2\" }");

        // delete webhook
        log.info("Deleting webhook {}", webhookName);
        webhookService.delete(webhookName);

        // re-add webhook
        log.info("Re-creating webhook {}", webhookName);
        webhookService.insertAndVerify(webhook);
        assertTrue(callbackService.isErrorListEventuallyCleared(webhookName));

        // add new item and wait to hear about it
        String thirdUrl = itemCreator.addItem(channelName, "{ name:\"item3\" }").getItemUrl();
        log.info("Adding new item to channel {}", thirdUrl);
        assertTrue(callbackService.areItemsEventuallySentToWebhook(webhookName, Collections.singletonList(thirdUrl)));

        // assert has no errors at all
        assertTrue(callbackService.getCallbackErrorsInHub(webhookName).isEmpty());
        log.info("Verified no errors exist for callback in hub.");
    }

    @RepeatedTest(3)
    @SneakyThrows
    void testSettingCursorBeyondErrorClearsErrorStateAndContinues() {
        // verify that errors are created for the first item
        WebhookCallbackSetting item = WebhookCallbackSetting.builder()
                .failureStatusCode(500)
                .build();
        String firstUrl = itemCreator.addItem(channelName, item).getItemUrl();

        verifyHasReceivedErrorForItem(firstUrl);

        // add new item
        String secondUrl = itemCreator.addItem(channelName, "{ name:\"item2\" }").getItemUrl();
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
    }

    @AfterEach
    void after(TestInfo testInfo, RepetitionInfo repetitionInfo) {
        if (repetitionInfo.getCurrentRepetition() == repetitionInfo.getTotalRepetitions()) {
            log.info("deleting {} and {}", channelName, webhookName);
            this.channelConfigService.delete(channelName);
            this.webhookService.delete(webhookName);
        }
    }
}
