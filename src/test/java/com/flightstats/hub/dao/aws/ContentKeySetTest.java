package com.flightstats.hub.dao.aws;

import com.flightstats.hub.model.ContentKey;
import org.joda.time.DateTime;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ContentKeySetTest {

    private final static Logger logger = LoggerFactory.getLogger(ContentKeySetTest.class);

    @Test
    public void testLimit() {
        long millis = System.currentTimeMillis();
        final DateTime endTime = new DateTime(millis + 8);

        List<ContentKey> keys = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            keys.add(new ContentKey(new DateTime(millis + i), "" + i));
        }
        logger.info("keys {}", keys);
        ContentKeySet contentKeys = new ContentKeySet(5, keys.get(7));
        contentKeys.addAll(keys);
        logger.info("contentKeys {}", contentKeys);
        assertEquals(5, contentKeys.size());
        List<ContentKey> list = new ArrayList<>(contentKeys);
        for (int i = 0; i < 5; i++) {
            assertEquals(keys.get(i + 2), list.get(i));
        }
    }

}