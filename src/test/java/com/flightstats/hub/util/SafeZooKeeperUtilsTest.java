package com.flightstats.hub.util;

import com.flightstats.hub.cluster.ZooKeeperState;
import com.flightstats.hub.test.Integration;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SafeZooKeeperUtilsTest {
    private static CuratorFramework curator;
    private static SafeZooKeeperUtils zooKeeperUtils;

    @BeforeAll
    public static void setup() throws Exception {
        curator = Integration.startZooKeeper();
        zooKeeperUtils = new SafeZooKeeperUtils(curator);
    }

    @BeforeEach
    public void setupNode() throws Exception {
        createNode("/some");
    }

    @AfterEach
    public void cleanupNode() throws Exception {
        curator.delete().deletingChildrenIfNeeded().forPath("/some");
    }

    @Test
    public void testGetChildren_success() throws Exception {
        createNode("/some/path");
        createNode("/some/path/or");
        createNode("/some/path/or/another");
        createNode("/some/path/and/anotherer");

        List<String> children = zooKeeperUtils.getChildren("/some", "path");

        assertEquals(newArrayList("or", "and"), children);
    }

    @Test
    public void testGetChildren_unknownNode() throws Exception {
        List<String> children = zooKeeperUtils.getChildren("/some", "path");

        assertEquals(emptyList(), children);
    }

    @Test
    public void testGetData_success() throws Exception {
        createNodeWithData("/some/path", "hey");
        createNodeWithData("/some/path/or", "hi");

        String data = zooKeeperUtils.getData("/some", "path", "or")
                .orElseThrow(AssertionError::new);

        assertEquals("hi", data);
    }

    @Test
    public void testGetData_noData() throws Exception {
        createNode("/some/path");
        createNodeWithData("/some/path/or", "hi");

        Optional<String> data = zooKeeperUtils.getData("/some", "path");

        assertTrue(data.isPresent());
    }

    @Test
    public void testGetData_unknownNode() throws Exception {
        createNode("/some/path");

        Optional<String> data = zooKeeperUtils.getData("/some", "path", "or");

        assertFalse(data.isPresent());
    }

    @Test
    public void testGetDataWithStat_success() throws Exception {
        long startTime = TimeUtil.now().getMillis();
        createNodeWithData("/some/path", "hey");
        createNodeWithData("/some/path/or", "hi");

        Optional<SafeZooKeeperUtils.DataWithStat> data = zooKeeperUtils.getDataWithStat("/some", "path", "or");

        assertTrue(data.isPresent());
        assertEquals("hi", data.get().getData());
        assertTrue(data.get().getStat().getCtime() > startTime);
    }

    @Test
    public void testGetDataWithStat_unknownNode() {
        Optional<SafeZooKeeperUtils.DataWithStat> data = zooKeeperUtils.getDataWithStat("/some", "path", "or");

        assertFalse(data.isPresent());
    }

    @Test
    public void testCreatePathAndParents_success() throws Exception {
        zooKeeperUtils.createPathAndParents("/some", "path", "or", "another");

        assertEquals(newArrayList("another"), curator.getChildren().forPath("/some/path/or"));
    }

    @Test
    public void testCreatePathAndParents_NodeExists() throws Exception {
        createNode("/some/path/or/another/foo");

        zooKeeperUtils.createPathAndParents("/some", "path", "or", "another");

        assertEquals(newArrayList("foo"), curator.getChildren().forPath("/some/path/or/another"));
    }

    @Test
    public void testCreateData_success() throws Exception {
        zooKeeperUtils.createData("hi".getBytes(), "/some", "path");

        assertEquals("hi", new String(curator.getData().forPath("/some/path")));
    }

    @Test
    public void testCreateData_NodeExists() throws Exception {
        createNodeWithData("/some/path", "hey");

        zooKeeperUtils.createData("hi".getBytes(), "/some", "path");

        assertEquals("hey", new String(curator.getData().forPath("/some/path")));
    }

    @Test
    public void testDeletePathAndChildren_success() throws Exception {
        createNode("/some/path/or/another/foo");

        zooKeeperUtils.deletePathAndChildren("/some", "path", "or");

        assertThrows(KeeperException.NoNodeException.class, () -> curator.getData().forPath("/some/path/or"));
        assertEquals(newArrayList("path"), curator.getChildren().forPath("/some"));
    }

    @Test
    public void testDeletePathAndChildren_unknownNode() throws Exception {
        createNode("/some/path");
        zooKeeperUtils.deletePathAndChildren("/some", "path", "or", "another");

        assertThrows(KeeperException.NoNodeException.class, () -> curator.getData().forPath("/some/path/or/another"));
        assertEquals(newArrayList("path"), curator.getChildren().forPath("/some"));
    }

    @Test
    public void testDeletePathInBackground_success() throws Exception {
        createNode("/some/path/or/another");
        zooKeeperUtils.deletePathInBackground("/some", "path", "or", "another");

        assertThrows(KeeperException.NoNodeException.class, () -> curator.getData().forPath("/some/path/or/another"));
        assertEquals(newArrayList("or"), curator.getChildren().forPath("/some/path"));

    }

    @Test
    public void testDeletePathInBackground_unknownNode() throws Exception {
        createNode("/some/path/or");
        zooKeeperUtils.deletePathAndChildren("/some", "path", "or", "another");

        assertThrows(KeeperException.NoNodeException.class, () -> curator.getData().forPath("/some/path/or/another"));
        assertEquals(newArrayList("or"), curator.getChildren().forPath("/some/path"));
    }

    private void createNode(String path) throws Exception {
        curator.create().creatingParentsIfNeeded().forPath(path);
    }

    private void createNodeWithData(String path, String data) throws Exception {
        curator.create().creatingParentsIfNeeded().forPath(path, data.getBytes());
    }
}
