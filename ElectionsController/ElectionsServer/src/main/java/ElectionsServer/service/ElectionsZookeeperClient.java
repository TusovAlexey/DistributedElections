package ElectionsServer.service;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

public class ElectionsZookeeperClient {
    private CuratorFramework client;
    private String serverName;

    public ElectionsZookeeperClient(){
        this.serverName = System.getenv("DOCKER_ZOOKEEPER");
        this.client = null;
        try {
            // these are reasonable arguments for the ExponentialBackoffRetry. The first
            // retry will wait 1 second - the second will wait up to 2 seconds - the
            // third will wait up to 4 seconds.
            ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(1000, 10);
            // The simplest way to get a CuratorFramework instance. This will use default values.
            // The only required arguments are the connection string and the retry policy
            this.client = CuratorFrameworkFactory.newClient(this.serverName, retryPolicy);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        System.out.println("Created Zookeeper client to server: " + this.serverName);
    }
}
