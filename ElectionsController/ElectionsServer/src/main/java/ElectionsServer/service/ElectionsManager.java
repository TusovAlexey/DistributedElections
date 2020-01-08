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
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static ElectionsRemoteInterfaceRMI.ElectionsCommitteeInstruction.ElectionCommitteeInstructionType.*;
import static ElectionsServer.models.StateServer.ServerStatus.*;
import static ElectionsServer.models.StateServer.UpdateStatus.*;

public class ElectionsManager implements ElectionsCommitteeInstructionRemote {
    private static final Integer ALIVE_ATTEMPTS = 100;
    private static final Integer UPDATE_ATTEMPTS = 100;
    private static final Integer REFRESH_STATUS_DELAY_SECONDS = 2;
    private Server gRPCServer;
    private HashMap<Integer, Candidate> candidates;
    private final Semaphore mutexGRpcChannel = new Semaphore(1);
    private final Semaphore mutexResultsDatabaseAccess = new Semaphore(1);
    private String stateName;
    private String hostName;
    private String GRpcPort;
    private Integer RmiPort;
    private HashMap<Integer, Voter> voters;
    // Mapping hostname -> StateServer
    private HashMap<String, StateServer> servers;
    // Mapping hostname -> StateServer
    private HashMap<String, StateServer> clusterServers;
    private Integer electors = 0;
    //TODO - add RPC from committee
    private Registry registry;
    private boolean systemUp = false;
    private boolean electionsOpen = true;
    private String leaderName;
    private boolean isLeader;
    private boolean updateCompleted;
    private ElectionsProtoRequest lastUpdateRequest;

    private void parseVotersFile(){
        String line;
        BufferedReader br = null;
        String csvSplitBy = ",";

        try {
            Resource resource = new ClassPathResource("csv_files" + File.separator +"voters" + File.separator + this.stateName + ".csv");
            InputStream input = resource.getInputStream();
            InputStreamReader isr = new InputStreamReader(input);
            br = new BufferedReader(isr);
            //br = new BufferedReader(new FileReader(pathPrefix + "voters" + File.separator + this.name + ".csv"));
            while ((line = br.readLine()) != null){
                // Vote csv indexes- 0:id, 1:state, 2:vote
                String[] voterCsv = line.split(csvSplitBy);
                Voter voter = new Voter(Integer.parseInt(voterCsv[0]), voterCsv[1], Integer.parseInt(voterCsv[2]));
                this.voters.put(Integer.parseInt(voterCsv[0]), voter);
            }
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }finally {
            if (br != null){
                try {
                    br.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }

    private void parseElectorsFile(){
        String line;
        BufferedReader br = null;
        String csvSplitBy = ",";

        try {
            Resource resource = new ClassPathResource("csv_files" + File.separator +"states" + File.separator + "electors.csv");
            InputStream input = resource.getInputStream();
            InputStreamReader isr = new InputStreamReader(input);
            br = new BufferedReader(isr);
            //br = new BufferedReader(new FileReader(pathPrefix + "/states/electors.csv"));
            while ((line = br.readLine()) != null){
                // electors csv indexes- 0:state, 1:electors
                String[] voterCsv = line.split(csvSplitBy);
                if (voterCsv[0].equals(this.stateName)){
                    this.electors = Integer.parseInt(voterCsv[1]);
                    return;
                    //break;
                }
            }
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }finally {
            if (br != null){
                try {
                    br.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }

    private void parseServersFile(){
        String line;
        BufferedReader br = null;
        String csvSplitBy = ",";

        try {
            Resource resource = new ClassPathResource("csv_files" + File.separator +"servers" + File.separator + "servers.csv");
            InputStream input = resource.getInputStream();
            InputStreamReader isr = new InputStreamReader(input);
            br = new BufferedReader(isr);
            //br = new BufferedReader(new FileReader(pathPrefix + "servers/servers.csv"));
            while ((line = br.readLine()) != null){
                String[] serverCsv = line.split(csvSplitBy);
                // Servers csv indexes- 0:state_name 1:ip 2:port(REST) 3:gRPC port 4: RMI port
                StateServer server = new StateServer(serverCsv[0], serverCsv[1], serverCsv[2], serverCsv[3], serverCsv[4]);
                this.servers.put(server.getIp(),server);
                if(server.getIp().equals(this.hostName)){
                    this.GRpcPort = server.getGRpcPort();
                }
                if(server.getState().equals(this.stateName) && !server.getIp().equals(this.hostName)){
                    this.clusterServers.put(server.getIp(), server);
                }
            }
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }finally {
            if (br != null){
                try {
                    br.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }

    private void parseCandidatesFile(){
        String line;
        BufferedReader br = null;
        String csvSplitBy = ",";

        try {
            Resource resource = new ClassPathResource("csv_files" + File.separator +"candidates" + File.separator + "candidates.csv");
            InputStream input = resource.getInputStream();
            InputStreamReader isr = new InputStreamReader(input);
            br = new BufferedReader(isr);

            //File file = resource.getFile();


            //br = new BufferedReader(new FileReader(pathPrefix + "candidates/candidates.csv"));
            while ((line = br.readLine()) != null){
                // Candidates csv indexes- 0:id, 1:name
                String[] voterCsv = line.split(csvSplitBy);
                Candidate candidate = new Candidate(voterCsv[1], Integer.parseInt(voterCsv[0]));
                this.candidates.put(candidate.getIndex(), candidate);
            }
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }finally {
            if (br != null){
                try {
                    br.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }

    private void startGRPCServer(){
        System.out.println("Starting gRPC server on port: " + GRpcPort);
        this.gRPCServer = ServerBuilder.forPort(Integer.parseInt(GRpcPort)).addService(new ElectionsProtoServiceImpl(this)).build();
        try {
            this.gRPCServer.start();
        }catch (Exception e){
            e.printStackTrace();
            return;
        }
        System.out.println("gRPC server started successfully");
    }

    public ElectionsManager(){
        this.stateName = System.getenv("DOCKER_ELECTIONS_STATE");
        this.hostName = System.getenv("DOCKER_ELECTIONS_HOSTNAME");
        this.leaderName = System.getenv("DOCKER_ELECTIONS_LEADER_NAME");
        if(this.leaderName.equals(this.hostName)){
            this.isLeader = true;
        }

        this.voters = new HashMap<>();
        this.servers = new HashMap<>();
        this.candidates = new HashMap<>();
        this.clusterServers = new HashMap<>();
        System.out.println("Starting Elections Manager for State: " + this.stateName);
        System.out.println("Loading database...");
        this.parseVotersFile();
        this.parseElectorsFile();
        this.parseServersFile();
        this.parseCandidatesFile();
        this.startGRPCServer();

        //this.startRmiServer();

        System.out.println("Elections Manager initialization completed!");
        System.out.println("Waiting until all servers are ready, committee should send system up instruction");
        while (!systemUp){
            try {
                TimeUnit.SECONDS.sleep(1);
            }catch (InterruptedException e) {
                e.printStackTrace();
                System.out.println("Initialization failed");
                return;
            }
        }

        System.out.println("Ok! we are ready to go. Just let me know when elections officially starts");
        System.out.println("Voter can't vote yet");
        System.out.println("Committee should send start message");
    }

    public List<Voter> getAllVoters(){
        return new ArrayList<>(this.voters.values());
    }

    public String getName(){return this.stateName;}

    private void voteUpdateLocally(Voter vote){
        try {
            mutexResultsDatabaseAccess.acquire(1);

            Voter voter = voters.get(vote.getId());
            voter.setVote(vote.getVote());
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            mutexResultsDatabaseAccess.release(1);
        }
    }

    public String addVoteFromREST(Voter newVote){
        if (!this.systemUp){
            return "We are sorry, our systems not ready yet.";
        }else if(!this.electionsOpen){
            return "We are sorry, elections not started yet. We are waiting to start command from the committee";
        }

        String answer = "";
        boolean result = false;
        if(!newVote.getState().equals(this.stateName)){
            result = addVoteToAnotherStateFromREST(newVote);
            if(result){
                answer = "Dear citizen (Id " + newVote.getId() + "), your vote registered in servers handling election in your state " + newVote.getState()
                        + ", Thank you for your vote.";
            }
        }else {
            while (result!=true){
                result = this.updateCluster(this.hostName, 1000, newVote);
                if(result){
                    this.voteUpdateLocally(newVote);
                    answer = "Dear citizen (Id " + newVote.getId() + "), your vote registered successfully in " + newVote.getState() + ", " +
                            "Thank you for your vote";
                }
            }
        }
        return answer;
    }

    private void sendAsyncGRpcRequest(StateServer server, ElectionsProtoRequest request){
        ManagedChannel channel = ManagedChannelBuilder.forAddress(server.getIp(), Integer.parseInt(server.getGRpcPort())).usePlaintext().build();
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

    private ElectionsProtoResponse sendSyncGRpcRequest(StateServer server, ElectionsProtoRequest request){
        ManagedChannel channel = ManagedChannelBuilder.forAddress(server.getIp(), Integer.parseInt(server.getGRpcPort())).usePlaintext().build();
        electionsProtoServiceGrpc.electionsProtoServiceBlockingStub stub = electionsProtoServiceGrpc.newBlockingStub(channel);
        //System.out.println("Sending sync gRPC request to: " + server.getIp());
        ElectionsProtoResponse response = stub.electionsProtoHandler(request);
        //System.out.println("Got response from: " + response.getName() + ", type: " + response.getType());
        channel.shutdown();
        return response;
    }

    private void refreshServersStatus(Collection<StateServer> servers){
        for(StateServer server: servers){
            StateServer.ServerStatus status = DIED;
            Integer attempts = ALIVE_ATTEMPTS;
            while (attempts > 0){
                ElectionsProtoRequest request = ElectionsProtoRequest.newBuilder().setId(0).setState("a")
                        .setVote(0).setMagic(0).setType(ElectionsProtoServiceImpl.REQUEST_TYPE_PING)
                        .setName(this.hostName).build();
                ElectionsProtoResponse response = this.sendSyncGRpcRequest(server, request);
                if(response.getType() == ElectionsProtoServiceImpl.RESPONSE_TYPE_PING){
                    status = ALIVE;
                    attempts = 0;
                }
            }
            server.setStatus(status);
        }
    }

    private void refreshClusterServersStatus(){
        this.refreshServersStatus(this.clusterServers.values());
    }

    private void refreshStateServersStatus(String stateName){
        this.refreshServersStatus(this.servers.values().stream().filter(s -> s.getState().equals(stateName)).collect(Collectors.toSet()));
    }

    // Called only by leader while waiting for all cluster servers ACK last update request
    private boolean refreshUpdateStatus(){
        for(StateServer server : this.clusterServers.values()){
            if(server.getState().equals(ALIVE) && server.getUpdateStatus().equals(PENDING)){
                return false;
            }
        }

        return true;
    }

    private void leaderReceivedACK(String from, Integer magic, Voter vote){
        this.clusterServers.get(from).setUpdateStatus(ACK);
    }

    private boolean leaderUpdateCluster(String initiator, Integer magic, Voter vote){
        Integer updateAttempts = UPDATE_ATTEMPTS;
        this.refreshClusterServersStatus();
        // Save request
        this.lastUpdateRequest = ElectionsProtoRequest.newBuilder().setId(vote.getId()).setState(vote.getState())
                .setVote(vote.getVote()).setMagic(magic).setType(0).setName(this.hostName).build();

        // Set class update flag
        this.updateCompleted = false;

        // First send requests for all alive servers
        for(StateServer server: this.clusterServers.values()){
            if(server.getIp().equals(initiator) || server.getStatus().equals(DIED)){
                // Dont sent update request to initiator and to died servers
                server.setUpdateStatus(ACK);
            }else{
                server.setUpdateStatus(PENDING);
                ElectionsProtoRequest request = ElectionsProtoRequest.newBuilder().setId(vote.getId()).setState(vote.getState())
                        .setVote(vote.getVote()).setMagic(magic).setType(0).setName(this.hostName).build();
                // Send async gRPC request for better performances
                this.sendAsyncGRpcRequest(server, request);
            }
        }

        while (!this.updateCompleted && updateAttempts>0){
            try {
                TimeUnit.SECONDS.sleep(REFRESH_STATUS_DELAY_SECONDS);
            }catch (InterruptedException ignored){}
            this.updateCompleted = this.refreshUpdateStatus();
        }

        if(this.updateCompleted){
            return true;
        }

        return false;
    }

    private boolean updateCluster(String name, Integer magic, Voter vote){
        // TODO - change to zookeeper
        if(this.isLeader){
            return leaderUpdateCluster(name, magic, vote);
        }

        ElectionsProtoRequest request = ElectionsProtoRequest.newBuilder().setId(vote.getId()).setState(vote.getState())
                .setVote(vote.getVote()).setMagic(magic).setType(2).setName(name).build();
        // TODO - get leader by zookeeper
        StateServer leader = this.clusterServers.get(leaderName);
        ElectionsProtoResponse response = this.sendSyncGRpcRequest(leader, request);

        // Add node to zookeeper

        // Polling on znodes

        // if all committed return true, else false

        return response.getSucceed();
    }


    // types:
    // 0. Leader sends an update to servers in local cluster
    // 1. Servers in local cluster answer to leader ACK for update request (for asynchronous update use case)
    // 2. Server from local cluster sends update request to leader, asking to update all servers
    //// 3. Leader answer to update initiator about cluster update (success based on succeed field)
    // 4. Server from another state/cluster ask from current state to proceed vote
    //// 5. Server from local state answer to remote server from another cluster (success based on succeed field)
    // magic is optional value, sender can verify that he received response for specific request
    public ElectionsProtoResponse electionsManagerProtoHandler(ElectionsProtoRequest request){
        boolean result = false;
        int type = request.getType();
        ElectionsProtoResponse response;

        if(type==0){
            Voter vote = new Voter(request.getId(), request.getState(), request.getVote());
            // Dont update locally yet, wait for ack from zookeeper api
            String leaderName = request.getName();
            // Answer to leader that you received the update
            response = ElectionsProtoResponse.newBuilder()
                    .setId(request.getId()).setState(request.getState()).setVote(request.getVote()).setMagic(request.getMagic())
                    .setSucceed(true).setType(1).setName(this.hostName).build();

            // Polling on z-node in zookeeper
            // if polling true update locally, else nothing
        }else if (type==1){
            // Current is leader, some server sent ACK for last update, register it
            this.leaderReceivedACK(request.getName(), request.getMagic(), new Voter(request.getId(), request.getState(), request.getVote()));
            response = ElectionsProtoResponse.newBuilder()
                    .setId(request.getId()).setState(request.getState()).setVote(request.getVote()).setMagic(request.getMagic())
                    .setSucceed(true).setType(6).setName(this.hostName).build();
        }else if(type==2){
            // This is blocking method
            result = this.leaderUpdateCluster(request.getName(), request.getMagic(), new Voter(request.getId(), request.getState(), request.getVote()));
            // All live servers in cluster sent ACK, can answer to initiator
            response = ElectionsProtoResponse.newBuilder()
                    .setId(request.getId()).setState(request.getState()).setVote(request.getVote()).setMagic(request.getMagic())
                    .setSucceed(result).setType(3).setName(this.hostName).build();
            // Update locally (if not failed)
            if(result){
                this.voteUpdateLocally(new Voter(request.getId(), request.getState(), request.getVote()));
            }
        }else if(type==3){
            // Should'nt be here
        }else if(type==4){
            // Send request (or handle by self if current is leader) to leader for cluster update (blocking method)
            result = this.updateCluster(request.getName(), request.getMagic(), new Voter(request.getId(), request.getState(), request.getVote()));
            // Answer to initiator
            response = ElectionsProtoResponse.newBuilder()
                    .setId(request.getId()).setState(request.getState()).setVote(request.getVote()).setMagic(request.getMagic())
                    .setSucceed(result).setType(5).setName(this.hostName).build();
            if(result){
                // Update locally (even if it's leader, self re-update is accepted)
                this.voteUpdateLocally(new Voter(request.getId(), request.getState(), request.getVote()));
            }
        }else if(type==5){
            // Should'nt be here
        }

         response= ElectionsProtoResponse.newBuilder()//.setId(vote.getId()).setState(vote.getState()).setMagic(magic)
                .setSucceed(result).setType(type).build();
        return response;
    }

    public boolean addVoteToAnotherStateFromREST(Voter vote){
        this.refreshStateServersStatus(vote.getState());
        for(StateServer server: this.servers.values().stream().filter(s -> s.getState().equals(vote.getState())).collect(Collectors.toSet())){
            if(server.getStatus() == ALIVE){
                ElectionsProtoRequest request = ElectionsProtoRequest.newBuilder().setId(vote.getId()).setState(vote.getState())
                        .setVote(vote.getVote()).setMagic(1000).setType(4).setName(this.hostName).build();
                ElectionsProtoResponse response = this.sendSyncGRpcRequest(server, request);
                if(response.getSucceed()){
                    return true;
                }

                // Try another alive server in remote state
            }
        }

        // Unable to add vote remotely
        return false;
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

    // The result if a map from candidate index to number of his electors
    private HashMap<Integer, Candidate> getResults(){
        HashMap<Integer, Candidate> results = new HashMap<>();
        try {
            // Acquire mutex to prevent result change from other contexts
            mutexResultsDatabaseAccess.acquire(1);
            updateElectors();
            results = new HashMap<>(this.candidates);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            mutexResultsDatabaseAccess.release(1);
        }

        return results;
    }

    // Handle function for RPC results request from committee
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


        // types:
        // 0. Leader sends an update to servers in local cluster
        // 1. Servers in local cluster answer to leader ACK for update request
        // 2. Server from local cluster sends update request to leader, asking to update all servers
        // 3. Leader answer to update initiator about cluster update (success based on succeed field)
        // 4. Server from another state/cluster ask from current state to proceed vote
        // 5. Server from local state answer to remote server from another cluster (success based on succeed field)
        // 6. None
        // 7. ping
        // 8. ping-response

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
            }else {
                response = manager.electionsManagerProtoHandler(request);
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
