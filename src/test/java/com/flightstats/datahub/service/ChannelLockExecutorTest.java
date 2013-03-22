package com.flightstats.datahub.service;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.assertSame;
import static junit.framework.TestCase.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ChannelLockExecutorTest {

	private static final String CHANNEL_NAME = "spoon";
	private ConcurrentHashMap<String, Lock> locks;
	private Lock lock;

	@Before
	public void setup() {
		locks = new ConcurrentHashMap<>();
		lock = mock(Lock.class);
	}

	@Test
	public void testExecuteReturnsCallableResult() throws Exception {
		ChannelLockExecutor testClass = new ChannelLockExecutor(locks);
		Callable<String> callable = new Callable<String>() {
			@Override
			public String call() throws Exception {
				return "howdy";
			}
		};
		String result = testClass.execute(CHANNEL_NAME, callable);
		assertEquals("howdy", result);
	}

	@Test
	public void testExecuteCreatesLock() throws Exception {
		ChannelLockExecutor testClass = new ChannelLockExecutor(locks);
		Callable<String> callable = new Callable<String>() {
			@Override
			public String call() throws Exception {
				return "not relevant";
			}
		};
		testClass.execute(CHANNEL_NAME, callable);
		assertNotNull(locks.get(CHANNEL_NAME));
	}

	@Test
	public void testLockAlreadyExists() throws Exception {
		locks.put(CHANNEL_NAME, lock);
		ChannelLockExecutor testClass = new ChannelLockExecutor(locks);
		Callable<String> callable = new Callable<String>() {
			@Override
			public String call() throws Exception {
				verify(lock).lock();
				return "not relevant";
			}
		};
		testClass.execute(CHANNEL_NAME, callable);
		assertSame(lock, locks.get(CHANNEL_NAME));    //same lock is there
	}

	@Test
	public void testCallableExplodes() throws Exception {
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
