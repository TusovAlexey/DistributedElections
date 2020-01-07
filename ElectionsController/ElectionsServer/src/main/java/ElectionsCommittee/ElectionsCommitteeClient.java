package ElectionsCommittee;

import ElectionsRemoteInterfaceRMI.ElectionsCommitteeInstruction;
import ElectionsRemoteInterfaceRMI.ElectionsCommitteeInstructionRemote;
import ElectionsServer.models.Candidate;
import ElectionsServer.models.StateServer;

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
    private HashMap<String, StateServer> servers;
    private HashMap<Integer, Candidate> candidates;

    public ElectionsCommitteeClient(Set<StateServer> servers){
        this.servers = new HashMap<>();
        this.candidates = new HashMap<>();

        for(StateServer server : servers){
            Integer attempts = 100;
            while (attempts > 0){
                try {
                    Registry registry = LocateRegistry.getRegistry(server.getIp(), Integer.parseInt(server.getRmiPort()));
                    ElectionsCommitteeInstructionRemote remoteExec = (ElectionsCommitteeInstructionRemote)registry
                            .lookup("ElectionsRMI");
                    server.setRemoteExecutor(remoteExec);
                    server.setStatus(StateServer.ServerStatus.ALIVE);
                } catch (RemoteException | NotBoundException e) {
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

    void systemUp(){
        for(StateServer server: this.servers.values()){
            ElectionsCommitteeInstruction systemUpRequest = new ElectionsCommitteeInstruction(ElectionsCommitteeInstruction.ElectionCommitteeInstructionType.SYSTEM_UP);
            ElectionsCommitteeInstruction systemUpResponse = server.remoteRMI(systemUpRequest);
            if(systemUpResponse.getInstructionStatus()== ElectionsCommitteeInstruction.ElectionsCommitteeInstructionStatus.SUCCESS){
                System.out.println("Systems in server " + server.getIp() + " is up");
            }else{
                System.out.println("Systems in server " + server.getIp() + " is down");
            }
        }
    }

    void startElections(){
        for(StateServer server: this.servers.values()){
            ElectionsCommitteeInstruction systemUpRequest = new ElectionsCommitteeInstruction(ElectionsCommitteeInstruction.ElectionCommitteeInstructionType.START_ELECTIONS);
            ElectionsCommitteeInstruction systemUpResponse = server.remoteRMI(systemUpRequest);
            if(systemUpResponse.getInstructionStatus()== ElectionsCommitteeInstruction.ElectionsCommitteeInstructionStatus.SUCCESS){
                System.out.println("Elections in server " + server.getIp() + " is started");
            }else{
                if (server.getStatus().equals(StateServer.ServerStatus.ALIVE)){
                    System.out.println("Elections in server " + server.getIp() + " is ended");
                }else {
                    System.out.println("Server " + server.getIp() + " is down, unable to proceed instruction");
                }
            }
        }
    }

    void stopElections(){
        for(StateServer server: this.servers.values()){
            ElectionsCommitteeInstruction systemUpRequest = new ElectionsCommitteeInstruction(ElectionsCommitteeInstruction.ElectionCommitteeInstructionType.STOP_ELECTIONS);
            ElectionsCommitteeInstruction systemUpResponse = server.remoteRMI(systemUpRequest);
            if(systemUpResponse.getInstructionStatus()== ElectionsCommitteeInstruction.ElectionsCommitteeInstructionStatus.SUCCESS){
                System.out.println("Elections in server " + server.getIp() + " is started");
            }else{
                if (server.getStatus().equals(StateServer.ServerStatus.ALIVE)){
                    System.out.println("Elections in server " + server.getIp() + " is ended");
                }else {
                    System.out.println("Server " + server.getIp() + " is down, unable to proceed instruction");
                }
            }
        }
    }

    HashMap<Integer, Candidate> getStateResults(String state){
        HashMap<Integer, Candidate> result = new HashMap<>();
        for(StateServer server: this.servers.values().stream().filter(s -> s.getState().equals(state)).collect(Collectors.toSet())){
            ElectionsCommitteeInstruction getResultsRequest = new ElectionsCommitteeInstruction(ElectionsCommitteeInstruction.ElectionCommitteeInstructionType.GET_RESULTS);
            ElectionsCommitteeInstruction getResultsResponse = server.remoteRMI(getResultsRequest);
            if(getResultsResponse.getInstructionStatus()== ElectionsCommitteeInstruction.ElectionsCommitteeInstructionStatus.SUCCESS){
                System.out.println("Got results from state " + server.getState());
                result = new HashMap<>(getResultsResponse.getResults());
                return result;
            }
        }
        System.out.println("Failed to get results from state " + state);
        return result;
    }

    void getResults(){
        Set<String> states = this.servers.values().stream().map(StateServer::getState).collect(Collectors.toSet());
        for(String state : states){
            HashMap<Integer, Candidate> stateResults = this.getStateResults(state);
            System.out.println("State " + state + " results:");
            for(Candidate candidate : stateResults.values()){
                System.out.println(candidate);
                Candidate localCandidate = this.candidates.get(candidate.getIndex());
                localCandidate.setVotes(localCandidate.getVotes() + candidate.getVotes());
                localCandidate.setElectors(localCandidate.getElectors() + candidate.getElectors());
            }
        }

        System.out
    }


}
