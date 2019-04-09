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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Builder
public class IntegrationUdpServer {
    private final static Logger logger = LoggerFactory.getLogger(IntegrationUdpServer.class);
    private int port;
    private AtomicBoolean listening;
    private CountDownLatch startupCountDownLatch;
    private AtomicBoolean canListen;
    private ExecutorService executorService;

    private final Map<String, String> store = new HashMap<>();

    public CompletableFuture<Map<String, String>> getServerFuture() {
        return  CompletableFuture.supplyAsync(() -> {
            try {
                DatagramSocket serverSocket = new DatagramSocket(port);
                while (listening.get()) {
                    byte[] data = new byte[70];
                    DatagramPacket receivePacket = new DatagramPacket(data, data.length);
                    startupCountDownLatch.countDown();
                    serverSocket.receive(receivePacket);

                    String result = listen(receivePacket);
                    addValueToStore(result);
                }
            } catch (IOException e) {
                listening.set(false);
            }
//        logger.info(":::::::::, {}", store);
            return store;
        }, executorService);
    }

    private String listen(DatagramPacket receivePacket) throws IOException {
        if (!canListen.get()) {
            return "";
        }

        InputStream inputStream = new ByteArrayInputStream(receivePacket.getData());
        InputStreamReader streamReader =  new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(streamReader);
        String result = reader
                .lines()
                .peek(logger::info)
                .collect(Collectors.toList())
                .get(0)
                .trim();

        if (result.contains("closeSocket")) {
            listening.set(false);
        }

        reader.close();
        streamReader.close();
        logger.info(result);
        return result;
    }

    private void addValueToStore(String currentResult) {
        String key = currentResult.substring(0, currentResult.indexOf(":"));
        store.put(key, currentResult);
    }

    public Map<String, String> getResult () {
        return store;
    }
}
