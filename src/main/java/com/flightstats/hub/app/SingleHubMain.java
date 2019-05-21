package com.flightstats.hub.app;

import com.flightstats.hub.config.properties.PropertiesLoader;
import com.google.common.io.Files;

/**
 * Main entry point for the hub.  This is the runnable class for a stand-alone single hub server.
 * <p>
 * The primary value to set here the system property 'storage.path', which is where all the local data files
 * are stored.
 */
@SuppressWarnings("WeakerAccess")
public class SingleHubMain {

    public static void main(String[] args) throws Exception {

        System.out.println("***************************");
        System.out.println("starting up single Hub");
        System.out.println("***************************");

        final PropertiesLoader propertiesLoader = PropertiesLoader.getInstance();

        setProperty(propertiesLoader, "hub.type", "test");
        setProperty(propertiesLoader, "app.name", "hub");
        setProperty(propertiesLoader, "app.environment", "single");

        setProperty(propertiesLoader, "zookeeper.connection", "localhost:2181");
        setProperty(propertiesLoader, "runSingleZookeeperInternally", "singleNode");
        setProperty(propertiesLoader, "app.lib_path", "");
        setProperty(propertiesLoader, "alert.run", "false");
        setProperty(propertiesLoader, "app.url", "http://localhost:8080/");
        setProperty(propertiesLoader, "http.bind_port", "8080");
        setProperty(propertiesLoader, "spoke.enforceTTL", "true");
        setProperty(propertiesLoader, "channel.enforceTTL", "false");
        setProperty(propertiesLoader, "app.stable_seconds", "2");
        setProperty(propertiesLoader, "app.shutdown_delay_seconds", "0");
        setProperty(propertiesLoader, "hub.protect.channels", "false");
        setProperty(propertiesLoader, "app.runNtpMonitor", "false");
        setProperty(propertiesLoader, "metrics.enable", "false");
        setProperty(propertiesLoader, "app.large.payload.MB", "10000");

        String storagePath = Files.createTempDir().getAbsolutePath();
        setProperty(propertiesLoader, "storage.path", storagePath);
        String spokePath = Files.createTempDir().getAbsolutePath();
        setProperty(propertiesLoader, "spoke.path", spokePath);
        setProperty(propertiesLoader, "app.remoteTimeFile", storagePath + "/remoteTime");

        new HubMain().run(true);
    }

    private static void setProperty(PropertiesLoader propertiesLoader, String name, String defaultValue) {
        String value = System.getProperty(name, defaultValue);
        propertiesLoader.setProperty(name, value);
        System.out.println("setting " + name + "=" + value + " . over ride this value with -D" + name + "=value");
    }

}
