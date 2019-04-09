package com.flightstats.hub.util;

import lombok.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Builder
public class IntegrationUdpServer {
    private final static Logger logger = LoggerFactory.getLogger(IntegrationUdpServer.class);
    private long timeoutMillis;
    private final List<String> queue = new ArrayList<>();
    private int port;
    private boolean listening;

    private final Map<String, String> store = new HashMap<>();

    private final CompletableFuture<Map<String, String>> serverFuture = CompletableFuture.supplyAsync(() -> {
        try {
            DatagramSocket serverSocket = new DatagramSocket(port);
            while (listening) {
                byte[] data = new byte[70];
                String result = openSocket(data, serverSocket);
                addValueToStore(result);
            }
        } catch(Exception ex) {
            listening = false;
        }
//        logger.info(":::::::::, {}", store);
        return store;
    });

    private String openSocket(byte[] data, DatagramSocket serverSocket) {
        DatagramPacket receivePacket = new DatagramPacket(data, data.length);
        try {
            serverSocket.receive(receivePacket);
            InputStream inputStream = new ByteArrayInputStream(receivePacket.getData());
            InputStreamReader streamReader =  new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            BufferedReader reader = new BufferedReader(streamReader);
            String result = reader
                    .lines()
                    .collect(Collectors.toList())
                    .get(0)
                    .trim();
            if (result.contains("closeSocket")) {
                listening = false;
            }
            reader.close();
            streamReader.close();
            return result;
        } catch (IOException ex) {
            logger.error("error io exception at open socket", ex);
            serverFuture.completeExceptionally(ex);
            return "";
        }
    }

    private void addValueToStore(String currentResult) {
        String key = currentResult.substring(0, currentResult.indexOf(":"));
        store.put(key, currentResult);
    }

    public Map<String, String> getResult () {
        try {
            return serverFuture.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (Exception ex) {
            logger.error("error in udp server " + ex);
            serverFuture.completeExceptionally(ex);
            return store;
        }
    }
}
