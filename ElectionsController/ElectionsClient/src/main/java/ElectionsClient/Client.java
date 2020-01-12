package ElectionsClient;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

class RunnableClient implements Runnable{
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String BLACK_BOLD = "\033[1;30m";  // BLACK
    public static final String RED_BOLD = "\033[1;31m";    // RED
    public static final String GREEN_BOLD = "\033[1;32m";  // GREEN
    public static final String YELLOW_BOLD = "\033[1;33m"; // YELLOW
    public static final String BLUE_BOLD = "\033[1;34m";   // BLUE
    public static final String PURPLE_BOLD = "\033[1;35m"; // PURPLE
    public static final String CYAN_BOLD = "\033[1;36m";   // CYAN
    public static final String WHITE_BOLD = "\033[1;37m";  // WHITE

    // Underline
    public static final String BLACK_UL = "\033[4;30m";  // BLACK
    public static final String RED_UL = "\033[4;31m";    // RED
    public static final String GREEN_UL = "\033[4;32m";  // GREEN
    public static final String YELLOW_UL = "\033[4;33m"; // YELLOW
    public static final String BLUE_UL = "\033[4;34m";   // BLUE
    public static final String PURPLE_UL = "\033[4;35m"; // PURPLE
    public static final String CYAN_UL = "\033[4;36m";   // CYAN
    public static final String WHITE_UL = "\033[4;37m";  // WHITE

    // Regular Colors
    public static final String BLACK = "\033[0;30m";   // BLACK
    public static final String RED = "\033[0;31m";     // RED
    public static final String GREEN = "\033[0;32m";   // GREEN
    public static final String YELLOW = "\033[0;33m";  // YELLOW
    public static final String BLUE = "\033[0;34m";    // BLUE
    public static final String PURPLE = "\033[0;35m";  // PURPLE
    public static final String CYAN = "\033[0;36m";    // CYAN
    public static final String WHITE = "\033[0;37m";   // WHITE

    String ansiState(){
        switch (this.stateName){
            case "CA":{
                return RED_BOLD;
            }
            case "TX":{
                return GREEN_BOLD;
            }
            case "UT":{
                return BLUE_BOLD;
            }
            case "WA":{
                return WHITE_BOLD;
            }
            case "KS":{
                return RED_UL;
            }
            case "WY":{
                return GREEN_UL;
            }
            default:{
                return ANSI_RESET;
            }
        }
    }

    String ansiClient(){
        switch (this.clientIndex){
            case 0:{
                return CYAN_BOLD;
            }
            case 1:{
                return PURPLE_BOLD;
            }
            case 2:{
                return YELLOW_BOLD;
            } default:{
                return ANSI_RESET;
            }
        }
    }

    String stateName;
    Set<String> servers;
    Set<Integer> candidates;
    HashSet<Voter> voters;
    Integer clientIndex;

    RunnableClient(String stateName, Set<String> servers, Set<Integer> candidates, HashSet<Voter> voters, Integer clientIndex){
        this.stateName = stateName;
        this.servers = servers;
        this.candidates = candidates;
        this.voters = voters;
        this.clientIndex = clientIndex;
    }

    void generateVotes(){
        for(Voter voter : this.voters){
            voter.setVote(chooseRandomVote(this.candidates));
        }
    }

    public static Integer chooseRandomVote(Set<Integer> set){
        int size = set.size();
        int item = new Random().nextInt(size);
        int i = 0;
        for(Integer obj : set){
            if(i == item){
                return obj;
            }
            ++i;
        }

        return i;
    }

    public String chooseRandomServer(){
        int item = new Random().nextInt(this.servers.size());
        int i = 0;
        for(String obj : this.servers){
            if(i == item){
                return obj;
            }
            ++i;
        }

        return this.servers.iterator().next();
    }

    private void log(String msg){
        System.out.println("[ " + ansiState() +"State "+ this.stateName + ansiClient() +" - Client " + this.clientIndex + ANSI_RESET + " ]: " + msg);
    }

    @Override
    public void run() {
        generateVotes();

        for(Voter voter : this.voters){
            String attemptServer = chooseRandomServer();
            log("Attempting to register vote for voter " + voter.getId() + " in server " + attemptServer.split(":")[0]);
            Integer attempt = 10;
            RestTemplate restTemplate = new RestTemplate();
            String paramrRequest = "http://" + attemptServer + "/elections";
            String response = "";
            while (true){
                HttpEntity<Voter> request = new HttpEntity<>(new Voter(voter));
                try {
                    --attempt;
                    response = restTemplate.postForObject(paramrRequest, request, String.class);
                }catch (ResourceAccessException e){
                    log("Connection to " + attemptServer + " refused, " + attempt + " attempts left.");
                    if (attempt==0){
                        log("Assumes server down, routing other requests to another server");
                        // Choose another random server
                        break;
                    }else{
                        log("Will try again");

                        try {
                            Integer rand = new Random().nextInt(10);
                            log("Going to sleep for " + rand + " seconds");
                            TimeUnit.SECONDS.sleep(rand);
                        }catch (InterruptedException ignored){}
                    }
                }
                log(response);
                break;
            }
            try {
                Integer rand = new Random().nextInt(10);
                TimeUnit.SECONDS.sleep(15 + rand);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }
}


public class Client{
    String stateName;// = System.getenv("DOCKER_ELECTIONS_STATE");
    Set<String> servers;
    Set<Integer> candidates;
    Set<Voter> voters0;
    Set<Voter> voters1;
    Set<Voter> voters2;

    public Client(String stateName){
        this.stateName = stateName;
        this.servers = new HashSet<>();
        this.voters0 = new HashSet<>();
        this.voters1 = new HashSet<>();
        this.voters2 = new HashSet<>();
        this.candidates = new HashSet<>();
        this.candidates.add(1);
        this.candidates.add(2);
        this.candidates.add(3);

        parseServers();
        parseVoters();
    }

    void parseServers(){
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
                if(serverCsv[0].equals(this.stateName)){
                    String server = serverCsv[1] +":"+ serverCsv[2];
                    //String server = serverCsv[1].concat(":").concat(serverCsv[2]);
                    servers.add(server);
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

    void parseVoters(){
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
                if(voter.getId()%3 == 0){
                    voters0.add(voter);
                }else if(voter.getId()%3 ==1){
                    voters1.add(voter);
                }else {
                    voters2.add(voter);
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

    public static void main(String[] args) {
        String state0 = System.getenv("DOCKER_ELECTIONS_STATE0");
        String state1 = System.getenv("DOCKER_ELECTIONS_STATE1");
        String state2 = System.getenv("DOCKER_ELECTIONS_STATE2");
        String state3 = System.getenv("DOCKER_ELECTIONS_STATE3");
        Client client0 = new Client(state0);
        Client client1 = new Client(state1);
        Client client2 = new Client(state2);
        Client client3 = new Client(state3);
        RunnableClient tunnable00 = new RunnableClient(client0.stateName, new HashSet<>(client0.servers), new HashSet<>(client0.candidates), new HashSet<>(client0.voters0), 0);
        RunnableClient tunnable01 = new RunnableClient(client0.stateName, new HashSet<>(client0.servers), new HashSet<>(client0.candidates), new HashSet<>(client0.voters1), 1);
        RunnableClient tunnable02 = new RunnableClient(client0.stateName, new HashSet<>(client0.servers), new HashSet<>(client0.candidates), new HashSet<>(client0.voters2), 2);

        RunnableClient tunnable10 = new RunnableClient(client1.stateName, new HashSet<>(client1.servers), new HashSet<>(client1.candidates), new HashSet<>(client1.voters0), 0);
        RunnableClient tunnable11 = new RunnableClient(client1.stateName, new HashSet<>(client1.servers), new HashSet<>(client1.candidates), new HashSet<>(client1.voters1), 1);
        RunnableClient tunnable12 = new RunnableClient(client1.stateName, new HashSet<>(client1.servers), new HashSet<>(client1.candidates), new HashSet<>(client1.voters2), 2);

        RunnableClient tunnable20 = new RunnableClient(client2.stateName, new HashSet<>(client2.servers), new HashSet<>(client2.candidates), new HashSet<>(client2.voters0), 0);
        RunnableClient tunnable21 = new RunnableClient(client2.stateName, new HashSet<>(client2.servers), new HashSet<>(client2.candidates), new HashSet<>(client2.voters1), 1);
        RunnableClient tunnable22 = new RunnableClient(client2.stateName, new HashSet<>(client2.servers), new HashSet<>(client2.candidates), new HashSet<>(client2.voters2), 2);

        RunnableClient tunnable30 = new RunnableClient(client3.stateName, new HashSet<>(client3.servers), new HashSet<>(client3.candidates), new HashSet<>(client3.voters0), 0);
        RunnableClient tunnable31 = new RunnableClient(client3.stateName, new HashSet<>(client3.servers), new HashSet<>(client3.candidates), new HashSet<>(client3.voters1), 1);
        RunnableClient tunnable32 = new RunnableClient(client3.stateName, new HashSet<>(client3.servers), new HashSet<>(client3.candidates), new HashSet<>(client3.voters2), 2);

        Thread thread00 = new Thread(tunnable00);
        Thread thread01 = new Thread(tunnable01);
        Thread thread02 = new Thread(tunnable02);

        Thread thread10 = new Thread(tunnable10);
        Thread thread11 = new Thread(tunnable11);
        Thread thread12 = new Thread(tunnable12);

        Thread thread20 = new Thread(tunnable20);
        Thread thread21 = new Thread(tunnable21);
        Thread thread22 = new Thread(tunnable22);

        Thread thread30 = new Thread(tunnable30);
        Thread thread31 = new Thread(tunnable31);
        Thread thread32 = new Thread(tunnable32);

        thread00.start();
        thread01.start();
        thread02.start();

        thread10.start();
        thread11.start();
        thread12.start();


        thread20.start();
        thread21.start();
        thread22.start();


        thread30.start();
        thread31.start();
        thread32.start();
    }

}