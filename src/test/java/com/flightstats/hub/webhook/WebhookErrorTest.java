package com.flightstats.hub.webhook;

import com.flightstats.hub.test.TestMain;
import com.google.inject.Injector;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class WebhookErrorTest {

    private static WebhookError webhookError;

    @BeforeClass
    public static void setUpClass() throws Exception {
//        ChannelService channelService = mock(ChannelService.class);
//        CuratorFramework curator = TestApplication.startZooKeeper();
//        webhookError = new WebhookError(curator, channelService);
        Injector injector = TestMain.start();
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