package com.flightstats.hub.webhook;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.test.IntegrationTestSetup;
import com.flightstats.hub.util.SafeZooKeeperUtils;
import com.flightstats.hub.webhook.error.WebhookErrorPruner;
import com.flightstats.hub.webhook.error.WebhookErrorRepository;
import org.apache.curator.framework.CuratorFramework;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class WebhookErrorServiceTest {

    private static WebhookErrorService webhookErrorService;

    @BeforeAll
    static void setUpClass() {
        ChannelService channelService = mock(ChannelService.class);
        CuratorFramework curator = IntegrationTestSetup.run().getZookeeperClient();
        SafeZooKeeperUtils zooKeeperUtils = new SafeZooKeeperUtils(curator);
        WebhookErrorRepository.ErrorNodeNameGenerator errorNameGenerator = new WebhookErrorRepository.ErrorNodeNameGenerator();
        WebhookErrorRepository webhookErrorRepository = new WebhookErrorRepository(zooKeeperUtils, errorNameGenerator);
        WebhookErrorPruner webhookErrorPruner = new WebhookErrorPruner(webhookErrorRepository);
        webhookErrorService = new WebhookErrorService(webhookErrorRepository, webhookErrorPruner, channelService);
    }

    @Test
    void testErrors() {
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