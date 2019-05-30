package com.flightstats.hub.cluster;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.app.ShutdownManager;
import com.flightstats.hub.health.HubHealthCheck;
import com.google.common.util.concurrent.AbstractIdleService;
import javax.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * SpokeDecommissionManager is responsible for:
 * setting a node as decomm'd
 * clearing decomm setting
 * getting cached list of decomm'd servers
 * preventing a node from starting if it is decomm'd and old
 * allowing a node to start as decomm'd and young
 */
@Slf4j
@Singleton
public class SpokeDecommissionManager implements DecommissionManager {
    private final CuratorCluster spokeCuratorCluster;
    private final CuratorCluster hubCuratorCluster;
    private final HubHealthCheck hubHealthCheck;
    private final SpokeDecommissionCluster decommissionCluster;
    private final ShutdownManager shutdownManager;

    @Inject
    public SpokeDecommissionManager(SpokeDecommissionCluster decommissionCluster,
                                    @Named("SpokeCuratorCluster") CuratorCluster spokeCuratorCluster,
                                    @Named("HubCuratorCluster") CuratorCluster hubCuratorCluster,
                                    ShutdownManager shutdownManager,
                                    HubHealthCheck hubHealthCheck) throws Exception {
        this.decommissionCluster = decommissionCluster;
        this.spokeCuratorCluster = spokeCuratorCluster;
        this.hubCuratorCluster = hubCuratorCluster;
        this.shutdownManager = shutdownManager;
        this.hubHealthCheck = hubHealthCheck;
        HubServices.register(new SpokeDecommissionManagerService(), HubServices.TYPE.BEFORE_HEALTH_CHECK);
    }

    @Override
    public boolean decommission() throws Exception {
        hubHealthCheck.decommissionWithinSpoke();

        decommissionCluster.decommission();

        scheduleDoNotRestart();
        return true;
    }

    @Override
    public void recommission(String server) throws Exception {
        decommissionCluster.recommission(server);
    }

    private void startCheck() throws Exception {
        decommissionCluster.initialize();
        if (decommissionCluster.doNotRestartExists()) {
            String msg = "We can not start a server with a 'do not start' key";
            log.error(msg);
            throw new RuntimeException(msg);
        }
        if (decommissionCluster.withinSpokeExists()) {
            hubHealthCheck.decommissionWithinSpoke();
            scheduleDoNotRestart();
        }
    }

    private void scheduleDoNotRestart() throws Exception {
        if (decommissionCluster.doNotRestartExists()) {
            log.warn("do not restart already exists");
            spokeCuratorCluster.delete();
            return;
        }
        if (decommissionCluster.withinSpokeExists()) {
            long doNotRestartMinutes = decommissionCluster.getDoNotRestartMinutes();
            if (doNotRestartMinutes > 0) {
                log.info("scheduling doNotRestart in {} minutes", doNotRestartMinutes);
                Executors.newSingleThreadScheduledExecutor().schedule(this::doNotRestart,
                        doNotRestartMinutes, TimeUnit.MINUTES);
            } else {
                doNotRestart();
            }
        } else {
            doNotRestart();
        }
    }

    private void doNotRestart() {
        try {
            log.info("doNotRestart starting ...");
            decommissionCluster.doNotRestart();
            log.info("deleting spoke cluster ");
            spokeCuratorCluster.delete();
            hubCuratorCluster.delete();
            log.info("doNotRestart complete");
            hubHealthCheck.decommissionedDoNotRestart();
            shutdownManager.shutdown(false);
        } catch (Exception e) {
            log.warn("unable to complete ", e);
        }
    }

    private class SpokeDecommissionManagerService extends AbstractIdleService {
        @Override
        protected void startUp() throws Exception {
            startCheck();
        }

        @Override
        protected void shutDown() throws Exception {
            //do nothing
        }
    }

}
