package com.flightstats.hub.spoke;

import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.test.Integration;
import com.google.inject.Injector;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class SpokeContentDaoTest {

    private static SpokeContentDao spokeContentDao;

    @BeforeClass
    public static void setUpClass() throws Exception {
        Injector injector = Integration.startRealHub();
        spokeContentDao = injector.getInstance(SpokeContentDao.class);
    }

    @Test
    public void testWriteRead() throws Exception {
        Content content = Content.builder()
                .withData("this is my dataz!".getBytes())
                .withContentType("typing")
                .withUser("awesome")
                .withContentLanguage("english")
                .build();
        ContentKey insertedContentKey = spokeContentDao.write("stuff", content);
        assertNotNull(insertedContentKey);

        Content read = spokeContentDao.read("stuff", insertedContentKey);
        assertNotNull(read);
        assertArrayEquals(content.getData(), read.getData());
        assertEquals(content, read);
    }

}