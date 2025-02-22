package ElectionsCommittee;

import ElectionsRemoteInterfaceRMI.ElectionsCommitteeInstruction;
import ElectionsRemoteInterfaceRMI.ElectionsCommitteeInstructionRemote;
import ElectionsServer.models.Candidate;
import ElectionsServer.models.StateServer;

import java.io.*;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ElectionsCommitteeClient {
    private final static String rootPath = "src" + File.separator + "main" + File.separator + "resources" + File.separator + "csv_files" + File.separator;
    private HashMap<String, StateServer> servers;
    private HashMap<Integer, Candidate> candidates;

    private void parseServers(){
        String line;
        BufferedReader br = null;
        String csvSplitBy = ",";

        try {
            //Resource resource = new ClassPathResource("csv_files" + File.separator +"servers" + File.separator + "servers.csv");
            //InputStream input = resource.getInputStream();
            //InputStreamReader isr = new InputStreamReader(input);
            //br = new BufferedReader(isr);
            br = new BufferedReader(new FileReader( rootPath + "servers" + File.separator + "servers.csv"));
            while ((line = br.readLine()) != null){
                String[] serverCsv = line.split(csvSplitBy);
                // Servers csv indexes- 0:state_name 1:ip(hostname) 2:port(REST) 3:gRPC port 4: RMI port 5:zookeeper 6:instance
                StateServer server = new StateServer(serverCsv[0], serverCsv[1], serverCsv[2], serverCsv[3], serverCsv[4], serverCsv[5], serverCsv[6]);
                this.servers.put(server.getHostName(),server);
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

    private void parseCandidates(){
        String line;
        BufferedReader br = null;
        String csvSplitBy = ",";

        try {
            //Resource resource = new ClassPathResource("csv_files" + File.separator +"candidates" + File.separator + "candidates.csv");
            //InputStream input = resource.getInputStream();
            //InputStreamReader isr = new InputStreamReader(input);
            //br = new BufferedReader(isr);
            br = new BufferedReader(new FileReader( rootPath + "candidates" + File.separator + "candidates.csv"));
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

    public ElectionsCommitteeClient(){
        this.servers = new HashMap<>();
        this.candidates = new HashMap<>();
        this.parseServers();
        this.parseCandidates();
        for(StateServer server : this.servers.values()){
            Integer attempts = 100;
            while (attempts > 0){
                try {
                    System.out.println("Trying to get registry from " + server.getHostName() + ":" + server.getRmiPort());
                    Registry registry = LocateRegistry.getRegistry(server.getIp(), Integer.parseInt(server.getRmiPort()));
                    System.out.println("Looking for Election RMI in registry");
                    ElectionsCommitteeInstructionRemote remoteExec = (ElectionsCommitteeInstructionRemote)registry
                            .lookup(server.getHostName());
                    server.setRemoteExecutor(remoteExec);
                    server.setStatus(StateServer.ServerStatus.ALIVE);
                    System.out.println( "+ " + server.getHostName() + " status is " + server.getStatus());
                    break;
                } catch (RemoteException | NotBoundException e) {
                    e.printStackTrace();
                    --attempts;
                    if (attempts==0){
                        server.setStatus(StateServer.ServerStatus.DIED);
                    }
                    try {
                        TimeUnit.SECONDS.sleep(2);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }

        }
    }

//    void systemUp(){
//        for(StateServer server: this.servers.values()){
//            ElectionsCommitteeInstruction systemUpRequest =
//                    new ElectionsCommitteeInstruction(ElectionsCommitteeInstruction.ElectionCommitteeInstructionType.SYSTEM_UP);
//            ElectionsCommitteeInstruction systemUpResponse = server.remoteRMI(systemUpRequest);
//            if(systemUpResponse.getInstructionStatus()== ElectionsCommitteeInstruction.ElectionsCommitteeInstructionStatus.SUCCESS){
//                System.out.println("Systems in server " + server.getIp() + " is up");
//            }else{
//                System.out.println("Systems in server " + server.getIp() + " is down");
//            }
//        }
//    }

    void startElections(){
        for(StateServer server: this.servers.values()){
            ElectionsCommitteeInstruction systemUpRequest =
                    new ElectionsCommitteeInstruction(ElectionsCommitteeInstruction.ElectionCommitteeInstructionType.START_ELECTIONS);
            ElectionsCommitteeInstruction systemUpResponse = server.remoteRMI(systemUpRequest);
            if(systemUpResponse.getInstructionStatus()== ElectionsCommitteeInstruction.ElectionsCommitteeInstructionStatus.SUCCESS){
                System.out.println("Elections in server " + server.getHostName() + " is started");
            }else{
                if (server.getStatus().equals(StateServer.ServerStatus.ALIVE)){
                    System.out.println("Elections in server " + server.getHostName() + " is started");
                }else {
                    System.out.println("Server " + server.getHostName() + " is down, unable to proceed instruction");
                    System.out.println("Server " + server.getHostName() + " Status is  " + server.getStatus());
                }
            }
        }
    }

    void stopElections(){
        for(StateServer server: this.servers.values()){
            ElectionsCommitteeInstruction systemUpRequest =
                    new ElectionsCommitteeInstruction(ElectionsCommitteeInstruction.ElectionCommitteeInstructionType.STOP_ELECTIONS);
            ElectionsCommitteeInstruction systemUpResponse = server.remoteRMI(systemUpRequest);
            if(systemUpResponse.getInstructionStatus()== ElectionsCommitteeInstruction.ElectionsCommitteeInstructionStatus.SUCCESS){
                System.out.println("Elections in server " + server.getHostName() + " is ended");
            }else{
                if (server.getStatus().equals(StateServer.ServerStatus.ALIVE)){
                    System.out.println("Elections in server " + server.getHostName() + " is ended");
                }else {
                    System.out.println("Server " + server.getHostName() + " is down, unable to proceed instruction");
                }
            }
        }
    }

    HashMap<Integer, Candidate> getStateResults(String state){
        HashMap<Integer, Candidate> result = new HashMap<>();
        for(StateServer server: this.servers.values().stream().filter(s -> s.getState().equals(state)).collect(Collectors.toSet())){
            ElectionsCommitteeInstruction getResultsRequest =
                    new ElectionsCommitteeInstruction(ElectionsCommitteeInstruction.ElectionCommitteeInstructionType.GET_RESULTS);

            ElectionsCommitteeInstruction getResultsResponse = server.remoteRMI(getResultsRequest);

            if(getResultsResponse.getInstructionStatus() == ElectionsCommitteeInstruction.ElectionsCommitteeInstructionStatus.SUCCESS){
                result = new HashMap<>(getResultsResponse.getResults());
                return result;
            }
        }
        System.out.println("Failed to get results from state " + state);
        return result;
    }

    void getResults(){
        for (Candidate candidate: this.candidates.values()){
            candidate.setVotes(0);
            candidate.setElectors(0);
        }
        Set<String> states = this.servers.values().stream().map(StateServer::getState).collect(Collectors.toSet());
        for(String state : states){
            HashMap<Integer, Candidate> stateResults = this.getStateResults(state);
            System.out.print(state + ":");
            for(Candidate candidate : stateResults.values()){
                System.out.print("  " + candidate);
                Candidate localCandidate = this.candidates.get(candidate.getIndex());
                localCandidate.setVotes(localCandidate.getVotes() + candidate.getVotes());
                localCandidate.setElectors(localCandidate.getElectors() + candidate.getElectors());
            }
            System.out.println("");
        }

        System.out.println("Federal results:");
        for (Candidate candidate: this.candidates.values()){
            System.out.println("    " + candidate);
        }
    }


}
