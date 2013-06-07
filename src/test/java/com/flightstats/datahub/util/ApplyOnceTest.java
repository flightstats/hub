package com.flightstats.datahub.util;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class ApplyOnceTest {

	@Test
	public void testOnlyRunsOnce() throws Exception {
		//GIVEN
		Function<String, String> function = mock(Function.class);
		when(function.apply(anyString())).thenReturn("bar");
		ApplyOnce<String, String> testClass = new ApplyOnce<>(function);
		//WHEN
		Optional<String> result = testClass.apply("foo");
		Optional<String> result2 = testClass.apply("foo");
		//THEN
		assertTrue(result.isPresent());
		assertEquals("bar", result.get());
		assertFalse(result2.isPresent());
		verify(function, times(1)).apply("foo");
	}

	@Test
	public void testConcurrentExecutions() throws Exception {
		//GIVEN
		Function<String, String> function = mock(Function.class);
		when(function.apply(anyString())).thenReturn("bar");
		final ApplyOnce<String, String> testClass = new ApplyOnce<>(function);

		final CountDownLatch allReadyLatch = new CountDownLatch(250);
		final CountDownLatch goAheadLatch = new CountDownLatch(1);
		final CountDownLatch allFinishedLatch = new CountDownLatch(250);
		ExecutorService pool = Executors.newFixedThreadPool(250);

		//WHEN
		for (int i = 0; i < 250; i++) {
			pool.submit(new Runnable() {
				@Override
				public void run() {
					try {
						allReadyLatch.countDown();
						goAheadLatch.await();
						testClass.apply("foo");
						allFinishedLatch.countDown();
					} catch (InterruptedException e) {
						fail("Interrupted.");
					}
				}
			});
		}
		allReadyLatch.await();
		goAheadLatch.countDown();
		allFinishedLatch.await();

		//THEN
		verify(function, times(1)).apply("foo");
	}

	@Test
	public void testResultWhenDelegateReturnsNull() throws Exception {
		//GIVEN
		Function<String, String> function = mock(Function.class);
		when(function.apply(anyString())).thenReturn(null);
		ApplyOnce<String, String> testClass = new ApplyOnce<>(function);
		//WHEN
		Optional<String> result = testClass.apply("foo");
		//THEN
		assertFalse(result.isPresent());
	}
}
