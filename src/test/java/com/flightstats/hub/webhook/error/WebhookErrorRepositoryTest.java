package com.flightstats.hub.webhook.error;

import com.flightstats.hub.util.SafeZooKeeperUtils;
import com.flightstats.hub.util.TimeUtil;
import org.apache.zookeeper.data.Stat;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WebhookErrorRepositoryTest {
    private final SafeZooKeeperUtils zooKeeperUtils = mock(SafeZooKeeperUtils.class);
    private final WebhookErrorRepository.ErrorNodeNameGenerator errorNameGenerator = mock(WebhookErrorRepository.ErrorNodeNameGenerator.class);

    private final static String ZK_BASE_PATH = "/GroupError";
    public static final String WEBHOOK_NAME = "webhookName";

    @Test
    public void testAdd() {
        String errorMessage = "someError";
        String newErrorId = "error10";

        when(errorNameGenerator.generateName())
                .thenReturn(newErrorId);

        WebhookErrorRepository errorRepo = new WebhookErrorRepository(zooKeeperUtils, errorNameGenerator);
        errorRepo.add(WEBHOOK_NAME, errorMessage);
        verify(zooKeeperUtils).createData(errorMessage.getBytes(), ZK_BASE_PATH, WEBHOOK_NAME, newErrorId);
    }

    @Test
    public void testDeleteWebhook() {
        WebhookErrorRepository errorRepo = new WebhookErrorRepository(zooKeeperUtils, errorNameGenerator);
        errorRepo.deleteWebhook(WEBHOOK_NAME);
        verify(zooKeeperUtils).deletePathAndChildren(ZK_BASE_PATH, WEBHOOK_NAME);
    }

    @Test
    public void testDelete() {
        String errorId = "error10";

        WebhookErrorRepository errorRepo = new WebhookErrorRepository(zooKeeperUtils, errorNameGenerator);
        errorRepo.delete(WEBHOOK_NAME, errorId);
        verify(zooKeeperUtils).deletePathInBackground(ZK_BASE_PATH, WEBHOOK_NAME, errorId);
    }

    @Test
    public void testGetWebhooks() {
        List<String> webhooks = newArrayList("1", "2", "3");
        when(zooKeeperUtils.getChildren(ZK_BASE_PATH))
                .thenReturn(webhooks);

        WebhookErrorRepository errorRepo = new WebhookErrorRepository(zooKeeperUtils, errorNameGenerator);

        assertEquals(newHashSet(webhooks), errorRepo.getWebhooks());
    }

    @Test
    public void testGetErrors() {
        List<String> errors = newArrayList("1", "2", "3");
        when(zooKeeperUtils.getChildren(ZK_BASE_PATH, WEBHOOK_NAME))
                .thenReturn(errors);

        DateTime createdStart = TimeUtil.now();
        when(zooKeeperUtils.getDataWithStat(ZK_BASE_PATH, WEBHOOK_NAME, "1"))
                .thenReturn(buildData("error1", createdStart.plusMinutes(1)));
        when(zooKeeperUtils.getDataWithStat(ZK_BASE_PATH, WEBHOOK_NAME, "2"))
                .thenReturn(Optional.empty());
        when(zooKeeperUtils.getDataWithStat(ZK_BASE_PATH, WEBHOOK_NAME, "3"))
                .thenReturn(buildData("error3", createdStart.plusMinutes(3)));

        WebhookErrorRepository errorRepo = new WebhookErrorRepository(zooKeeperUtils, errorNameGenerator);

        List<WebhookError> expectedErrors = newArrayList(
                WebhookError.builder().name("1").data("error1").creationTime(createdStart.plusMinutes(1).withZone(DateTimeZone.getDefault())).build(),
                WebhookError.builder().name("3").data("error3").creationTime(createdStart.plusMinutes(3).withZone(DateTimeZone.getDefault())).build()
        );
        assertEquals(expectedErrors, errorRepo.getErrors(WEBHOOK_NAME));
    }

    private Optional<SafeZooKeeperUtils.DataWithStat> buildData(String errorMessage, DateTime ctime) {
        Stat stat = new Stat();
        stat.setCtime(ctime.getMillis());
        return Optional.of(SafeZooKeeperUtils.DataWithStat.builder()
                .data(errorMessage)
                .stat(stat)
                .build());
    }
}
