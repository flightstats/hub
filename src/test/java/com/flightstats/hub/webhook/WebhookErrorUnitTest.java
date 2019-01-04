package com.flightstats.hub.webhook;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.util.TimeUtil;
import com.flightstats.hub.util.SafeZooKeeperUtils;
import com.flightstats.hub.webhook.error.WebhookErrorReaper;
import com.flightstats.hub.webhook.error.WebhookErrorService;
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
    private final ChannelService channelService = mock(ChannelService.class);
    private final WebhookErrorReaper errorReaper = new WebhookErrorReaper();
    private final WebhookErrorService errorService = mock(WebhookErrorService.class);

    private final static String ZK_BASE_PATH = "/GroupError";

    @Test
    public void testAddCleansUpOldestExcessiveErrors() {
        String webhookName = "webhookName";
        String errorMessage = "someError";

        setupErrorMocks(webhookName, 12, errorIndex -> Duration.ofMinutes(12 - errorIndex));

        WebhookError webhookError = new WebhookError(errorService, errorReaper, channelService);

        webhookError.add(webhookName, errorMessage);
        verify(errorService).add(webhookName, errorMessage);

        verify(errorService, times(2)).delete(anyString(), anyString());
        verify(errorService).delete(webhookName, "error0");
        verify(errorService).delete(webhookName, "error1");
    }

    @Test
    public void testAddCleansUpErrorsOlderThanADay() {
        String webhookName = "webhookName";
        String errorMessage = "someError";

        setupErrorMocks(webhookName, 2, errorIndex -> Duration.ofDays(1 - errorIndex).plusMinutes(1));

        WebhookError webhookError = new WebhookError(errorService, errorReaper, channelService);

        webhookError.add(webhookName, errorMessage);
        verify(errorService, times(1)).delete(anyString(), anyString());
        verify(errorService).delete(webhookName, "error0");
    }

    @Test
    public void testGetClearsExcessiveAndOldErrorsBeforeReturning() {
        String webhookName = "webhookName";
        setupErrorMocks(webhookName, 6, errorIndex -> Duration.ofDays(errorIndex % 2).plusMinutes(1));

        WebhookError webhookError = new WebhookError(errorService, errorReaper, channelService);

        List<String> errors = webhookError.get(webhookName);

        assertEquals(newArrayList("0 message", "2 message", "4 message"), errors);

        verify(errorService, times(3)).delete(anyString(), anyString());
        verify(errorService).delete(webhookName, "error1");
        verify(errorService).delete(webhookName, "error3");
        verify(errorService).delete(webhookName, "error5");
    }

    private void setupErrorMocks(String webhookName, int numberOfErrors, Function<Integer, Duration> createdAtOffsetCalculator) {
        List<com.flightstats.hub.webhook.error.WebhookError> webhookErrors = IntStream.range(0, numberOfErrors)
                .mapToObj(number -> com.flightstats.hub.webhook.error.WebhookError.builder()
                        .name("error" + number)
                        .data(number + " message")
                        .creationTime(TimeUtil.now().minus(createdAtOffsetCalculator.apply(number).toMillis()))
                        .build())
                .collect(toList());
        when(errorService.getErrors(webhookName)).thenReturn(webhookErrors);
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
