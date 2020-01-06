package ElectionsClient;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class Client {

    public static Integer chooseRandomCandidate(Set<Integer> candidates){
        int size = candidates.size();
        int item = new Random().nextInt(size);
        int i = 0;
        for(Integer obj : candidates){
            if(i == item){
                return obj;
            }
            ++i;
        }

        return i;
    }

    static void generateVotes(HashMap<Integer, Voter> voters, Set<Integer> candidates){
        for(Voter voter : voters.values()){
            voter.setVote(chooseRandomCandidate(candidates));
        }
    }

    public static void main(String[] args) {
        String stateName = System.getenv("DOCKER_ELECTIONS_STATE");
        Integer clientIndex = Integer.parseInt(System.getenv("DOCKER_ELECTIONS_CLIENT_INDEX"));
        Integer clients = Integer.parseInt(System.getenv("DOCKER_ELECTIONS_CLIENTS"));
        String serverPort = System.getenv("DOCKER_ELECTIONS_SERVER_REST_PORT");
        String serverIp = System.getenv("DOCKER_ELECTIONS_SERVER_REST_NAME");
        HashMap<Integer, String> candidates = new HashMap<>();
        HashMap<Integer, Voter> voters = new HashMap<>();
        String line;
        BufferedReader br = null;
        String csvSplitBy = ",";

        try {
            Resource resource = new ClassPathResource("csv_files" + File.separator +"candidates" + File.separator + "candidates.csv");
            InputStream input = resource.getInputStream();
            InputStreamReader isr = new InputStreamReader(input);
            br = new BufferedReader(isr);
            while ((line = br.readLine()) != null){
                // Candidates csv indexes- 0:id, 1:name
                String[] voterCsv = line.split(csvSplitBy);
                candidates.put(Integer.parseInt(voterCsv[0]), voterCsv[1]);
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



        try {
            Resource resource = new ClassPathResource("csv_files" + File.separator +"voters" + File.separator + stateName + ".csv");
            InputStream input = resource.getInputStream();
            InputStreamReader isr = new InputStreamReader(input);
            br = new BufferedReader(isr);
            //br = new BufferedReader(new FileReader(pathPrefix + "voters" + File.separator + this.name + ".csv"));
            while ((line = br.readLine()) != null){
                // Vote csv indexes- 0:id, 1:state, 2:vote
                String[] voterCsv = line.split(csvSplitBy);
                if(Integer.parseInt(voterCsv[0])%clients == clientIndex){
                    Voter voter = new Voter(Integer.parseInt(voterCsv[0]), voterCsv[1], Integer.parseInt(voterCsv[2]));
                    voters.put(Integer.parseInt(voterCsv[0]), voter);
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

        while (true){
            try {
                TimeUnit.SECONDS.sleep(20);
            }catch (InterruptedException ignored){}

            generateVotes(voters, candidates.keySet());
            for(Voter voter : voters.values()){
                Integer attempt = 30;
                try {
                    TimeUnit.SECONDS.sleep(2);
                }catch (InterruptedException ignored){}


                RestTemplate restTemplate = new RestTemplate();
                String paramrRequest = "http://" + serverIp + ":" + serverPort + "/elections";
                String response = "";
                while (attempt > 0){
                    HttpEntity<Voter> request = new HttpEntity<>(new Voter(voter));
                    try {
                        response = restTemplate.postForObject(paramrRequest, request, String.class);
                    }catch (ResourceAccessException e){
                        System.out.println("Connection to " + serverIp + ":" + serverPort + " refused, " + attempt + " attempts left.");
                        if (attempt==0){
                            System.out.println("Assumes server down, routing other requests to another server");
                            //TODO - calculations and fun
                            throw e;
                        }

                        --attempt;
                        try {
                            TimeUnit.SECONDS.sleep(10);
                        }catch (InterruptedException ignored){}
                    }
                    System.out.println(response);
                    attempt = 0;
                }



            }
        }





    }
}