package io.alchevrier.raft;

import io.alchevrier.message.raft.AppendRequest;
import io.alchevrier.message.raft.AppendResponse;
import io.alchevrier.message.serializer.ByteBufferDeserializer;
import io.alchevrier.message.serializer.ByteBufferSerializer;
import io.alchevrier.tcpclient.TcpClient;

import java.io.IOException;

class ClusterRaftNodeTestHarness {
    private final ClusterRaftNode clusterRaftNode;
    private final TcpClient brokerClient;
    private final ByteBufferSerializer serializer;
    private final ByteBufferDeserializer deserializer;

    public ClusterRaftNodeTestHarness(int brokerPort, ClusterRaftNode clusterRaftNode) {
        this.clusterRaftNode = clusterRaftNode;
        this.brokerClient = new TcpClient("localhost", brokerPort, 5000);

        this.serializer = new ByteBufferSerializer();
        this.deserializer = new ByteBufferDeserializer();
    }

    public AppendResponse append(AppendRequest appendRequest) throws IOException {
        return this.brokerClient.forwardToServer(appendRequest, serializer::serialize, deserializer::deserializeAppendResponse);
    }

    public RaftNode getRaftNode() {
        return clusterRaftNode.getRaftNode();
    }

    public void start() {
        this.clusterRaftNode.start();
    }

    public void shutdown() throws Exception {
        this.clusterRaftNode.shutdown();
        this.brokerClient.close();
    }
}

