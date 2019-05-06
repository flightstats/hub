package com.flightstats.hub.spoke;

import com.flightstats.hub.model.ContentKey;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ReadOnlyClusterSpokeStoreTest {
    @Mock ClusterWriteSpoke writeSpoke;
    private ReadOnlyClusterSpokeStore dao;

    @Before
    public void setup() {
        dao = new ReadOnlyClusterSpokeStore(writeSpoke);
    }

    @Test
    @SneakyThrows
    public void testAllowsReadingFromWriteCluster() {
        dao.getFromWriteCluster("path", ContentKey.NONE);
        dao.readTimeBucketFromWriteCluster("channel", "timePath");

        verify(dao, times(1)).getFromWriteCluster("path", ContentKey.NONE);
        verify(dao, times(1)).readTimeBucketFromWriteCluster("channel", "timePath");
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testPreventsInsert() {
        dao.insertToWriteCluster( "path", new byte[]{}, "api", "channel");
    }

    @Test(expected=UnsupportedOperationException.class)
    @SneakyThrows
    public void testPreventsDelete() {
        dao.deleteFromWriteCluster( "path");
    }

}