package com.flightstats.hub.dao;

import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.util.StringUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import static org.mockito.Mockito.mock;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;


@Slf4j
class TtlEnforcerTest {

    @Mock
    private ChannelService channelService;

    private Consumer<ChannelConfig> callback = config -> {
    };
    private String uniqueParentPath;

    private String[] getSpokePathDirs(File spokePath) {
        return Optional.ofNullable(spokePath.list()).orElse(new String[] {});
    }

    private boolean createMockSpokeFiles(Path tempDir, String... fileNames) {
        return Stream.of(fileNames).allMatch(str -> {
            try {
                return new File(tempDir.toRealPath().toString() + "/" + uniqueParentPath + "/" + str).createNewFile();
            } catch (Exception e) {
                log.error("error adding files to spoke path {}", e);
                return false;
            }
        });
    }

    @SneakyThrows
    private File spokeDirectoryFactory(Path tempDir) {
        uniqueParentPath = StringUtils.randomAlphaNumeric(4);
        String path = tempDir.toRealPath().toString();
        File spokePath = new File(path + "/" + uniqueParentPath);

        return spokePath.mkdir() ? spokePath : spokePath;
    }

    @BeforeEach
    @SneakyThrows
    void setup() {
        initMocks(this);
    }

    @Test
    void enforce_deletesFiles_affirmThreeFilesDeleted(@TempDir Path tempDir) {
        File spokePath = spokeDirectoryFactory(tempDir);
        assertEquals(0, getSpokePathDirs(spokePath).length);
        assertTrue(createMockSpokeFiles(tempDir, "a", "b", "c", "lost+found"));
        assertEquals(4, getSpokePathDirs(spokePath).length);
        TtlEnforcer.enforce(spokePath.getPath(), channelService, callback);
        assertEquals(1, getSpokePathDirs(spokePath).length);
    }

    @Test
    void enforce_deletesUpperCaseFiles_affirmTwoFilesDeleted(@TempDir Path tempDir) {
        File spokePath = spokeDirectoryFactory(tempDir);
        assertEquals(0, getSpokePathDirs(spokePath).length);
        assertTrue(createMockSpokeFiles(tempDir, "BacA", "aCaB", "lost+found"));
        assertEquals(3, getSpokePathDirs(spokePath).length);
        TtlEnforcer.enforce(spokePath.getPath(), channelService, callback);
        assertEquals(1, getSpokePathDirs(spokePath).length);
    }

    @Test
    void enforce_ignoresDeleteOfRegisteredFiles_affirmNoFilesDeleted(@TempDir Path tempDir) {
        // GIVEN
        File spokePath = spokeDirectoryFactory(tempDir);
        assertEquals(0, getSpokePathDirs(spokePath).length);
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
        assertTrue(createMockSpokeFiles(tempDir, names[0], names[1], "lost+found"));
        assertEquals(3, getSpokePathDirs(spokePath).length);
        TtlEnforcer.enforce(spokePath.getPath(), channelService, callback);
        assertEquals(3, getSpokePathDirs(spokePath).length);
    }
}
