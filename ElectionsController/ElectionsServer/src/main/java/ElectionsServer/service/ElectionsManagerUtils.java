package ElectionsServer.service;

import ElectionsServer.models.Candidate;
import ElectionsServer.models.StateServer;
import ElectionsServer.models.Voter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import javax.swing.plaf.nimbus.State;
import java.io.*;
import java.util.HashMap;

public class ElectionsManagerUtils {
    String hostName;
    String stateName;
    String instance;


    public ElectionsManagerUtils(String hostName, String stateName){
        this.hostName = hostName;
        this.stateName = stateName;
        this.instance = "00";
    }

    public ElectionsManagerUtils(String hostName, String stateName, String instance){
        this(hostName, stateName);
        this.instance = instance;
    }

    public void log(String msg){
        System.out.println("[ " + this.hostName + " inst." + instance + " ]: " + msg);
    }

    public void dbg(String msg) {System.out.println("[ -- DBG - " + this.hostName + " inst." + instance + " -- ]:" + msg);}

    public void parserVoters(HashMap<Integer, Voter> voters){
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
                voters.put(Integer.parseInt(voterCsv[0]), voter);
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

    public Integer parseElectors(){
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
                    return Integer.parseInt(voterCsv[1]);
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

        return 0;
    }

    public void parseServers(HashMap<String, StateServer> FederalServers, HashMap<String, StateServer> clusterServers, StateServer currentServer){
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
                // Servers csv indexes- 0:state_name 1:ip 2:port(REST) 3:gRPC port 4: RMI port 5:ZK server name
                StateServer server = new StateServer(serverCsv[0], serverCsv[1], serverCsv[2], serverCsv[3], serverCsv[4], serverCsv[5]);
                FederalServers.put(server.getHostName(),server);
                if (server.getHostName().equals(this.hostName)){
                    currentServer.setState(server.getState());
                    currentServer.setRESTport(server.getRESTport());
                    currentServer.setGRpcPort(server.getGRpcPort());
                    currentServer.setRmiPort(server.getRmiPort());
                    currentServer.setZooKeeperServer(server.getZooKeeperServer());
                }
                if(server.getState().equals(this.stateName)){
                    clusterServers.put(server.getHostName(), server);
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

    public void parseCandidates(HashMap<Integer, Candidate> candidates){
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
                Candidate candidate = new Candidate(voterCsv[1], Integer.parseInt(voterCsv[0]));
                candidates.put(candidate.getIndex(), candidate);
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



}
