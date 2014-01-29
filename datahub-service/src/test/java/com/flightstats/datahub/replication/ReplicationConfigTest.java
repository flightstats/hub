package com.flightstats.datahub.replication;

import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 *
 */
public class ReplicationConfigTest {

    private List<String> list = Lists.newArrayList("one", "two", "three");

    @Test
    public void testInclude() throws Exception {
        ReplicationConfig config = getIncluded();
        assertEquals("incl", config.getDomain());
        assertEquals(0, config.getHistoricalDays());
        assertTrue(config.getIncludeExcept().containsAll(list));
        assertTrue(config.getExcludeExcept().isEmpty());
        assertTrue(config.isValid());
    }

    private ReplicationConfig getIncluded() {
        return ReplicationConfig.builder().withDomain("incl")
                    .withIncludedExcept(list)
                    .build();
    }

    @Test
    public void testExclude() throws Exception {
        ReplicationConfig config = ReplicationConfig.builder().withDomain("exclude")
                .withHistoricalDays(10)
                .withExcludedExcept(list)
                .build();
        assertEquals("exclude", config.getDomain());
        assertEquals(10, config.getHistoricalDays());
        assertTrue(config.getExcludeExcept().containsAll(list));
        assertTrue(config.getIncludeExcept().isEmpty());
        assertTrue(config.isValid());
        assertFalse(config.equals(getIncluded()));
    }

    @Test
    public void testInvalid() throws Exception {
        ReplicationConfig config = ReplicationConfig.builder().withDomain("exclude")
                .withExcludedExcept(list)
                .withIncludedExcept(list)
                .build();
        assertFalse(config.equals(getIncluded()));
        assertFalse(config.isValid());
    }

    @Test
    public void testNone() throws Exception {
        ReplicationConfig config = ReplicationConfig.builder().withDomain("exclude").build();
        assertFalse(config.equals(getIncluded()));
        assertFalse(config.isValid());
    }

    @Test
    public void testEquals() throws Exception {
        ReplicationConfig included1 = getIncluded();
        ReplicationConfig included2 = getIncluded();
        assertTrue(included1.equals(included2));
        assertTrue(included2.equals(included1));
    }
}
