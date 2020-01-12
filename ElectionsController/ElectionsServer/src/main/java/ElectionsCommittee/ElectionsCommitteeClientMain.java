package ElectionsCommittee;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public class ElectionsCommitteeClientMain {
    public static void main(String args[]){
        System.out.println("Preparing to election process ...");
        ElectionsCommitteeClient client = new ElectionsCommitteeClient();
        interactiveShell(client);
    }

    private static void interactiveShell(ElectionsCommitteeClient client){
        InputStreamReader r = new InputStreamReader(System.in);
        BufferedReader br = new BufferedReader(r);
        String command="";
        while(!command.equals("exit")) {
            System.out.println("Please, Enter The Command:  start | get | stop | exit ");
            try {
                command = br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(command.equals("start")) {
                client.startElections();
                System.out.println("Committee started Elections!");
            }
            if(command.equals("get")) {
                System.out.println("====================================================================");
                System.out.println("========================= Interim Results ==========================");
                System.out.println("====================================================================");
                client.getResults();
                System.out.println("====================================================================");
                System.out.println("====================================================================");
            }
            if(command.equals("stop")) {
                client.stopElections();
                System.out.println("====================================================================");
                System.out.println("====================== FINAL ELECTION RESULTS ======================");
                System.out.println("====================================================================");
                client.getResults();
                System.out.println("====================================================================");
                System.out.println("====================================================================");
            }
        }
        try {
            br.close();
            r.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
