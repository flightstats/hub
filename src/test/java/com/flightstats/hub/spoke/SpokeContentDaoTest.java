package com.flightstats.hub.spoke;

import com.flightstats.hub.config.PropertyLoader;
import com.flightstats.hub.config.SpokeProperty;
import com.flightstats.hub.model.ChannelContentKey;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.util.Commander;
import com.google.common.base.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SpokeContentDaoTest {

    private SpokeProperty spokeProperty = new SpokeProperty(PropertyLoader.getInstance());
    private Commander commander;
    private SpokeStore spokeStore;
    private String getOldestItemCommand;
    private String getItemCountCommand;

    @BeforeEach
    void initialize() {
        commander = mock(Commander.class);
        spokeStore = SpokeStore.WRITE;
        String spokeStorePath = spokeProperty.getPath(spokeStore);
        getOldestItemCommand = String.format(SpokeContentDao.GET_OLDEST_ITEM_COMMAND, spokeStorePath);
        getItemCountCommand = String.format(SpokeContentDao.GET_ITEM_COUNT_COMMAND, spokeStorePath);
    }

    @Test
    void getOldestItemDoesExist() {
        when(commander.runInBash(getOldestItemCommand, 3)).thenReturn("1999-12-31+23:59:59.9999999999 /spoke/write/foo/1999/12/31/23/59/59999l33t");
        SpokeContentDao dao = new SpokeContentDao(commander, spokeProperty);
        Optional<ChannelContentKey> potentialKey = dao.getOldestItem(spokeStore);
        assertTrue(potentialKey.isPresent());
        ChannelContentKey key = potentialKey.get();
        assertEquals("foo", key.getChannel());
        assertEquals(new ContentKey(1999, 12, 31, 23, 59, 59, 999, "l33t"), key.getContentKey());
    }

    @Test
    void getOldestItemDoesNotExist() {
        when(commander.runInBash(getOldestItemCommand, 3)).thenReturn("");
        SpokeContentDao dao = new SpokeContentDao(commander, spokeProperty);
        Optional<ChannelContentKey> potentialKey = dao.getOldestItem(spokeStore);
        assertFalse(potentialKey.isPresent());
    }

    @Test
    void getNumberOfItems() {
        when(commander.runInBash(getItemCountCommand, 1)).thenReturn("12345");
        SpokeContentDao dao = new SpokeContentDao(commander, spokeProperty);
        assertEquals(12345, dao.getNumberOfItems(spokeStore));
    }

}
