package ru.ifmo.rain.bobrov.hello;

import java.net.SocketAddress;
import java.nio.ByteBuffer;

/**
 *  Class is used to store server responses.
 */
class Response {
    private final SocketAddress address;
    private final ByteBuffer buffer;

    /**
     * @return socket address.
     */
    SocketAddress getAddress() {
        return address;
    }

    /**
     * @return stored buffer.
     */
    ByteBuffer getBuffer() {
        return buffer;
    }

    Response(ByteBuffer buffer, SocketAddress address) {
        this.address = address;
        this.buffer = buffer;
    }


}
