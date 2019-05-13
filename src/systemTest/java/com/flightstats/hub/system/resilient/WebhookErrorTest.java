package com.flightstats.hub.system.resilient;

import com.flightstats.hub.model.Webhook;
import com.flightstats.hub.system.ModelBuilder;
import com.flightstats.hub.system.config.DependencyInjector;
import com.flightstats.hub.system.service.CallbackService;
import com.flightstats.hub.system.service.ChannelService;
import com.flightstats.hub.system.service.WebhookService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Optional;

import static com.flightstats.hub.kubernetes.ServiceName.CALLBACK_SERVER;
import static com.flightstats.hub.model.ChannelContentStorageType.SINGLE;
import static com.flightstats.hub.util.StringUtils.randomAlphaNumeric;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class WebhookErrorTest extends DependencyInjector {
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
    public void hubSetup() {
        hubLifecycle.setup();
    }

    @BeforeEach
    public void before() {
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

    @Test
    @SneakyThrows
    public void testThatNewlyCreatedWebhookDoesntReceiveStaleErrors() {
        // verify that errors are created for the first item
        String firstUrl = channelService.addItem(channelName, "{ name:\"item1\" }");

        //Inject Fault in the hub by deleting call back service
        hubLifecycle.serviceDelete(Arrays.asList(CALLBACK_SERVER.value()));

        verifyHasReceivedErrorForItem(firstUrl);

        // adding second item to channel
        channelService.addItem(channelName, "{ name:\"item2\" }");

        // delete webhook
        log.info("Deleting webhook {}", webhookName);
        channelService.delete(webhookName);

        // re-add webhook
        log.info("Re-creating webhook {}", webhookName);
        webhookService.insertAndVerify(webhook);

        // add new item and wait to hear about it
        String thirdUrl = channelService.addItem(channelName, "{ name:\"item3\" }");
        log.info("Adding new item to channel {}", thirdUrl);
        callbackService.awaitItemCountSentToWebhook(webhookName, Optional.of(thirdUrl), 1);

        // assert has no errors at all
        assertFalse(callbackService.hasCallbackErrorInHub(webhookName, firstUrl));
        log.info("Verified no errors exist for callback in hub.");
    }

    @Test
    public void testSettingCursorBeyondErrorClearsErrorStateAndContinues() {
        // verify that errors are created for the first item
        String firstUrl = channelService.addItem(channelName, "{ name:\"item1\" }");

        //Inject Fault in the hub by deleting call back service
        hubLifecycle.serviceDelete(Arrays.asList(CALLBACK_SERVER.value()));

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
        Optional<String> opt = callbackService.awaitItemCountSentToWebhook(webhookName, Optional.of(secondUrl), 1).stream().findFirst();
        assertTrue(opt.isPresent());
        assertEquals(secondUrl, opt.get());

        // verify that no errors exist on the hub
        log.info("Verifying that no errors exist on the hub for webhook {}", webhookName);
        assertFalse(callbackService.hasCallbackErrorInHub(webhookName, firstUrl));
    }

    @Test
    @SneakyThrows
    public void testWebhookCursorUpdateLoopDoesntCorruptState() {
        for (int i = 1; i <= 3; i++) {
            log.info("Iteration {}", i);
            this.testSettingCursorBeyondErrorClearsErrorStateAndContinues();
            webhookService.delete(webhookName);
            this.createWebhook();
            log.info("Completed iteration {}", i);
        }
    }

    @Test
    @SneakyThrows
    public void testWebhookRecreationLoopDoesntCorruptState() {
        for (int i = 1; i <= 3; i++) {
            log.info("Iteration {}", i);
            this.testThatNewlyCreatedWebhookDoesntReceiveStaleErrors();
            log.info("Completed iteration {}", i);
        }
    }

    @AfterEach
    public void after() {
        this.channelService.delete(channelName);
        this.webhookService.delete(webhookName);
    }

    @AfterAll
    public void hubCleanup() {
        hubLifecycle.cleanup();
    }

}