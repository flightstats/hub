package com.flightstats.hub.spoke;

import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.ContentKey;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ReadOnlyRemoteSpokeStoreTest {
    @Mock RemoteSpokeStore delegate;
    private ReadOnlyRemoteSpokeStore dao;

    @Before
    public void setup() {
        dao = new ReadOnlyRemoteSpokeStore(delegate);
    }

    @Test
    @SneakyThrows
    public void testDelegatesReadOperations() {
        dao.get(SpokeStore.READ, "somePath", ContentKey.NONE);
        Traces mockTraces = mock(Traces.class);
        dao.getLatest("someChannel", "somePath", mockTraces);
        dao.getNext("channel", 0, "startKey");
        dao.readTimeBucket(SpokeStore.READ, "channel", "timePath");
        dao.testAll();
        dao.testOne(Arrays.asList("BobsAServer"));

        verify(delegate, times(1)).get(SpokeStore.READ, "somePath", ContentKey.NONE);
        verify(delegate, times(1)).getLatest("someChannel", "somePath", mockTraces);
        verify(delegate, times(1)).getNext("channel", 0, "startKey");
        verify(delegate, times(1)).readTimeBucket(SpokeStore.READ, "channel", "timePath");
        verify(delegate, times(1)).testAll();
        verify(delegate, times(1)).testOne(anyList());
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testPreventsInsert() {
        dao.insert(SpokeStore.WRITE, "path", new byte[]{}, "api", "channel");
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testPreventsInsertWithTraces() {
        dao.insert(SpokeStore.WRITE, "path", new byte[]{}, Arrays.asList("someServer"), mock(Traces.class), "api", "channel");
    }

    @Test(expected=UnsupportedOperationException.class)
    @SneakyThrows
    public void testPreventsDelete() {
        dao.delete(SpokeStore.WRITE, "path");
    }

}