package com.flightstats.hub.cluster;

import com.flightstats.hub.exception.NoSuchChannelException;
import com.flightstats.hub.util.RuntimeInterruptedException;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.CancelLeadershipException;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListener;
import org.apache.curator.framework.state.ConnectionState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * CuratorLeader calls Leader when it gets leadership.
 * Use a Leader when you have a long running task which needs to be canceled if the ZooKeeper connection is lost.
 * A Leader is useful when you always want a process with leadership.
 * Brief processes which don't always run can use a CuratorLock.
 */
public class CuratorLeader {

    private final static Logger logger = LoggerFactory.getLogger(CuratorLeader.class);
    private final CuratorFramework curator;
    private String leaderPath;
    private Leader leader;
    private LeaderSelector leaderSelector;
    private AtomicBoolean hasLeadership = new AtomicBoolean(false);

    public CuratorLeader(String leaderPath, Leader leader, CuratorFramework curator) {
        this.leaderPath = leaderPath;
        this.leader = leader;
        this.curator = curator;
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
        } else {
            leaderSelector.requeue();
        }
        LeaderRotator.add(this);
    }

    public void close() {
        hasLeadership.set(false);
        if (leaderSelector != null) {
            leaderSelector.close();
        }
        LeaderRotator.remove(this);
    }

    private class CuratorLeaderSelectionListener implements LeaderSelectorListener {

        public void takeLeadership(final CuratorFramework client) throws Exception {
            logger.info("have leadership for " + leaderPath);
            try {
                Thread.currentThread().setName("curator-leader-" + leaderPath);
                hasLeadership.set(true);
                leader.takeLeadership(hasLeadership);
            } catch (RuntimeInterruptedException e) {
                logger.info("interrupted " + leaderPath + e.getMessage());
            } catch (NoSuchChannelException e) {
                logger.debug("no channel {} ", e.getMessage());
            } catch (Exception e) {
                logger.warn("exception thrown from ElectedLeader " + leaderPath, e);
            } finally {
                logger.info("lost leadership " + leaderPath);
                Thread.currentThread().setName("curator-leader-empty");
            }
        }

        /**
         * Copied from LeaderSelectorListenerAdapter, with additional call setting hasLeadership to false.
         */
        @Override
        public void stateChanged(CuratorFramework client, ConnectionState newState) {
            if ((newState == ConnectionState.SUSPENDED) || (newState == ConnectionState.LOST)) {
                hasLeadership.set(false);
                throw new CancelLeadershipException();
            }
        }
    }

    void abdicate() {
        if (hasLeadership.get()) {
            logger.info("abdicating leadership for " + leaderPath);
            hasLeadership.set(false);
        }
    }

    double keepLeadershipRate() {
        return leader.keepLeadershipRate();
    }
}

