package io.alchevrier.message;

/**
 * Requesting messages for a given Topic and startingOffset and optionally asking for a specific batchSize
 * @param topic specific topic to consume messages from
 * @param partition specific partition within the topic to consume messages from
 * @param startingOffset offset at which to start consuming messages for the given topic.
 *                       ConsumeResponse will provide the next offset to consume from
 * @param batchSize maximum number of messages to be returned in ConsumeResponse
 */
public record ConsumeRequest(Topic topic, int partition, long startingOffset, int batchSize) {
}
