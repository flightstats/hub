package com.flightstats.hub.dao;

import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.util.Commander;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

import java.io.File;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import static org.mockito.Mockito.mock;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;


@Slf4j
class TtlEnforcerTest {
    // verify each time that the enforcer ignores this file
    private static final String LOST_AND_FOUND_DIR = "lost+found";

    @Mock
    private ChannelService channelService;
    @Mock
    private Commander commander;

    private TtlEnforcer ttlEnforcer;

    private final Consumer<ChannelConfig> callback = config -> {
    };

    private String[] getSpokePathList(File spoke) {
        return Optional.ofNullable(spoke.list()).orElse(new String[] {});
    }

    private boolean createMockSpokeFiles(File dir, String... fileNames) {
        return Stream.of(fileNames).allMatch(str -> {
            try {
                return new File(dir.toPath() + "/" + str).createNewFile();
            } catch (Exception error) {
                log.error("error adding files to spoke path {}", error.getMessage());
                return false;
            }
        });
    }

    @BeforeEach
    void setup() {
        initMocks(this);
        ttlEnforcer = new TtlEnforcer(commander);
    }

    @Test
    void enforce_deletesFilteredChildPaths_affirmThreeFilesDeleted(@TempDir File spoke) {
        assertEquals(0, getSpokePathList(spoke).length);
        assertTrue(createMockSpokeFiles(spoke, "a", "b", "c", LOST_AND_FOUND_DIR));
        ttlEnforcer.deleteFilteredPaths(spoke.getPath(), channelService, callback);
        verify(commander, times(3)).runInBash(anyString(), anyInt());
    }

    @Test
    void enforce_deletesFilteredUpperCaseChildPaths_affirmTwoFilesDeleted(@TempDir File spoke) {
        assertEquals(0, getSpokePathList(spoke).length);
        assertTrue(createMockSpokeFiles(spoke, "BacA", "aCaB", LOST_AND_FOUND_DIR));
        ttlEnforcer.deleteFilteredPaths(spoke.getPath(), channelService, callback);
        verify(commander, times(2)).runInBash(anyString(), anyInt());
    }

    @Test
    void testdDeleteFilteredPathsRunsSpecificCommand(@TempDir File spoke) {
        assertEquals(0, getSpokePathList(spoke).length);
        String directory = "testDir";
        assertTrue(createMockSpokeFiles(spoke, directory, LOST_AND_FOUND_DIR));
        ttlEnforcer.deleteFilteredPaths(spoke.getPath(), channelService, callback);
        String command = "find .* -type f --mmin +360 -delete";
        verify(commander, times(1)).runInBash(matches(command), anyInt());
    }

    @Test
    void enforce_ignoresDeleteOfRegisteredPaths_affirmNoFilesDeleted(@TempDir File spoke) {
        // GIVEN
        assertEquals(0, getSpokePathList(spoke).length);
        String[] names = new String[]{ "AoAo", "CCCp" };
        ChannelConfig channelConfig1 = mock(ChannelConfig.class);
        ChannelConfig channelConfig2 = mock(ChannelConfig.class);

        // WHEN
        when(channelConfig1.getDisplayName()).thenReturn(names[0]);
        when(channelConfig1.getLowerCaseName()).thenCallRealMethod();
        when(channelConfig2.getDisplayName()).thenReturn(names[1]);
        when(channelConfig2.getLowerCaseName()).thenCallRealMethod();
        when(channelService.getChannels()).thenReturn(Arrays.asList(channelConfig1, channelConfig2));

        // THEN
        assertTrue(createMockSpokeFiles(spoke, names[0], names[1], LOST_AND_FOUND_DIR));
        ttlEnforcer.deleteFilteredPaths(spoke.getPath(), channelService, callback);
        verify(commander, never()).runInBash(anyString(), anyInt());
    }
}
