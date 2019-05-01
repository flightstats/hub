package com.flightstats.hub.app;

import com.flightstats.hub.config.AppProperty;
import com.flightstats.hub.config.PropertyLoader;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
public class HubHost {

    private static final AppProperty appProperty = new AppProperty(PropertyLoader.getInstance());
    private static final int port;
    private static String scheme = "http://";

    static {
        port = appProperty.getHttpBindPort();
        if (appProperty.isAppEncrypted()) {
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
