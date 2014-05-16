package com.flightstats.hub.replication;

import com.flightstats.hub.model.exception.ForbiddenRequestException;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;

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
        ReplicationDomain domainA = ReplicationDomain.builder().withDomain("A").withExcludedExcept(Lists.newArrayList("one", "two", "three")).build();
        ReplicationDomain domainB = ReplicationDomain.builder().withDomain("B").withExcludedExcept(Lists.newArrayList("red", "yellow", "blue")).build();

        when(replicationDao.getDomains(anyBoolean())).thenReturn(Lists.newArrayList(domainA, domainB));

        validator = new ReplicationValidator(replicationDao);
    }

    @Test
    public void testValidateDomainExistingDomain() throws Exception {
        ReplicationDomain domain = ReplicationDomain.builder().withDomain("A")
                .withExcludedExcept(Lists.newArrayList("one", "two", "three", "four")).build();
        validator.validateDomain(domain);
    }

    @Test
    public void testValidateDomainNewDomain() throws Exception {
        ReplicationDomain domain = ReplicationDomain.builder().withDomain("C")
                .withExcludedExcept(Lists.newArrayList("apple", "orange", "mango")).build();
        validator.validateDomain(domain);
    }

    @Test(expected = ForbiddenRequestException.class)
    public void testValidateDomainExistingChannels() throws Exception {
        ReplicationDomain domain = ReplicationDomain.builder().withDomain("D")
                .withExcludedExcept(Lists.newArrayList("three")).build();
        validator.validateDomain(domain);
    }
}
