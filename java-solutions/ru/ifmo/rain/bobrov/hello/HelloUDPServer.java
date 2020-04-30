package ru.ifmo.rain.bobrov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Class of server working with UDP protocol implementing {@link HelloServer}
 */
public class HelloUDPServer implements HelloServer {
    private ExecutorService receiveService = null;
    private DatagramSocket socket = null;


    /**
     * Default constructor.
     */
    public HelloUDPServer() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(int port, int threads) {
        receiveService = Executors.newFixedThreadPool(threads);
        final int requestSize;
        try {
            socket = new DatagramSocket(port);
            socket.setSoTimeout(1000);
            requestSize = socket.getReceiveBufferSize();
        } catch (SocketException ex) {
            ex.printStackTrace();
            return;
        }
        for (int i = 0; i < threads; i++) {
            final byte[] request = new byte[requestSize];
            final DatagramPacket packet = new DatagramPacket(request, requestSize);
            receiveService.submit(() -> {
                while (!socket.isClosed()) {
                    try {
                        socket.receive(packet);
                        byte[] answer = ("Hello, " + new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8);
                        socket.send(new DatagramPacket(answer, answer.length, packet.getSocketAddress()));
                    } catch (IOException ignored) {

                    }
                }
            });
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        socket.close();
        receiveService.shutdown();
        try {
            if (!receiveService.awaitTermination(5L, TimeUnit.SECONDS)) {
                receiveService.shutdownNow();
            }
        } catch (InterruptedException ex) {
            receiveService.shutdownNow();
        }
        //receiveService.shutdownNow();
    }

    /**
     * Main method.
     *
     * @param args
     */
    public static void main(String[] args) {
        if (args.length != 2 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Wrong arguments");
            return;
        }
        HelloServer server = new HelloUDPServer();
        try {
            final int port = Integer.parseInt(args[0]);
            final int threads = Integer.parseInt(args[1]);
            server.start(port, threads);
        } catch (NumberFormatException ex) {
            System.err.println("Port and threads should be values of type int.");
        }
    }
}
