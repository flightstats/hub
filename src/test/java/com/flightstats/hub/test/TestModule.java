package com.flightstats.hub.test;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.cluster.ZooKeeperState;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.RetryPolicy;
import org.apache.curator.ensemble.fixed.FixedEnsembleProvider;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.BoundedExponentialBackoffRetry;
import org.apache.curator.test.TestingServer;

import java.util.Properties;

@Slf4j
public class TestModule extends AbstractModule {

    private final Properties properties;

    public TestModule(Properties properties) {
        this.properties = properties;
    }

    @Override
    protected void configure() {
    }

    @Provides
    public HubProperties providesHubProperties() {
        return new HubProperties(properties);
    }

    @Provides
    @Singleton
    public TestingServer providesTestingServer() throws Exception {
        log.info("starting test ZooKeeper");
        return new TestingServer(2181);
    }

    @Provides
    @Singleton
    public CuratorFramework providesCuratorFramework(ZooKeeperState zooKeeperState) throws Exception {
        log.info("connecting to test ZooKeeper");
        FixedEnsembleProvider ensembleProvider = new FixedEnsembleProvider("localhost:2181");
        RetryPolicy retryPolicy = new BoundedExponentialBackoffRetry(10, 10000, 20);
        CuratorFramework curatorFramework = CuratorFrameworkFactory.builder()
                .namespace("test")
                .ensembleProvider(ensembleProvider)
                .retryPolicy(retryPolicy)
                .build();
        curatorFramework.getConnectionStateListenable().addListener(zooKeeperState.getStateListener());
        curatorFramework.start();
        curatorFramework.checkExists().forPath("/");
        return curatorFramework;
    }

}
