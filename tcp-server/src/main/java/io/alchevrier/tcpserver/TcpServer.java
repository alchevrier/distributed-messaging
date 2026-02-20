package io.alchevrier.tcpserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class TcpServer implements AutoCloseable {
    private final int port;
    private final ServerHandler serverHandler;

    private boolean gracefullyShutdown;

    public TcpServer(int port, ServerHandler serverHandler) {
        this.port = port;
        this.serverHandler = serverHandler;
    }

    public void start() {
        try {
            var serverSockerChannel = ServerSocketChannel.open();
            serverSockerChannel.socket().bind(new InetSocketAddress(port));
            serverSockerChannel.configureBlocking(false);

            while (!gracefullyShutdown) {
                var socketChannel = serverSockerChannel.accept();
                if (socketChannel != null) {
                    try {
                        while (!gracefullyShutdown) {
                            var messageLength = getMessageLength(socketChannel);
                            var request = getRequest(messageLength, socketChannel);
                            writeResponseToClient(request, socketChannel);
                        }
                    } finally {
                        socketChannel.close();
                    }
                } else {
                    Thread.sleep(10);
                }
            }

            serverSockerChannel.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int getMessageLength(SocketChannel socketChannel) throws IOException {
        var lengthBuffer = ByteBuffer.allocate(4);
        while (lengthBuffer.hasRemaining()) {
            int bytesRead = socketChannel.read(lengthBuffer);
            if (bytesRead == -1) throw new IOException("Client disconnected");
        }
        lengthBuffer.flip();

        return lengthBuffer.getInt();
    }

    private byte[] getRequest(int messageLength, SocketChannel socketChannel) throws IOException {
        var messageBuffer = ByteBuffer.allocate(messageLength);
        while (messageBuffer.hasRemaining()) {
            int bytesRead = socketChannel.read(messageBuffer);
            if (bytesRead == -1) throw new IOException("Client disconnected");
        }
        messageBuffer.flip();
        return messageBuffer.array();
    }

    private void writeResponseToClient(byte[] request, SocketChannel socketChannel) throws IOException {
        var response = serverHandler.handle(request);
        var responseBuffer = ByteBuffer.wrap(response);
        while (responseBuffer.hasRemaining()) {
            socketChannel.write(responseBuffer);
        }
    }

    public void close() {
        this.gracefullyShutdown = true;
    }
}
