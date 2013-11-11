package com.flightstats.datahub.model.serialize;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.datahub.app.config.DataHubObjectMapperFactory;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.ValueInsertionResult;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ValueInsertionResultMixInTest {

	@Test
	public void testSerialize() throws Exception {
		ObjectMapper objectMapper = DataHubObjectMapperFactory.construct();
		DataHubKey key = new DataHubKey((short) 1033);
		ValueInsertionResult valueInsertionResult = new ValueInsertionResult(key);
		OutputStream out = new ByteArrayOutputStream();
		objectMapper.writeValue(out, valueInsertionResult);
		String result = out.toString();
        //todo - gfm - 11/5/13 - presume this needs time provider or ?
        assertTrue(result.contains("timestamp"));
        assertTrue(result.contains("T"));
        assertTrue(result.contains("Z"));
        //assertEquals("{\n  \"timestamp\" : \"2005-08-07T23:17:58.922Z\"\n}", result);

    }
}
