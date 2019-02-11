package com.flightstats.hub.util;

import lombok.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import com.google.common.collect.ImmutableMap;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Builder
public class IntegrationUdpServer {
    private long timeoutMillis;
    private int port;
    private boolean listening;
    private final Logger logger = LoggerFactory.getLogger(IntegrationUdpServer.class);

    @Builder.Default
    private Map<String, String> store = ImmutableMap.<String, String>builder().put("a", "b").build();

    private final CompletableFuture<Void> serverFuture = CompletableFuture.runAsync(() -> {
        try {
            DatagramSocket serverSocket = new DatagramSocket(port);
            listening = true;
            while(listening) {
                byte[] data = new byte[1024];
                String result = openSocket(data, serverSocket);
                addValueToStore(result);
            }
        } catch (Exception ex) {
            listening = false;
            throw new IllegalStateException(ex);
        }
    });

    private String openSocket(byte[] data, DatagramSocket serverSocket) {
        DatagramPacket receivePacket = new DatagramPacket(data, data.length);
        try {
            serverSocket.receive(receivePacket);
            return new String(receivePacket.getData()).trim();
        } catch (IOException ex) {
            serverFuture.completeExceptionally(ex);
            return "";
        }
    }

    private void addValueToStore(String currentResult) {
        String key = currentResult.substring(0, currentResult.indexOf(":"));
        store = ImmutableMap.<String, String>builder()
                .putAll(store)
                .put(key, currentResult)
                .build();
    }

    public void closeServer() {
        this.listening = false;
    }

    public Map<String, String> getResult () {
        try {
            serverFuture.get(timeoutMillis, TimeUnit.MILLISECONDS);
            return store;
        } catch (Exception ex) {
            logger.error("error in udp server " + ex);
            serverFuture.completeExceptionally(ex);
            return store;
        }
    }
}
