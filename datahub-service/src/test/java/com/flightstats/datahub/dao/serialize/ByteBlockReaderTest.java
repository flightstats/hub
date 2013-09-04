package com.flightstats.datahub.dao.serialize;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class ByteBlockReaderTest {

	@Test
	public void testReadByteBlock() throws Exception {
		//GIVEN
		final String byteString = "testing!";
		byte[] expected = byteString.getBytes();
		ByteBuffer correctedBuffer = mock(ByteBuffer.class);

		when(correctedBuffer.getInt()).thenReturn(8);
		doAnswer(new Answer() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				byte[] byteArray = (byte[]) invocation.getArguments()[0];
				assertEquals(8, byteArray.length);
				System.arraycopy(byteString.getBytes(), 0, byteArray, 0, 8);
				return null;
			}
		}).when(correctedBuffer).get(any(byte[].class));

		ByteBlockReader testClass = new ByteBlockReader();

		//WHEN
		byte[] result = testClass.readByteBlock(correctedBuffer);

		//THEN

		assertEquals(new String(expected), new String(result));
	}
}
