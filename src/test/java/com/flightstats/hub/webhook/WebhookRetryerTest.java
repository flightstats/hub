package com.flightstats.hub.webhook;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class WebhookRetryerTest {

    @Test
    public void testShouldGiveUpIf() {
        List<Predicate<DeliveryAttempt>> giveUpIfs = new ArrayList<>();
        giveUpIfs.add(attempt -> true);
        List<Predicate<DeliveryAttempt>> tryLaterIfs = new ArrayList<>();
        int timeoutSeconds = 10;
        WebhookError webhookError = mock(WebhookError.class);
        WebhookRetryer retryer = new WebhookRetryer(giveUpIfs, tryLaterIfs, timeoutSeconds, webhookError);
        DeliveryAttempt attempt = DeliveryAttempt.builder().build();

        assertTrue(retryer.shouldGiveUp(attempt));
    }

    @Test
    public void testShouldTryLaterIf() {
        List<Predicate<DeliveryAttempt>> giveUpIfs = new ArrayList<>();
        List<Predicate<DeliveryAttempt>> tryLaterIfs = new ArrayList<>();
        tryLaterIfs.add(attempt -> true);
        int timeoutSeconds = 10;
        WebhookError webhookError = mock(WebhookError.class);
        WebhookRetryer retryer = new WebhookRetryer(giveUpIfs, tryLaterIfs, timeoutSeconds, webhookError);
        DeliveryAttempt attempt = DeliveryAttempt.builder().build();

        assertTrue(retryer.shouldTryLater(attempt));
    }

    @Test
    public void testDetermineResultFromStatusCode() {
        List<Predicate<DeliveryAttempt>> giveUpIfs = new ArrayList<>();
        List<Predicate<DeliveryAttempt>> tryLaterIfs = new ArrayList<>();
        int timeoutSeconds = 10;
        WebhookError webhookError = mock(WebhookError.class);
        WebhookRetryer retryer = new WebhookRetryer(giveUpIfs, tryLaterIfs, timeoutSeconds, webhookError);

        assertEquals("200 OK", retryer.determineResult(DeliveryAttempt.builder().statusCode(200).build()));
        assertEquals("400 Bad Request", retryer.determineResult(DeliveryAttempt.builder().statusCode(400).build()));
    }

    @Test
    public void testDetermineResultFromException() {
        List<Predicate<DeliveryAttempt>> giveUpIfs = new ArrayList<>();
        List<Predicate<DeliveryAttempt>> tryLaterIfs = new ArrayList<>();
        int timeoutSeconds = 10;
        WebhookError webhookError = mock(WebhookError.class);
        WebhookRetryer retryer = new WebhookRetryer(giveUpIfs, tryLaterIfs, timeoutSeconds, webhookError);

        assertEquals("java.lang.NullPointerException: something", retryer.determineResult(DeliveryAttempt.builder().exception(new NullPointerException("something")).build()));
    }

    @Test
    public void testIsSuccessful() {
        List<Predicate<DeliveryAttempt>> giveUpIfs = new ArrayList<>();
        List<Predicate<DeliveryAttempt>> tryLaterIfs = new ArrayList<>();
        int timeoutSeconds = 10;
        WebhookError webhookError = mock(WebhookError.class);
        WebhookRetryer retryer = new WebhookRetryer(giveUpIfs, tryLaterIfs, timeoutSeconds, webhookError);

        assertTrue(retryer.isSuccessful(DeliveryAttempt.builder().statusCode(200).build()));
        assertTrue(retryer.isSuccessful(DeliveryAttempt.builder().statusCode(201).build()));
        assertTrue(retryer.isSuccessful(DeliveryAttempt.builder().statusCode(303).build()));
    }

    @Test
    public void calculateSleepTimeMS() {
        List<Predicate<DeliveryAttempt>> giveUpIfs = new ArrayList<>();
        List<Predicate<DeliveryAttempt>> tryLaterIfs = new ArrayList<>();
        int timeoutSeconds = 10;
        WebhookError webhookError = mock(WebhookError.class);
        WebhookRetryer retryer = new WebhookRetryer(giveUpIfs, tryLaterIfs, timeoutSeconds, webhookError);

        assertEquals(2000, retryer.calculateSleepTimeMS(DeliveryAttempt.builder().number(1).build(), 1000, 10000));
        assertEquals(4000, retryer.calculateSleepTimeMS(DeliveryAttempt.builder().number(2).build(), 1000, 10000));
        assertEquals(8000, retryer.calculateSleepTimeMS(DeliveryAttempt.builder().number(3).build(), 1000, 10000));
        assertEquals(10000, retryer.calculateSleepTimeMS(DeliveryAttempt.builder().number(4).build(), 1000, 10000));
    }

}
