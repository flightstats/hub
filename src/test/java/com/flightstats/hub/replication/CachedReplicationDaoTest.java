package com.flightstats.hub.replication;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class CachedReplicationDaoTest {

    private CachedReplicationDao cachedReplicationDao;
    private ReplicationDao dao;

    @Before
    public void setUp() throws Exception {
        dao = mock(ReplicationDao.class);
        Collection<ReplicationDomain> domains = getReplicationDomains("foo", "bar");
        when((dao.getDomains(anyBoolean()))).thenReturn(domains);
        cachedReplicationDao = new CachedReplicationDao(dao);

    }

    private Collection<ReplicationDomain> getReplicationDomains(String first, String second) {
        Collection<ReplicationDomain> domains = new ArrayList<>();
        domains.add(ReplicationDomain.builder().domain(first).build());
        domains.add(ReplicationDomain.builder().domain(second).build());
        return domains;
    }

    @Test
    public void testNullFalse() throws Exception {
        Collection<ReplicationDomain> domains = cachedReplicationDao.getDomains(false);
        assertEquals(2, domains.size());
    }

    @Test
    public void testNullTrue() throws Exception {
        Collection<ReplicationDomain> domains = cachedReplicationDao.getDomains(true);
        assertEquals(2, domains.size());
    }

    @Test
    public void testRefresh() throws Exception {
        when((dao.getDomains(anyBoolean()))).thenReturn(getReplicationDomains("foo", "bar")).thenReturn(getReplicationDomains("A", "B"));
        cachedReplicationDao.getDomains(false);
        Collection<ReplicationDomain> domains = cachedReplicationDao.getDomains(true);
        assertEquals(2, domains.size());
        assertTrue(domains.contains(ReplicationDomain.builder().domain("A").build()));
        assertTrue(domains.contains(ReplicationDomain.builder().domain("B").build()));
    }

    @Test
    public void testCached() throws Exception {
        when((dao.getDomains(anyBoolean()))).thenReturn(getReplicationDomains("foo", "bar")).thenReturn(getReplicationDomains("A", "B"));
        cachedReplicationDao.getDomains(false);
        Collection<ReplicationDomain> domains = cachedReplicationDao.getDomains(false);
        assertEquals(2, domains.size());
        assertTrue(domains.contains(ReplicationDomain.builder().domain("foo").build()));
        assertTrue(domains.contains(ReplicationDomain.builder().domain("bar").build()));
    }
}
