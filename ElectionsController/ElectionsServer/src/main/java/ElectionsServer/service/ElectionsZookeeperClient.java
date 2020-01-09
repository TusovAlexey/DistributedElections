package ElectionsServer.service;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

/**
 * Zookeeper ZNodes structure:
 *
 *                         States(root)
 *                          /         \
 *                        /
 *                     state0
 *                    /      \
 *                  /         \
 *            live_nodes    all_nodes
 *               /  \
 *             /     \
 *        server0  server1 ...
 *
 *
 *
 *
 */




public class ElectionsZookeeperClient {
    private String stateName;
    private String clientName;
    private CuratorFramework client;
    private String serverName;

    public ElectionsZookeeperClient(String clientName, String stateName){
        this.stateName = stateName;
        this.clientName = clientName;
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

            CuratorFramework client = CuratorFrameworkFactory.builder().connectString(this.serverName)
                    .sessionTimeoutMs(10000)
                    .connectionTimeoutMs(10000)
                    .retryPolicy(new ExponentialBackoffRetry(1000, 30, 5000))
                    .build();


            System.out.println("Created Zookeeper client to server: " + this.serverName);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

    }

    public void start(){
        this.client.start();
        System.out.println("Started Zookeeper client connection to server: " + this.serverName);
    }

    public void stop(){this.client.close();}



}
