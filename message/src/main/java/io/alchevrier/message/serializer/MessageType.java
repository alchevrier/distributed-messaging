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
    public static final byte CONSUME_REQUEST = 0x01;
    public static final byte CONSUME_RESPONSE = 0x02;
    public static final byte PRODUCE_REQUEST = 0x03;
    public static final byte PRODUCE_RESPONSE = 0x04;
    public static final byte FLUSH_REQUEST = 0x05;
    public static final byte FLUSH_RESPONSE = 0x06;

    /**
     * 50: REQUEST_VOTE_REQUEST
     * 51: REQUEST_VOTE_RESPONSE
     * 52: APPEND_ENTRIES_REQUEST
     * 53: APPEND_ENTRIES_RESPONSE
     * 54: APPEND_REQUEST
     * 55: APPEND_RESPONSE
     */
    public static final byte REQUEST_VOTE_REQUEST = 0x50;
    public static final byte REQUEST_VOTE_RESPONSE = 0x51;
    public static final byte APPEND_ENTRIES_REQUEST = 0x52;
    public static final byte APPEND_ENTRIES_RESPONSE = 0x53;
    public static final byte APPEND_REQUEST = 0x54;
    public static final byte APPEND_RESPONSE = 0x55;

    /**
     * 70: CHECK_LEADERSHIP_OF_PARTITION_REQUEST
     * 71: CHECK_LEADERSHIP_OF_PARTITION_RESPONSE
     * 72: CHECK_INDEX_OF_PARTITION_REQUEST
     * 73: CHECK_INDEX_OF_PARTITION_RESPONSE
     */
    public static final byte CHECK_LEADERSHIP_OF_PARTITION_REQUEST = 0x70;
    public static final byte CHECK_LEADERSHIP_OF_PARTITION_RESPONSE = 0x71;
    public static final byte CHECK_INDEX_OF_PARTITION_REQUEST = 0x72;
    public static final byte CHECK_INDEX_OF_PARTITION_RESPONSE = 0x73;
}
