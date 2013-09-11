package com.flightstats.datahub.app.config;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.list;

public class JettyServerConnectorFactory {
    private static final int DEFAULT_IDLE_TIMEOUT = 30000;

    private final Server server;
    private final ConnectionFactory connectionFactory;
    private final int listenPort;
    private final Supplier<List<NetworkDevice>> networkDeviceSupplier;

    public JettyServerConnectorFactory(Server server, ConnectionFactory connectionFactory, int listenPort) {
        this(new DefaultNetworkDeviceSupplier(), server, connectionFactory, listenPort);
    }

    public JettyServerConnectorFactory(Supplier<List<NetworkDevice>> networkDeviceSupplier, Server server, ConnectionFactory connectionFactory, int listenPort) {
        this.server = server;
        this.connectionFactory = connectionFactory;
        this.listenPort = listenPort;
        this.networkDeviceSupplier = networkDeviceSupplier;
    }

    public List<Connector> build(String commaSeparatedInterfaces) throws SocketException {
        if(isNullOrEmpty(commaSeparatedInterfaces)){
           return Arrays.asList(newServerConnector("0.0.0.0")); //bind to all interfaces
        }
        Collection<NetworkDevice> desiredNetworkDevices = findDesiredNetworkInterfaces(commaSeparatedInterfaces);
        List<String> desiredIvP4Addrs = getIpV4AddrsForInterfaces(desiredNetworkDevices);
        return buildConnectorsForAddresses(desiredIvP4Addrs);
    }

    private List<Connector> buildConnectorsForAddresses(List<String> desiredIvP4Addrs) {
        return Lists.transform(desiredIvP4Addrs, new Function<String, Connector>() {
            @Override
            public Connector apply(String input) {
                return newServerConnector(input);
            }
        });
    }

    private List<String> getIpV4AddrsForInterfaces(Collection<NetworkDevice> desiredNets) {
        List<String> result = new ArrayList<>();
        for (NetworkDevice net : desiredNets) {
            for (InetAddress addr : list(net.getInetAddresses())) {
                String address = addr.getHostAddress();
                if(isIpV4Style(address)){
                    result.add(address);
                }
            }
        }
        return result;
    }

    private Collection<NetworkDevice> findDesiredNetworkInterfaces(String commaSeparatedInterfaces) throws SocketException {
        final List<String> bindInterfaceNames = Arrays.asList(commaSeparatedInterfaces.split(",\\s*"));
        List<NetworkDevice> devices = networkDeviceSupplier.get();

        Collection<String> allInterfaces = Collections2.transform(devices, new Function<NetworkDevice, String>() {
            @Override
            public String apply(NetworkDevice input) {
                return input.getName();
            }
        });

        Collection<NetworkDevice> desiredDevices = Collections2.filter(devices, new Predicate<NetworkDevice>() {
            @Override
            public boolean apply(NetworkDevice input) {
                return bindInterfaceNames.contains(input.getName());
            }
        });

        if(newArrayList(bindInterfaceNames).retainAll(allInterfaces)){
            throw new RuntimeException("One or more configured interfaces is not available.  Configured: " + bindInterfaceNames + ", Available: " + allInterfaces);
        }
        return desiredDevices;
    }

    private boolean isIpV4Style(String address) {
        return address.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$");
    }

    private Connector newServerConnector(String hostAddress) {
        ServerConnector serverConnector = new ServerConnector(server, connectionFactory);
        serverConnector.setHost(hostAddress);
        serverConnector.setPort(listenPort);
        serverConnector.setIdleTimeout(DEFAULT_IDLE_TIMEOUT);
        return serverConnector;
    }

    private static class DefaultNetworkDeviceSupplier implements Supplier<List<NetworkDevice>> {
        @Override
        public List<NetworkDevice> get() {
            try {
                List<NetworkInterface> interfaces = list(NetworkInterface.getNetworkInterfaces());
                return Lists.transform(interfaces, new Function<NetworkInterface, NetworkDevice>() {
                    @Override
                    public NetworkDevice apply(NetworkInterface input) {
                        return new NetworkDeviceImpl(input);
                    }
                });
            } catch (SocketException e) {
                throw new RuntimeException("Error obtaining network interfaces", e);
            }
        }
    }
}
