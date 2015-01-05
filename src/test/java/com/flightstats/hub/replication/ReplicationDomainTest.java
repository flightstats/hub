package com.flightstats.hub.replication;

import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.List;
import java.util.TreeSet;

import static org.junit.Assert.*;

/**
 *
 */
public class ReplicationDomainTest {

    private List<String> list = Lists.newArrayList("one", "two", "three");

    private ReplicationDomain getExcluded() {
        return ReplicationDomain.builder().domain("exclude")
                .excludeExcept(new TreeSet<>(list))
                .historicalDays(10)
                .build();
    }

    @Test
    public void testExclude() throws Exception {
        ReplicationDomain domain = getExcluded();
        assertEquals("exclude", domain.getDomain());
        assertEquals(10, domain.getHistoricalDays());
        assertTrue(domain.getExcludeExcept().containsAll(list));
        assertTrue(domain.isValid());
    }

    @Test
    public void testNone() throws Exception {
        ReplicationDomain domain = ReplicationDomain.builder().domain("exclude").build();
        assertFalse(domain.equals(getExcluded()));
        assertFalse(domain.isValid());
        assertEquals(0, domain.getHistoricalDays());
    }

    @Test
    public void testEquals() throws Exception {
        ReplicationDomain included1 = getExcluded();
        ReplicationDomain included2 = getExcluded();
        assertTrue(included1.equals(included2));
        assertTrue(included2.equals(included1));
    }
}
