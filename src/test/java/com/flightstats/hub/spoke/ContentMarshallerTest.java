package com.flightstats.hub.spoke;

import com.flightstats.hub.dao.ContentMarshaller;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.util.StringUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ContentMarshallerTest {

    @Test
    public void testAllFieldsZero() throws IOException {
        verify(getContent(0), 0);
    }

    @Test
    public void testAllFieldsSmall() throws IOException {
        verify(getContent(10), 10);
    }

    @Test
    public void testAllFields10K() throws IOException {
        int size = 10 * 1024;
        verify(getContent(size), size);
    }

    @Test
    public void testAllFields100K() throws IOException {
        int size = 100 * 1024;
        verify(getContent(size), size);
    }

    @Test
    public void testAllFields1MB() throws IOException {
        int size = 1024 * 1024;
        verify(getContent(size), size);
    }

    @Test
    public void testAllFields10MB() throws IOException {
        int size = 10 * 1024 * 1024;
        verify(getContent(size), size);
    }

    @Test
    public void testAllFields40MB() throws IOException {
        int size = 40 * 1024 * 1024;
        verify(getContent(size), size);
    }

    @Test
    public void testJustData() throws IOException {
        Content content = Content.builder()
                .withData(StringUtils.randomAlphaNumeric(1024).getBytes())
                .withContentKey(new ContentKey())
                .build();
        verify(content, 1024);
    }

    private void verify(Content content, int size) throws IOException {
        Content cycled = ContentMarshaller.toContent(ContentMarshaller.toBytes(content), content.getContentKey().get());
        assertTrue(content.equals(cycled));
        assertEquals(size, cycled.getData().length);
        assertEquals(size, cycled.getSize().longValue());
    }

    private static Content getContent(int size) {
        String random = StringUtils.randomAlphaNumeric(size);
        return Content.builder()
                .withContentType("application/json")
                .withData(random.getBytes())
                .withContentKey(new ContentKey())
                .build();
    }
}
