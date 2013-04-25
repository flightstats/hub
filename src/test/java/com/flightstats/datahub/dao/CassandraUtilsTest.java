package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.exception.NoSuchChannelException;
import org.junit.Test;

import static com.flightstats.datahub.dao.CassandraUtils.maybePromoteToNoSuchChannel;
import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertSame;

public class CassandraUtilsTest {

	@Test
	public void testMaybePromoteToNoSuchChannel_invalidChannel() throws Exception {
		RuntimeException exception = new RuntimeException("unconfigured columnfamily");
		RuntimeException result = maybePromoteToNoSuchChannel(exception, "superchan");
		assertEquals(result.getClass(), NoSuchChannelException.class);
		assertEquals("Channel does not exist: superchan", result.getMessage());
		assertSame(exception, result.getCause());
	}

	@Test
	public void testMaybePromoteToNoSuchChannel_validChannel() throws Exception {
		RuntimeException exception = new ArithmeticException("boom");
		RuntimeException result = maybePromoteToNoSuchChannel(exception, "superchan");
		assertSame(exception, result);
	}

}
