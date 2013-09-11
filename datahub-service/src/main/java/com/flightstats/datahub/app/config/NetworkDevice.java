package com.flightstats.datahub.app.config;

import java.net.InetAddress;
import java.util.Enumeration;

public interface NetworkDevice {

    String getName();
    Enumeration<InetAddress> getInetAddresses();

}
