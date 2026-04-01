package io.alchevrier.message.serializer;

import io.alchevrier.message.*;
import io.alchevrier.message.broker.*;
import io.alchevrier.message.raft.AckMode;
import io.alchevrier.message.raft.AppendEntriesRequest;
import io.alchevrier.message.raft.AppendEntriesResponse;
import io.alchevrier.message.raft.AppendRequest;
import io.alchevrier.message.raft.AppendResponse;
import io.alchevrier.message.raft.RequestVoteRequest;
import io.alchevrier.message.raft.RequestVoteResponse;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static io.alchevrier.message.serializer.MessageType.*;

public class ByteBufferDeserializer {
    public ConsumeRequest deserializeConsumeRequest(byte[] source) {
        var buffer = ByteBuffer.wrap(source);

        var type = buffer.get();
        if (type != CONSUME_REQUEST) {
            throw new IllegalArgumentException("Could not deserialize to ConsumeRequest as it is of type: " + type);
        }

        var topicLength = buffer.getInt();
        var topicBytes = new byte[topicLength];
        buffer.get(topicBytes);

        var partition = buffer.getInt();
        var startingOffset = buffer.getLong();
        var batchSize = buffer.getInt();

        return new ConsumeRequest(new Topic(new String(topicBytes)), partition, startingOffset, batchSize);
    }

    public ConsumeResponse deserializeConsumeResponse(byte[] source) {
        var buffer = ByteBuffer.wrap(source);

        var type = buffer.get();
        if (type != CONSUME_RESPONSE) {
            throw new IllegalArgumentException("Could not deserialize to ConsumeRequest as it is of type: " + type);
        }

        var isSuccess = buffer.get() == 1;
        if (!isSuccess) {
            var errorLength = buffer.getInt();
            var errorBytes = new byte[errorLength];
            buffer.get(errorBytes);

            return new ConsumeResponse(null, null, new String(errorBytes));
        }

        var nextOffset = buffer.getLong();
        var messageCount = buffer.getInt();
        var messages = new ArrayList<Message>(messageCount);

        while (messageCount > 0) {
            var msgLength = buffer.getInt();
            var offset = buffer.getLong();
            var msgBytes = new byte[msgLength];
            buffer.get(msgBytes);

            messages.add(new Message(offset, msgBytes));

            messageCount--;
        }

        return new ConsumeResponse(messages, nextOffset, null);
    }

    public ProduceRequest deserializeProduceRequest(byte[] source) {
        var buffer = ByteBuffer.wrap(source);

        var type = buffer.get();
        if (type != PRODUCE_REQUEST) {
            throw new IllegalArgumentException("Could not deserialize to ProduceRequest as it is of type: " + type);
        }

        var topicLength = buffer.getInt();
        var topicBytes = new byte[topicLength];
        buffer.get(topicBytes);

        var keyLength = buffer.getInt();
        var keyBytes = new byte[keyLength];
        buffer.get(keyBytes);

        var dataLength = buffer.getInt();
        var dataBytes = new byte[dataLength];
        buffer.get(dataBytes);

        return new ProduceRequest(
                new Topic(new String(topicBytes)),
                keyBytes.length == 0 ? null : new String(keyBytes),
                dataBytes
        );
    }

    public ProduceResponse deserializeProduceResponse(byte[] source) {
        var buffer = ByteBuffer.wrap(source);

        var type = buffer.get();
        if (type != PRODUCE_RESPONSE) {
            throw new IllegalArgumentException("Could not deserialize to ProduceRequest as it is of type: " + type);
        }

        if (buffer.get() == 1) {
            var offset = buffer.getLong();
            var partition = buffer.getInt();
            return new ProduceResponse(partition, offset, null);
        }

        var errorLength = buffer.getInt();
        var errorBytes = new byte[errorLength];
        buffer.get(errorBytes);
        var partition = buffer.getInt();
        return new ProduceResponse(partition, null, new String(errorBytes));
    }

    public FlushRequest deserializeFlushRequest(byte[] source) {
        var buffer = ByteBuffer.wrap(source);

        var type = buffer.get();
        if (type != FLUSH_REQUEST) {
            throw new IllegalArgumentException("Could not deserialize to ProduceRequest as it is of type: " + type);
        }

        return new FlushRequest();
    }

    public FlushResponse deserializeFlushResponse(byte[] source) {
        var buffer = ByteBuffer.wrap(source);

        var type = buffer.get();
        if (type != FLUSH_RESPONSE) {
            throw new IllegalArgumentException("Could not deserialize to ProduceRequest as it is of type: " + type);
        }

        if (buffer.get() == 1) {
            return new FlushResponse(null);
        }

        var errorLength = buffer.getInt();
        var errorBytes = new byte[errorLength];
        buffer.get(errorBytes);

        return new FlushResponse(new String(errorBytes));
    }

    public RequestVoteRequest deserializeRequestVoteRequest(byte[] source) {
        var buffer = ByteBuffer.wrap(source);

        var type = buffer.get();
        if (type != REQUEST_VOTE_REQUEST) {
            throw new IllegalArgumentException("Could not deserialize to RequestVoteRequest as it is of type: " + type);
        }

        var candidateTerm = buffer.getLong();
        var candidateId = buffer.getInt();
        var lastLogIndex = buffer.getLong();
        var lastLogTerm = buffer.getLong();

        return new RequestVoteRequest(candidateTerm, candidateId, lastLogIndex, lastLogTerm);
    }

    public RequestVoteResponse deserializeRequestVoteResponse(byte[] source) {
        var buffer = ByteBuffer.wrap(source);

        var type = buffer.get();
        if (type != REQUEST_VOTE_RESPONSE) {
            throw new IllegalArgumentException("Could not deserialize to RequestVoteResponse as it is of type: " + type);
        }

        var voteGranted = buffer.get() == (byte) 1;
        var currentTerm = buffer.getLong();

        return new RequestVoteResponse(voteGranted, currentTerm);
    }

    public AppendEntriesRequest deserializeAppendEntriesRequest(byte[] source) {
        var buffer = ByteBuffer.wrap(source);

        var type = buffer.get();
        if (type != APPEND_ENTRIES_REQUEST) {
            throw new IllegalArgumentException("Could not deserialize to AppendEntriesRequest as it is of type: " + type);
        }

        var term = buffer.getLong();
        var leaderId = buffer.getInt();
        var leaderCommitIndex = buffer.getLong();
        var prevLogIndex = buffer.getLong();
        var prevLogTerm = buffer.getLong();
        var numberOfEntries = buffer.getInt();

        var entries = new byte[numberOfEntries][];
        for (var i = 0; i < numberOfEntries; i++) {
            var lengthOfEntry = buffer.getInt();
            var data = new byte[lengthOfEntry];
            buffer.get(data);
            entries[i] = data;
        }

        return new AppendEntriesRequest(
                term,
                leaderId,
                leaderCommitIndex,
                prevLogIndex,
                prevLogTerm,
                entries
        );
    }

    public AppendEntriesResponse deserializeAppendEntriesResponse(byte[] source) {
        var buffer = ByteBuffer.wrap(source);

        var type = buffer.get();
        if (type != APPEND_ENTRIES_RESPONSE) {
            throw new IllegalArgumentException("Could not deserialize to AppendEntriesResponse as it is of type: " + type);
        }

        var success = buffer.get() == (byte) 1;
        var term = buffer.getLong();

        if (success) {
            return new AppendEntriesResponse(true, term, null, null);
        }

        var conflictTerm = buffer.getLong();
        var conflictIndex = buffer.getLong();

        return new AppendEntriesResponse(false, term, conflictTerm, conflictIndex);
    }

    public AppendRequest deserializeAppendRequest(byte[] source) {
        var buffer = ByteBuffer.wrap(source);

        var type = buffer.get();
        if (type != APPEND_REQUEST) {
            throw new IllegalArgumentException("Could not deserialize to AppendRequest for type: " + type);
        }

        var keyLength = buffer.getInt();
        var keyBuf = new byte[keyLength];
        buffer.get(keyBuf);
        var ackMode = toAckMode(buffer.get());

        var entriesSize = buffer.getInt();
        var entries = new byte[entriesSize][];
        for (var i = 0; i < entriesSize; i++) {
            var entryLength = buffer.getInt();
            var entry = new byte[entryLength];
            buffer.get(entry);
            entries[i] = entry;
        }
        return new AppendRequest(new String(keyBuf), entries, ackMode);
    }

    private AckMode toAckMode(byte ackModeByte) {
        return switch (ackModeByte) {
            case 0x00 -> AckMode.NONE;
            case 0x01 -> AckMode.LEADER;
            case 0x02 -> AckMode.ALL;
            default -> throw new IllegalArgumentException("Can not resolve ackMode for byte: " + ackModeByte);
        };
    }

    public AppendResponse deserializeAppendResponse(byte[] source) {
        var buffer = ByteBuffer.wrap(source);

        var type = buffer.get();
        if (type != APPEND_RESPONSE) {
            throw new IllegalArgumentException("Could not deserialize to AppendResponse for type: " + type);
        }

        var success = buffer.get() == (byte) 1;
        var peerSize = buffer.getInt();
        var peersAck = new HashMap<Integer, Boolean>();
        if (peerSize == 0) {
            return new AppendResponse(success, null);
        }
        while (peerSize > 0) {
            peersAck.put(buffer.getInt(), buffer.get() == (byte) 1);
            peerSize--;
        }
        return new AppendResponse(success, peersAck);
    }
}
