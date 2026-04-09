package io.alchevrier.tcpclient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class TcpClient implements AutoCloseable {
    private final String host;
    private final int port;
    private final long readTimeout;
    private SocketChannel channel;

    public TcpClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.readTimeout = 1000;
    }

    public TcpClient(String host, int port, long readTimeout) {
        this.host = host;
        this.port = port;
        this.readTimeout = readTimeout;
    }

    public synchronized <Req, Res> Res forwardToServer(Req message, Function<Req, byte[]> serializer, Function<byte[], Res> deserializer) throws IOException {
        if (this.channel == null) {
            connectToServer();
        }
        forwardToServer(serializer.apply(message));
        try {
            var rawResponse = CompletableFuture.supplyAsync(() -> {
                try {
                    return this.readResponse();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).get(readTimeout, TimeUnit.MILLISECONDS);
            return deserializer.apply(rawResponse);
        } catch (Exception e) {
            try { channel.close(); } catch (IOException ignored) {}
            channel = null;
            throw new IOException("Read Timeout");
        }
    }

    private void forwardToServer(byte[] toForward) throws IOException {
        var bufferToForward = ByteBuffer.wrap(toForward);
        while (bufferToForward.hasRemaining()) {
            channel.write(bufferToForward);
        }
    }

    private byte[] readResponse() throws IOException {
        var responseMessageBuffer = ByteBuffer.allocate(readResponseLength() - 4);
        while (responseMessageBuffer.hasRemaining()) {
            var read = channel.read(responseMessageBuffer);
            if (read == -1) {
                throw new IOException("Connection closed by server");
            }
        }

        return responseMessageBuffer.array();
    }

    private int readResponseLength() throws IOException {
        var responseLengthBuffer = ByteBuffer.allocate(4);
        while (responseLengthBuffer.hasRemaining()) {
            var read = channel.read(responseLengthBuffer);
            if (read == -1) {
                throw new IOException("Connection closed by server");
            }
        }
        responseLengthBuffer.flip();

        return responseLengthBuffer.getInt();
    }

    private void connectToServer() throws IOException {
        this.channel = SocketChannel.open();
        this.channel.connect(new InetSocketAddress(host, port));
    }

    @Override
    public void close() throws Exception {
        if (this.channel != null) {
            this.channel.close();
        }
    }
}
