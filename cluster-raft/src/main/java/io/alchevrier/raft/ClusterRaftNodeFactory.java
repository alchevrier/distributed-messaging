package io.alchevrier.raft;

import io.alchevrier.message.serializer.ByteBufferDeserializer;
import io.alchevrier.message.serializer.ByteBufferSerializer;
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
import java.util.Properties;
import java.util.stream.Collectors;

public class ClusterRaftNodeFactory {
    public ClusterRaftNode buildFromFile(String filePath) throws Exception {
        var props = new Properties();
        props.load(new FileInputStream(filePath));

        var peers = props.getProperty("peers").split(",");
        var raftPeers = new ArrayList<RaftPeer>(peers.length);
        for (var peer: peers) {
            var peerProps = peer.split(":");
            var peerNodeId = Integer.parseInt(peerProps[0]);
            var peerHost = peerProps[1];
            var peerPort = Integer.parseInt(peerProps[2]);
            raftPeers.add(new RaftPeer(peerNodeId, peerHost, peerPort));
        }

        var nodeProperties = new NodeProperties(
                Integer.parseInt(props.getProperty("node.id")),
                Integer.parseInt(props.getProperty("node.port")),
                raftPeers
        );

        var raftProperties = new RaftProperties(
                new LogProperties(
                        props.getProperty("raft.indexer.path"),
                        Long.parseLong(props.getProperty("raft.indexer.size")),
                        props.getProperty("raft.logger.path")
                ),
                new SchedulerProperties(
                        Long.parseLong(props.getProperty("scheduled.triggerElection.lowerBound")),
                        Long.parseLong(props.getProperty("scheduled.triggerElection.upperBound")),
                        Long.parseLong(props.getProperty("scheduled.heartbeat.fixedRate"))
                )
        );

        var brokerProperties = new BrokerProperties(Integer.parseInt(props.getProperty("broker.port")), Long.parseLong(props.getProperty("broker.timeout")));

        return build(nodeProperties, raftProperties, brokerProperties);
    }

    public ClusterRaftNode build(NodeProperties nodeProperties, RaftProperties raftProperties, BrokerProperties brokerProperties) {

        var electionTimerService = new ScheduledElectionTimerService(
                raftProperties.schedulerProperties().triggerElectionLowerBound(),
                raftProperties.schedulerProperties().triggerElectionUpperBound()
        );

        var heartbeatTimeService = new ScheduledHeartbeatTimerService(raftProperties.schedulerProperties().heartbeatFixedRate());

        var raftNode = new RaftNode(
                nodeProperties.nodeId(),
                nodeProperties.peers(),
                new CompositeRaftLog(
                        new MemoryMappedRaftIndexer(raftProperties.logProperties().indexerPath(), raftProperties.logProperties().indexerSize()),
                        new FileChannelRaftLogger(raftProperties.logProperties().loggerPath())
                ),
                null,
                electionTimerService,
                heartbeatTimeService
        );

        var serializer = new ByteBufferSerializer();
        var deserializer = new ByteBufferDeserializer();

        var clients = nodeProperties.peers().stream()
                .collect(Collectors.toMap(RaftPeer::nodeId, it -> new TcpClient(it.host(), it.port())));

        var raftClient = new RaftTcpClient(clients, serializer, deserializer, raftNode::handleAppendEntriesResponse);
        raftNode.setRaftClient(raftClient);

        var raftHandler = new RaftServerHandler(serializer, deserializer, raftNode);
        var raftServer = new TcpServer(nodeProperties.port(), raftHandler);

        var brokerHandler = new RaftBrokerHandler(deserializer, serializer, raftNode, brokerProperties.timeout());
        var brokerServer = new TcpServer(brokerProperties.port(), brokerHandler);

        return new ClusterRaftNode(
                raftServer,
                brokerServer,
                electionTimerService,
                heartbeatTimeService,
                raftNode
        );
    }
}
