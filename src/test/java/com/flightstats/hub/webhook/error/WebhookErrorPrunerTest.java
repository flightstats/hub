package com.flightstats.hub.webhook.error;

import com.flightstats.hub.util.TimeUtil;
import org.junit.Test;

import java.time.Duration;
import java.util.List;
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

public class WebhookErrorPrunerTest {
    private final WebhookErrorStateService errorService = mock(WebhookErrorStateService.class);

    @Test
    public void testAddCleansUpOldestExcessiveErrors() {
        String webhookName = "webhookName";

        List<WebhookError> webhookErrors = setupErrorMocks(webhookName, 12, errorIndex -> Duration.ofMinutes(12 - errorIndex));

        WebhookErrorPruner webhookErrorPruner = new WebhookErrorPruner(errorService);

        List<WebhookError> prunedErrors = webhookErrorPruner.pruneErrors(webhookName, webhookErrors);

        assertEquals(newArrayList("error0", "error1"), prunedErrors.stream().map(WebhookError::getName).collect(toList()));

        verify(errorService, times(2)).delete(anyString(), anyString());
        verify(errorService).delete(webhookName, "error0");
        verify(errorService).delete(webhookName, "error1");
    }

    @Test
    public void testAddCleansUpErrorsOlderThanADay() {
        String webhookName = "webhookName";

        List<WebhookError> webhookErrors = setupErrorMocks(webhookName, 2, errorIndex -> Duration.ofDays(1 - errorIndex).plusMinutes(1));

        WebhookErrorPruner webhookErrorPruner = new WebhookErrorPruner(errorService);

        List<WebhookError> prunedErrors = webhookErrorPruner.pruneErrors(webhookName, webhookErrors);

        assertEquals(newArrayList("error0"), prunedErrors.stream().map(WebhookError::getName).collect(toList()));

        verify(errorService, times(1)).delete(anyString(), anyString());
        verify(errorService).delete(webhookName, "error0");
    }

    private List<WebhookError> setupErrorMocks(String webhookName, int numberOfErrors, Function<Integer, Duration> createdAtOffsetCalculator) {
        List<WebhookError> webhookErrors = IntStream.range(0, numberOfErrors)
                .mapToObj(number -> WebhookError.builder()
                        .name("error" + number)
                        .data(number + " message")
                        .creationTime(TimeUtil.now().minus(createdAtOffsetCalculator.apply(number).toMillis()))
                        .build())
                .collect(toList());
        when(errorService.getErrors(webhookName)).thenReturn(webhookErrors);
        return webhookErrors;
    }

}
