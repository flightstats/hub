package com.flightstats.hub.cluster;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import static com.flightstats.hub.app.HubServices.TYPE;
import static com.flightstats.hub.app.HubServices.register;

/**
 * Curator's LeaderSelector uses a fair algorithm, which means that the oldest lock always wins.
 * After a rolling restart, all of the existing leaders will be on the longest running server.
 * LeaderRotator causes leaders to abdicate leadership at random intervals to balance the load.
 */
@Singleton
public class LeaderRotator {

    private final static Logger logger = LoggerFactory.getLogger(LeaderRotator.class);
    private static final Set<CuratorLeader> leaders = new ConcurrentHashSet<>();

    @Inject
    @Named("HubCuratorCluster")
    private CuratorCluster hubCuratorCluster;

    public LeaderRotator() {
        register(new LeaderRotatorService(), TYPE.PRE_STOP, TYPE.AFTER_HEALTHY_START);
    }

    public static synchronized void add(CuratorLeader leader) {
        leaders.add(leader);
    }

    public static synchronized void remove(CuratorLeader leader) {
        leaders.remove(leader);
    }

    private class LeaderRotatorService extends AbstractScheduledService {

        @Override
        protected synchronized void runOneIteration() throws Exception {
            logger.debug("running...");
            try {
                for (CuratorLeader leader : leaders) {
                    if (Math.random() > leader.keepLeadershipRate()) {
                        leader.abdicate();
                    } else {
                        leader.limitChildren(hubCuratorCluster.getServers().size());
                    }
                }
            } catch (Exception e) {
                logger.warn("unexpected LeaderRotator issue ", e);
            }
        }

        @Override
        protected Scheduler scheduler() {
            return Scheduler.newFixedDelaySchedule(1, 1, TimeUnit.MINUTES);
        }

    }

    static Collection<CuratorLeader> getLeaders() {
        Map<String, CuratorLeader> leaderMap = new TreeMap<>();
        for (CuratorLeader leader : leaders) {
            leaderMap.put(leader.getLeaderPath(), leader);
        }
        return leaderMap.values();
    }
}
