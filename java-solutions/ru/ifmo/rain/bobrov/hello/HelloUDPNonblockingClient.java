package ru.ifmo.rain.bobrov.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

/**
 * Public class provides client interface working with UDP protocol and implementing {@link HelloClient}.
 * Uses non-blocking I/O.
 */
public class HelloUDPNonblockingClient implements HelloClient {

    /**
     * {@inheritDoc}
     */
    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        try (final Selector selector = Selector.open()) {
            final InetSocketAddress address = new InetSocketAddress(host, port);
            try {
                for (int i = 0; i < threads; i++) {
                    final DatagramChannel channel = DatagramChannel.open();
                    channel.connect(address);
                    channel.configureBlocking(false);
                    channel.register(selector, SelectionKey.OP_WRITE, new ChannelAttachment(prefix, i, requests));
                }
            } catch (IOException ex) {
                System.err.println("Impossible to create channels.");
                return;
            }
            int openChannels = threads;
            while (openChannels > 0) {
                selector.select(10);
                if (selector.selectedKeys().isEmpty()) {
                    selector.keys().forEach(key -> key.interestOps(SelectionKey.OP_WRITE));
                    continue;
                }
                final ByteBuffer buffer = ByteBuffer.allocate(1 << 16);
                for (final Iterator<SelectionKey> curKey = selector.selectedKeys().iterator(); curKey.hasNext(); ) {
                    final SelectionKey key = curKey.next();
                    final DatagramChannel channel = (DatagramChannel) key.channel();
                    final ChannelAttachment attachment = (ChannelAttachment) key.attachment();
                    buffer.clear();
                    try {
                        if (key.isReadable()) {
                            channel.read(buffer);
                            final String correctResponse = "Hello, " + attachment.getCurrentString();
                            final String received = HelloUtils.extractFromBuffer(buffer);
                            if (received.equals(correctResponse)) {
                                //System.out.println(received);
                                attachment.increaseRequests();
                            }
                            if (attachment.isDone()) {
                                channel.close();
                                openChannels--;
                                continue;
                            }
                            key.interestOps(SelectionKey.OP_WRITE);
                        } else if (key.isWritable()) {
                            HelloUtils.fillBuffer(attachment.getCurrentString(), buffer);
                            channel.write(buffer);
                            key.interestOps(SelectionKey.OP_READ);
                        }
                    } catch (IOException ex) {
                        channel.close();
                        continue;
                    }
                    curKey.remove();
                }
            }
        } catch (IOException ex) {
            System.err.println("I/O exception occurred.");
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
        HelloUtils.runClient(new HelloUDPNonblockingClient(), args);
    }
}
