package io.alchevrier.tcpclient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.function.Function;

public class TcpClient {
    private final SocketChannel channel;

    public TcpClient(String host, int port) {
        try {
            this.channel = SocketChannel.open();
            this.channel.connect(new InetSocketAddress(host, port));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public <Req, Res> Res forwardToServer(Req message, Function<Req, byte[]> serializer, Function<byte[], Res> deserializer) throws IOException {
        byte[] toForward = serializer.apply(message);
        var bufferToForward = ByteBuffer.wrap(toForward);
        while (bufferToForward.hasRemaining()) {
            channel.write(bufferToForward);
        }

        var responseLengthBuffer = ByteBuffer.allocate(4);
        while (responseLengthBuffer.hasRemaining()) {
            var read = channel.read(responseLengthBuffer);
            if (read == -1) {
                throw new IOException("Connection closed by server");
            }
        }
        responseLengthBuffer.flip();

        var responseMessageBuffer = ByteBuffer.allocate(responseLengthBuffer.getInt());
        while (responseMessageBuffer.hasRemaining()) {
            var read = channel.read(responseMessageBuffer);
            if (read == -1) {
                throw new IOException("Connection closed by server");
            }
        }

        return deserializer.apply(responseMessageBuffer.array());
    }
}
