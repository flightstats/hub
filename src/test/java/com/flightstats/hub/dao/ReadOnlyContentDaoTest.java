package com.flightstats.hub.dao;

import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.DirectionQuery;
import com.flightstats.hub.model.TimeQuery;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ReadOnlyContentDaoTest {
    @Mock private Traces traces;
    @Mock private ContentDao delegate;
    private ReadOnlyContentDao dao;

    @BeforeEach
    public void setup() {
        this.dao = new ReadOnlyContentDao(delegate);
    }

    @Test
    public void testAllowsReads() {
        dao.initialize();
        dao.get("channelName", ContentKey.NONE);
        dao.getLatest("channelName", ContentKey.NONE, traces);
        dao.query(mock(DirectionQuery.class));
        dao.queryByTime(TimeQuery.builder().build());
        // Mockito 2.0 will support default methods.
        //dao.readBatch("channelName", ContentKey.NONE);
        //dao.streamMinute("channelName", mock(MinutePath.class), true, mock(Consumer.class));
        verify(delegate, times(1)).initialize();
        verify(delegate, times(1)).get("channelName", ContentKey.NONE);
        verify(delegate, times(1)).getLatest("channelName", ContentKey.NONE, traces);
        verify(delegate, times(1)).query(any(DirectionQuery.class));
        verify(delegate, times(1)).queryByTime(any(TimeQuery.class));
    }

    @Test
    @SneakyThrows
    public void testPreventsInsert() {
        assertThrows(UnsupportedOperationException.class, () -> dao.insert("channelName", mock(Content.class)));
    }

    @Test
    public void testPreventsDelete() {
        assertThrows(UnsupportedOperationException.class, () -> dao.delete("channelName"));
    }

    @Test
    public void testPreventsDeleteBefore() {
        assertThrows(UnsupportedOperationException.class, () -> dao.deleteBefore("channelName", ContentKey.NONE));
    }
}