package com.flightstats.hub.spoke;

import com.flightstats.hub.config.properties.SpokeProperties;
import com.flightstats.hub.model.ChannelContentKey;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.util.Commander;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.flightstats.hub.constant.ContentConstant.GET_ITEM_COUNT_COMMAND;
import static com.flightstats.hub.constant.ContentConstant.GET_OLDEST_ITEM_COMMAND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Execution(ExecutionMode.SAME_THREAD)
@ExtendWith(MockitoExtension.class)
class SpokeContentDaoTest {
    private Commander commander;
    private SpokeStore spokeStore;
    private String getOldestItemCommand;
    private String getItemCountCommand;
    @Mock
    private SpokeProperties spokeProperties;

    @BeforeEach
    void initialize() {
        commander = mock(Commander.class);
        spokeStore = SpokeStore.WRITE;
        final String spokeStorePath = spokeProperties.getPath(spokeStore);
        getOldestItemCommand = String.format(GET_OLDEST_ITEM_COMMAND, spokeStorePath);
        getItemCountCommand = String.format(GET_ITEM_COUNT_COMMAND, spokeStorePath);
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
