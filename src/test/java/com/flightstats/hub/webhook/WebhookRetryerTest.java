package com.flightstats.hub.webhook;

import com.flightstats.hub.metrics.StatsdReporter;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class WebhookRetryerTest {

    private List<Predicate<DeliveryAttempt>> giveUpIfs = new ArrayList<>();
    private List<Predicate<DeliveryAttempt>> tryLaterIfs = new ArrayList<>();
    private int connectTimeoutSeconds = 10;
    private int readTimeoutSeconds = 10;
    private WebhookErrorService webhookErrorService = mock(WebhookErrorService.class);
    private StatsdReporter statsdReporter = mock(StatsdReporter.class);

    @Test
    public void testShouldGiveUpIf() {
        giveUpIfs.add(attempt -> true);
        WebhookRetryer retryer = new WebhookRetryer(giveUpIfs, tryLaterIfs, connectTimeoutSeconds, readTimeoutSeconds, webhookErrorService, statsdReporter);
        assertTrue(retryer.shouldGiveUp(DeliveryAttempt.builder().build()));
    }

    @Test
    public void testShouldTryLaterIf() {
        tryLaterIfs.add(attempt -> true);
        WebhookRetryer retryer = new WebhookRetryer(giveUpIfs, tryLaterIfs, connectTimeoutSeconds, readTimeoutSeconds, webhookErrorService, statsdReporter);
        assertTrue(retryer.shouldTryLater(DeliveryAttempt.builder().build()));
    }

    @Test
    public void testDetermineResultFromStatusCode() {
        WebhookRetryer retryer = new WebhookRetryer(giveUpIfs, tryLaterIfs, connectTimeoutSeconds, readTimeoutSeconds, webhookErrorService, statsdReporter);
        assertEquals("200 OK", retryer.determineResult(DeliveryAttempt.builder().statusCode(200).build()));
        assertEquals("400 Bad Request", retryer.determineResult(DeliveryAttempt.builder().statusCode(400).build()));
    }

    @Test
    public void testDetermineResultFromException() {
        WebhookRetryer retryer = new WebhookRetryer(giveUpIfs, tryLaterIfs, connectTimeoutSeconds, readTimeoutSeconds, webhookErrorService, statsdReporter);
        assertEquals("something", retryer.determineResult(DeliveryAttempt.builder().exception(new NullPointerException("something")).build()));
    }

    @Test
    public void calculateSleepTimeMS() {
        WebhookRetryer retryer = new WebhookRetryer(giveUpIfs, tryLaterIfs, connectTimeoutSeconds, readTimeoutSeconds, webhookErrorService, statsdReporter);
        assertEquals(2000, retryer.calculateSleepTimeMS(DeliveryAttempt.builder().number(1).build(), 1000, 10000));
        assertEquals(4000, retryer.calculateSleepTimeMS(DeliveryAttempt.builder().number(2).build(), 1000, 10000));
        assertEquals(8000, retryer.calculateSleepTimeMS(DeliveryAttempt.builder().number(3).build(), 1000, 10000));
        assertEquals(10000, retryer.calculateSleepTimeMS(DeliveryAttempt.builder().number(4).build(), 1000, 10000));
    }

}
