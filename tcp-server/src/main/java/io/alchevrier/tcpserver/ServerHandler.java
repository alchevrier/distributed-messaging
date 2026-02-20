package io.alchevrier.tcpserver;

public interface ServerHandler {
    byte[] handle(byte[] message);
}
