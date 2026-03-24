package io.alchevrier.cluster.raft;

import io.alchevrier.message.serializer.ByteBufferDeserializer;
import io.alchevrier.message.serializer.ByteBufferSerializer;
import io.alchevrier.raft.RaftNode;
import io.alchevrier.raft.RaftPeer;
import io.alchevrier.raft.election.ScheduledElectionTimerService;
import io.alchevrier.raft.log.InMemoryRaftLog;
import io.alchevrier.raft.transport.RaftServerHandler;
import io.alchevrier.raft.transport.RaftTcpClient;
import io.alchevrier.tcpclient.TcpClient;
import io.alchevrier.tcpserver.TcpServer;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

public class ClusterRaftNode {
    public static void main(String... args) throws Exception {
        var props = new Properties();
        props.load(new FileInputStream(args[0]));

        var nodeId = Integer.parseInt(props.getProperty("node.id"));
        var nodePort = Integer.parseInt(props.getProperty("node.port"));

        var peers = props.getProperty("peers").split(",");
        var raftPeers = new ArrayList<RaftPeer>(peers.length);
        var clients = new HashMap<Integer, TcpClient>();
        for (var peer: peers) {
            var peerProps = peer.split(":");
            var peerNodeId = Integer.parseInt(peerProps[0]);
            var peerHost = peerProps[1];
            var peerPort = Integer.parseInt(peerProps[2]);
            raftPeers.add(new RaftPeer(peerNodeId, peerHost, peerPort));
            clients.put(peerNodeId, new TcpClient(peerHost, peerPort));
        }

        var serializer = new ByteBufferSerializer();
        var deserializer = new ByteBufferDeserializer();

        var raftNode = new RaftNode(
                nodeId,
                raftPeers,
                new InMemoryRaftLog(),
                null,
                new ScheduledElectionTimerService()
        );

        var raftClient = new RaftTcpClient(clients, serializer, deserializer, raftNode::handleAppendEntriesResponse);

        raftNode.setRaftClient(raftClient);

        var serverHandler = new RaftServerHandler(serializer, deserializer, raftNode);

        new TcpServer(nodePort, serverHandler).start();

        raftNode.startElection();
    }
}
