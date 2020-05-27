package ru.ifmo.rain.bobrov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;
import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Utils for UDP client/server.
 */
class HelloUtils {

    static String getString(final DatagramPacket packet) {
        return new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
    }

    private static String decodeByteBuffer(final ByteBuffer buffer) {
        return StandardCharsets.UTF_8.decode(buffer).toString();
    }

    static void shutdownPool(final ExecutorService service) {
        service.shutdown();
        try {
            if (!service.awaitTermination(10L, TimeUnit.SECONDS)) {
                service.shutdownNow();
            }
        } catch (InterruptedException ex) {
            service.shutdownNow();
        }
    }

    static void runClient(final HelloClient client, final String[] args) {
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

    static void runServer(final HelloServer server, final String[] args) {
        try {
            final int port = Integer.parseInt(args[0]);
            final int threads = Integer.parseInt(args[1]);
            server.start(port, threads);
        } catch (NumberFormatException ex) {
            System.err.println("Port and threads should be values of type int.");
        }
    }

    static boolean validateArguments(final int argsAmount, final String[] args) {
        return args.length != argsAmount || Arrays.stream(args).anyMatch(Objects::isNull);
    }

    static String extractFromBuffer(final ByteBuffer buffer) {
        buffer.flip();
        return HelloUtils.decodeByteBuffer(buffer);
    }

    static void fillBuffer(final String content, final ByteBuffer buffer) {
        buffer.put(ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8)));
        buffer.flip();
    }
}
