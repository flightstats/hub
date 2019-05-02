package com.flightstats.hub.app;

import com.flightstats.hub.config.AppProperties;
import com.flightstats.hub.config.PropertiesLoader;
import com.flightstats.hub.config.SystemProperties;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
public class HubHost {

    private static final AppProperties appProperties = new AppProperties(PropertiesLoader.getInstance());
    private static final SystemProperties systemProperties = new SystemProperties(PropertiesLoader.getInstance());
    private static final int port;
    private static String scheme = "http://";

    static {
        port = systemProperties.getHttpBindPort();
        if (appProperties.isAppEncrypted()) {
            scheme = "https://";
        }
    }

    public static String getLocalName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            log.warn("unable to get local host...", e);
            throw new RuntimeException("unable to figure out local host :/", e);
        }
    }

    public static String getLocalhostUri() {
        return getScheme() + "localhost:" + getLocalPort();
    }

    public static String getLocalHttpIpUri() {
        return getScheme() + getLocalAddressPort();
    }

    public static String getLocalAddressPort() {
        return getLocalAddress() + ":" + getLocalPort();
    }

    public static String getLocalNamePort() {
        return getLocalName() + ":" + getLocalPort();
    }

    public static String getLocalHttpNameUri() {
        return getScheme() + getLocalNamePort();
    }

    public static String getLocalAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.warn("unable to get local address...", e);
            throw new RuntimeException("unable to figure out local address :/", e);
        }
    }

    public static int getLocalPort() {
        return port;
    }

    public static String getScheme() {
        return scheme;
    }
}
