package com.flightstats.hub.webhook;

import com.flightstats.hub.app.HubBindings;
import com.flightstats.hub.cluster.ZooKeeperState;
import org.apache.curator.framework.CuratorFramework;

class ZkLastUpdated {

    public static void main(String[] args) throws Exception {

        //CuratorFramework curator = HubBindings.buildCurator("hub-v2", "staging", "hub-zk-01.iad.staging.flightstats.io:2181,hub-zk-02.iad.staging.flightstats.io:2181,hub-zk-03.iad.staging.flightstats.io:2181", retryPolicy, new ZooKeeperState());
        CuratorFramework curator = HubBindings.buildCurator("hub-v2", "prod", "hub-zk-01.iad.prod.flightstats.io:2181,hub-zk-02.iad.prod.flightstats.io:2181,hub-zk-03.iad.prod.flightstats.io:2181", new ZooKeeperState());
        //CuratorFramework curator = HubBindings.buildCurator("hub-v2", "dev", "hub-zk-01.cloud-east.dev:2181,hub-zk-02.cloud-east.dev:2181,hub-zk-03.cloud-east.dev:2181", retryPolicy, new ZooKeeperState());

        String basePath = "/HistoricalLastUpdated/historicalProcessedFlightHistory";
        byte[] lastUpdated = curator.getData().forPath(basePath);

        System.out.println("last updated " + new String(lastUpdated));
        curator.setData().forPath(basePath, "2007/12/18/05/23/00/001/pdeZYO".getBytes());
    }
}
