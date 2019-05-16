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

    private final Consumer<ChannelConfig> callback = config -> {
    };

    private String[] getSpokePathList(File spokePath) {
        return Optional.ofNullable(spokePath.list()).orElse(new String[] {});
    }

    private boolean createMockSpokeFiles(File dir, String... fileNames) {
        return Stream.of(fileNames).allMatch(str -> {
            try {
                return new File(dir.toPath() + "/" + str).createNewFile();
            } catch (Exception e) {
                log.error("error adding files to spoke path {}", e);
                return false;
            }
        });
    }

    @SneakyThrows
    private File createUniqueParentDir(File tempDir) {
        String uniqueParentPath = StringUtils.randomAlphaNumeric(4);
        File spokePath = new File(tempDir.getPath() + "/" + uniqueParentPath);
        spokePath.mkdir();
        return spokePath;
    }

    @BeforeEach
    @SneakyThrows
    void setup() {
        initMocks(this);
    }

    @Test
    void enforce_deletesFiles_affirmThreeFilesDeleted(@TempDir File tempDir) {
        File spokePath = createUniqueParentDir(tempDir);
        assertEquals(0, getSpokePathList(spokePath).length);
        assertTrue(createMockSpokeFiles(spokePath, "a", "b", "c", "lost+found"));
        assertEquals(4, getSpokePathList(spokePath).length);
        TtlEnforcer.enforce(spokePath.getPath(), channelService, callback);
        assertEquals(1, getSpokePathList(spokePath).length);
    }

    @Test
    void enforce_deletesUpperCaseFiles_affirmTwoFilesDeleted(@TempDir File tempDir) {
        File spokePath = createUniqueParentDir(tempDir);
        assertEquals(0, getSpokePathList(spokePath).length);
        assertTrue(createMockSpokeFiles(spokePath, "BacA", "aCaB", "lost+found"));
        assertEquals(3, getSpokePathList(spokePath).length);
        TtlEnforcer.enforce(spokePath.getPath(), channelService, callback);
        assertEquals(1, getSpokePathList(spokePath).length);
    }

    @Test
    void enforce_ignoresDeleteOfRegisteredFiles_affirmNoFilesDeleted(@TempDir File tempDir) {
        // GIVEN
        File spokePath = createUniqueParentDir(tempDir);
        assertEquals(0, getSpokePathList(spokePath).length);
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
        assertTrue(createMockSpokeFiles(spokePath, names[0], names[1], "lost+found"));
        assertEquals(3, getSpokePathList(spokePath).length);
        TtlEnforcer.enforce(spokePath.getPath(), channelService, callback);
        assertEquals(3, getSpokePathList(spokePath).length);
    }
}
