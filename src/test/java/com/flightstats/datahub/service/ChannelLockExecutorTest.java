package com.flightstats.datahub.service;

import com.flightstats.datahub.cluster.ReentrantChannelLockFactory;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChannelLockExecutorTest {

	private static final String CHANNEL_NAME = "spoon";
	ReentrantChannelLockFactory lockFactory;

	@Before
	public void setup() {
		lockFactory = mock(ReentrantChannelLockFactory.class);
	}

	@Test
	public void testExecuteReturnsCallableResult() throws Exception {
		ReentrantLock lock = mock(ReentrantLock.class);
		when(lockFactory.getLock(anyString())).thenReturn(lock);
		ChannelLockExecutor testClass = new ChannelLockExecutor(lockFactory);
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
		ReentrantLock lock = mock(ReentrantLock.class);
		when(lockFactory.getLock(anyString())).thenReturn(lock);
		ChannelLockExecutor testClass = new ChannelLockExecutor(lockFactory);
		Callable<String> callable = new Callable<String>() {
			@Override
			public String call() throws Exception {
				return "not relevant";
			}
		};
		testClass.execute(CHANNEL_NAME, callable);
		verify(lockFactory).getLock(CHANNEL_NAME);
	}

	@Test
	public void testCallableExplodes() throws Exception {
		ReentrantLock lock = mock(ReentrantLock.class);
		when(lockFactory.getLock(anyString())).thenReturn(lock);
		ChannelLockExecutor testClass = new ChannelLockExecutor(lockFactory);
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
		ReentrantLock lock = mock(ReentrantLock.class);
		when(lockFactory.getLock(anyString())).thenReturn(lock);
		ChannelLockExecutor testClass = new ChannelLockExecutor(lockFactory);
		Callable<String> callable = new Callable<String>() {
			@Override
			public String call() throws Exception {
				return "sure";
			}
		};
		testClass.execute(CHANNEL_NAME, callable);
		verify(lock).unlock();
	}

	@Test(expected = EsotericException.class)
	public void testCallableThrows() throws Exception {
		ReentrantLock lock = mock(ReentrantLock.class);
		when(lockFactory.getLock(anyString())).thenReturn(lock);
		ChannelLockExecutor testClass = new ChannelLockExecutor(lockFactory);
		Callable<String> callable = new Callable<String>() {
			@Override
			public String call() throws Exception {
				throw new EsotericException();
			}
		};
		testClass.execute(CHANNEL_NAME, callable);
	}

	static class EsotericException extends Exception {

	}
}
