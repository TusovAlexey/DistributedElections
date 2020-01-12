package ElectionsServer.service;

import ElectionsServer.models.StateServer;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListenerAdapter;
import org.apache.zookeeper.CreateMode;

import java.util.concurrent.CountDownLatch;

public class ElectionsLeaderElectionZKService {
    private LeaderSelector leaderSelector;
    private CountDownLatch leaderLatch = new CountDownLatch(1);

    public ElectionsLeaderElectionZKService(CuratorFramework client, String electionPath, StateServer candidate){
        this.leaderSelector = new LeaderSelector(client, electionPath, new LeaderSelectorListenerAdapter() {
            @Override
            public void takeLeadership(CuratorFramework curatorFramework) throws Exception {
                System.out.println(candidate.getHostName() + "selected as new leader for state " + candidate.getState() + " cluster");

                candidate.takeLeadership();
                // Add self node under leader root
                client.create()
                        .creatingParentsIfNeeded()
                        .withMode(CreateMode.EPHEMERAL)
                        .forPath("/" + candidate.getState() + "/leader/" + candidate.getHostName());

                leaderLatch.await();

                client.delete().forPath("/" + candidate.getState() + "/leader/" + candidate.getHostName());
                System.out.println(candidate.getHostName() + "is no longer leader in state " + candidate.getState() + "cluster");
                candidate.stopLeadership();
            }
        });

        leaderSelector.autoRequeue();
        leaderSelector.setId(candidate.getHostName());
    }

    public String getLeader() throws Exception {
        return leaderSelector.getLeader().getId();
    }

    public void start(){
        System.out.println("Starting leader election service");
        leaderSelector.start();
    }

    public void stop(){
        System.out.println("Leader election service stopped");
        leaderSelector.close();
    }

}
