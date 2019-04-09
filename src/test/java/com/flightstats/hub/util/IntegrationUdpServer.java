package com.flightstats.hub.util;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Builder
public class IntegrationUdpServer {
    private int port;
    private boolean listening;
    private CountDownLatch startupCountDownLatch;
    private ExecutorService executorService;

    private final Map<String, String> store = new HashMap<>();

    public CompletableFuture<Map<String, String>> getServerFuture() {
        return  CompletableFuture.supplyAsync(() -> {
            try {
                DatagramSocket serverSocket = new DatagramSocket(port);
                while (listening) {
                    byte[] data = new byte[70];
                    DatagramPacket receivePacket = new DatagramPacket(data, data.length);
                    startupCountDownLatch.countDown();
                    serverSocket.receive(receivePacket);

                    String result = listen(receivePacket);
                    addValueToStore(result);
                }
            } catch (IOException e) {
                listening = false;
            }
//        log.info(":::::::::, {}", store);
            return store;
        }, executorService);
    }

    private String listen(DatagramPacket receivePacket) throws IOException {
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
    }

    private void addValueToStore(String currentResult) {
        String key = currentResult.substring(0, currentResult.indexOf(":"));
        store.put(key, currentResult);
    }

    public Map<String, String> getResult () {
        return store;
    }
}
