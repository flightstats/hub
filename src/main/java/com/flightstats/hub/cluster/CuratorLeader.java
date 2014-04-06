package com.flightstats.hub.cluster;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CuratorLeader calls ElectedLeader when it gets leadership.
 */
public class CuratorLeader {

    private final static Logger logger = LoggerFactory.getLogger(CuratorLeader.class);

    private String leaderPath;
    private ElectedLeader electedLeader;
    private final CuratorFramework curator;
    private LeaderSelector leaderSelector;

    public CuratorLeader(String leaderPath, ElectedLeader electedLeader, CuratorFramework curator) {
        this.leaderPath = leaderPath;
        this.electedLeader = electedLeader;
        this.curator = curator;
    }

    /**
     * Attempt leadership. This attempt is done in the background - i.e. this method returns
     * immediately.
     * The ElectedLeader will be called from an ExecutorService.
     */
    public void start() {
        leaderSelector = new LeaderSelector(curator, leaderPath, new CuratorLeaderSelectionListener());
        leaderSelector.start();
    }

    private class CuratorLeaderSelectionListener extends LeaderSelectorListenerAdapter {

        public void takeLeadership(final CuratorFramework client) throws Exception {
            logger.info("have leadership for " + leaderPath);
            try {
                electedLeader.doWork();
            } catch (Exception e) {
                logger.warn("exception thrown from ElectedLeader " + leaderPath, e);
            }
            leaderSelector.close();
            logger.info("lost leadership " + leaderPath);
        }
    }

}

