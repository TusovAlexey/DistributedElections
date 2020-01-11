package ElectionsServer.service;

import ElectionsRemoteInterfaceRMI.ElectionsCommitteeInstruction;
import ElectionsRemoteInterfaceRMI.ElectionsCommitteeInstructionRemote;
import ElectionsRemoteInterfaceRMI.ElectionsCommitteeTask;
import ElectionsServer.gRPC.ElectionsProtoResponse;
import ElectionsServer.gRPC.ElectionsProtoRequest;
import ElectionsServer.gRPC.electionsProtoServiceGrpc;
import ElectionsServer.models.Candidate;
import ElectionsServer.models.StateServer;
import ElectionsServer.models.Voter;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;

import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import org.apache.zookeeper.data.Stat;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static ElectionsRemoteInterfaceRMI.ElectionsCommitteeInstruction.ElectionCommitteeInstructionType.*;
import static ElectionsServer.models.StateServer.ServerStatus.*;
import static ElectionsServer.models.StateServer.UpdateStatus.*;


/**
 * Main assumptions:
 * - Only gRPC server thread can handle vote update
 * - Only gRPC server thread can commit vote
 */
public class ElectionsManager extends UnicastRemoteObject implements ElectionsCommitteeInstructionRemote {
    ElectionsManagerUtils utils;

    // Current state server
    StateServer server;
    Integer electors;
    //boolean systemUp = false;
    boolean systemUp = true;
    //boolean electionsOpen = false;
    boolean electionsOpen = true;
    private ElectionsZookeeperClient zookeeperClient;
    // Mapping hostname -> StateServer
    private HashMap<String, StateServer> clusterServers;
    private Server gRPCServer;
    // Mutex
    private final Semaphore databaseMutex = new Semaphore(1);

    // Data base
    // Mapping hostname -> StateServer
    private HashMap<String, StateServer> federalServers;
    // Mapping index -> Candidate
    private HashMap<Integer, Candidate> candidates;
    // Mapping id -> Voter
    private HashMap<Integer, Voter> voters;


    public ElectionsManager() throws RemoteException {
        super();
        this.utils = new ElectionsManagerUtils();
        utils.log("Starting Elections Manager");
        this.server = new StateServer(System.getenv("DOCKER_ELECTIONS_HOSTNAME"));
        this.clusterServers = new HashMap<>();
        this.federalServers = new HashMap<>();
        this.candidates = new HashMap<>();
        this.voters = new HashMap<>();

        this.utils = new ElectionsManagerUtils();
        utils.log("Loading data base...");
        this.utils.parseServers(this.federalServers, this.clusterServers, this.server);
        this.utils.parserVoters(this.voters);
        this.utils.parseCandidates(this.candidates);
        this.electors = this.utils.parseElectors();
        utils.log("Data base loaded");

        utils.dbg(this.server.toString());
        utils.dbg(this.clusterServers.toString());

        utils.log("Starting gRPC server...");
        this.startGRPCServer();
        utils.log("gRPC server started");

        utils.log("Starting ZooKeeper client service...");
        try {
            this.zookeeperClient = new ElectionsZookeeperClient(this.server, this.federalServers, this.clusterServers);
            this.zookeeperClient.start();
        }catch (Exception e){
            e.printStackTrace();
            utils.log("Zookeeper client service start failed");
        }
        utils.log("Zookeeper client service started");

        utils.log("Elections Manager initialization completed");
    }

    public void syncSystemUp(){
        utils.log("Waiting for system up command from committee");
        while (!systemUp){}
        utils.log("Systems is up, we are ready to go");
    }

    public void waitElectionsOpen(){
        utils.log("Waiting for committee command to start elections, voters can'n vote yet");
        while (!electionsOpen){}
        utils.log("Elections Started!!!");
    }

    private void startGRPCServer(){
        this.gRPCServer = ServerBuilder.forPort(Integer.parseInt(this.server.getGRpcPort())).addService(new ElectionsManager.ElectionsProtoServiceImpl(this)).build();
        try {
            this.gRPCServer.start();
        }catch (Exception e){
            e.printStackTrace();
            return;
        }
        System.out.println("gRPC server started successfully");
    }

    public List<Voter> getAllVoters(){return new ArrayList<>(this.voters.values());}

    private void sendAsyncGRpcRequest(StateServer server, ElectionsProtoRequest request){
        ManagedChannel channel = ManagedChannelBuilder.forAddress(server.getIp(), Integer.parseInt(server.getGRpcPort())).usePlaintext(true).build();
        electionsProtoServiceGrpc.electionsProtoServiceStub stub = electionsProtoServiceGrpc.newStub(channel);
        StreamObserver<ElectionsProtoResponse> responseObserver = new StreamObserver<ElectionsProtoResponse>() {
            @Override
            public void onNext(ElectionsProtoResponse electionsProtoResponse) {
                //System.out.println("On next Async gRPC request");
            }

            @Override
            public void onError(Throwable throwable) {
                //System.out.println("Error in Async gRPC request");
            }

            @Override
            public void onCompleted() {
                //System.out.println("Finished Async gRPC request");
            }
        };

        stub.electionsProtoHandler(request, responseObserver);
    }

    private ElectionsProtoResponse sendSyncGRpcRequest(StateServer server1, ElectionsProtoRequest request){
        ManagedChannel channel = ManagedChannelBuilder.forAddress(server1.getHostName(), Integer.parseInt(server1.getGRpcPort())).usePlaintext(true).build();
        electionsProtoServiceGrpc.electionsProtoServiceBlockingStub stub = electionsProtoServiceGrpc.newBlockingStub(channel);
        //System.out.println("Sending sync gRPC request to: " + server.getIp());
        ElectionsProtoResponse response = stub.electionsProtoHandler(request);
        //System.out.println("Got response from: " + response.getName() + ", type: " + response.getType());
        channel.shutdown();
        return response;
    }

    private boolean requestGRpcServerAddVote(StateServer server, Voter vote){
        Integer type;
        if(server.getState().equals(this.server.getState())){
            type = 2;
        }else {
            type = 4;
        }

        utils.dbg("Sending request to " + server.getHostName() + " gRPC server to add vote: " + vote);
        ElectionsProtoRequest request = ElectionsProtoRequest.newBuilder()
                .setId(vote.getId())
                .setState(vote.getState())
                .setVote(vote.getVote())
                .setType(type)
                .setName(this.server.getHostName())
                .build();
        ElectionsProtoResponse response = sendSyncGRpcRequest(server, request);
        utils.dbg("Received proto response with value " + response.getSucceed());
        return response.getSucceed();
    }

    public String proceedVoteFromClient(Voter vote) throws Exception {
        // This context can be done only by main thread (REST controller)
        if (!this.systemUp){
            return "We are sorry, our systems not ready yet.";
        }else if(!this.electionsOpen){
            return "We are sorry, elections not started yet. We are waiting to start command from the committee";
        }

        String answer = "";
        boolean result = false;
        if(!vote.getState().equals(this.server.getState())){
            result = requestGRpcServerAddVote(federalServers.get(zookeeperClient.getStateLeader(vote.getState())), vote);
            if(result){
                answer = "Dear citizen (Id " + vote.getId() + "), your vote registered in servers handling election in your state " + vote.getState()
                        + ", Thank you for your vote.";
            }
        }else {
            while (result!=true){
                result = this.requestGRpcServerAddVote(clusterServers.get(zookeeperClient.getLeader()),vote);
                if(result){
                    answer = "Dear citizen (Id " + vote.getId() + "), your vote registered successfully in " + vote.getState() + ", " +
                            "Thank you for your vote";
                }
            }
        }
        return answer;

    }

    private boolean electionsAtomicBroadcast(String initiatorName, Voter vote){
        // This context can be done only by gRPC server thread
        boolean result = false;
        utils.dbg("Registering for atomic broadcast in Zk for vote: " + vote + ", from " + initiatorName);
        result = zookeeperClient.atomicBroadcast(initiatorName);
        utils.dbg("Returned from atomic broadcast with result " + result);
        if(result){
            try {
                databaseMutex.acquire(1);

                voters.get(vote.getId()).setVote(vote.getVote());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }finally {
                databaseMutex.release(1);
            }
        }

        return result;
    }

    private boolean leaderUpdateCluster(String initiatorName, Voter vote) throws Exception {
        // This context can be done only by leader and by his gRPC server thread

        // Send gRPC request type 0 to all alive servers in cluster instead of his self (leader)
        for(String name: zookeeperClient.getLiveServers()){
            if(!name.equals(zookeeperClient.getLeader())){
                StateServer server1 = clusterServers.get(name);
                utils.dbg("Leader("+this.server.getHostName()+") sending update to " + server1 + ", vote: " + vote);
                ElectionsProtoRequest request = ElectionsProtoRequest.newBuilder()
                        .setId(vote.getId())
                        .setState(vote.getState())
                        .setVote(vote.getVote())
                        .setType(0)
                        .setName(initiatorName)
                        .build();
                sendSyncGRpcRequest(server1, request);
                //sendAsyncGRpcRequest(server, request);
            }
        }

        // Verify all live servers in class received the message
        //while(!zookeeperClient.getAtomicBroadcastPendingServers().equals(zookeeperClient.getLiveServers())){
        //
        //}


        // Proceed to atomic broadcast
        return electionsAtomicBroadcast(initiatorName, new Voter(vote.getId(), vote.getState(), vote.getVote()));

    }

    ElectionsProtoResponse gRPCServerProtoHandler(ElectionsProtoRequest request) throws Exception {
        // This context can be done only by gRPC server thread
        // Name field:
        //          - For voter update instructions contains initiators name
        //          - For other, sender name
        // Types:
        // 0. Current may be both, server requesting (by async request) to update new vote in database
        // 1. Current is leader, another server from cluster received the update info and waiting for commit
        // 2. Current is leader, another server from cluster asks to update vote (initiator blocked until completion)
        // 3. (Not in this context)
        // 4. Current is leader, server from another state requests to update vote in local state cluster (remote server blocked)
        // 5. (Not in this context)

        Integer type = request.getType();

        utils.dbg("Proto handler with type: " + type);

        switch (type){
            //case 0: {
            //    // Current may be both, server requesting (by async request) to update new vote in database
            //    ElectionsProtoResponse response;
            //    if(electionsAtomicBroadcast(request.getName(), new Voter(request.getId(), request.getState(), request.getVote()))){
            //        // Response not really relevant because leader wouldn't see it
            //        response = ElectionsProtoResponse.newBuilder().setSucceed(true).build();
            //    }else {
            //        response = ElectionsProtoResponse.newBuilder().setSucceed(false).build();
            //    }
            //    return response;
            //}
            case 1: {
                // Current is leader, another server from cluster received the update info and waiting for commit
                // Not used
            }
            case 2: {
                // Current is leader, another server from cluster asks to update vote (initiator blocked until completion)
                ElectionsProtoResponse response;
                if(leaderUpdateCluster(request.getName(), new Voter(request.getId(), request.getState(), request.getVote()))){
                    // Response not really relevant because leader wouldn't see it
                    response = ElectionsProtoResponse.newBuilder().setSucceed(true).build();
                }else {
                    response = ElectionsProtoResponse.newBuilder().setSucceed(false).build();
                }
                return response;
            }
            case 4: {
                // Current is leader, server from another state requests to update vote in local state cluster (remote server blocked)
                ElectionsProtoResponse response;
                if(leaderUpdateCluster(server.getHostName(), new Voter(request.getId(), request.getState(), request.getVote()))){
                    // Response not really relevant because leader wouldn't see it
                    response = ElectionsProtoResponse.newBuilder().setSucceed(true).build();
                }else {
                    response = ElectionsProtoResponse.newBuilder().setSucceed(false).build();
                }
                return response;
            }
            default: {
                return ElectionsProtoResponse.newBuilder().setSucceed(false).build();
            }

        }
    }

    private Integer getVotesForCandidate(Integer candidate){
        return this.voters.values().stream()
                .map(Voter::getVote)
                .filter(vote -> vote.equals(candidate))
                .collect(Collectors.toList())
                .size();
    }

    private Integer getActualNumberOfVotes(){
        return this.voters.values().stream()
                .map(Voter::getVote)
                .filter(vote -> !vote.equals(0))
                .collect(Collectors.toList())
                .size();
    }

    private void updateElectors(){
        HashMap<Integer, Integer> candidateToVotes = new HashMap<>();
        HashMap<Integer, Integer> newResults = new HashMap<>();
        Integer electorsNumber = this.electors;
        Integer totalVotes = getActualNumberOfVotes();
        Integer voterForElector = (Integer) (totalVotes / electorsNumber);

        for(Candidate candidate : this.candidates.values()){
            Integer candidateVotes = this.getVotesForCandidate(candidate.getIndex());
            candidate.setVotes(candidateVotes);
            while (candidateVotes > voterForElector){
                candidate.setElectors(candidate.getElectors() + 1);
                candidateVotes = candidateVotes - voterForElector;
            }
        }
    }

    private HashMap<Integer, Candidate> getResults(){
        HashMap<Integer, Candidate> results = new HashMap<>();
        try {
            // Acquire mutex to prevent result change from other contexts
            databaseMutex.acquire(1);
            updateElectors();
            results = new HashMap<>(this.candidates);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            databaseMutex.release(1);
        }

        return results;
    }

    @Override
    public <T> T executeTask(ElectionsCommitteeTask<T> task) throws RemoteException {
        ElectionsCommitteeInstruction instruction = (ElectionsCommitteeInstruction)task;
        ElectionsCommitteeInstruction.ElectionCommitteeInstructionType instructionType = instruction.getInstructionType();
        if(instructionType.equals(START_ELECTIONS)){
            this.electionsOpen = true;
            return task.responseStartElections();
        }else if(instructionType.equals(STOP_ELECTIONS)){
            this.electionsOpen = false;
            return task.responseStopElections();
        }else if(instructionType.equals(GET_RESULTS)){
            HashMap<Integer, Candidate> candidateResults = getResults();
            return task.responseGetResults(candidateResults);
        }else if(instructionType.equals(SYSTEM_UP)){
            this.systemUp = true;
            return task.responseSystemUp();
        }

        // Shouldn't be here
        return task.execute();
    }



    public static class ElectionsProtoServiceImpl extends electionsProtoServiceGrpc.electionsProtoServiceImplBase{
        ElectionsManager manager;
        static final int REQUEST_TYPE_PING = 7;
        static final int RESPONSE_TYPE_PING = 8;

        @Override
        public void electionsProtoHandler(ElectionsProtoRequest request, StreamObserver<ElectionsProtoResponse> responseObserver){
            ElectionsProtoResponse response;
            //System.out.println("Got new request from: " + request.getName() + ", type: " + request.getType());
            if(request.getType() == REQUEST_TYPE_PING){
                //System.out.println("Handling ping request from: " + request.getName());
                response = ElectionsProtoResponse.newBuilder()
                        .setId(request.getId())
                        .setState(request.getState())
                        .setVote(request.getVote())
                        .setMagic(request.getMagic())
                        .setSucceed(true)
                        .setType(RESPONSE_TYPE_PING)
                        .build();
            }else if(request.getType() == 0) {
                response = ElectionsProtoResponse.newBuilder().setSucceed(true).build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                manager.electionsAtomicBroadcast(request.getName(), new Voter(request.getId(), request.getState(), request.getVote()));
                return;
            }else{
                try {
                    response = manager.gRPCServerProtoHandler(request);
                }catch (Exception e){
                    System.out.println("Proto handler: exception received - returning false result");
                    e.printStackTrace();
                    response = ElectionsProtoResponse.newBuilder().setSucceed(false).build();
                }
                //System.out.println("Request handled by elections manager, response type: " + response.getType());
            }

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }


        public ElectionsProtoServiceImpl(ElectionsManager manager){
            super();
            this.manager = manager;
        }
    }


}


























