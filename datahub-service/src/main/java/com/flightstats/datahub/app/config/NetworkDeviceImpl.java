package com.flightstats.datahub.app.config;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class NetworkDeviceImpl implements NetworkDevice {
    
    private final NetworkInterface networkInterface;

    public NetworkDeviceImpl(NetworkInterface networkInterface) {
        this.networkInterface = networkInterface;
    }

    @Override
    public String getName() {
        return networkInterface.getName();
    }

    @Override
    public Enumeration<InetAddress> getInetAddresses() {
        return networkInterface.getInetAddresses();
    }
}
