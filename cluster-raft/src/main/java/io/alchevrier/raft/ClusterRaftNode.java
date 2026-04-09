package io.alchevrier.raft;

import io.alchevrier.raft.election.ElectionTimerService;
import io.alchevrier.raft.election.HeartbeatTimerService;
import io.alchevrier.tcpserver.TcpServer;

public class ClusterRaftNode {
    private final TcpServer raftServer;
    private final TcpServer brokerServer;
    private final ElectionTimerService electionTimerService;
    private final HeartbeatTimerService heartbeatTimerService;
    private final RaftNode raftNode;

    public ClusterRaftNode(
            TcpServer raftServer,
            TcpServer brokerServer,
            ElectionTimerService electionTimerService,
            HeartbeatTimerService heartbeatTimerService,
            RaftNode raftNode
    ) {
        this.raftServer = raftServer;
        this.brokerServer = brokerServer;
        this.electionTimerService = electionTimerService;
        this.heartbeatTimerService = heartbeatTimerService;
        this.raftNode = raftNode;
    }

    public void start() {
        raftNode.start();
        raftServer.start();
        brokerServer.start();
    }

    public void shutdown() {
        raftServer.close();
        brokerServer.close();
        electionTimerService.stop();
        heartbeatTimerService.stop();
        raftNode.stop();
    }

    public RaftNode getRaftNode() {
        return raftNode;
    }
}
