package com.flightstats.hub.app;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServerMain;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

@Slf4j
public class ZooKeeperInProcess {

    public void start() {
        log.info("starting ZooKeeper in process");
        try {
            QuorumPeerConfig config = new QuorumPeerConfig();
            Properties zkProperties = new Properties();
            zkProperties.setProperty("clientPort", "2181");
            zkProperties.setProperty("initLimit", "5");
            zkProperties.setProperty("syncLimit", "2");
            zkProperties.setProperty("maxClientCnxns", "0");
            zkProperties.setProperty("tickTime", "2000");
            Path tempZookeeper = Files.createTempDirectory("zookeeper_");
            zkProperties.setProperty("dataDir", tempZookeeper.toString());
            zkProperties.setProperty("dataLogDir", tempZookeeper.toString());
            config.parseProperties(zkProperties);
            ServerConfig serverConfig = new ServerConfig();
            serverConfig.readFrom(config);

            new ZooKeeperServerMain().runFromConfig(serverConfig);
        } catch (Exception e) {
            log.warn("unable to start zookeeper", e);
        }
    }

}
