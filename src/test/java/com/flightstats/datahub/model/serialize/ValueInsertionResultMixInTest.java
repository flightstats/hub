package com.flightstats.datahub.model.serialize;

import com.flightstats.datahub.app.config.DataHubObjectMapperFactory;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.ValueInsertionResult;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Date;

import static junit.framework.TestCase.assertEquals;

public class ValueInsertionResultMixInTest {

	@Test
	public void testSerialize() throws Exception {
		ObjectMapper objectMapper = new DataHubObjectMapperFactory().build();
		DataHubKey key = new DataHubKey(new Date(1123456678922L), (short) 33);
		ValueInsertionResult valueInsertionResult = new ValueInsertionResult(key);
		OutputStream out = new ByteArrayOutputStream();
		objectMapper.writeValue(out, valueInsertionResult);
		String result = out.toString();
		assertEquals("{\n" +
				"  \"timestamp\" : \"2005-08-07T23:17:58.922Z\"\n" +
				"}", result);
	}
}
