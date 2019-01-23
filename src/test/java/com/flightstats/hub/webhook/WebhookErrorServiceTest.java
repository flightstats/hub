package com.flightstats.hub.webhook;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.test.Integration;
import com.flightstats.hub.util.SafeZooKeeperUtils;
import com.flightstats.hub.webhook.error.WebhookErrorPruner;
import com.flightstats.hub.webhook.error.WebhookErrorStateService;
import org.apache.curator.framework.CuratorFramework;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class WebhookErrorServiceTest {

    private static WebhookErrorService webhookErrorService;

    @BeforeClass
    public static void setUpClass() throws Exception {
        ChannelService channelService = mock(ChannelService.class);
        CuratorFramework curator = Integration.startZooKeeper();
        SafeZooKeeperUtils zooKeeperUtils = new SafeZooKeeperUtils(curator);
        WebhookErrorStateService.ErrorNodeNameGenerator errorNameGenerator = new WebhookErrorStateService.ErrorNodeNameGenerator();
        WebhookErrorStateService webhookErrorStateService = new WebhookErrorStateService(zooKeeperUtils, errorNameGenerator);
        WebhookErrorPruner webhookErrorPruner = new WebhookErrorPruner(webhookErrorStateService);
        webhookErrorService = new WebhookErrorService(webhookErrorStateService, webhookErrorPruner, channelService);
    }

    @Test
    public void testErrors() {
        for (int i = 0; i < 20; i++) {
            webhookErrorService.add("testErrors", "stuff" + i);
        }
        List<String> errors = webhookErrorService.lookup("testErrors");
        assertEquals(10, errors.size());

        /*
        //todo - gfm - 7/13/16 - turn off for now
        for (int i = 0; i < 10; i++) {

            assertEquals("stuff" + (i + 10), errors.get(i));
        }*/
    }
}