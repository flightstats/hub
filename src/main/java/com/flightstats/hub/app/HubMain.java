package com.flightstats.hub.app;

import com.flightstats.hub.app.config.GuiceContext;
import com.flightstats.hub.dao.aws.AwsModule;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Injector;
import org.apache.commons.io.IOUtils;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServerMain;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

/**
 * Main entry point for the data hub.  This is the main runnable class.
 */
public class HubMain {

    private static final Logger logger = LoggerFactory.getLogger(HubMain.class);
    private static final DateTime startTime = new DateTime();
    private static Injector injector;

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            throw new UnsupportedOperationException("HubMain requires a property filename, or 'useDefault'");
        }
        final Properties properties = loadProperties(args[0]);
        HubProperties.setProperties(properties);

        startZookeeperIfSingle(properties);

        JettyServer server = startServer(properties);

        final CountDownLatch latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                logger.info("Jetty Server shutting down...");
                latch.countDown();
            }
        });
        latch.await();
        server.halt();
        HubServices.stopAll();
        logger.info("Server shutdown complete.  Exiting application.");
    }

    private static void startZookeeperIfSingle(final Properties properties) {
        new Thread(new Runnable() {
            @Override
            public void run() {

                String zkConfigFile = properties.getProperty("zookeeper.cfg", "");
                if ("singleNode".equals(zkConfigFile)) {
                    startSingleZookeeper();
                }
            }

            private void startSingleZookeeper() {
                try {
                    warn("using zookeeper single node config file");
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
                    logger.warn("unable to start zookeeper", e);
                }
            }
        }).start();
    }

    private static void warn(String message) {
        logger.warn("**********************************************************");
        logger.warn("*** " + message);
        logger.warn("**********************************************************");
    }

    public static JettyServer startServer(Properties properties) throws IOException {
        AwsModule awsModule = new AwsModule(properties);
        GuiceContext.HubGuiceServlet guice = GuiceContext.construct(properties, awsModule);
        injector = guice.getInjector();
        JettyServer server = new JettyServer(guice);
        HubServices.start(HubServices.TYPE.PRE_START);
        server.start();
        logger.info("Jetty server has been started.");
        HubServices.start(HubServices.TYPE.POST_START);
        return server;
    }

    public  static Properties loadProperties(String fileName) throws IOException {
        if (fileName.equals("useDefault")) {
            return getLocalProperties("default");
        } else if (fileName.equals("useEncryptedDefault")) {
            return getLocalProperties("defaultEncrypted");
        }
        return loadProperties(new File(fileName).toURI().toURL());
    }

    private static Properties getLocalProperties(String fileNameRoot) throws IOException {
        warn("using " + fileNameRoot + " properties file");
        Properties defaultProperties = getProperties("/" + fileNameRoot + ".properties", true);
        Properties localProperties = getProperties("/" + fileNameRoot + "_local.properties", false);
        for (String localKey : localProperties.stringPropertyNames()) {
            String newVal = localProperties.getProperty(localKey);
            logger.info("overriding " + localKey + " using '" + newVal +
                    "' instead of '" + defaultProperties.getProperty(localKey, "") + "'");
            defaultProperties.setProperty(localKey, newVal);
        }
        return defaultProperties;
    }

    private static Properties getProperties(String name, boolean required) throws IOException {
        URL resource = HubMain.class.getResource(name);
        return loadProperties(resource);
    }

    private static Properties loadProperties(URL url)
            throws IOException {
        Properties properties = new Properties();
        InputStream inputStream = null;
        try {
            inputStream = url.openStream();
            properties.load(inputStream);
        } catch (IOException e) {
            String message = "Unable to load required properties file from location: " + url.toString();
            logger.error(message, e);
            throw new IOException(message, e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        return properties;
    }

    @VisibleForTesting
    public static Injector getInjector() {
        return injector;
    }

    public static DateTime getStartTime() {
        return startTime;
    }
}
