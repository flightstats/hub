package com.flightstats.hub.app;

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
        HubProperties.setProperty("hub.type", "test");
        HubProperties.setProperty("app.name", "hub");
        HubProperties.setProperty("app.environment", "single");

        setProperty("zookeeper.connection", "localhost:2181");
        setProperty("runSingleZookeeperInternally", "singleNode");
        setProperty("app.lib_path", "");
        setProperty("alert.run", "false");
        setProperty("app.url", "http://localhost:8080/");
        setProperty("http.bind_port", "8080");
        String storagePath = Files.createTempDir().getAbsolutePath();
        setProperty("storage.path", storagePath);
        String spokePath = Files.createTempDir().getAbsolutePath();
        setProperty("spoke.path", spokePath);
        setProperty("spoke.enforceTTL", "true");
        setProperty("channel.enforceTTL", "false");
        setProperty("app.stable_seconds", "2");
        setProperty("app.remoteTimeFile", storagePath + "/remoteTime");
        setProperty("app.shutdown_delay_seconds", "0");
        setProperty("hub.protect.channels", "false");
        setProperty("app.runNtpMonitor", "false");
        setProperty("metrics.enable", "false");
        setProperty("app.large.payload.MB", "10000");
        new HubMain().run();
    }

    private static void setProperty(String name, String defaultValue) {
        String value = System.getProperty(name, defaultValue);
        HubProperties.setProperty(name, value);
        System.out.println("setting " + name + "=" + value + " . over ride this value with -D" + name + "=value");
    }

}
