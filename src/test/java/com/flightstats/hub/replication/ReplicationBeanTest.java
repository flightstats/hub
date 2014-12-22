package com.flightstats.hub.replication;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class ReplicationBeanTest {

    @Test
    public void testReplicationDomainOrdering() throws Exception {
        ReplicationDomain a = ReplicationDomain.builder().domain("A").build();
        ReplicationDomain b = ReplicationDomain.builder().domain("B").build();
        ReplicationDomain c = ReplicationDomain.builder().domain("C").build();
        List<ReplicationDomain> replicationDomains = Arrays.asList(c, a, b);
        ReplicationBean replicationBean = new ReplicationBean(replicationDomains, Collections.EMPTY_LIST);
        ArrayList<ReplicationDomain> domains = new ArrayList<>(replicationBean.getDomains());
        assertEquals("A", domains.get(0).getDomain());
        assertEquals("B", domains.get(1).getDomain());
        assertEquals("C", domains.get(2).getDomain());
    }

    @Test
    public void testReplicationStatusOrdering() throws Exception {
        ReplicationStatus aOne = new ReplicationStatus();
        aOne.setChannel(new Channel("one", "http://hub.a/channel/one"));
        ReplicationStatus aTwo = new ReplicationStatus();
        aTwo.setChannel(new Channel("two", "http://hub.a/channel/two"));
        ReplicationStatus bOne = new ReplicationStatus();
        bOne.setChannel(new Channel("one", "http://hub.b/channel/one"));
        List<ReplicationStatus> statuses = Arrays.asList(bOne, aOne, aTwo);
        ReplicationBean replicationBean = new ReplicationBean(Collections.EMPTY_LIST, statuses);
        ArrayList<ReplicationStatus> statusArrayList = new ArrayList<>(replicationBean.getStatus());
        assertEquals(aOne.getUrl(), statusArrayList.get(0).getUrl());
        assertEquals(aTwo.getUrl(), statusArrayList.get(1).getUrl());
        assertEquals(bOne.getUrl(), statusArrayList.get(2).getUrl());

    }
}
