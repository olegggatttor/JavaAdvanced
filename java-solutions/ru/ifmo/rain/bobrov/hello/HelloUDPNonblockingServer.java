package ru.ifmo.rain.bobrov.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Class of server working with UDP protocol implementing {@link HelloServer}
 * Uses non-blocking I/O.
 */
public class HelloUDPNonblockingServer implements HelloServer {
    private final ExecutorService manager = Executors.newSingleThreadExecutor();
    private ExecutorService workersPool;
    private Selector selector;

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(int port, int threads) {
        workersPool = Executors.newFixedThreadPool(threads);
        try {
            selector = Selector.open();
        } catch (IOException e) {
            System.err.println("Impossible to open selector.");
            return;
        }
        try {
            final DatagramChannel serverChannel = DatagramChannel.open();
            serverChannel.bind(new InetSocketAddress(port));
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_READ);
        } catch (IOException e) {
            System.err.println("I/O error occurred.");
            return;
        }
        manager.submit(() -> {
            final ConcurrentLinkedQueue<Response> readyResponses = new ConcurrentLinkedQueue<>();
            while (selector.isOpen()) {
                try {
                    selector.select(10);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                for (final Iterator<SelectionKey> i = selector.selectedKeys().iterator(); i.hasNext(); ) {
                    final SelectionKey key = i.next();
                    final DatagramChannel channel = (DatagramChannel) key.channel();
                    if (key.isReadable()) {
                        final ByteBuffer readBuffer = ByteBuffer.allocate(1 << 16);
                        final SocketAddress clientAddress;
                        try {
                            clientAddress = channel.receive(readBuffer);
                        } catch (IOException e) {
                            close();
                            return;
                        }
                        workersPool.submit(() -> {
                            String answer = "Hello, " + HelloUtils.extractFromBuffer(readBuffer);
                            readBuffer.clear();
                            HelloUtils.fillBuffer(answer, readBuffer);
                            readyResponses.add(new Response(readBuffer, clientAddress));
                            key.interestOps(SelectionKey.OP_WRITE);
                        });
                    } else if (key.isWritable()) {
                        while (!readyResponses.isEmpty()) {
                            final Response response = readyResponses.poll();
                            try {
                                channel.send(response.getBuffer(), response.getAddress());
                            } catch (IOException e) {
                                close();
                                e.printStackTrace();
                            }
                        }
                        key.interestOps(SelectionKey.OP_READ);
                    }
                    i.remove();
                }
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        HelloUtils.shutdownPool(workersPool);
        manager.shutdownNow();
        try {
            selector.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        final HelloServer server = new HelloUDPNonblockingServer();
        HelloUtils.runServer(server, args);
    }
}
