package com.flightstats.hub.util;

import lombok.Builder;
import lombok.Getter;

import java.util.concurrent.CompletableFuture;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Builder
public class IntegrationUdpServer {
    private long timeoutMillis;
    private int port;

    @Builder.Default
    private boolean listening = false;

    private final CompletableFuture<Void> timeoutFuture = CompletableFuture.runAsync(new TimeoutRunnable());

    private final CompletableFuture<String> serverFuture = CompletableFuture.supplyAsync(() -> {
        String result = "";
        try {
            DatagramSocket serverSocket = new DatagramSocket(port);
            byte[] data = new byte[1024];
            listening = true;
            while(listening) {
                result = openSocket(data, serverSocket);
                if (!result.equals("")) {
                    listening = false;
                }
            }
        } catch (Exception ex) {
            listening = false;
            throw new IllegalStateException(ex);
        }
        return result;
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

    public String getAsyncResult() {
        try {
            return serverFuture.get();
        } catch (Exception ex) {
            serverFuture.completeExceptionally(ex);
            return "";
        }
    }

    private class TimeoutRunnable implements Runnable {
        @Override
        public void run() {
            try {
                Thread.sleep(timeoutMillis);
                if (!serverFuture.isDone()) {
                    serverFuture.completeExceptionally(new TimeoutException("timed out waiting for test task completion"));
                }
            } catch (Exception ex) {
                serverFuture.completeExceptionally(ex);
            }
        }
    }
}
