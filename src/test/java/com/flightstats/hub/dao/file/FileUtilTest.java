package com.flightstats.hub.dao.file;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Comparator;

import static org.junit.Assert.assertTrue;

public class FileUtilTest {

    private final static String CANONICAL_NAME = "com.flightstats.hub.dao.file.FileUtilTest";
    private final static String ROOT_DIRECTORY = "/tmp/" + CANONICAL_NAME + "-" + Instant.now().getEpochSecond();

    @BeforeClass
    public static void setup() throws IOException {
        Path root = Paths.get(ROOT_DIRECTORY);
        Files.createDirectories(root);
    }

    @AfterClass
    public static void teardown() throws IOException {
        Path root = Paths.get(ROOT_DIRECTORY);
        Files.walk(root)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @Test
    public void mergeDirectories() throws IOException {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        String workspace = ROOT_DIRECTORY + "/" + methodName;
        String from = workspace + "/from";
        String to = workspace + "/to";

        String payloadOne = "foo/0000/00/00/00/00/00/000/123456abcdef";
        Path pathA = Paths.get(from + "/" + payloadOne);
        Files.createDirectories(pathA.getParent());
        Files.createFile(pathA);

        String payloadTwo = "foo/0000/00/00/00/00/00/000/789012ghijkl";
        Path pathB = Paths.get(to + "/" + payloadTwo);
        Files.createDirectories(pathB.getParent());
        Files.createFile(pathB);

        FileUtil.mergeDirectories(from, to);

        assertTrue(Files.exists(Paths.get(to + "/" + payloadOne)));
        assertTrue(Files.exists(Paths.get(to + "/" + payloadTwo)));
    }
}