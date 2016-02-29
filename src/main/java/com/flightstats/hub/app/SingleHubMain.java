package com.flightstats.hub.app;

import com.google.common.io.Files;
import com.google.inject.Injector;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for the hub.  This is the runnable class for a stand-alone single hub server.
 * <p>
 * The primary value to set here the system property 'storage.path', which is where all the local data files
 * are stored.
 */
public class SingleHubMain {

    private static final Logger logger = LoggerFactory.getLogger(SingleHubMain.class);
    private static final DateTime startTime = new DateTime();
    private static Injector injector;

    public static void main(String[] args) throws Exception {
        System.out.println("starting up single Hub");
        HubProperties.setProperty("hub.type", "test");
        HubProperties.setProperty("app.name", "hub");
        HubProperties.setProperty("app.environment", "single");

        setProperty("zookeeper.connection", "localhost:2181");
        setProperty("runSingleZookeeperInternally", "singleNode");
        setProperty("app.lib_path", "");
        setProperty("alert.run", "false");
        setProperty("app.url", "http://localhost:8080/");
        setProperty("http.bind_port", "8080");
        String tempPath = Files.createTempDir().getAbsolutePath();
        setProperty("storage.path", tempPath);
        setProperty("app.remoteTimeFile", tempPath + "/remoteTime");

        HubMain.start();
    }

    private static void setProperty(String name, String defaultValue) {
        String value = System.getProperty(name, defaultValue);
        HubProperties.setProperty(name, value);
        System.out.println("setting " + name + "=" + value + " . over ride this value with -D" + name + "=value");
    }

}
