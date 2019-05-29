package com.flightstats.hub.config.properties;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
public class LocalHostProperties {

    private String scheme = "http://";
    private final int port;

    @Inject
    public LocalHostProperties(AppProperties appProperties, SystemProperties systemProperties) {
        this.port =  systemProperties.getHttpBindPort();
        if (appProperties.isAppEncrypted()) {
            scheme = "https://";
        }
    }

    public String getName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            log.warn("unable to get local host...", e);
            throw new RuntimeException("unable to figure out local host :/", e);
        }
    }

    public String getAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.warn("unable to get local address...", e);
            throw new RuntimeException("unable to figure out local address :/", e);
        }
    }

    public int getPort() {
        return port;
    }

    public String getScheme() {
        return scheme;
    }

    public String getNameWithPort() {
        return getName() + ":" + getPort();
    }

    public String getAddressWithPort() {
        return getAddress() + ":" + getPort();
    }

    public String getUri() {
        return getScheme() + "localhost:" + getPort();
    }

    public String getUriWithHostIp() {
        return getScheme() + getAddressWithPort();
    }

    public String getUriWithHostName() {
        return getScheme() + getNameWithPort();
    }

    public String getHost(boolean useName) {
        return useName? getNameWithPort() : getAddressWithPort();
    }

}
