package com.flightstats.hub.app;

import com.flightstats.hub.config.PropertyLoader;
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

        final PropertyLoader propertyLoader = PropertyLoader.getInstance();

        setProperty(propertyLoader, "hub.type", "test");
        setProperty(propertyLoader, "app.name", "hub");
        setProperty(propertyLoader, "app.environment", "single");

        setProperty(propertyLoader, "zookeeper.connection", "localhost:2181");
        setProperty(propertyLoader, "runSingleZookeeperInternally", "singleNode");
        setProperty(propertyLoader, "app.lib_path", "");
        setProperty(propertyLoader, "alert.run", "false");
        setProperty(propertyLoader, "app.url", "http://localhost:8080/");
        setProperty(propertyLoader, "http.bind_port", "8080");
        setProperty(propertyLoader, "spoke.enforceTTL", "true");
        setProperty(propertyLoader, "channel.enforceTTL", "false");
        setProperty(propertyLoader, "app.stable_seconds", "2");
        setProperty(propertyLoader, "app.shutdown_delay_seconds", "0");
        setProperty(propertyLoader, "hub.protect.channels", "false");
        setProperty(propertyLoader, "app.runNtpMonitor", "false");
        setProperty(propertyLoader, "metrics.enable", "false");
        setProperty(propertyLoader, "app.large.payload.MB", "10000");

        String storagePath = Files.createTempDir().getAbsolutePath();
        setProperty(propertyLoader, "storage.path", storagePath);
        String spokePath = Files.createTempDir().getAbsolutePath();
        setProperty(propertyLoader, "spoke.path", spokePath);
        setProperty(propertyLoader, "app.remoteTimeFile", storagePath + "/remoteTime");

        new HubMain().run();
    }

    private static void setProperty(PropertyLoader propertyLoader, String name, String defaultValue) {
        String value = System.getProperty(name, defaultValue);
        propertyLoader.setProperty(name, value);
        System.out.println("setting " + name + "=" + value + " . over ride this value with -D" + name + "=value");
    }

}
