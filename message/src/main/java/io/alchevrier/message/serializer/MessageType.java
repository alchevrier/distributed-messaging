package io.alchevrier.message.serializer;

public class MessageType {
    /**
     * 01: CONSUME_REQUEST
     * 02: CONSUME_RESPONSE
     * 03: PRODUCE_REQUEST
     * 04: PRODUCE_RESPONSE
     * 05: FLUSH_REQUEST
     * 06: FLUSH_RESPONSE
     */
    public static byte CONSUME_REQUEST = 0x01;
    public static byte CONSUME_RESPONSE = 0x02;
    public static byte PRODUCE_REQUEST = 0x03;
    public static byte PRODUCE_RESPONSE = 0x04;
    public static byte FLUSH_REQUEST = 0x05;
    public static byte FLUSH_RESPONSE = 0x06;

}
