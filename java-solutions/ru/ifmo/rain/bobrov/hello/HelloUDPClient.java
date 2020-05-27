package ru.ifmo.rain.bobrov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Public class provides client interface working with UDP protocol and implementing {@link HelloClient}
 */
public class HelloUDPClient implements HelloClient {

    /**
     * Default constructor.
     */
    public HelloUDPClient() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(final String host, final int port, final String prefix, final int threads, final int requests) {
        final ExecutorService requestService = Executors.newFixedThreadPool(threads);
        final InetSocketAddress serverHost = new InetSocketAddress(host, port);
        for (int i = 0; i < threads; i++) {
            final int pos = i;
            requestService.submit(() -> sendRequest(prefix, requests, pos, serverHost));
        }
        HelloUtils.shutdownPool(requestService);
    }

    private void sendRequest(final String prefix, final int requests, final int n, final InetSocketAddress socketAddress) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(500);
            final byte[] response = new byte[socket.getReceiveBufferSize()];
            final DatagramPacket packetRequest = new DatagramPacket(new byte[0], 0, socketAddress);
            final DatagramPacket packetReceive = new DatagramPacket(response, response.length);
            for (int i = 0; i < requests; i++) {
                final String requestName = (prefix + n + "_" + i);
                packetRequest.setData(requestName.getBytes(StandardCharsets.UTF_8));
                while (!socket.isClosed()) {
                    try {
                        socket.send(packetRequest);
                        socket.receive(packetReceive);
                        final String answer = HelloUtils.getString(packetReceive);
                        if (answer.equals("Hello, " + requestName)) {
                            //System.out.println(answer);
                            break;
                        }
                    } catch (IOException ignored) {

                    }
                }
            }
        } catch (SocketException ex) {
            System.err.println(ex.getMessage());
        }
    }

    /**
     * Main method.
     *
     * @param args
     */
    public static void main(String[] args) {
        if (HelloUtils.validateArguments(5, args)) {
            System.err.println("Wrong arguments");
            return;
        }
        final HelloClient client = new HelloUDPClient();
        HelloUtils.runClient(client, args);
    }
}
