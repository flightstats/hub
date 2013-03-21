package com.flightstats.datahub.service;

import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ChannelLockExecutorTest {

	private static final String CHANNEL_NAME = "spoon";

	@Test
	public void testSimpleFirst() throws Exception {
		ConcurrentHashMap<String, Lock> locks = new ConcurrentHashMap<>();
		ChannelLockExecutor testClass = new ChannelLockExecutor(locks);
		Callable<String> callable = new Callable<String>() {
			@Override
			public String call() throws Exception {
				return "howdy";
			}
		};
		String result = testClass.execute(CHANNEL_NAME, callable);
		assertEquals("howdy", result);
		assertNotNull(locks.get(CHANNEL_NAME));
	}

	@Test
	public void testLockExists() throws Exception {
		ConcurrentHashMap<String, Lock> locks = new ConcurrentHashMap<>();
		final Lock lock = mock(Lock.class);
		locks.put(CHANNEL_NAME, lock);
		ChannelLockExecutor testClass = new ChannelLockExecutor(locks);
		Callable<String> callable = new Callable<String>() {
			@Override
			public String call() throws Exception {
				verify(lock).lock();
				return "howdy";
			}
		};
		String result = testClass.execute(CHANNEL_NAME, callable);
		assertEquals("howdy", result);
		assertEquals(lock, locks.get(CHANNEL_NAME));
	}

	@Test
	public void testCallableExplodes() throws Exception {
		ConcurrentHashMap<String, Lock> locks = new ConcurrentHashMap<>();
		final Lock lock = mock(Lock.class);
		locks.put(CHANNEL_NAME, lock);
		ChannelLockExecutor testClass = new ChannelLockExecutor(locks);
		Callable<String> callable = new Callable<String>() {
			@Override
			public String call() throws Exception {
				throw new IllegalStateException("Testing!");
			}
		};
		try {
			testClass.execute(CHANNEL_NAME, callable);
			fail("Expected exception");
		} catch (RuntimeException e) {
			verify(lock).unlock();
		}
	}

	@Test
	public void testLockIsReleased() throws Exception {
		ConcurrentHashMap<String, Lock> locks = new ConcurrentHashMap<>();
		final Lock lock = mock(Lock.class);
		locks.put(CHANNEL_NAME, lock);
		ChannelLockExecutor testClass = new ChannelLockExecutor(locks);
		Callable<String> callable = new Callable<String>() {
			@Override
			public String call() throws Exception {
				return "sure";
			}
		};
		testClass.execute(CHANNEL_NAME, callable);
		verify(lock).unlock();
	}
}
