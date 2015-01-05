package com.flightstats.hub.replication;

import com.flightstats.hub.model.exception.ForbiddenRequestException;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;

import java.util.TreeSet;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class ReplicationValidatorTest {

    private ReplicationValidator validator;

    @Before
    public void setUp() throws Exception {
        ReplicationDao replicationDao = mock(ReplicationDao.class);
        ReplicationDomain domainA = ReplicationDomain.builder().domain("A").excludeExcept(new TreeSet<>(Sets.newHashSet("one", "two", "three"))).build();
        ReplicationDomain domainB = ReplicationDomain.builder().domain("B").excludeExcept(new TreeSet<>(Sets.newHashSet("red", "yellow", "blue"))).build();

        when(replicationDao.getDomains(anyBoolean())).thenReturn(Sets.newHashSet(domainA, domainB));

        validator = new ReplicationValidator(replicationDao);
    }

    @Test
    public void testValidateDomainExistingDomain() throws Exception {
        ReplicationDomain domain = ReplicationDomain.builder().domain("A")
                .excludeExcept(new TreeSet<>(Sets.newHashSet("one", "two", "three", "four"))).build();
        validator.validateDomain(domain);
    }

    @Test
    public void testValidateDomainNewDomain() throws Exception {
        ReplicationDomain domain = ReplicationDomain.builder().domain("C")
                .excludeExcept(new TreeSet<>(Sets.newHashSet("apple", "orange", "mango"))).build();
        validator.validateDomain(domain);
    }

    @Test(expected = ForbiddenRequestException.class)
    public void testValidateDomainExistingChannels() throws Exception {
        ReplicationDomain domain = ReplicationDomain.builder().domain("D")
                .excludeExcept(new TreeSet<>(Sets.newHashSet("three"))).build();
        validator.validateDomain(domain);
    }
}
