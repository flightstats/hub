package com.flightstats.datahub.model.serialize;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.datahub.app.config.DataHubObjectMapperFactory;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.MetadataResponse;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MetadataResponseMixInTest {

	@Test
	public void test() throws Exception {
		ObjectMapper mapper = DataHubObjectMapperFactory.construct();
		OutputStream out = new ByteArrayOutputStream();
		Date date = new Date(902103L);
		ChannelConfiguration config = new ChannelConfiguration("The Name", new Date(808), null);
		MetadataResponse response = new MetadataResponse(config, date);
		mapper.writeValue(out, response);
		String result = out.toString();
		List<String> lines = Arrays.asList(result.split("\n"));
		//We have to do some hoop jumping because the field order can differ between runs.
		lines = Lists.transform(lines, new Function<String, String>() {
			@Override
			public String apply(String input) {
				return input.replaceFirst(",$", "");
			}
		});
		assertEquals(5, lines.size());
		assertTrue(lines.contains("{"));
		assertTrue(lines.contains("  \"creationDate\" : \"1970-01-01T00:00:00.808Z\""));
		assertTrue(lines.contains("  \"lastUpdateDate\" : \"1970-01-01T00:15:02.103Z\""));
		assertTrue(lines.contains("  \"name\" : \"The Name\""));
		assertTrue(lines.contains("}"));
	}
}
