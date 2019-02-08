package com.flightstats.hub.util;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.IOException;

public class IntegrationUdpServer {
    private int port;
    private boolean listen = false;
    private String results = "";
    private Runnable runnable  = () -> {
        try {
            start();
        } catch (Exception ex) {
            System.out.println("caught exception" + ex);
        }
    };

    public void startServer() {
        new Thread(runnable).start();
    }

    public IntegrationUdpServer(int port) {
        this.port = port;
    }

            public void start() throws IOException {
                DatagramSocket serverSocket = new DatagramSocket(port);
                byte[] data = new byte[1024];
                this.listen = true;
                while(this.listen) {
                    DatagramPacket receivePacket = new DatagramPacket(data, data.length);
                    serverSocket.receive(receivePacket);
                    String sentence = new String( receivePacket.getData());
                    this.results = sentence;
                    System.out.println("RECEIVED: " + sentence);
                    InetAddress IPAddress = receivePacket.getAddress();
                    int port = receivePacket.getPort();
                    String capitalizedSentence = sentence.toUpperCase();
                    data = capitalizedSentence.getBytes();
                    DatagramPacket sendPacket =
                            new DatagramPacket(data, data.length, IPAddress, port);
                    serverSocket.send(sendPacket);
                }
            }

            public String getResults() {
                return results;
            }

            public void stop() {
                this.listen = false;
            }


}
