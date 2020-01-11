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
import org.apache.zookeeper.data.Stat;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Zookeeper ZNodes structure:
 *
 *                                         /(root)
 *                                       /
 *                                     /
 *                      --------state0------------------------------------------
 *                    /          |       \                \          \          \
 *                  /           |         \                \          \          \
 *            live_nodes    all_nodes    leader_selection  update   update_out   leader
 *               /  \
 *             /     \
 *        server0  server1 ...
 *
 *
 *
 *
 */




public class ElectionsZookeeperClient {
    private static final boolean dbgEnable = true;

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

    private void dbg(String msg){
        if(dbgEnable){
            System.out.println("[ -- DBG - "+this.server.getHostName()+" --] " + msg);
        }
    }

    private void createSubRootNodes() throws Exception {
        try {
            this.client.create()
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.PERSISTENT)
                    .forPath("/" + this.server.getState() + "/all_nodes");
            this.client.create()
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.PERSISTENT)
                    .forPath("/" + this.server.getState() + "/live_nodes");
            this.client.create()
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.PERSISTENT)
                    .forPath("/" + this.server.getState() + "/update");
            this.client.create()
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.PERSISTENT)
                    .forPath("/" + this.server.getState() + "/update_out");
        }catch (Exception e){
            dbg("Failed to create sub root nodes");
            throw e;
        }
    }

    public void start() throws Exception {
        this.client.start();
        dbg("Starting Zookeeper client connection");

        dbg("Assigning listeners");
        addAllNodesListener(client, "/" + this.server.getState() + "/all_nodes");
        addLiveNodesListener(client, "/" + this.server.getState() + "/live_nodes");
        addUpdateListener(client, "/" + this.server.getState() + "/update");
        dbg("Listeners started");

        dbg("Adding persistent node to all_nodes root");
        createSubRootNodes();
        this.electionService.start();
        dbg("Zookeeper client connection started");
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

    private void deleteNode(String path) throws Exception {
        try {
            client.delete().guaranteed().forPath(path);
        }catch (Exception e){
            dbg("Failed to delete node " + path);
            throw e;
        }
    }

    private void createNodeWithData(CreateMode mode, String path, String data) throws Exception {
        this.client.create()
                .creatingParentsIfNeeded()
                .withMode(mode)
                .forPath(path, data.getBytes());
    }

    //  "/<state>/<stateSubTreeNode>/<hostname>"
    private String pathToServerName(String stateSubTreeNode){
        return "/" + this.server.getState() + "/" + stateSubTreeNode + "/" + this.server.getHostName();
    }

    //  "/<state>/<stateSubTreeNode>"
    private String pathCreate(String stateSubTreeNode){
        return "/" + this.server.getState() + "/" + stateSubTreeNode;
    }

    //  "/<state>/update"
    private String nodeUpdatePath(){
        return pathCreate("update");
    }

    //  "/<state>/live_nodes"
    private String liveNodesPath(){
        return pathCreate("live_nodes");
    }

    private List<String> getNodes(String rootSubTree){
        try {
            return client.getChildren().forPath("/" + this.server.getState() + "/" + rootSubTree);
        }catch (Exception e){
            dbg("Failed to get nodes from " + rootSubTree);
            return new ArrayList<>();
        }
    }

    private void setNodeData(String path, String data) throws Exception {
        try {
            client.setData().forPath(path, data.getBytes());
        }catch (Exception e){
            dbg("Failed to set data for " + path);
            throw e;
        }
    }

    private String getNodeData(String path) throws Exception {
        byte[] bytes = null;
        try{
            bytes = client.getData().forPath(path);
        }catch (Exception e){
            dbg("Failed to get data from " + path);
            throw e;
        }
        if(bytes!=null){
            return new String(bytes);
        }

        return "";
    }

    public boolean atomicBroadcast(String initiatorName){
        try {
            // Wait until update_out got empty
            dbg("Waiting until update_out got empty");
            while (getNodes("update_out").size()>0){}

            // Update "update" root node data to PENDING
            setNodeData(pathCreate("update_out"), "PENDING");

            // Add myself to as node in "update" subtree
            createNodeWithData(CreateMode.EPHEMERAL, pathToServerName("update"), initiatorName);

            // Wait until "update"s root node data is PENDING
            dbg("Waiting until update root node contains PENDING");
            while (getNodeData(pathToServerName(nodeUpdatePath())).equals("PENDING")){}

            // Remove from "update" and go to "update_out"
            deleteNode(pathToServerName("update"));
            createNode(CreateMode.EPHEMERAL, pathToServerName("update_out"));

            // Wait until "update" have nodes
            dbg("Waiting until all servers exit from update");
            while (getNodes(nodeUpdatePath()).size()>0){}

            // Remove from "update_out" and back to business
            deleteNode(pathToServerName("update_out"));
        } catch (Exception e) {
            dbg("Atomic broadcast failed");
            return false;
        }

        return true;
    }

    public String getStateLeader(String state) throws Exception {
        List<String> leaders = client.getChildren().forPath("/" + state + "/leader");
        return leaders.get(0);
    }

    public String getLeader() throws Exception {
        return getStateLeader(server.getState());
    }

    private void dbgTreeCache(String rootTree, TreeCacheEvent treeCacheEvent){
        if(treeCacheEvent.getData()!=null && treeCacheEvent.getData().getData()!=null){
            dbg(rootTree + "=>" + treeCacheEvent.getType() + "  " + treeCacheEvent.getData().getPath() + "  " + new String(treeCacheEvent.getData().getData()));
        }else if(treeCacheEvent.getData()!=null){
            dbg(rootTree + "=>" + treeCacheEvent.getType() + "  " + treeCacheEvent.getData().getPath());
        }else {
            dbg(rootTree + "=>" + treeCacheEvent.getType());
        }
    }

    // Listener for sub tree /<state>/all_nodes/...
    // Where registered persistent nodes for new added servers
    private void addAllNodesListener(CuratorFramework curatorFramework, String path) throws Exception {
        TreeCache cache = new TreeCache(curatorFramework, path);
        TreeCacheListener listener = new TreeCacheListener() {
            @Override
            public void childEvent(CuratorFramework curatorFramework, TreeCacheEvent treeCacheEvent) throws Exception {
                dbgTreeCache("All nodes listener", treeCacheEvent);
                switch (treeCacheEvent.getType()){
                    case NODE_REMOVED:{
                        dbg(treeCacheEvent.getData().getPath() + " REMOVED");
                        return;
                    }
                    case NODE_UPDATED:{
                        dbg(treeCacheEvent.getData().getPath() + " UPDATED");
                        return;
                    }
                    case NODE_ADDED:{
                        dbg(treeCacheEvent.getData().getPath() + " ADDED");
                        return;
                    }
                    case INITIALIZED:{
                        dbg(treeCacheEvent.getData().getPath() + " INITIALIZED");
                        return;
                    }
                    case CONNECTION_LOST:{
                        dbg(treeCacheEvent.getData().getPath() + " CONNECTION LOST");
                        return;
                    }
                    case CONNECTION_SUSPENDED:{
                        dbg(treeCacheEvent.getData().getPath() + " CONNECTION SUSPEND");
                        return;
                    }
                    case CONNECTION_RECONNECTED:{
                        dbg(treeCacheEvent.getData().getPath() + " CONNECTION RECONNECTED");
                        return;
                    }
                    default:{
                        dbg("All nodes listener received undefined type");
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
                dbgTreeCache("Live nodes listener", treeCacheEvent);
                switch (treeCacheEvent.getType()){
                    case NODE_REMOVED:{
                        dbg(treeCacheEvent.getData().getPath() + " REMOVED");
                        return;
                    }
                    case NODE_UPDATED:{
                        dbg(treeCacheEvent.getData().getPath() + " UPDATED");
                        return;
                    }
                    case NODE_ADDED:{
                        dbg(treeCacheEvent.getData().getPath() + " ADDED");
                        return;
                    }
                    case INITIALIZED:{
                        dbg(treeCacheEvent.getData().getPath() + " INITIALIZED");
                        return;
                    }
                    case CONNECTION_LOST:{
                        dbg(treeCacheEvent.getData().getPath() + " CONNECTION LOST");
                        return;
                    }
                    case CONNECTION_SUSPENDED:{
                        dbg(treeCacheEvent.getData().getPath() + " CONNECTION SUSPEND");
                        return;
                    }
                    case CONNECTION_RECONNECTED:{
                        dbg(treeCacheEvent.getData().getPath() + " CONNECTION RECONNECTED");
                        return;
                    }
                    default:{
                        dbg("All nodes listener received undefined type");
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

    private String getNodeFromPath(String path){
        String znode = "";
        int i = path.lastIndexOf("/");
        if (i < 0) {
            znode = path;
        } else {
            znode = i + 1 >= path.length() ? "" : path.substring(i + 1);
        }
        return znode;
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
                dbgTreeCache("Update nodes listener", treeCacheEvent);
                switch (treeCacheEvent.getType()){
                    case NODE_REMOVED:{
                        dbg(treeCacheEvent.getData().getPath() + " REMOVED");
                        String path = treeCacheEvent.getData().getPath();

                        if (getNodeFromPath(treeCacheEvent.getData().getPath()).equals(getLeader())
                            && getNodeData(pathToServerName(nodeUpdatePath())).equals("PENDING")){
                            dbg("Leader went down before commit, update should be un-validated");
                            setNodeData(pathCreate("update_out"), "ERROR");
                        }
                        return;
                    }
                    case NODE_UPDATED:{
                        dbg(treeCacheEvent.getData().getPath() + " UPDATED");
                        return;
                    }
                    case NODE_ADDED:{
                        dbg(treeCacheEvent.getData().getPath() + " ADDED");
                        if(getNodes(nodeUpdatePath()).size() == getNodes(liveNodesPath()).size()){
                            dbg("All live nodes received update, changing state to COMMIT");
                            setNodeData(pathCreate("update_out"), "COMMIT");
                        }
                        return;
                    }
                    case INITIALIZED:{
                        dbg(treeCacheEvent.getData().getPath() + " INITIALIZED");
                        return;
                    }
                    case CONNECTION_LOST:{
                        dbg(treeCacheEvent.getData().getPath() + " CONNECTION LOST");
                        return;
                    }
                    case CONNECTION_SUSPENDED:{
                        dbg(treeCacheEvent.getData().getPath() + " CONNECTION SUSPEND");
                        return;
                    }
                    case CONNECTION_RECONNECTED:{
                        dbg(treeCacheEvent.getData().getPath() + " CONNECTION RECONNECTED");
                        return;
                    }
                    default:{
                        dbg("All nodes listener received undefined type");
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
