package io.alchevrier.message;

/**
 * Data passed around from a consumer to a producer
 * @param offset
 * @param data
 */
public record Message(long offset, byte[] data) {
}
