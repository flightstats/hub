package com.flightstats.hub.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class HubHost {
    private final static Logger logger = LoggerFactory.getLogger(HubHost.class);

    private static int port;
    private static String scheme = "http://";

    static {
        port = HubProperties.getProperty("http.bind_port", 8080);
        if (HubProperties.getProperty("encrypted", false)) {
            scheme = "https://";
        }
    }

    public static String getLocalName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            logger.warn("unable to get local host...", e);
            return "none";
        }
    }

    public static String getLocalUriRoot() {
        return getScheme() + getLocalAddress() + ":" + getLocalPort();
    }

    public static String getLocalAddressPort() {
        return getLocalAddress() + ":" + getLocalPort();
    }

    public static String getLocalAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            logger.warn("unable to get local host...", e);
            return "none";
        }
    }

    public static int getLocalPort() {
        return port;
    }


    public static String getScheme() {
        return scheme;
    }

}
