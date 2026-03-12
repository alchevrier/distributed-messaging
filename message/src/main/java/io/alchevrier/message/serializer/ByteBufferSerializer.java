package io.alchevrier.message.serializer;

import io.alchevrier.message.broker.*;
import io.alchevrier.message.raft.AppendEntriesRequest;
import io.alchevrier.message.raft.AppendEntriesResponse;
import io.alchevrier.message.raft.RequestVoteRequest;
import io.alchevrier.message.raft.RequestVoteResponse;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static io.alchevrier.message.serializer.MessageType.*;

public class ByteBufferSerializer {
    public byte[] serialize(ConsumeRequest consumeRequest) {
        var topicName = consumeRequest.topic().name().getBytes(StandardCharsets.UTF_8);
        var length = 4 + 1 + 4 + topicName.length + 4 + 8 + 4;

        var buffer = ByteBuffer.allocate(length);
        buffer.putInt(length);
        buffer.put(CONSUME_REQUEST);
        buffer.putInt(topicName.length);
        buffer.put(topicName);
        buffer.putInt(consumeRequest.partition());
        buffer.putLong(consumeRequest.startingOffset());
        buffer.putInt(consumeRequest.batchSize());

        return buffer.array();
    }

    public byte[] serialize(ConsumeResponse consumeResponse) {
        if (consumeResponse.isError()) {
            var error = consumeResponse.error().getBytes(StandardCharsets.UTF_8);
            var length = 4 + 1 + 1 + 4 + error.length;

            var buffer = ByteBuffer.allocate(length);
            buffer.putInt(length);
            buffer.put(CONSUME_RESPONSE);
            buffer.put((byte) 0);
            buffer.putInt(error.length);
            buffer.put(error);

            return buffer.array();
        } else {
            var messageLength = consumeResponse.messages()
                    .stream()
                    .map(it -> 4 + 8 + it.data().length)
                    .reduce(Integer::sum)
                    .orElse(0);

            var length = 4 + 1 + 1 + 8 + 4 + messageLength;

            var buffer = ByteBuffer.allocate(length);
            buffer.putInt(length);
            buffer.put(CONSUME_RESPONSE);
            buffer.put((byte) 1);
            buffer.putLong(consumeResponse.nextOffset());
            buffer.putInt(consumeResponse.messages().size());

            for (int i = 0; i < consumeResponse.messages().size(); i++) {
                var message = consumeResponse.messages().get(i);
                buffer.putInt(message.data().length);
                buffer.putLong(message.offset());
                buffer.put(message.data());
            }

            return buffer.array();
        }
    }

    public byte[] serialize(ProduceRequest produceRequest) {
        var producerKey = produceRequest.key() != null ? produceRequest.key().getBytes(StandardCharsets.UTF_8) : new byte[0];
        var topicName = produceRequest.topic().name().getBytes(StandardCharsets.UTF_8);
        var length = 4 + 1 + 4 + topicName.length + 4 + producerKey.length + 4 + produceRequest.data().length;

        var buffer = ByteBuffer.allocate(length);
        buffer.putInt(length);
        buffer.put(PRODUCE_REQUEST);
        buffer.putInt(topicName.length);
        buffer.put(topicName);
        buffer.putInt(producerKey.length);
        if (produceRequest.key() != null) {
            buffer.put(producerKey);
        }
        buffer.putInt(produceRequest.data().length);
        buffer.put(produceRequest.data());

        return buffer.array();
    }

    public byte[] serialize(ProduceResponse produceResponse) {
        var errorBytes = produceResponse.isError() ? produceResponse.error().getBytes(StandardCharsets.UTF_8) : new byte[0];
        var errorLength = produceResponse.isError() ? 4 : 0;
        var offsetLength = produceResponse.isError() ? 0 : 8;

        var length = 4 + 1 + 1 + errorLength + errorBytes.length + offsetLength + 4;

        var buffer = ByteBuffer.allocate(length);
        buffer.putInt(length);
        buffer.put(PRODUCE_RESPONSE);
        buffer.put((byte) (produceResponse.isError() ? 0 : 1));

        if (!produceResponse.isError()) {
            buffer.putLong(produceResponse.offset());
        } else {
            buffer.putInt(errorBytes.length);
            buffer.put(errorBytes);
        }

        buffer.putInt(produceResponse.partition());

        return buffer.array();
    }

    public byte[] serialize(FlushRequest flushRequest) {
        var length = 4 + 1;
        var buffer = ByteBuffer.allocate(length);
        buffer.putInt(length);
        buffer.put(FLUSH_REQUEST);
        return buffer.array();
    }

    public byte[] serialize(FlushResponse flushResponse) {
        if (flushResponse.isError()) {
            var errorArray = flushResponse.error().getBytes(StandardCharsets.UTF_8);
            var length = 4 + 1 + 1 + 4 + errorArray.length;
            var buffer = ByteBuffer.allocate(length);
            buffer.putInt(length);
            buffer.put(FLUSH_RESPONSE);
            buffer.put((byte) 0);
            buffer.putInt(errorArray.length);
            buffer.put(errorArray);
            return buffer.array();
        } else {
            var length = 4 + 1 + 1;
            var buffer = ByteBuffer.allocate(length);
            buffer.putInt(length);
            buffer.put(FLUSH_RESPONSE);
            buffer.put((byte) 1);
            return buffer.array();
        }
    }

    public byte[] serialize(RequestVoteRequest request) {
        var length = 4 + 1 + 8 + 4 + 8 + 8;
        var buffer = ByteBuffer.allocate(length);
        buffer.putInt(length);
        buffer.put(REQUEST_VOTE_REQUEST);
        buffer.putLong(request.candidateTerm());
        buffer.putInt(request.candidateId());
        buffer.putLong(request.lastLogIndex());
        buffer.putLong(request.lastLogTerm());
        return buffer.array();
    }

    public byte[] serialize(RequestVoteResponse response) {
        var length = 4 + 1 + 1 + 8;
        var buffer = ByteBuffer.allocate(length);
        buffer.putInt(length);
        buffer.put(REQUEST_VOTE_RESPONSE);
        buffer.put(response.voteGranted() ? (byte) 1 : (byte) 0);
        buffer.putLong(response.currentTerm());
        return buffer.array();
    }

    public byte[] serialize(AppendEntriesRequest request) {
        var lengthOfEntries = 0;
        for (var entry: request.entries()) {
            lengthOfEntries += 4 + entry.length;
        }
        var length = 4 + 1 + 8 + 4 + 8 + 8 + 8 + 4 + lengthOfEntries;
        var buffer = ByteBuffer.allocate(length);
        buffer.putInt(length);
        buffer.put(APPEND_ENTRIES_REQUEST);
        buffer.putLong(request.term());
        buffer.putInt(request.leaderId());
        buffer.putLong(request.leaderCommitIndex());
        buffer.putLong(request.prevLogIndex());
        buffer.putLong(request.prevLogTerm());
        buffer.putInt(request.entries().length);

        for (var entry: request.entries()) {
            buffer.putInt(entry.length);
            buffer.put(entry);
        }

        return buffer.array();
    }

    public byte[] serialize(AppendEntriesResponse response) {
        var length = 4 + 1 + 1 + 8 + 8 + 8;
        var buffer = ByteBuffer.allocate(length);
        buffer.putInt(length);
        buffer.put(APPEND_ENTRIES_RESPONSE);

        buffer.put(response.success() ? (byte) 1 : (byte) 0);
        buffer.putLong(response.term());

        if (!response.success()) {
            buffer.putLong(response.conflictTerm());
            buffer.putLong(response.conflictIndex());
        }

        return buffer.array();
    }
}
