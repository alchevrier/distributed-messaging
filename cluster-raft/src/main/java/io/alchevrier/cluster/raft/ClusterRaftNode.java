package io.alchevrier.cluster.raft;

import io.alchevrier.message.serializer.ByteBufferDeserializer;
import io.alchevrier.message.serializer.ByteBufferSerializer;
import io.alchevrier.raft.RaftNode;
import io.alchevrier.raft.RaftPeer;
import io.alchevrier.raft.election.ScheduledElectionTimerService;
import io.alchevrier.raft.election.ScheduledHeartbeatTimerService;
import io.alchevrier.raft.log.CompositeRaftLog;
import io.alchevrier.raft.log.FileChannelRaftLogger;
import io.alchevrier.raft.log.MemoryMappedRaftIndexer;
import io.alchevrier.raft.transport.RaftBrokerHandler;
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

        var nodeId = Integer.parseInt(props.getProperty("node.id"));
        var raftNode = new RaftNode(
                nodeId,
                raftPeers,
                new CompositeRaftLog(
                        new MemoryMappedRaftIndexer(props.getProperty("raft.indexer.path"), Long.parseLong(props.getProperty("raft.indexer.size"))),
                        new FileChannelRaftLogger(props.getProperty("raft.logger.path"))
                ),
                null,
                new ScheduledElectionTimerService(
                        Long.parseLong(props.getProperty("scheduled.triggerElection.lowerBound")),
                        Long.parseLong(props.getProperty("scheduled.triggerElection.upperBound"))
                ),
                new ScheduledHeartbeatTimerService(Long.parseLong(props.getProperty("scheduled.heartbeat.fixedRate")))
        );

        var raftClient = new RaftTcpClient(clients, serializer, deserializer, raftNode::handleAppendEntriesResponse);
        raftNode.setRaftClient(raftClient);
        raftNode.start();

        var serverHandler = new RaftServerHandler(serializer, deserializer, raftNode);
        var nodePort = Integer.parseInt(props.getProperty("node.port"));
        new TcpServer(nodePort, serverHandler).start();

        var brokerTimeout = Long.parseLong(props.getProperty("broker.timeout"));
        var brokerHandler = new RaftBrokerHandler(deserializer, serializer, raftNode, brokerTimeout);
        var brokerPort = Integer.parseInt(props.getProperty("broker.port"));
        new TcpServer(brokerPort, brokerHandler).start();
    }
}
