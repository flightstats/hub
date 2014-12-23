package com.flightstats.hub.spoke;

import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SpokeMarshallerTest {

    @Test
    public void testAllFieldsSmall() throws IOException {
        verify(getContent(10), 10);
    }

    @Test
    public void testAllFieldsLarge() throws IOException {
        int size = 100 * 1024;
        verify(getContent(size), size);
    }

    @Test
    public void testJustData() throws IOException {
        Content content = Content.builder()
                .withData(RandomStringUtils.randomAlphanumeric(1024).getBytes())
                .withContentKey(new ContentKey())
                .build();
        verify(content, 1024);
    }

    private void verify(Content content, int size) throws IOException {
        byte[] bytes = SpokeMarshaller.toBytes(content);
        Content cycled = SpokeMarshaller.toContent(new ByteArrayInputStream(bytes), content.getContentKey().get());
        assertTrue(content.equals(cycled));
        assertEquals(size, cycled.getData().length);
    }

    private static Content getContent(int size) {
        String random = RandomStringUtils.randomAlphanumeric(size);
        return Content.builder()
                .withContentType("application/json")
                .withData(random.getBytes())
                .withContentKey(new ContentKey())
                .withContentLanguage("russian")
                .withUser("bob")
                .build();
    }
}
