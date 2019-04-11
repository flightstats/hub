package com.flightstats.hub.testcase;

import com.flightstats.hub.model.Webhook;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Slf4j
public class WebhookErrorTest extends WebhookTest {

    private Webhook webhook;

    @Before
    public void setup() {
        super.setup();

        createChannel();

        webhook = buildWebhook().withParallelCalls(1).withMaxAttempts(0);
        super.insertAndVerifyWebhook(webhook);
    }

    @Test
    @SneakyThrows
    public void testThatNewlyCreatedWebhookDoesntReceiveStaleErrors() {
        // verify that errors are created only for the first item
        String firstUrl = super.addItemToChannel("{ name:\"item1\" }");
        super.callbackResource.errorOnCreate(
                (callback) -> callback.getUris().stream().anyMatch(
                        (uri) -> uri.contains(firstUrl)));
        verifyHasReceivedErrorForItem(firstUrl);

        // adding second item to channel
        super.addItemToChannel("{ name:\"item2\" }");

        // delete webhook
        log.info("Deleting webhook {}", webhookName);
        super.webhookResourceClient.delete(webhookName).execute();

        // re-add webhook
        log.info("Re-creating webhook {}", webhookName);
        super.insertAndVerifyWebhook(webhook);

        // add new item
        String thirdUrl = super.addItemToChannel("{ name:\"item3\" }");
        log.info("Adding new item to channel {}", thirdUrl);
        super.awaitItemCountSentToWebhook(Optional.of(thirdUrl), 1);

        // assert has no errors at all
        assertFalse(super.hasCallbackErrorInHub(webhookName, firstUrl));
        log.info("Verified no errors exist for callback in hub.");
    }

    @Test
    public void testSettingCursorBeyondErrorClearsErrorStateAndContinues() {
        // verify that errors are created only for the first item
        String firstUrl = super.addItemToChannel("{ name:\"item1\" }");
        super.callbackResource.errorOnCreate(
                (callback) -> callback.getUris().stream().anyMatch(
                        (uri) -> uri.contains(firstUrl)));
        verifyHasReceivedErrorForItem(firstUrl);

        // add new item
        String secondUrl = super.addItemToChannel("{ name:\"item2\" }");
        log.info("Adding new item to channel {}", secondUrl);

        // move cursor to firstUrl so it skips over the error
        log.info("Updating webhook to startItem {}", firstUrl);
        webhook = webhook.withStartItem(firstUrl);
        super.updateAndVerifyWebhook(webhook);

        // verify that you get the second item's data
        Optional<String> opt = super.awaitItemCountSentToWebhook(Optional.of(secondUrl), 1).stream().findFirst();
        assertTrue(opt.isPresent());
        assertEquals(secondUrl, opt.get());
        assertFalse(super.hasCallbackErrorInHub(webhookName, firstUrl));
    }

    @Override
    protected Logger getLog() {
        return log;
    }

    private void verifyHasReceivedErrorForItem(String firstUrl) {
        awaitHubHasCallbackErrorForItemPath(firstUrl);
        assertTrue(super.hasCallbackErrorInHub(webhookName, firstUrl));
        log.info("Found callback error for first item {} {}", webhookName, firstUrl);
    }

}
