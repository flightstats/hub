package com.flightstats.hub.cluster;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SpokeRingsTest {

    private static final int STEP = 10 * 1000;
    private static final int HALF_STEP = STEP / 2;
    private long[] steps;

    @Before
    public void setUp() throws Exception {
        long start = System.currentTimeMillis() - 100 * STEP;
        steps = new long[100];
        for (int i = 0; i < steps.length; i++) {
            steps[i] = start + STEP * i;
        }
    }

    @Test
    public void test3Nodes() {
        List<ClusterEvent> clusterEvents = new ArrayList<>();
        clusterEvents.add(new ClusterEvent("/SCE/" + steps[0] + "|A|ADDED", steps[0]));
        clusterEvents.add(new ClusterEvent("/SCE/" + steps[1] + "|B|ADDED", steps[1]));
        clusterEvents.add(new ClusterEvent("/SCE/" + steps[2] + "|C|ADDED", steps[2]));
        SpokeRings spokeRings = new SpokeRings();
        spokeRings.process(clusterEvents);
        assertTrue(spokeRings.getNodes("test1").containsAll(Arrays.asList("A", "B", "C")));
        assertTrue(spokeRings.getNodes("channel2").containsAll(Arrays.asList("A", "B", "C")));
        assertTrue(spokeRings.getNodes("channel2", getTime(steps[0])).containsAll(Arrays.asList("A")));
        assertTrue(spokeRings.getNodes("channel2", getTime(steps[1])).containsAll(Arrays.asList("A", "B")));
        assertTrue(spokeRings.getNodes("channel2", getTime(steps[2])).containsAll(Arrays.asList("A", "B", "C")));
        assertTrue(spokeRings.getNodes("channel2", getTime(steps[0]), getTime(steps[2])).containsAll(Arrays.asList("A", "B", "C")));
    }

    @Test
    public void test4Nodes() {
        List<ClusterEvent> clusterEvents = new ArrayList<>();
        clusterEvents.add(new ClusterEvent("/SCE/" + steps[0] + "|A|ADDED", steps[0]));
        clusterEvents.add(new ClusterEvent("/SCE/" + steps[1] + "|B|ADDED", steps[1]));
        clusterEvents.add(new ClusterEvent("/SCE/" + steps[2] + "|C|ADDED", steps[2]));
        clusterEvents.add(new ClusterEvent("/SCE/" + steps[3] + "|D|ADDED", steps[3]));
        SpokeRings spokeRings = new SpokeRings();
        spokeRings.process(clusterEvents);

        assertTrue(spokeRings.getNodes("test1").containsAll(Arrays.asList("A", "B", "C")));
        assertTrue(spokeRings.getNodes("channel2").containsAll(Arrays.asList("A", "B", "D")));
        assertTrue(spokeRings.getNodes("other").containsAll(Arrays.asList("B", "C", "D")));
        assertTrue(spokeRings.getNodes("name").containsAll(Arrays.asList("A", "D", "C")));
        assertTrue(spokeRings.getNodes("channel2", getTime(steps[0])).containsAll(Arrays.asList("A")));
        assertTrue(spokeRings.getNodes("channel2", getTime(steps[1])).containsAll(Arrays.asList("A", "B")));
        assertTrue(spokeRings.getNodes("channel2", getTime(steps[2])).containsAll(Arrays.asList("A", "B", "C")));
        assertTrue(spokeRings.getNodes("channel2", getTime(steps[3])).containsAll(Arrays.asList("A", "B", "D")));
        assertTrue(spokeRings.getNodes("channel2", getTime(steps[0]), getTime(steps[3])).containsAll(Arrays.asList("A", "B", "C", "D")));
        assertTrue(spokeRings.getNodes("channel2", getTime(steps[3]), getTime(steps[4])).containsAll(Arrays.asList("A", "B", "D")));
    }

    @Test
    public void test5Nodes() {
        List<ClusterEvent> clusterEvents = new ArrayList<>();
        clusterEvents.add(new ClusterEvent("/SCE/" + steps[0] + "|A|ADDED", steps[0]));
        clusterEvents.add(new ClusterEvent("/SCE/" + steps[1] + "|B|ADDED", steps[1]));
        clusterEvents.add(new ClusterEvent("/SCE/" + steps[2] + "|C|ADDED", steps[2]));
        clusterEvents.add(new ClusterEvent("/SCE/" + steps[3] + "|D|ADDED", steps[3]));
        clusterEvents.add(new ClusterEvent("/SCE/" + steps[4] + "|E|ADDED", steps[4]));
        SpokeRings spokeRings = new SpokeRings();
        spokeRings.process(clusterEvents);
        assertTrue(spokeRings.getNodes("test1").containsAll(Arrays.asList("B", "C", "E")));
        assertTrue(spokeRings.getNodes("channel2").containsAll(Arrays.asList("A", "B", "D")));
        assertTrue(spokeRings.getNodes("other").containsAll(Arrays.asList("B", "C", "E")));
        assertTrue(spokeRings.getNodes("name").containsAll(Arrays.asList("A", "D", "E")));

        assertTrue(spokeRings.getNodes("channel2", getTime(steps[0])).containsAll(Arrays.asList("A")));
        assertTrue(spokeRings.getNodes("channel2", getTime(steps[1])).containsAll(Arrays.asList("A", "B")));
        assertTrue(spokeRings.getNodes("channel2", getTime(steps[2])).containsAll(Arrays.asList("A", "B", "C")));
        assertTrue(spokeRings.getNodes("channel2", getTime(steps[3])).containsAll(Arrays.asList("A", "B", "D")));
        assertTrue(spokeRings.getNodes("channel2", getTime(steps[0]), getTime(steps[4])).containsAll(Arrays.asList("A", "B", "C", "D")));
        assertTrue(spokeRings.getNodes("other", getTime(steps[0]), getTime(steps[4])).containsAll(Arrays.asList("A", "B", "C", "D", "E")));
        assertTrue(spokeRings.getNodes("other", getTime(steps[5]), getTime(steps[6])).containsAll(Arrays.asList("B", "C", "E")));
        assertTrue(spokeRings.getNodes("other", getTime(steps[4]), getTime(steps[6])).containsAll(Arrays.asList("B", "C", "E")));
    }

    @Test
    public void test5NodeRollingRestart() {
        Collection<ClusterEvent> events = ClusterEvent.set();

        events.add(new ClusterEvent("/SCE/" + steps[0] + "|A|ADDED", steps[0]));
        events.add(new ClusterEvent("/SCE/" + steps[1] + "|B|ADDED", steps[1]));
        events.add(new ClusterEvent("/SCE/" + steps[2] + "|C|ADDED", steps[2]));
        events.add(new ClusterEvent("/SCE/" + steps[3] + "|D|ADDED", steps[3]));
        events.add(new ClusterEvent("/SCE/" + steps[4] + "|E|ADDED", steps[4]));

        events.add(new ClusterEvent("/SCE/" + steps[0] + "|A|REMOVED", steps[5]));
        events.add(new ClusterEvent("/SCE/" + steps[6] + "|A|ADDED", steps[6]));

        events.add(new ClusterEvent("/SCE/" + steps[1] + "|B|REMOVED", steps[7]));
        events.add(new ClusterEvent("/SCE/" + steps[8] + "|B|ADDED", steps[8]));

        events.add(new ClusterEvent("/SCE/" + steps[2] + "|C|REMOVED", steps[9]));
        events.add(new ClusterEvent("/SCE/" + steps[10] + "|C|ADDED", steps[10]));

        events.add(new ClusterEvent("/SCE/" + steps[3] + "|D|REMOVED", steps[11]));
        events.add(new ClusterEvent("/SCE/" + steps[12] + "|D|ADDED", steps[12]));

        events.add(new ClusterEvent("/SCE/" + steps[4] + "|E|REMOVED", steps[13]));
        events.add(new ClusterEvent("/SCE/" + steps[14] + "|E|ADDED", steps[14]));

        SpokeRings spokeRings = new SpokeRings();
        spokeRings.process(events);

        compare(spokeRings.getNodes("test1"), Arrays.asList("B", "C", "E"));
        compare(spokeRings.getNodes("channel2"), Arrays.asList("A", "B", "D"));
        compare(spokeRings.getNodes("name"), Arrays.asList("A", "D", "E"));
        compare(spokeRings.getNodes("test3"), Arrays.asList("C", "D", "E"));
        compare(spokeRings.getNodes("other"), Arrays.asList("B", "C", "E"));

        compare(spokeRings.getNodes("other", getTime(steps[0] + HALF_STEP)), Arrays.asList("A"));
        compare(spokeRings.getNodes("other", getTime(steps[1] + HALF_STEP)), Arrays.asList("A", "B"));
        compare(spokeRings.getNodes("other", getTime(steps[2] + HALF_STEP)), Arrays.asList("A", "B", "C"));
        compare(spokeRings.getNodes("other", getTime(steps[3] + HALF_STEP)), Arrays.asList("B", "C", "D"));
        compare(spokeRings.getNodes("other", getTime(steps[4] + HALF_STEP)), Arrays.asList("B", "C", "E"));
        compare(spokeRings.getNodes("other", getTime(steps[2] + HALF_STEP), getTime(steps[4] + HALF_STEP)), Arrays.asList("A", "B", "C", "D", "E"));

        compare(spokeRings.getNodes("other", getTime(steps[5] + HALF_STEP)), Arrays.asList("B", "C", "E"));

        compare(spokeRings.getNodes("other", getTime(steps[6] + HALF_STEP)), Arrays.asList("B", "C", "E"));
        compare(spokeRings.getNodes("other", getTime(steps[7] + HALF_STEP)), Arrays.asList("C", "D", "E"));

        compare(spokeRings.getNodes("other", getTime(steps[6] + HALF_STEP), getTime(steps[7] + HALF_STEP)), Arrays.asList("B", "C", "D", "E"));

        compare(spokeRings.getNodes("other", getTime(steps[8] + HALF_STEP)), Arrays.asList("B", "C", "E"));
        compare(spokeRings.getNodes("other", getTime(steps[9] + HALF_STEP)), Arrays.asList("B", "D", "E"));
        compare(spokeRings.getNodes("other", getTime(steps[10] + HALF_STEP)), Arrays.asList("B", "C", "E"));
        compare(spokeRings.getNodes("other", getTime(steps[11] + HALF_STEP)), Arrays.asList("B", "C", "E"));
        compare(spokeRings.getNodes("other", getTime(steps[12] + HALF_STEP)), Arrays.asList("B", "C", "E"));
        compare(spokeRings.getNodes("other", getTime(steps[13] + HALF_STEP)), Arrays.asList("B", "C", "D"));
        compare(spokeRings.getNodes("other", getTime(steps[14] + HALF_STEP)), Arrays.asList("B", "C", "E"));
    }

    private void compare(Collection<String> found, Collection<String> expected) {
        assertEquals(expected.size(), found.size());
        assertEquals(new TreeSet<>(expected).toString(), new TreeSet<>(found).toString());
    }

    private DateTime getTime(long millis) {
        return new DateTime(millis, DateTimeZone.UTC);
    }
}