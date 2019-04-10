package com.flightstats.hub.testcase;

import com.flightstats.hub.model.Webhook;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Slf4j
public class WebhookErrorTest extends WebhookTest {

    @Before
    public void setup() {
        super.setup();
    }

    @Test
    @SneakyThrows
    public void testThatDeletingHookDeletesErrors() {
        createChannel();

        Webhook webhook = buildWebhook().withParallelCalls(1).withMaxAttempts(0);
        super.addWebhook(webhook);

        // verify that errors are created only for the first item
        String firstUrl = super.addItemToChannel("{ name:\"item1\" }");
        // there is room here for an aggressive callback to beat the error predicate registration
        super.callbackResource.errorOnCreate(
                (callback) -> callback.getUris().stream().anyMatch(
                        (uri) -> uri.contains(firstUrl)));

        Thread.sleep(5*1000);  // awaitHasFailedCallBackForItem(first)
        assertTrue(super.hasCallbackErrorForFullUrl(webhookName, firstUrl));
        String secondUrl = super.addItemToChannel("{ name:\"item2\" }");
        Thread.sleep(5*1000);  // awaitHasSuccessfullyCalledBackForItem(second)
        assertFalse(super.hasCallbackErrorForFullUrl(webhookName, secondUrl));

        // delete webhook
        super.webhookResourceClient.delete(webhookName);
        Thread.sleep(5*1000);  // awaitWebhookNoLongerExists
        // re-add webhook
        super.addWebhook(webhook);
        Thread.sleep(5*1000);  // awaitHasSuccessfullyCalledBackForItem(third)
        // verify errors are clear
        // assert has no errors at all
        assertFalse(super.hasCallbackErrorForFullUrl(webhookName, firstUrl));
        for (;;);

        /*
        --- happy path
        insert record at getTime()
        add predicate to fail if path is start time, down to second
        wait for next second
        add new item
        verify that errors continue for first item, but second does not error
        --- case one
        delete webhook
        re-add webhook
        verify that no errors are heard  (should fail currently)
        add item
        verify that no errors are heard  (should fail currently)
        --- case two
        adjust webhook starting key to first second + 1
        verify that no subsequent errors are heard  (should fail currently)
        add item
        verify that no errors are heard  (should fail currently)
         */
    }

    @Test
    public void testThatSettingHookCursorBeyondErrorClearsErrorState() {

    }

    @Override
    protected Logger getLog() {
        return log;
    }
}
