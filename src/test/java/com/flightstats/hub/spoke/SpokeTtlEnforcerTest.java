package com.flightstats.hub.spoke;

import com.flightstats.hub.util.TimeUtil;
import com.google.common.io.Files;
import org.apache.commons.lang3.RandomStringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class SpokeTtlEnforcerTest {

    //todo - gfm - 11/14/14 - this comes from SpokeContentDao.  should be standardized somewhere
    private final static DateTimeFormatter pathFormatter = DateTimeFormat.forPattern("yyyy/MM/dd/HH/mm/ssSSS").withZoneUTC();

    @Test
    public void testCleanup() throws Exception {
        String tempDir = Files.createTempDir().getPath();
        FileSpokeStore spokeStore = new FileSpokeStore(tempDir);

        List<String> insideTtl = new ArrayList<>();
        List<String> outsideTtl = new ArrayList<>();
        int ttlMinutes = 60;
        for (int i = 0; i < 2; i++) {
            String channel = RandomStringUtils.randomAlphanumeric(5);
            DateTime now = TimeUtil.now();
            for (int j = 0; j <= ttlMinutes; j += 10) {
                String path = channel + "/" + now.minusMinutes(j).toString(pathFormatter);
                spokeStore.write(path, "stuff".getBytes());
                insideTtl.add(path);
            }
            for (int j = ttlMinutes + 1; j <= ttlMinutes * 50; j += 50) {
                String path = channel + "/" + now.minusMinutes(j).toString(pathFormatter);
                spokeStore.write(path, "stuff".getBytes());
                outsideTtl.add(path);
            }

        }

        SpokeTtlEnforcer enforcer = new SpokeTtlEnforcer(tempDir, ttlMinutes);
        enforcer.run();

        for (String path : insideTtl) {
            assertNotNull(spokeStore.read(path));
        }

        for (String path : outsideTtl) {
            assertNull(spokeStore.read(path));
        }


    }
}