package com.flightstats.hub.test;

import com.flightstats.hub.util.Sleeper;
import spark.Route;
import spark.Spark;

/**
 * Sometimes Spark does not register an endpoint quickly enough for testing.fixing
 * Use SparkUtil instead of using Spark directly.
 * import static com.flightstats.hub.test.SparkUtil.*;
 */
public class SparkUtil {

    private static void sleep() {
        Sleeper.sleep(50);
    }

    public static synchronized void get(final String path, final Route route) {
        Spark.get(path, route);
        sleep();
    }

    public static synchronized void post(String path, Route route) {
        Spark.post(path, route);
        sleep();
    }

    public static synchronized void put(String path, Route route) {
        Spark.put(path, route);
        sleep();
    }

    public static synchronized void stop() {
        Spark.stop();
    }
}
