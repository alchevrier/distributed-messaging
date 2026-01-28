package io.alchevrier.message;

import java.util.List;

/**
 * Response sent to the consumer after a ConsumeRequest
 * @param nextOffset next offset to consume with a new ConsumeRequest
 */
public record ConsumeResponse(List<Message> messages, long nextOffset) {
}
