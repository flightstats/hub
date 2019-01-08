package com.flightstats.hub.webhook;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.webhook.error.WebhookErrorPruner;
import com.flightstats.hub.webhook.error.WebhookErrorService;
import org.junit.Test;

import java.util.List;
import java.util.stream.IntStream;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class WebhookErrorUnitTest {
    private final ChannelService channelService = mock(ChannelService.class);
    private final WebhookErrorPruner errorPruner = mock(WebhookErrorPruner.class);
    private final WebhookErrorService errorService = mock(WebhookErrorService.class);

    @Test
    public void testAdd() {
        String webhookName = "webhookName";
        String errorMessage = "someError";

        List<com.flightstats.hub.webhook.error.WebhookError> webhookErrors = setupErrorMocks(webhookName, 12);
        List<com.flightstats.hub.webhook.error.WebhookError> errorsToDelete = webhookErrors.subList(0, 1);
        when(errorPruner.pruneErrors(webhookName, webhookErrors)).thenReturn(errorsToDelete);

        WebhookError webhookError = new WebhookError(errorService, errorPruner, channelService);

        webhookError.add(webhookName, errorMessage);
        verify(errorService).add(webhookName, errorMessage);
        verify(errorPruner).pruneErrors(webhookName, webhookErrors);
    }

    @Test
    public void testGet() {
        String webhookName = "webhookName";

        List<com.flightstats.hub.webhook.error.WebhookError> webhookErrors = setupErrorMocks(webhookName, 6);
        List<com.flightstats.hub.webhook.error.WebhookError> errorsToDelete = newArrayList(webhookErrors.get(1), webhookErrors.get(3), webhookErrors.get(5));
        when(errorPruner.pruneErrors(webhookName, webhookErrors)).thenReturn(errorsToDelete);

        WebhookError webhookError = new WebhookError(errorService, errorPruner, channelService);

        List<String> errors = webhookError.get(webhookName);

        assertEquals(newArrayList("0 message", "2 message", "4 message"), errors);
    }

    private List<com.flightstats.hub.webhook.error.WebhookError> setupErrorMocks(String webhookName, int numberOfErrors) {
        List<com.flightstats.hub.webhook.error.WebhookError> webhookErrors = IntStream.range(0, numberOfErrors)
                .mapToObj(number -> com.flightstats.hub.webhook.error.WebhookError.builder()
                        .name("error" + number)
                        .data(number + " message")
                        .build())
                .collect(toList());
        when(errorService.getErrors(webhookName)).thenReturn(webhookErrors);
        return webhookErrors;
    }
}
