package com.flightstats.hub.spoke;

import com.flightstats.hub.model.ContentKey;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ReadOnlyClusterSpokeStoreTest {
    @Mock ClusterWriteSpoke writeSpoke;
    private ReadOnlyClusterSpokeStore dao;

    @BeforeEach
    public void setup() {
        dao = new ReadOnlyClusterSpokeStore(writeSpoke);
    }

    @Test
    @SneakyThrows
    public void testAllowsReadingFromWriteCluster() {
        dao.getFromWriteCluster("path", ContentKey.NONE);
        dao.readTimeBucketFromWriteCluster("channel", "timePath");

        verify(writeSpoke, times(1)).getFromWriteCluster("path", ContentKey.NONE);
        verify(writeSpoke, times(1)).readTimeBucketFromWriteCluster("channel", "timePath");
    }

    @Test
    public void testPreventsInsert() {
        assertThrows(UnsupportedOperationException.class, () -> dao.insertToWriteCluster( "path", new byte[]{}, "api", "channel"));
    }

    @Test
    @SneakyThrows
    public void testPreventsDelete() {
        assertThrows(UnsupportedOperationException.class, () -> dao.deleteFromWriteCluster( "path"));
    }

}