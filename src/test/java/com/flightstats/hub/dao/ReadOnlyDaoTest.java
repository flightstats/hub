package com.flightstats.hub.dao;

import com.flightstats.hub.webhook.Webhook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ReadOnlyDaoTest {
    @Mock Dao<Webhook> delegate;
    @Mock Webhook webhook;

    @Test
    public void testAllowsReads() {
        Dao<Webhook> dao = new ReadOnlyDao<>(delegate);

        dao.getCached("getCached");
        dao.get("get");
        dao.getAll(true);
        dao.exists("exists");
        dao.refresh();

        verify(delegate, times(1)).getAll(true);
        // TODO: Mockito 2.0 will fix testing default methods.
        //verify(delegate, times(1)).getCached("getCached");
        //verify(delegate, times(1)).get("get");
        //verify(delegate, times(1)).exists("exists");
        //verify(delegate, times(1)).refresh();
        //verify(delegate, atMost(5));
    }

    @Test
    public void testPreventsUpsert() {
        assertThrows(UnsupportedOperationException.class, () -> new ReadOnlyDao<>(delegate).upsert(webhook));
    }

    @Test
    public void testPreventsDelete() {
        assertThrows(UnsupportedOperationException.class, () -> new ReadOnlyDao<>(delegate).delete("delete"));
    }

}