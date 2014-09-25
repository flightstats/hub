package com.flightstats.hub.app;

import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServerMain;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 *
 */
public class ZookeeperMain {
    public static void main(String[] args) throws IOException, QuorumPeerConfig.ConfigException {
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
    }
}
