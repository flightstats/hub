package com.flightstats.hub.dao;

import com.flightstats.hub.webhook.Webhook;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
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

    @Test(expected=UnsupportedOperationException.class)
    public void testPreventsUpsert() {
        new ReadOnlyDao<>(delegate).upsert(webhook);
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testPreventsDelete() {
        new ReadOnlyDao<>(delegate).delete("delete");
    }

}