package com.flightstats.datahub.replication;

import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 *
 */
public class ReplicationDomainTest {

    private List<String> list = Lists.newArrayList("one", "two", "three");

    @Test
    public void testInclude() throws Exception {
        ReplicationDomain config = getIncluded();
        assertEquals("incl", config.getDomain());
        assertEquals(0, config.getHistoricalDays());
        assertTrue(config.getIncludeExcept().containsAll(list));
        assertTrue(config.getExcludeExcept().isEmpty());
        assertTrue(config.isValid());
        assertTrue(config.isInclusive());
    }

    private ReplicationDomain getIncluded() {
        return ReplicationDomain.builder().withDomain("incl")
                    .withIncludedExcept(list)
                    .build();
    }

    @Test
    public void testExclude() throws Exception {
        ReplicationDomain config = ReplicationDomain.builder().withDomain("exclude")
                .withHistoricalDays(10)
                .withExcludedExcept(list)
                .build();
        assertEquals("exclude", config.getDomain());
        assertEquals(10, config.getHistoricalDays());
        assertTrue(config.getExcludeExcept().containsAll(list));
        assertTrue(config.getIncludeExcept().isEmpty());
        assertTrue(config.isValid());
        assertFalse(config.equals(getIncluded()));
        assertFalse(config.isInclusive());
    }

    @Test
    public void testInvalid() throws Exception {
        ReplicationDomain config = ReplicationDomain.builder().withDomain("exclude")
                .withExcludedExcept(list)
                .withIncludedExcept(list)
                .build();
        assertFalse(config.equals(getIncluded()));
        assertFalse(config.isValid());
    }

    @Test
    public void testNone() throws Exception {
        ReplicationDomain config = ReplicationDomain.builder().withDomain("exclude").build();
        assertFalse(config.equals(getIncluded()));
        assertFalse(config.isValid());
    }

    @Test
    public void testEquals() throws Exception {
        ReplicationDomain included1 = getIncluded();
        ReplicationDomain included2 = getIncluded();
        assertTrue(included1.equals(included2));
        assertTrue(included2.equals(included1));
    }
}
