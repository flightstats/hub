package com.flightstats.hub.spoke;

import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.InsertedContentKey;
import com.google.common.io.Files;
import org.junit.Test;

import static org.junit.Assert.*;

public class SpokeContentDaoTest {

    @Test
    public void testWriteRead() throws Exception {

        SpokeContentDao spokeContentDao = new SpokeContentDao(new FileSpokeStore(Files.createTempDir().getPath()));
        Content content = Content.builder()
                .withData("this is my dataz!".getBytes())
                .withContentType("typing")
                .withUser("awesome")
                .withContentLanguage("english")
                .build();
        InsertedContentKey insertedContentKey = spokeContentDao.write("stuff", content, 1);
        assertNotNull(insertedContentKey);

        Content read = spokeContentDao.read("stuff", insertedContentKey.getKey());
        assertNotNull(read);
        assertArrayEquals(content.getData(), read.getData());
        assertEquals(content, read);
    }

}