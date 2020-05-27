package ru.ifmo.rain.bobrov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
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
                        byte[] answer = ("Hello, " + HelloUtils.getString(packet)).getBytes(StandardCharsets.UTF_8);
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
        HelloUtils.shutdownPool(receiveService);
    }

    /**
     * Main method.
     *
     * @param args
     */
    public static void main(String[] args) {
        if (HelloUtils.validateArguments(2, args)) {
            System.err.println("Wrong arguments");
            return;
        }
        try(HelloServer server = new HelloUDPServer()) {
            HelloUtils.runServer(server, args);
        }

    }
}
