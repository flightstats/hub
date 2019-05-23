package com.flightstats.hub.util;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.Backgroundable;
import org.apache.curator.framework.api.ChildrenDeletable;
import org.apache.curator.framework.api.DeleteBuilder;
import org.apache.curator.framework.api.GetDataBuilder;
import org.apache.curator.framework.api.Pathable;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;

@Singleton
@Slf4j
// NOTE: The purpose of this ZooKeeper wrapper is to encapsulate and standardize the logging + error-swallowing that's
// a common hub pattern.
public class SafeZooKeeperUtils {
    private final CuratorFramework curator;

    @Inject
    public SafeZooKeeperUtils(CuratorFramework curator) {
        this.curator = curator;
    }

    public PathChildrenCache initializeCache(String... pathParts) throws Exception {
        PathChildrenCache cache = new PathChildrenCache(curator, getPath(newArrayList(pathParts)), true);
        cache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
        return cache;
    }

    public List<String> getChildren(String... parentPathParts) {
        return doItSafely(path -> curator.getChildren().forPath(path),
                newArrayList(parentPathParts),
                "unable to get children",
                emptyList());
    }

    public Optional<String> getData(String... pathParts) {
        return getData(builder -> builder, pathParts);
    }

    public Optional<DataWithStat> getDataWithStat(String... pathParts) {
        Stat stat = new Stat();
        return getData(builder -> builder.storingStatIn(stat), pathParts)
                .map(data -> DataWithStat.builder().data(data).stat(stat).build());
    }

    public void createPathAndParents(String... pathParts) {
        doItSafely(path -> curator.create().creatingParentsIfNeeded().forPath(path),
                newArrayList(pathParts),
                "unable to create",
                null);
    }

    public void createData(byte[] data, String... pathParts) {
        doItSafely(path -> curator.create().creatingParentsIfNeeded().forPath(path, data),
                newArrayList(pathParts),
                "unable to create with data",
                null
        );
    }

    public void delete(String... pathParts) {
        deletePath(builder -> builder, pathParts);
    }

    public void deletePathAndChildren(String... pathParts) {
        deletePath(ChildrenDeletable::deletingChildrenIfNeeded, pathParts);
    }

    public void deletePathInBackground(String... pathParts) {
        deletePath(Backgroundable::inBackground, pathParts);
    }

    private Optional<String> getData(Function<GetDataBuilder, Pathable<byte[]>> dataBuilder, String... pathParts) {
        return doItSafely(path -> {
                    GetDataBuilder data = curator.getData();
                    byte[] bytes = dataBuilder.apply(data).forPath(path);
                    return Optional.of(new String(bytes));
                },
                newArrayList(pathParts),
                "unable to get data",
                Optional.empty());
    }

    private void deletePath(Function<DeleteBuilder, Pathable<Void>> deleteBuilder, String... pathParts) {
        doItSafely(path -> deleteBuilder.apply(curator.delete()).forPath(path),
                newArrayList(pathParts),
                "unable to delete",
                null);
    }

    private <T> T doItSafely(CheckedCuratorAction<String, T> curatorAction, List<String> pathParts, String failureMessage, T defaultValue) {
        String path = getPath(pathParts);
        try {
            return curatorAction.apply(path);
        } catch (KeeperException.NodeExistsException ignore) {
            log.info("node exists " + path);
        } catch (KeeperException.NoNodeException ignore) {
            log.info("no node exists " + path);
        } catch (Exception e) {
            log.warn(failureMessage + " " + path, e);
        }
        return defaultValue;
    }

    private String getPath(List<String> pathParts) {
        return String.join("/", pathParts);
    }

    @FunctionalInterface
    private interface CheckedCuratorAction<String, R> {
        R apply(String t) throws Exception;
    }

    @Builder
    @Getter
    public static class DataWithStat {
        Stat stat;
        String data;
    }
}
