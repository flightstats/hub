package com.flightstats.hub.dao.aws;

import com.flightstats.hub.model.ContentKey;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class ContentKeySetTest {

    @Test
    public void testLimit() {
        long millis = System.currentTimeMillis();
        final DateTime endTime = new DateTime(millis + 8);
        ContentKeySet contentKeys = new ContentKeySet(5, new ContentKey(endTime, "1"));

        for (int i = 0; i < 10; i++) {
            contentKeys.add(new ContentKey(new DateTime(millis + i), "" + i));
        }
        for (ContentKey contentKey : contentKeys) {
            System.out.println(contentKey);
        }
        assertEquals(5, contentKeys.size());
        ArrayList<ContentKey> list = new ArrayList(contentKeys);
        for (int i = 0; i < 5; i++) {
            assertEquals("" + (i + 3), list.get(i).getHash());
        }
    }

}