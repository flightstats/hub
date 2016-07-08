package com.flightstats.hub.webhook;

import com.flightstats.hub.test.Integration;
import org.apache.curator.framework.CuratorFramework;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class GroupErrorTest {

    private static CuratorFramework curator;
    private static GroupError groupError;

    @BeforeClass
    public static void setUpClass() throws Exception {
        curator = Integration.startZooKeeper();
        groupError = new GroupError(curator);
    }

    @Test
    public void testErrors() {
        for (int i = 0; i < 20; i++) {
            groupError.add("testErrors", "stuff" + i);
        }
        List<String> errors = groupError.get("testErrors");
        assertEquals(10, errors.size());

        for (int i = 0; i < 10; i++) {
            assertEquals("stuff" + (i + 10), errors.get(i));
        }
    }

}