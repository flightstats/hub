package com.flightstats.hub.dao;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.dao.nas.NasContentService;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.google.common.base.Optional;
import com.google.common.io.Files;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NasContentServiceTest {

    private static NasContentService contentService;

    @BeforeClass
    public static void setUpClass() throws Exception {
        File tempDir = Files.createTempDir();
        HubProperties.setProperty("storage.path", tempDir.toString());
        contentService = new NasContentService();
    }

    @Test
    public void testReadWrite() throws Exception {
        Content content = getContent(10 * 1024);
        String channelName = "testReadWrite";
        ContentKey key = contentService.insert(channelName, content);
        Optional<Content> value = contentService.getValue(channelName, key);
        assertTrue(value.isPresent());
        assertEquals(content, value.get());
    }

    private static Content getContent(int size) {
        String random = RandomStringUtils.randomAlphanumeric(size);
        return Content.builder()
                .withContentType("application/json")
                .withData(random.getBytes())
                .withContentKey(new ContentKey())
                .withContentLanguage("russian")
                .build();
    }
}
