package com.flightstats.hub.spoke;

import com.flightstats.hub.config.PropertiesLoader;
import com.flightstats.hub.config.SpokeProperties;
import com.flightstats.hub.model.ChannelContentKey;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.util.Commander;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpokeContentDaoTest {

    private SpokeProperties spokeProperties = new SpokeProperties(PropertiesLoader.getInstance());
    private Commander commander;
    private SpokeStore spokeStore;
    private String getOldestItemCommand;
    private String getItemCountCommand;

    @BeforeEach
    void initialize() {
        commander = mock(Commander.class);
        spokeStore = SpokeStore.WRITE;
        final String spokeStorePath = spokeProperties.getPath(spokeStore);
        getOldestItemCommand = String.format(SpokeContentDao.GET_OLDEST_ITEM_COMMAND, spokeStorePath);
        getItemCountCommand = String.format(SpokeContentDao.GET_ITEM_COUNT_COMMAND, spokeStorePath);
    }

    @Test
    void getOldestItemDoesExist() {
        when(commander.runInBash(getOldestItemCommand, 3)).thenReturn("1999-12-31+23:59:59.9999999999 /spoke/write/foo/1999/12/31/23/59/59999l33t");
        SpokeContentDao dao = new SpokeContentDao(commander, spokeProperties);
        Optional<ChannelContentKey> potentialKey = dao.getOldestItem(spokeStore);
        assertTrue(potentialKey.isPresent());
        ChannelContentKey key = potentialKey.get();
        assertEquals("foo", key.getChannel());
        assertEquals(new ContentKey(1999, 12, 31, 23, 59, 59, 999, "l33t"), key.getContentKey());
    }

    @Test
    void getOldestItemDoesNotExist() {
        when(commander.runInBash(getOldestItemCommand, 3)).thenReturn("");
        SpokeContentDao dao = new SpokeContentDao(commander, spokeProperties);
        Optional<ChannelContentKey> potentialKey = dao.getOldestItem(spokeStore);
        assertFalse(potentialKey.isPresent());
    }

    @Test
    void getNumberOfItems() {
        when(commander.runInBash(getItemCountCommand, 1)).thenReturn("12345");
        SpokeContentDao dao = new SpokeContentDao(commander, spokeProperties);
        assertEquals(12345, dao.getNumberOfItems(spokeStore));
    }

}
