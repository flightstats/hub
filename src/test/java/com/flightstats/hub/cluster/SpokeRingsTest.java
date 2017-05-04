package com.flightstats.hub.cluster;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SpokeRingsTest {

    private static final int STEP = 1000;
    private static final int HALF_STEP = STEP / 2;
    private long[] steps;

    @Before
    public void setUp() throws Exception {
        long start = System.currentTimeMillis() - 100 * 1000;
        steps = new long[100];
        for (int i = 0; i < steps.length; i++) {
            steps[i] = start + STEP * i;
        }
    }

    @Test
    public void test3Nodes() {
        List<String> strings = Arrays.asList(steps[0] + "|A|ADDED", steps[1] + "|B|ADDED", steps[2] + "|C|ADDED");
        SpokeRings spokeRings = new SpokeRings();
        spokeRings.process(strings);
        assertTrue(spokeRings.getNodes("test1").containsAll(Arrays.asList("A", "B", "C")));
        assertTrue(spokeRings.getNodes("channel2").containsAll(Arrays.asList("A", "B", "C")));
        assertTrue(spokeRings.getNodes("channel2", getTime(steps[0])).containsAll(Arrays.asList("A")));
        assertTrue(spokeRings.getNodes("channel2", getTime(steps[1])).containsAll(Arrays.asList("A", "B")));
        assertTrue(spokeRings.getNodes("channel2", getTime(steps[2])).containsAll(Arrays.asList("A", "B", "C")));
        assertTrue(spokeRings.getNodes("channel2", getTime(steps[0]), getTime(steps[2])).containsAll(Arrays.asList("A", "B", "C")));
    }

    @Test
    public void test4Nodes() {
        List<String> strings = Arrays.asList(steps[0] + "|A|ADDED", steps[1] + "|B|ADDED", steps[2] + "|C|ADDED", steps[3] + "|D|ADDED");
        SpokeRings spokeRings = new SpokeRings();
        spokeRings.process(strings);
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
        List<String> strings = Arrays.asList(steps[0] + "|A|ADDED", steps[1] + "|B|ADDED", steps[2] + "|C|ADDED",
                steps[3] + "|D|ADDED", steps[4] + "|E|ADDED");
        SpokeRings spokeRings = new SpokeRings();
        spokeRings.process(strings);
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
        List<String> strings = create5RollingRestartEvents();
        check5RollingRestart(strings);
    }

    private List<String> create5RollingRestartEvents() {
        List<String> strings = new ArrayList<>();
        strings.add("10|A|ADDED");
        strings.add("11|B|ADDED");
        strings.add("12|C|ADDED");
        strings.add("13|D|ADDED");
        strings.add("14|E|ADDED");
        strings.add(steps[0] + "|A|REMOVED");
        strings.add(steps[1] + "|A|ADDED");
        strings.add(steps[2] + "|B|REMOVED");
        strings.add(steps[3] + "|B|ADDED");
        strings.add(steps[4] + "|C|REMOVED");
        strings.add(steps[5] + "|C|ADDED");
        strings.add(steps[6] + "|D|REMOVED");
        strings.add(steps[7] + "|D|ADDED");
        strings.add(steps[8] + "|E|REMOVED");
        strings.add(steps[9] + "|E|ADDED");
        return strings;
    }

    @Test
    public void test5NodeRollingRestartDupes() {
        List<String> strings = create5RollingRestartEvents();
        List<String> dupes = new ArrayList<>();
        dupes.addAll(strings);
        dupes.addAll(strings);
        Collections.shuffle(dupes);
        check5RollingRestart(dupes);
    }

    private void compare(Collection<String> found, Collection<String> expected) {
        assertEquals(expected.size(), found.size());
        assertEquals(new TreeSet<>(expected).toString(), new TreeSet<>(found).toString());
    }

    private void check5RollingRestart(List<String> strings) {
        SpokeRings spokeRings = new SpokeRings();
        spokeRings.process(strings);

        compare(spokeRings.getNodes("test1"), Arrays.asList("B", "C", "E"));
        compare(spokeRings.getNodes("channel2"), Arrays.asList("A", "B", "D"));
        compare(spokeRings.getNodes("name"), Arrays.asList("A", "D", "E"));
        compare(spokeRings.getNodes("test3"), Arrays.asList("C", "D", "E"));

        compare(spokeRings.getNodes("other"), Arrays.asList("B", "C", "E"));
        compare(spokeRings.getNodes("other", getTime(steps[0] - HALF_STEP)), Arrays.asList("B", "C", "E"));
        compare(spokeRings.getNodes("other", getTime(steps[0] + HALF_STEP)), Arrays.asList("B", "C", "E"));
        compare(spokeRings.getNodes("other", getTime(steps[1] + HALF_STEP)), Arrays.asList("B", "C", "E"));

        compare(spokeRings.getNodes("other", getTime(steps[2] + HALF_STEP)), Arrays.asList("C", "D", "E"));
        compare(spokeRings.getNodes("other", getTime(steps[3] + HALF_STEP)), Arrays.asList("B", "C", "E"));
        compare(spokeRings.getNodes("other", getTime(steps[4] + HALF_STEP)), Arrays.asList("B", "D", "E"));
        compare(spokeRings.getNodes("other", getTime(steps[5] + HALF_STEP)), Arrays.asList("B", "C", "E"));
        compare(spokeRings.getNodes("other", getTime(steps[6] + HALF_STEP)), Arrays.asList("B", "C", "E"));
        compare(spokeRings.getNodes("other", getTime(steps[7] + HALF_STEP)), Arrays.asList("B", "C", "E"));
        compare(spokeRings.getNodes("other", getTime(steps[8] + HALF_STEP)), Arrays.asList("B", "C", "D"));
        compare(spokeRings.getNodes("other", getTime(steps[9] + HALF_STEP)), Arrays.asList("B", "C", "E"));
    }

    private DateTime getTime(long millis) {
        return new DateTime(millis, DateTimeZone.UTC);
    }
}