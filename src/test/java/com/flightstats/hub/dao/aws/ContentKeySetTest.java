package com.flightstats.hub.dao.aws;

import com.flightstats.hub.model.ContentKey;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContentKeySetTest {

    private final static Logger logger = LoggerFactory.getLogger(ContentKeySetTest.class);

    @Test
    void testSizeAndLimitKey() {
        long millis = System.currentTimeMillis();
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

    @Test
    void testLimitKey() {
        long millis = System.currentTimeMillis();
        List<ContentKey> keys = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            keys.add(new ContentKey(new DateTime(millis + i), "" + i));
        }
        logger.info("keys {}", keys);
        ContentKeySet contentKeys = new ContentKeySet(0, keys.get(7));
        contentKeys.addAll(keys);
        logger.info("contentKeys {}", contentKeys);
        assertEquals(7, contentKeys.size());
        List<ContentKey> list = new ArrayList<>(contentKeys);
        for (int i = 0; i < 7; i++) {
            assertEquals(keys.get(i), list.get(i));
        }
    }

}