package io.alchevrier.raft;

public class ClusterRaftNodeApplication {
    public void main(String... args) throws Exception {
        new ClusterRaftNodeFactory().buildFromFile(args[0]).start();
    }
}
