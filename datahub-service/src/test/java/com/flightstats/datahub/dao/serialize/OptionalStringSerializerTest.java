package com.flightstats.datahub.dao.serialize;

import com.google.common.base.Optional;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;

public class OptionalStringSerializerTest {

	@Test
	public void testRoundTripString() throws Exception {
		//GIVEN
		OptionalStringSerializer testClass = new OptionalStringSerializer();
		Optional<String> input = Optional.of("this is a test");

		//WHEN
		ByteBuffer byteBuffer = testClass.toByteBuffer(input);
		Optional<String> reconstituted = testClass.fromByteBuffer(byteBuffer);

		//THEN
		assertEquals(reconstituted, input);
	}

	@Test
	public void testRoundTripEmptyString() throws Exception {
		//GIVEN
		OptionalStringSerializer testClass = new OptionalStringSerializer();
		Optional<String> input = Optional.of("");

		//WHEN
		ByteBuffer byteBuffer = testClass.toByteBuffer(input);
		Optional<String> reconstituted = testClass.fromByteBuffer(byteBuffer);

		//THEN
		assertEquals(reconstituted, Optional.absent());
	}

	@Test
	public void testRoundTripAbsent() throws Exception {
		//GIVEN
		OptionalStringSerializer testClass = new OptionalStringSerializer();
		Optional<String> input = Optional.absent();

		//WHEN
		ByteBuffer byteBuffer = testClass.toByteBuffer(input);
		Optional<String> reconstituted = testClass.fromByteBuffer(byteBuffer);

		//THEN
		assertEquals(reconstituted, input);
	}
}
