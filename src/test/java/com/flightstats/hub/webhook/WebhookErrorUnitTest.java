package com.flightstats.hub.webhook;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.util.TimeUtil;
import com.flightstats.hub.util.SafeZooKeeperUtils;
import org.apache.zookeeper.data.Stat;
import org.junit.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.IntStream;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WebhookErrorUnitTest {
    private final SafeZooKeeperUtils zooKeeperUtils = mock(SafeZooKeeperUtils.class);
    private final ChannelService channelService = mock(ChannelService.class);
    private final WebhookError.ErrorNodeNameGenerator errorNameGenerator = mock(WebhookError.ErrorNodeNameGenerator.class);
    private final WebhookError.WebhookErrorReaper errorReaper = new WebhookError.WebhookErrorReaper(zooKeeperUtils);

    private final static String ZK_BASE_PATH = "/GroupError";

    @Test
    public void testAddCleansUpOldestExcessiveErrors() {
        String webhookName = "webhookName";
        String errorMessage = "someError";
        String newErrorId = "error10";

        when(errorNameGenerator.generateName())
                .thenReturn(newErrorId);
        setupErrorMocks(webhookName, 12, errorIndex -> Duration.ofMinutes(12 - errorIndex));

        WebhookError webhookError = new WebhookError(zooKeeperUtils, channelService, errorNameGenerator, errorReaper);

        webhookError.add(webhookName, errorMessage);
        verify(zooKeeperUtils).createData(errorMessage.getBytes(), ZK_BASE_PATH, webhookName, newErrorId);
        verify(zooKeeperUtils, times(2)).deletePathInBackground(anyString(), anyString(), anyString());
        verify(zooKeeperUtils).deletePathInBackground(ZK_BASE_PATH, webhookName, "error0");
        verify(zooKeeperUtils).deletePathInBackground(ZK_BASE_PATH, webhookName, "error1");
    }

    @Test
    public void testAddCleansUpErrorsOlderThanADay() {
        String webhookName = "webhookName";
        String errorMessage = "someError";
        String newErrorId = "error1";

        when(errorNameGenerator.generateName())
                .thenReturn(newErrorId);
        setupErrorMocks(webhookName, 2, errorIndex -> Duration.ofDays(1 - errorIndex).plusMinutes(1));

        WebhookError webhookError = new WebhookError(zooKeeperUtils, channelService, errorNameGenerator, errorReaper);

        webhookError.add(webhookName, errorMessage);
        verify(zooKeeperUtils).createData(errorMessage.getBytes(), ZK_BASE_PATH, webhookName, newErrorId);
        verify(zooKeeperUtils, times(1)).deletePathInBackground(anyString(), anyString(), anyString());
        verify(zooKeeperUtils).deletePathInBackground(ZK_BASE_PATH, webhookName, "error0");
    }

    @Test
    public void testGetClearsExcessiveAndOldErrorsBeforeReturning() {
        String webhookName = "webhookName";
        setupErrorMocks(webhookName, 6, errorIndex -> Duration.ofDays(errorIndex % 2).plusMinutes(1));

        WebhookError webhookError = new WebhookError(zooKeeperUtils, channelService, errorNameGenerator, errorReaper);

        List<String> errors = webhookError.get(webhookName);

        assertEquals(newArrayList("0 message", "2 message", "4 message"), errors);

        verify(zooKeeperUtils, times(3)).deletePathInBackground(anyString(), anyString(), anyString());
        verify(zooKeeperUtils).deletePathInBackground(ZK_BASE_PATH, webhookName, "error1");
        verify(zooKeeperUtils).deletePathInBackground(ZK_BASE_PATH, webhookName, "error3");
        verify(zooKeeperUtils).deletePathInBackground(ZK_BASE_PATH, webhookName, "error5");
    }

    private void setupErrorMocks(String webhookName, int numberOfErrors, Function<Integer, Duration> createdAtOffsetCalculator) {
        List<String> errors = IntStream.range(0, numberOfErrors).mapToObj(String::valueOf).map(number ->  "error" + number).collect(toList());
        when(zooKeeperUtils.getChildren(ZK_BASE_PATH, webhookName)).thenReturn(errors);
        IntStream.range(0, numberOfErrors).forEach(error ->
                when(zooKeeperUtils.getDataWithStat(ZK_BASE_PATH, webhookName, errors.get(error)))
                        .thenReturn(buildData(error + " message", TimeUtil.now().minus(createdAtOffsetCalculator.apply(error).toMillis()).getMillis())));
    }

    private Optional<SafeZooKeeperUtils.DataWithStat> buildData(String errorMessage, long ctime) {
        Stat stat = new Stat();
        stat.setCtime(ctime);
        return Optional.of(SafeZooKeeperUtils.DataWithStat.builder()
                .data(errorMessage)
                .stat(stat)
                .build());
    }
}
