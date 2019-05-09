package com.flightstats.hub.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ReadOnlyDocumentationDaoTest {
    @Mock private DocumentationDao delegate;
    private ReadOnlyDocumentationDao dao;

    @BeforeEach
    public void setup() {
        dao = new ReadOnlyDocumentationDao(delegate);
    }

    @Test
    public void testCanReadDocs() {
        dao.get("channelName");
        verify(delegate, times(1)).get("channelName");
    }

    @Test
    public void testPreventsInsert() {
        assertThrows(UnsupportedOperationException.class, () -> dao.upsert("channelName", new byte[]{}));
    }

    @Test
    public void testPreventsDelete() {
        assertThrows(UnsupportedOperationException.class, () -> dao.delete("channelName"));
    }

}