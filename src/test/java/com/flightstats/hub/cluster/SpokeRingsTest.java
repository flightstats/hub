package com.flightstats.hub.cluster;

import com.flightstats.hub.app.HubProperties;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SpokeRingsTest {

    private static final int STEP = 10 * 1000;
    private static final int HALF_STEP = STEP / 2;
    private long[] steps;
    private long start;

    @Before
    public void setUp() throws Exception {
        start = System.currentTimeMillis() - 100 * STEP;
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
        assertTrue(spokeRings.getServers("test1").containsAll(Arrays.asList("A", "B", "C")));
        assertTrue(spokeRings.getServers("channel2").containsAll(Arrays.asList("A", "B", "C")));
        assertTrue(spokeRings.getServers("channel2", getTime(steps[0])).containsAll(Arrays.asList("A")));
        assertTrue(spokeRings.getServers("channel2", getTime(steps[1])).containsAll(Arrays.asList("A", "B")));
        assertTrue(spokeRings.getServers("channel2", getTime(steps[2])).containsAll(Arrays.asList("A", "B", "C")));
        assertTrue(spokeRings.getServers("channel2", getTime(steps[0]), getTime(steps[2])).containsAll(Arrays.asList("A", "B", "C")));
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

        assertTrue(spokeRings.getServers("test1").containsAll(Arrays.asList("A", "B", "C")));
        assertTrue(spokeRings.getServers("channel2").containsAll(Arrays.asList("A", "B", "D")));
        assertTrue(spokeRings.getServers("other").containsAll(Arrays.asList("B", "C", "D")));
        assertTrue(spokeRings.getServers("name").containsAll(Arrays.asList("A", "D", "C")));
        assertTrue(spokeRings.getServers("channel2", getTime(steps[0])).containsAll(Arrays.asList("A")));
        assertTrue(spokeRings.getServers("channel2", getTime(steps[1])).containsAll(Arrays.asList("A", "B")));
        assertTrue(spokeRings.getServers("channel2", getTime(steps[2])).containsAll(Arrays.asList("A", "B", "C")));
        assertTrue(spokeRings.getServers("channel2", getTime(steps[3])).containsAll(Arrays.asList("A", "B", "D")));
        assertTrue(spokeRings.getServers("channel2", getTime(steps[0]), getTime(steps[3])).containsAll(Arrays.asList("A", "B", "C", "D")));
        assertTrue(spokeRings.getServers("channel2", getTime(steps[3]), getTime(steps[4])).containsAll(Arrays.asList("A", "B", "D")));
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
        assertTrue(spokeRings.getServers("test1").containsAll(Arrays.asList("B", "C", "E")));
        assertTrue(spokeRings.getServers("channel2").containsAll(Arrays.asList("A", "B", "D")));
        assertTrue(spokeRings.getServers("other").containsAll(Arrays.asList("B", "C", "E")));
        assertTrue(spokeRings.getServers("name").containsAll(Arrays.asList("A", "D", "E")));

        assertTrue(spokeRings.getServers("channel2", getTime(steps[0])).containsAll(Arrays.asList("A")));
        assertTrue(spokeRings.getServers("channel2", getTime(steps[1])).containsAll(Arrays.asList("A", "B")));
        assertTrue(spokeRings.getServers("channel2", getTime(steps[2])).containsAll(Arrays.asList("A", "B", "C")));
        assertTrue(spokeRings.getServers("channel2", getTime(steps[3])).containsAll(Arrays.asList("A", "B", "D")));
        assertTrue(spokeRings.getServers("channel2", getTime(steps[0]), getTime(steps[4])).containsAll(Arrays.asList("A", "B", "C", "D")));
        assertTrue(spokeRings.getServers("other", getTime(steps[0]), getTime(steps[4])).containsAll(Arrays.asList("A", "B", "C", "D", "E")));
        assertTrue(spokeRings.getServers("other", getTime(steps[5]), getTime(steps[6])).containsAll(Arrays.asList("B", "C", "E")));
        assertTrue(spokeRings.getServers("other", getTime(steps[4]), getTime(steps[6])).containsAll(Arrays.asList("B", "C", "E")));
    }

    @Test
    public void test5NodeRollingRestart() {
        Collection<ClusterEvent> events = ClusterEvent.set();

        addRollingRestarts(events, 0);

        SpokeRings spokeRings = new SpokeRings();
        spokeRings.process(events);

        compare(spokeRings.getServers("test1"), Arrays.asList("B", "C", "E"));
        compare(spokeRings.getServers("channel2"), Arrays.asList("A", "B", "D"));
        compare(spokeRings.getServers("name"), Arrays.asList("A", "D", "E"));
        compare(spokeRings.getServers("test3"), Arrays.asList("C", "D", "E"));
        compare(spokeRings.getServers("other"), Arrays.asList("B", "C", "E"));

        compare(spokeRings.getServers("other", getTime(steps[0] + HALF_STEP)), Arrays.asList("A"));
        compare(spokeRings.getServers("other", getTime(steps[1] + HALF_STEP)), Arrays.asList("A", "B"));
        compare(spokeRings.getServers("other", getTime(steps[2] + HALF_STEP)), Arrays.asList("A", "B", "C"));
        compare(spokeRings.getServers("other", getTime(steps[3] + HALF_STEP)), Arrays.asList("B", "C", "D"));
        compare(spokeRings.getServers("other", getTime(steps[4] + HALF_STEP)), Arrays.asList("B", "C", "E"));
        compare(spokeRings.getServers("other", getTime(steps[2] + HALF_STEP), getTime(steps[4] + HALF_STEP)), Arrays.asList("A", "B", "C", "D", "E"));

        compare(spokeRings.getServers("other", getTime(steps[5] + HALF_STEP)), Arrays.asList("B", "C", "E"));

        compare(spokeRings.getServers("other", getTime(steps[6] + HALF_STEP)), Arrays.asList("B", "C", "E"));
        compare(spokeRings.getServers("other", getTime(steps[7] + HALF_STEP)), Arrays.asList("C", "D", "E"));

        compare(spokeRings.getServers("other", getTime(steps[6] + HALF_STEP), getTime(steps[7] + HALF_STEP)), Arrays.asList("B", "C", "D", "E"));

        compare(spokeRings.getServers("other", getTime(steps[8] + HALF_STEP)), Arrays.asList("B", "C", "E"));
        compare(spokeRings.getServers("other", getTime(steps[9] + HALF_STEP)), Arrays.asList("B", "D", "E"));
        compare(spokeRings.getServers("other", getTime(steps[10] + HALF_STEP)), Arrays.asList("B", "C", "E"));
        compare(spokeRings.getServers("other", getTime(steps[11] + HALF_STEP)), Arrays.asList("B", "C", "E"));
        compare(spokeRings.getServers("other", getTime(steps[12] + HALF_STEP)), Arrays.asList("B", "C", "E"));
        compare(spokeRings.getServers("other", getTime(steps[13] + HALF_STEP)), Arrays.asList("B", "C", "D"));
        compare(spokeRings.getServers("other", getTime(steps[14] + HALF_STEP)), Arrays.asList("B", "C", "E"));
    }

    private void addRollingRestarts(Collection<ClusterEvent> events, long offset) {

        events.add(new ClusterEvent("/SCE/" + (steps[0] + offset) + "|A|ADDED", (steps[0] + offset)));
        events.add(new ClusterEvent("/SCE/" + (steps[1] + offset) + "|B|ADDED", (steps[1] + offset)));
        events.add(new ClusterEvent("/SCE/" + (steps[2] + offset) + "|C|ADDED", (steps[2] + offset)));
        events.add(new ClusterEvent("/SCE/" + (steps[3] + offset) + "|D|ADDED", (steps[3] + offset)));
        events.add(new ClusterEvent("/SCE/" + (steps[4] + offset) + "|E|ADDED", (steps[4] + offset)));
        events.add(new ClusterEvent("/SCE/" + (steps[0] + offset) + "|A|REMOVED", (steps[5] + offset)));

        events.add(new ClusterEvent("/SCE/" + (steps[6] + offset) + "|A|ADDED", (steps[6] + offset)));//
        events.add(new ClusterEvent("/SCE/" + (steps[1] + offset) + "|B|REMOVED", (steps[7] + offset)));
        events.add(new ClusterEvent("/SCE/" + (steps[8] + offset) + "|B|ADDED", (steps[8] + offset)));
        events.add(new ClusterEvent("/SCE/" + (steps[2] + offset) + "|C|REMOVED", (steps[9] + offset)));
        events.add(new ClusterEvent("/SCE/" + (steps[10] + offset) + "|C|ADDED", (steps[10] + offset)));
        events.add(new ClusterEvent("/SCE/" + (steps[3] + offset) + "|D|REMOVED", (steps[11] + offset)));
        events.add(new ClusterEvent("/SCE/" + (steps[12] + offset) + "|D|ADDED", (steps[12] + offset)));
        events.add(new ClusterEvent("/SCE/" + (steps[4] + offset) + "|E|REMOVED", (steps[13] + offset)));
        events.add(new ClusterEvent("/SCE/" + (steps[14] + offset) + "|E|ADDED", (steps[14] + offset)));
    }

    @Test
    public void testProcessCycleGenerateOld() {
        int spokeTtlMinutes = HubProperties.getSpokeTtlMinutes() + 100;
        long offset = -TimeUnit.MILLISECONDS.convert(spokeTtlMinutes, TimeUnit.MINUTES);

        Collection<ClusterEvent> events = ClusterEvent.set();
        addRollingRestarts(events, offset);
        addRollingRestarts(events, 0);

        SpokeRings spokeRings = new SpokeRings();
        Collection<ClusterEvent> oldEventIds = spokeRings.generateOld(events);
        assertEquals(10, oldEventIds.size());
        Set<ClusterEvent> oldSet = new HashSet<>(oldEventIds);
        Collection<ClusterEvent> filteredEvents = ClusterEvent.set();
        for (ClusterEvent event : events) {
            if (!oldSet.contains(event)) {
                filteredEvents.add(event);
            }
        }
        assertEquals(20, filteredEvents.size());
        SpokeRings newRings = new SpokeRings();
        Collection<ClusterEvent> newOldEventIds = newRings.generateOld(filteredEvents);

        assertEquals(0, spokeRings.compareTo(newRings));
        assertEquals(0, newOldEventIds.size());
    }

    @Test
    public void testProcessCycle2() {
        int spokeTtlMinutes = HubProperties.getSpokeTtlMinutes() + 100;
        long offset = -TimeUnit.MILLISECONDS.convert(spokeTtlMinutes, TimeUnit.MINUTES);

        Collection<ClusterEvent> events = ClusterEvent.set();
        addRollingRestarts(events, offset);
        addRollingRestarts(events, 0);

        for (ClusterEvent event : events) {
            System.out.println(event);
        }
    }


    private void compare(Collection<String> found, Collection<String> expected) {
        assertEquals(expected.size(), found.size());
        assertEquals(new TreeSet<>(expected).toString(), new TreeSet<>(found).toString());
    }

    private DateTime getTime(long millis) {
        return new DateTime(millis, DateTimeZone.UTC);
    }
}