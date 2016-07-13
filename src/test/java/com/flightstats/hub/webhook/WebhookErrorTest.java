package com.flightstats.hub.webhook;

import com.flightstats.hub.test.Integration;
import org.apache.curator.framework.CuratorFramework;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class WebhookErrorTest {

    private static WebhookError webhookError;

    @BeforeClass
    public static void setUpClass() throws Exception {
        CuratorFramework curator = Integration.startZooKeeper();
        webhookError = new WebhookError(curator);
    }

    @Test
    public void testErrors() {
        for (int i = 0; i < 20; i++) {
            webhookError.add("testErrors", "stuff" + i);
        }
        List<String> errors = webhookError.get("testErrors");
        assertEquals(10, errors.size());

        /*
        //todo - gfm - 7/13/16 - turn off for now
        for (int i = 0; i < 10; i++) {

            assertEquals("stuff" + (i + 10), errors.get(i));
        }*/
    }

}