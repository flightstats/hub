package com.flightstats.hub.dao;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ReadOnlyDocumentationDaoTest {
    @Mock private DocumentationDao delegate;
    private ReadOnlyDocumentationDao dao;

    @Before
    public void setup() {
        dao = new ReadOnlyDocumentationDao(delegate);
    }

    @Test
    public void testCanReadDocs() {
        dao.get("channelName");
        verify(delegate, times(1)).get("channelName");
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testPreventsInsert() {
        dao.upsert("channelName", new byte[]{});
    }

    @Test(expected=UnsupportedOperationException.class)
    public void testPreventsDelete() {
        dao.delete("channelName");
    }

}