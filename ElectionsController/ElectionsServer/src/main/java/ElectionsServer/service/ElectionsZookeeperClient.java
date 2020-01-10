package ElectionsServer.service;

import ElectionsServer.models.StateServer;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.TreeCache;
import org.apache.curator.framework.recipes.cache.TreeCacheEvent;
import org.apache.curator.framework.recipes.cache.TreeCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Zookeeper ZNodes structure:
 *
 *                                         /(root)
 *                                       /
 *                                     /
 *                      --------state0-------------------------------
 *                    /          |       \                \          \
 *                  /           |         \                \          \
 *            live_nodes    all_nodes    leader_selection  update    leader
 *               /  \
 *             /     \
 *        server0  server1 ...
 *
 *
 *
 *
 */




public class ElectionsZookeeperClient {
    private enum UpdateState{
        NOT_ASSIGNED,
        PENDING,
        ERROR,
        SUCCESS
    }

    private UpdateState updateState;
    private CuratorFramework client;
    ElectionsLeaderElectionZKService electionService;
    private StateServer server;
    private HashMap<String, StateServer> clusterServers;
    private HashMap<String, StateServer> allServers;

    TreeCache allNodeListener;
    TreeCache liveNodesListener;
    TreeCache updateListener;

    public ElectionsZookeeperClient(StateServer server,
                                    HashMap<String, StateServer> allServers,
                                    HashMap<String, StateServer> clusterServers){
        this.allServers = allServers;
        this.clusterServers = clusterServers;
        this.server = server;
        this.client = null;
        try {
            // these are reasonable arguments for the ExponentialBackoffRetry. The first
            // retry will wait 1 second - the second will wait up to 2 seconds - the
            // third will wait up to 4 seconds.
            ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(1000, 10);
            // The simplest way to get a CuratorFramework instance. This will use default values.
            // The only required arguments are the connection string and the retry policy
            //this.client = CuratorFrameworkFactory.newClient(this.server.getIp(), retryPolicy);

            this.client = CuratorFrameworkFactory.builder().connectString(this.server.getZooKeeperServer())
                    .sessionTimeoutMs(10000000)
                    .connectionTimeoutMs(10000000)
                    .retryPolicy(new ExponentialBackoffRetry(1000, 20, 50000))
                    .build();


            System.out.println("Created Zookeeper client service to server: " + this.server.getZooKeeperServer());
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        this.electionService = new ElectionsLeaderElectionZKService(client, "/leader_selection", this.server);
    }

    public void start() throws Exception {
        this.client.start();
        System.out.println("Started Zookeeper client connection to server: " + this.server.getIp());

        System.out.println("Assigning listeners");
        addAllNodesListener(client, "/" + this.server.getState() + "/all_nodes");
        addLiveNodesListener(client, "/" + this.server.getState() + "/live_nodes");
        addUpdateListener(client, "/" + this.server.getState() + "/update");
        System.out.println("Listeners started");

        System.out.println("Adding persistent node to all_nodes root");
        try {
            this.client.create()
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.PERSISTENT)
                    .forPath("/" + this.server.getState() + "/all_nodes/" + this.server.getHostName());
        } catch (Exception e) {
            System.out.println("Failed to create persistent node");
            e.printStackTrace();
        }

        try {
            this.client.create()
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.EPHEMERAL)
                    .forPath("/" + this.server.getState() + "/live_nodes/" + this.server.getHostName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.electionService.start();
    }

    public void stop(){
        this.electionService.stop();
        this.client.close();
        this.allNodeListener.close();
        this.liveNodesListener.close();
        this.updateListener.close();
        System.out.println("Zookeeper client connection to server " + this.server.getZooKeeperServer() + " stopped");
    }

    private void createNode(CreateMode mode, String path) throws Exception {
        this.client.create()
                .creatingParentsIfNeeded()
                .withMode(mode)
                .forPath(path);
    }

    private void createNodeWithData(CreateMode mode, String path, String data) throws Exception {
        this.client.create()
                .creatingParentsIfNeeded()
                .withMode(mode)
                .forPath(path, data.getBytes());
    }

    public boolean atomicBroadcast(String initiatorName){
        this.updateState = UpdateState.PENDING;
        try {
            createNodeWithData(CreateMode.EPHEMERAL,
                    "/" + this.server.getState() + "/update/" + this.server.getHostName(),
                    initiatorName);
        } catch (Exception e) {
            this.updateState = UpdateState.NOT_ASSIGNED;
            return false;
        }

        System.out.println("Waiting for all servers to add node");
        try {
            while (this.getAtomicBroadcastPendingServers().size()==this.getLiveServers().size()){}
        }catch (Exception e){

        }

        //while(this.updateState.equals(UpdateState.PENDING)){}
        System.out.println("Update status is " + this.updateState);
        if(this.updateState.equals(UpdateState.SUCCESS)){
            this.updateState = UpdateState.NOT_ASSIGNED;
            try {
                client.delete().guaranteed().forPath("/" + this.server.getState() + "/update/" + this.server.getHostName());

                while (client.getChildren().forPath("/" + this.server.getState() + "/update").size() > 0){}

            }catch (Exception e){
                return false;
            }
            return true;
        }else {
            this.updateState = UpdateState.NOT_ASSIGNED;
            //try {
            //    client.delete().guaranteed().forPath("/" + this.server.getState() + "/update/" + this.server.getHostName());
//
            //    while (client.getChildren().forPath("/" + this.server.getState() + "/update").size() > 0){}
            //}catch (Exception ignored){}
            return false;
        }
    }

    public String getStateLeader(String state) throws Exception {
        List<String> leaders = client.getChildren().forPath("/" + state + "/leader");
        return leaders.get(0);
    }

    public String getLeader() throws Exception {
        return getStateLeader(server.getState());
    }

    // Listener for sub tree /<state>/all_nodes/...
    // Where registered persistent nodes for new added servers
    private void addAllNodesListener(CuratorFramework curatorFramework, String path) throws Exception {
        TreeCache cache = new TreeCache(curatorFramework, path);
        TreeCacheListener listener = new TreeCacheListener() {
            @Override
            public void childEvent(CuratorFramework curatorFramework, TreeCacheEvent treeCacheEvent) throws Exception {
                if(treeCacheEvent.getData()!=null && treeCacheEvent.getData().getData()!=null){
                    System.out.println("All listener " + treeCacheEvent.getType() + "  " + treeCacheEvent.getData().getPath() + "  " + Arrays.toString((byte[])treeCacheEvent.getData().getData()));
                }else if(treeCacheEvent.getData()!=null){
                    System.out.println("All listener " + treeCacheEvent.getType() + "  " + treeCacheEvent.getData().getPath());
                }else {
                    System.out.println("All listener " + treeCacheEvent.getType());
                }

                if(treeCacheEvent.getType().equals(TreeCacheEvent.Type.NODE_ADDED)){
                    String path = treeCacheEvent.getData().getPath();
                    String znode = "";
                    int i = path.lastIndexOf("/");
                    if (i < 0) {
                        znode = path;
                    } else {
                        znode = i + 1 >= path.length() ? "" : path.substring(i + 1);
                    }

                    if(!clusterServers.keySet().contains(znode)){
                        return;
                    }

                    switch (treeCacheEvent.getType()){
                        case NODE_ADDED: {
                            clusterServers.put(znode, allServers.get(znode));
                            break;
                        }
                        default:
                            System.out.println("Unhandled event type: " + treeCacheEvent.getType().name() + "in listener" +
                                    " assigned to all_nodes root");
                    }
                }

            }
        };

        cache.getListenable().addListener(listener);
        cache.start();
        this.allNodeListener = cache;
    }

    // Listener for sub tree /<state>/live_nodes/...
    // Where registered EPHEMERAL nodes for live servers and removed in case of server down
    private void addLiveNodesListener(CuratorFramework curatorFramework, String path) throws Exception {
        TreeCache cache = new TreeCache(curatorFramework, path);
        TreeCacheListener listener = new TreeCacheListener() {
            @Override
            public void childEvent(CuratorFramework curatorFramework, TreeCacheEvent treeCacheEvent) throws Exception {
                if(treeCacheEvent.getData()!=null && treeCacheEvent.getData().getData()!=null){
                    System.out.println("Live listener " + treeCacheEvent.getType() + "  " + treeCacheEvent.getData().getPath() + "  " + Arrays.toString((byte[])treeCacheEvent.getData().getData()));
                }else if(treeCacheEvent.getData()!=null){
                    System.out.println("Live listener " + treeCacheEvent.getType() + "  " + treeCacheEvent.getData().getPath());
                }else {
                    System.out.println("Live listener " + treeCacheEvent.getType());
                }
                if(treeCacheEvent.getType().equals(TreeCacheEvent.Type.NODE_ADDED) || treeCacheEvent.getType().equals(TreeCacheEvent.Type.NODE_REMOVED)){
                    String path = treeCacheEvent.getData().getPath();
                    String znode = "";
                    int i = path.lastIndexOf("/");
                    if (i < 0) {
                        znode = path;
                    } else {
                        znode = i + 1 >= path.length() ? "" : path.substring(i + 1);
                    }

                    if(!clusterServers.keySet().contains(znode)){
                        return;
                    }

                    //String znode = ZKPaths.getNodeFromPath(treeCacheEvent.getData().getPath());
                    switch (treeCacheEvent.getType()){
                        case NODE_ADDED: {
                            clusterServers.get(znode).setStatus(StateServer.ServerStatus.ALIVE);
                            break;
                        }
                        case NODE_REMOVED: {
                            // TODO add use case when last waited server fall
                            clusterServers.get(znode).setStatus(StateServer.ServerStatus.DIED);
                            break;
                        }
                        default:
                            System.out.println("Unhandled event type: " + treeCacheEvent.getType().name() + " in listener" +
                                    " assigned to live_nodes root");
                    }
                }

            }
        };

        cache.getListenable().addListener(listener);
        cache.start();
        this.liveNodesListener = cache;
    }

    private static <T> boolean listEqualsIgnoreOrder(List<T> list1, List<T> list2) {
        return new HashSet<>(list1).equals(new HashSet<>(list2));
    }

    // Listener for sub tree /<state>/update/...
    // Where registered EPHEMERAL nodes for each server (including leader) which got last update info from leader
    // When set of children nodes equal to set of children nodes under "live_nodes" it means that the cluster updated
    // and the current server can commit the update to his database
    private void addUpdateListener(CuratorFramework curatorFramework, String path) throws Exception {
        TreeCache cache = new TreeCache(curatorFramework, path);
        TreeCacheListener listener = new TreeCacheListener() {
            @Override
            public void childEvent(CuratorFramework curatorFramework, TreeCacheEvent treeCacheEvent) throws Exception {
                if(treeCacheEvent.getData()!=null && treeCacheEvent.getData().getData()!=null){
                    System.out.println("Update listener " + treeCacheEvent.getType() + "  " + treeCacheEvent.getData().getPath() + "  " + Arrays.toString((byte[])treeCacheEvent.getData().getData()));
                }else if(treeCacheEvent.getData()!=null){
                    System.out.println("Update listener " + treeCacheEvent.getType() + "  " + treeCacheEvent.getData().getPath());
                }else {
                    System.out.println("Update listener " + treeCacheEvent.getType());
                }
                if(treeCacheEvent.getType().equals(TreeCacheEvent.Type.NODE_ADDED) || treeCacheEvent.getType().equals(TreeCacheEvent.Type.NODE_REMOVED)){
                    String path = treeCacheEvent.getData().getPath();
                    String znode ="";
                    int i = path.lastIndexOf("/");
                    if (i < 0) {
                        znode = path;
                    } else {
                        znode = i + 1 >= path.length() ? "" : path.substring(i + 1);
                    }


                    switch (treeCacheEvent.getType()){
                        case NODE_ADDED: {
                            if(listEqualsIgnoreOrder(curatorFramework.getChildren().forPath("/" + server.getState() + "/update"),
                                    curatorFramework.getChildren().forPath("/" + server.getState() + "/live_nodes"))){
                                // All servers in the cluster know update info, can commit
                                updateState = UpdateState.SUCCESS;
                                return;
                            }

                        }
                        case NODE_REMOVED: {
                            //if(updateState.equals(UpdateState.PENDING)){
                            //    // We are waiting for update completion and some server quit
                            //    if(treeCacheEvent.getData().getData().equals(znode)){
                            //        // Initiator quit - bad news
                            //        updateState = UpdateState.ERROR;
                            //    }else if(clusterServers.get(Arrays.toString(treeCacheEvent.getData().getData())).isLeader()){
                            //        // Leader quit - bad news
                            //        updateState = UpdateState.ERROR;
                            //    }
                            //    // Some other server quit, back to business
                            //}
                            updateState = UpdateState.SUCCESS;
                            return;
                        }
                        default:
                            System.out.println("Unhandled event type: " + treeCacheEvent.getType().name() + "in listener" +
                                    " assigned to live_nodes root");
                    }
                }



            }
        };

        cache.getListenable().addListener(listener);
        cache.start();
        this.updateListener = cache;
    }

    public List<String> getLiveServers() throws Exception {
        return client.getChildren().forPath("/" + this.server.getState() + "/live_nodes");
    }

    public List<String> getAtomicBroadcastPendingServers() throws Exception {
        return client.getChildren().forPath("/" + this.server.getState() + "/update");
    }
}
