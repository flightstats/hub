package com.flightstats.hub.testcase;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

@Slf4j
public class WebhookErrorTest extends WebhookTest {

    @Before
    public void setup() {
        super.setup();
    }

    @Test
    public void testThatDeletingHookDeletesErrors() {
        createChannel();

        super.addWebhook(buildWebhook().withParallelCalls(1).withMaxAttempts(0));
        super.callbackResource.errorOnNextCreate();

        super.addItemToChannel("{ name:\"item1\" }");
        for(;;);
        // verify error exists

        // delete webhook
        // re-add webhook
        // verify errors are clear

    }

    @Test
    public void testThatSettingHookCursorBeyondErrorClearsErrorState() {

    }

    @Override
    protected Logger getLog() {
        return log;
    }
}
