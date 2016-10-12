package com.flightstats.hub.cluster;

import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.app.HubMain;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.app.ShutdownManager;
import com.flightstats.hub.exception.NoSuchChannelException;
import com.flightstats.hub.metrics.DataDog;
import com.flightstats.hub.util.RuntimeInterruptedException;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.timgroup.statsd.Event;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.CancelLeadershipException;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListener;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.zookeeper.data.Stat;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Executors;

/**
 * CuratorLeader calls Leader when it gets leadership.
 * Use a Leader when you have a long running task which needs to be canceled if the ZooKeeper connection is lost.
 * A Leader is useful when you always want a process with leadership.
 * Brief processes which don't always run can use a CuratorLock.
 */
public class CuratorLeader {

    private final static Logger logger = LoggerFactory.getLogger(CuratorLeader.class);
    private final static CuratorFramework curator = HubProvider.getInstance(CuratorFramework.class);
    private final static ShutdownManager shutdownManager = HubProvider.getInstance(ShutdownManager.class);
    private String leaderPath;
    private Leader leader;
    private LeaderSelector leaderSelector;
    private Leadership leadership = new Leadership();

    private String id;

    public CuratorLeader(String leaderPath, Leader leader) {
        this.leaderPath = leaderPath;
        this.leader = leader;
        id = leaderPath + "-" + leader.getId();
    }

    /**
     * Attempt leadership. This method returns immediately, and is re-entrant.
     * The Leader.takeLeadership() will be called from an ExecutorService.
     */
    public void start() {
        if (leaderSelector == null) {
            leaderSelector = new LeaderSelector(curator, leaderPath, new CuratorLeaderSelectionListener());
            leaderSelector.autoRequeue();
            leaderSelector.start();
            logger.info("start {}", id);
        } else {
            leaderSelector.requeue();
            logger.info("requeue {}", id);
        }
        Leaders.add(this);
    }

    public void close() {
        logger.info("closing leader {}", id);
        leadership.close();
        if (leaderSelector != null) {
            leaderSelector.close();
        }
        Leaders.remove(this);
        logger.info("closed {}", id);
    }

    private class CuratorLeaderSelectionListener implements LeaderSelectorListener {

        public void takeLeadership(final CuratorFramework client) throws Exception {
            logger.info("takeLeadership " + id);
            try {
                Thread.currentThread().setName("leader-" + id);
                leadership.setLeadership(true);
                leader.takeLeadership(leadership);
            } catch (RuntimeInterruptedException e) {
                logger.info("interrupted " + leaderPath + e.getMessage());
            } catch (NoSuchChannelException e) {
                logger.debug("no channel {} ", e.getMessage());
            } catch (Exception e) {
                logger.warn("exception thrown from ElectedLeader " + leaderPath, e);
            } finally {
                logger.info("lost leadership " + leaderPath);
                Thread.currentThread().setName("leader-empty");
            }
        }

        /**
         * Copied from LeaderSelectorListenerAdapter, with additional call setting hasLeadership to false.
         */
        @Override
        public void stateChanged(CuratorFramework client, ConnectionState newState) {
            if ((newState == ConnectionState.SUSPENDED) || (newState == ConnectionState.LOST)) {
                logger.info("stateChanged {}", newState);
                leadership.setLeadership(false);
                throw new CancelLeadershipException();
            }
        }
    }

    String getLeaderPath() {
        return leaderPath;
    }

    void limitChildren(int maxChildren) {
        try {
            List<String> childNames = curator.getChildren().forPath(getLeaderPath());
            if (childNames.size() > maxChildren) {
                logger.info("found more than limit {} {}", getLeaderPath(), childNames.size());
                limit(childNames);
            }
        } catch (Exception e) {
            logger.info("unable to limit children " + getLeaderPath(), e);
        }
    }

    private void limit(List<String> childNames) throws Exception {
        ListMultimap<String, PathDate> childData = ArrayListMultimap.create();
        for (String child : childNames) {
            Stat stat = new Stat();
            byte[] bytes = curator.getData().storingStatIn(stat).forPath(getLeaderPath() + "/" + child);
            String server = new String(bytes);
            PathDate pathDate = new PathDate(new DateTime(stat.getCtime()), child);
            logger.info("server {} {} {}", server, pathDate, getLeaderPath());
            childData.put(server, pathDate);
        }
        String localServer = HubHost.getLocalAddress();
        SortedSet<PathDate> pathDates = new TreeSet<>(childData.get(localServer));
        if (pathDates.size() >= 2) {
            if (pathDates.first().dateTime.isAfter(HubMain.getStartTime())) {
                logger.warn("found too many locks {} for this server {} {}", getLeaderPath(), localServer, pathDates);
                Event event = DataDog.getEventBuilder()
                        .withTitle("Hub Leader Lock")
                        .withText(getLeaderPath() + " " + pathDates.toString())
                        .build();
                DataDog.statsd.recordEvent(event, "restart", "leader", "shutdown");
                Executors.newSingleThreadExecutor().submit(shutdownManager::shutdown);
            }
        }

    }

    @EqualsAndHashCode
    @AllArgsConstructor
    @ToString
    private class PathDate implements Comparable<PathDate> {
        DateTime dateTime;
        String child;

        @Override
        public int compareTo(PathDate other) {
            return dateTime.compareTo(other.dateTime);
        }
    }
}

