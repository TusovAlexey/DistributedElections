package ElectionsServer.models;


import java.io.*;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;
//import com.elections.grpc.server.MyGrpcServer;
//import com.elections.grpc.client.MyGrpcClient;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.InputStream;

public class State {
    private final String serverAddress = "172.28.1.1";
    private final Integer port = 8081;
    private static final String pathPrefix = "./resources/csv_files/";
    private HashMap<Integer, String> candidates;
    private final Semaphore mutex = new Semaphore(1);
    // Mapping for all permitted voters in current state
    private HashMap<Integer, Voter> voters;
    // State name
    private String name;
    // Save list of all election servers, including servers from another states
    private List<StateServer> servers;
    // Number of electors from current state
    private Integer electors = 0;
    // Mapping from candidate index to his number of electors in current state
    private HashMap<Integer, Integer> results;
    private boolean isLeader;

    private void parseVotersFile(){
        String line;
        BufferedReader br = null;
        String csvSplitBy = ",";

        try {
            Resource resource = new ClassPathResource("csv_files" + File.separator +"voters" + File.separator + this.name + ".csv");
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
                if (voterCsv[0].equals(this.name)){
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
                // Servers csv indexes- 0:state_name 1:ip 2:port
                StateServer server = new StateServer(serverCsv[0], serverCsv[1], serverCsv[2]);
                this.servers.add(server);
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
                this.candidates.put(Integer.parseInt(voterCsv[0]), voterCsv[1]);
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

    private void start_gRPC_server(){
        // Start gRPC server
        //try {
        //    MyGrpcServer.grpc_init_service(this.port);
        //}catch (IOException | InterruptedException e){
        //    e.printStackTrace();
        //}
    }

    public State(){
        this.name = System.getenv("DOCKER_ELECTIONS_STATE");
        this.voters = new HashMap<>();
        this.servers = new ArrayList<>();
        this.results = new HashMap<>();
        this.candidates = new HashMap<>();
        System.out.println("Starting State " + this.name + " model...");
        System.out.println(" - parsing csv files");
        this.parseVotersFile();
        this.parseElectorsFile();
        this.parseServersFile();
        this.parseCandidatesFile();
        //System.out.println(" - parsing csv files done");
        //System.out.println(" - starting gRPC server...");
        //this.start_gRPC_server();
        //System.out.println(" - gRPC server started, port:" + this.port);
        System.out.println("State model started successfully");
    }

    public List<Voter> getAllVoters(){
        return new ArrayList<>(this.voters.values());
    }

    public String getName(){return this.name;}

    public String addVote(Voter newVote){
        String result = "";
        try {
            // Catch mutex to prevent RPC request concurrent with results change
            mutex.acquire(1);

            if (!this.isLeader){
                // Update leader by gRPC

                // Wait for ACK
            }else{
                // I'm leader, update all other

                // Wait for ACK
            }

            // Update locally
            Voter voter = voters.get(newVote.getId());
            voter.setVote(newVote.getVote());

            result = "Dear citizen (Id number " + newVote.getId() + "), your electoral vote registered" +
                    " in state " + this.name + ". Thank you!";


        }catch (Exception e){
            e.printStackTrace();
        }finally {
            mutex.release(1);
        }

        return result;
    }

    public String suggestAnotherServerByState(String stateName){
        String result = "You accessed to server in " + this.name + " state. " +
                "Please refer to another server in your state for voting,\n" +
                "Vote servers in your state: ";
        for(StateServer server: servers){
            if (server.getState().equals(stateName)){
                result = result.concat(server + ", ");
            }
        }
        result = result.substring(0, result.length()-2);
        result = result.concat(".");
        return result;
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
        for(Integer candidate : this.results.keySet()){
            candidateToVotes.put(candidate, this.getVotesForCandidate(candidate));
        }
        Integer votesForElector = (Integer) (this.getActualNumberOfVotes() / electorsNumber);
        for(Integer candidate : this.results.keySet()){
            while (candidateToVotes.get(candidate) >= votesForElector){
                newResults.put(candidate,newResults.get(candidate) + 1);
                candidateToVotes.put(candidateToVotes.get(candidate), candidateToVotes.get(candidate) - votesForElector);
                --electorsNumber;
            }
        }

        while (electorsNumber > 0){
            // Add last elector to candidate with max left votes after reduction
            Integer maxValue = Collections.max(candidateToVotes.values());
            for (Integer candidate : candidateToVotes.keySet()){
                if(candidateToVotes.get(candidate).equals(maxValue)){
                    newResults.put(candidate, newResults.get(candidate)+1);
                    candidateToVotes.put(candidateToVotes.get(candidate), candidateToVotes.get(candidate) - maxValue);
                    --electorsNumber;
                }
            }
        }

        this.results = newResults;
    }

    // Handle function for RPC results request from committee
    // The result if a map from candidate index to number of his electors
    private HashMap<Integer, Integer> getResults(){
        HashMap<Integer, Integer> results = new HashMap<>();
        try {
            // Acquire mutex to prevent result change from other contexts
            mutex.acquire(1);
            updateElectors();
            results = new HashMap<>(this.results);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            mutex.release(1);
        }
        return results;
    }

}
