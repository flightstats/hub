package com.flightstats.hub.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Singleton
public class HubHost {

    private final static Logger logger = LoggerFactory.getLogger(HubHost.class);

    @Inject
    @Named("HubPort")
    private static int port;

    @Inject
    @Named("HubScheme")
    private static String scheme;

    public static String getLocalName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            logger.warn("unable to get local host...", e);
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
            logger.warn("unable to get local address...", e);
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
