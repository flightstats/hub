package com.flightstats.hub.dao;

import org.apache.curator.framework.recipes.shared.SharedValue;
import org.junit.Test;

public class SequenceKeyCoordinationTest {
    private SharedValue sharedValue;
    private SequenceKeyCoordination keyCoordination;

    //todo - gfm - 5/23/14 - figure these out
    @Test
    public void test() throws Exception {

    }
    /*@Before
    public void setUp() throws Exception {
        WebsocketPublisher publisher = mock(WebsocketPublisher.class);
        CuratorFramework curator = mock(CuratorFramework.class);
        sharedValue = mock(SharedValue.class);
        keyCoordination = new SequenceKeyCoordination(publisher, curator) {
            @Override
            SharedValue getSharedValue(String channelName) {
                return sharedValue;
            }
        };
    }

    @Test
    public void testLow() throws Exception {
        when(sharedValue.getValue()).thenReturn(Longs.toByteArray(2000));
        keyCoordination.insert("blah", new SequenceContentKey(1999));
        verify(sharedValue, never()).trySetValue((byte[]) any());
    }

    @Test
    public void testSame() throws Exception {
        when(sharedValue.getValue()).thenReturn(Longs.toByteArray(2000));
        keyCoordination.insert("blah", new SequenceContentKey(2000));
        verify(sharedValue, never()).trySetValue((byte[]) any());
    }

    @Test
    public void testHigh() throws Exception {
        when(sharedValue.getValue()).thenReturn(Longs.toByteArray(2000));
        when(sharedValue.trySetValue((byte[]) any())).thenReturn(true);
        keyCoordination.insert("blah", new SequenceContentKey(2001));
        verify(sharedValue, times(1)).trySetValue(Longs.toByteArray(2001));
    }

    @Test
    public void testTrySetFail() throws Exception {
        when(sharedValue.getValue()).thenReturn(Longs.toByteArray(2000));
        when(sharedValue.trySetValue((byte[]) any())).thenReturn(false);
        keyCoordination.insert("blah", new SequenceContentKey(2001));
        verify(sharedValue, times(3)).trySetValue(Longs.toByteArray(2001));
    }

    @Test
    public void testTrySetSucceed() throws Exception {
        when(sharedValue.getValue()).thenReturn(Longs.toByteArray(2000));
        when(sharedValue.trySetValue((byte[]) any())).thenReturn(false).thenReturn(true);
        keyCoordination.insert("blah", new SequenceContentKey(2001));
        verify(sharedValue, times(2)).trySetValue(Longs.toByteArray(2001));
    }

    @Test
    public void testTrySetChanged() throws Exception {
        when(sharedValue.getValue()).thenReturn(Longs.toByteArray(2000)).thenReturn(Longs.toByteArray(2002));
        when(sharedValue.trySetValue((byte[]) any())).thenReturn(false);
        keyCoordination.insert("blah", new SequenceContentKey(2001));
        verify(sharedValue, times(1)).trySetValue(Longs.toByteArray(2001));
    }*/

}
