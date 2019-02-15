package com.flightstats.hub.webhook.error;

import com.flightstats.hub.util.TimeUtil;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WebhookErrorPrunerTest {
    private final WebhookErrorRepository errorRepo = mock(WebhookErrorRepository.class);
    private final DateTime nowish = TimeUtil.now();

    @Test
    public void testAddCleansUpOldestExcessiveErrors() {
        String webhookName = "webhookName";

        // insert errors in the list in reverse order newest-to-oldest
        List<WebhookError> webhookErrors = setupErrorMocks(webhookName, 12, nowish::minusMinutes);

        WebhookErrorPruner webhookErrorPruner = new WebhookErrorPruner(errorRepo);

        List<WebhookError> prunedErrors = webhookErrorPruner.pruneErrors(webhookName, webhookErrors);

        assertEquals(newArrayList("error11", "error10"), prunedErrors.stream().map(WebhookError::getName).collect(toList()));

        verify(errorRepo, times(2)).delete(anyString(), anyString());
        verify(errorRepo).delete(webhookName, "error10");
        verify(errorRepo).delete(webhookName, "error11");
    }

    @Test
    public void testAddCleansUpErrorsOlderThanADay() {
        String webhookName = "webhookName";

        List<WebhookError> webhookErrors = setupErrorMocks(webhookName, 2, errorIndex -> nowish.minus(Duration.ofDays(1 - errorIndex).plusMinutes(1).toMillis()));

        WebhookErrorPruner webhookErrorPruner = new WebhookErrorPruner(errorRepo);

        List<WebhookError> prunedErrors = webhookErrorPruner.pruneErrors(webhookName, webhookErrors);

        assertEquals(newArrayList("error0"), prunedErrors.stream().map(WebhookError::getName).collect(toList()));

        verify(errorRepo, times(1)).delete(anyString(), anyString());
        verify(errorRepo).delete(webhookName, "error0");
    }

    private List<WebhookError> setupErrorMocks(String webhookName, int numberOfErrors, Function<Integer, DateTime> createdAtOffsetCalculator) {
        List<WebhookError> webhookErrors = IntStream.range(0, numberOfErrors)
                .mapToObj(number -> WebhookError.builder()
                        .name("error" + number)
                        .data(number + " message")
                        .creationTime(createdAtOffsetCalculator.apply(number))
                        .build())
                .collect(toList());
        when(errorRepo.getErrors(webhookName)).thenReturn(webhookErrors);
        return webhookErrors;
    }

}
