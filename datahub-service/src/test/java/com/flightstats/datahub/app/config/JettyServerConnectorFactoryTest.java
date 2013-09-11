package com.flightstats.datahub.app.config;

import com.google.common.base.Supplier;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.Test;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JettyServerConnectorFactoryTest {

    @Test
    public void testHappyPath() throws Exception {
    	//GIVEN
        Supplier<List<NetworkDevice>> deviceSupplier = mock(Supplier.class);
        Server server = mock(Server.class);
        ConnectionFactory factory = mock(ConnectionFactory.class);
        NetworkDevice net1 = mock(NetworkDevice.class);
        NetworkDevice net2 = mock(NetworkDevice.class);
        InetAddress addr1 = mock(InetAddress.class);
        InetAddress addr2 = mock(InetAddress.class);
        InetAddress addr3 = mock(InetAddress.class);
        Enumeration<InetAddress> net1Addresses = Collections.enumeration(Arrays.asList(addr1, addr2));
        Enumeration<InetAddress> net2Addresses = Collections.enumeration(Arrays.asList(addr3));

        when(factory.getProtocol()).thenReturn("http");
        when(addr1.getHostAddress()).thenReturn("10.0.0.1");
        when(addr2.getHostAddress()).thenReturn("fe80::22c9:d0ff:fe86:a5");
        when(addr3.getHostAddress()).thenReturn("127.0.0.1");

        when(net1.getName()).thenReturn("eth0");
        when(net1.getInetAddresses()).thenReturn(net1Addresses);

        when(net2.getName()).thenReturn("lo0");
        when(net2.getInetAddresses()).thenReturn(net2Addresses);
        when(deviceSupplier.get()).thenReturn(Arrays.asList(net1, net2));

        JettyServerConnectorFactory testClass = new JettyServerConnectorFactory(deviceSupplier, server, factory, 8080);

        //WHEN
        List<Connector> result = testClass.build("eth0,lo0");

        //THEN
        assertEquals(2, result.size());
        assertEquals("10.0.0.1", ((ServerConnector) result.get(0)).getHost());
        assertEquals("127.0.0.1", ((ServerConnector) result.get(1)).getHost());
    }

    @Test
    public void testEmptyString() throws Exception {
        //GIVEN
        Supplier<List<NetworkDevice>> deviceSupplier = mock(Supplier.class);
        Server server = mock(Server.class);
        ConnectionFactory factory = mock(ConnectionFactory.class);

        when(factory.getProtocol()).thenReturn("http");

        JettyServerConnectorFactory testClass = new JettyServerConnectorFactory(deviceSupplier, server, factory, 8080);

        //WHEN
        List<Connector> result = testClass.build("");

        //THEN
        assertEquals(1, result.size());
        assertEquals("0.0.0.0", ((ServerConnector) result.get(0)).getHost());

    }

    @Test
    public void testNull() throws Exception {
        //GIVEN
        Supplier<List<NetworkDevice>> deviceSupplier = mock(Supplier.class);
        Server server = mock(Server.class);
        ConnectionFactory factory = mock(ConnectionFactory.class);

        when(factory.getProtocol()).thenReturn("http");

        JettyServerConnectorFactory testClass = new JettyServerConnectorFactory(deviceSupplier, server, factory, 8080);

        //WHEN
        List<Connector> result = testClass.build(null);

        //THEN
        assertEquals(1, result.size());
        assertEquals("0.0.0.0", ((ServerConnector) result.get(0)).getHost());
    }

    @Test(expected = RuntimeException.class)
    public void testInterfaceNotFound() throws Exception {
        //GIVEN
        Supplier<List<NetworkDevice>> deviceSupplier = mock(Supplier.class);
        Server server = mock(Server.class);
        ConnectionFactory factory = mock(ConnectionFactory.class);
        NetworkDevice net1 = mock(NetworkDevice.class);
        InetAddress addr1 = mock(InetAddress.class);
        Enumeration<InetAddress> net1Addresses = Collections.enumeration(Arrays.asList(addr1));

        when(factory.getProtocol()).thenReturn("http");
        when(addr1.getHostAddress()).thenReturn("10.0.0.1");
        when(net1.getName()).thenReturn("eth0");
        when(net1.getInetAddresses()).thenReturn(net1Addresses);

        JettyServerConnectorFactory testClass = new JettyServerConnectorFactory(deviceSupplier, server, factory, 8080);

        //WHEN
        testClass.build("boom");

        //THEN
    }

}
