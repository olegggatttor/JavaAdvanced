package ru.ifmo.rain.bobrov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
        final InetAddress serverHost;
        try {
            serverHost = InetAddress.getByName(host);
        } catch (UnknownHostException ex) {
            System.err.println("Unknown host name:" + host);
            return;
        }
        for (int i = 0; i < threads; i++) {
            final int pos = i;
            requestService.submit(() -> sendRequest(port, prefix, requests, pos, serverHost));
        }
        requestService.shutdown();
        try {
            if (!requestService.awaitTermination(10000, TimeUnit.SECONDS)) {
                requestService.shutdownNow();
            }
        } catch (InterruptedException ex) {
            requestService.shutdownNow();
        }
    }

    private void sendRequest(final int port, final String prefix, final int requests, final int n, final InetAddress serverHost) {
        DatagramSocket socket;
        final int responseSize;
        try {
            socket = new DatagramSocket();
            socket.setSoTimeout(500);
            responseSize = socket.getReceiveBufferSize();
        } catch (SocketException ex) {
            return;
        }
        for (int i = 0; i < requests; i++) {
            final String requestName = (prefix + n + "_" + i);
            final byte[] request = requestName.getBytes(StandardCharsets.UTF_8);
            final DatagramPacket packet = new DatagramPacket(request, request.length, serverHost, port);
            while (!socket.isClosed()) {
                try {
                    socket.send(packet);
                    final byte[] response = new byte[responseSize];
                    final DatagramPacket packetReceive = new DatagramPacket(response, response.length);
                    socket.receive(packetReceive);
                    final String answer = new String(packetReceive.getData(), packetReceive.getOffset(), packetReceive.getLength(),
                            StandardCharsets.UTF_8);
                    if (answer.equals("Hello, " + requestName)) {
                        System.out.println(answer);
                        break;
                    }
                } catch (IOException ignored) {

                }
            }
        }
    }

    /**
     * Main method.
     *
     * @param args
     */
    public static void main(String[] args) {
        if (args.length != 5 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Wrong arguments");
            return;
        }
        final HelloClient client = new HelloUDPClient();
        try {
            final String host = args[0];
            final int port = Integer.parseInt(args[1]);
            final String prefix = args[2];
            final int threads = Integer.parseInt(args[3]);
            final int requestsPerThread = Integer.parseInt(args[4]);
            client.run(host, port, prefix, threads, requestsPerThread);
        } catch (NumberFormatException ex) {
            System.err.println("Port, thread and requests per thread must be values of type int.");
        }
    }
}
