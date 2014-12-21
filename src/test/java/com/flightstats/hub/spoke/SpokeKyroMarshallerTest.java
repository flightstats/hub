package com.flightstats.hub.spoke;

import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

public class SpokeKyroMarshallerTest {

    @Test
    public void testCycle() throws IOException {
        Content content = getContent(10);
        Content cycle = SpokeKyroMarshaller.toContent(SpokeKyroMarshaller.toBytes(content), content.getContentKey().get());
        assertTrue(content.equals(cycle));
    }

    private Content getContent(int size) {
        return Content.builder()
                .withContentType("application/json")
                .withContentKey(new ContentKey())
                .withContentLanguage("russian")
                .withData(RandomStringUtils.randomAlphanumeric(size).getBytes())
                .build();
    }

}