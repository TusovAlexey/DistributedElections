package ElectionsCommittee;

//import jline.console.ConsoleReader;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ElectionsCommitteeClientMain {
    public static void main(String args[]){
        System.out.println("Committee client started");
        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Integer attempts = 10;
        ElectionsCommitteeClient client = new ElectionsCommitteeClient();
        client.systemUp();
        client.startElections();

        while (attempts>0){
            client.getResults();
            --attempts;
        }

        client.stopElections();
    }

    //private void interactiveShell(){
    //    ConsoleReader r = null;
    //    try {
    //        r = new ConsoleReader();
    //        while (true)
    //        {
    //            r.println("Good morning");
    //            r.flush();
//
    //            String input = r.readLine("prompt>");
//
    //            if ("clear".equals(input))
    //                r.clearScreen();
    //            else if ("exit".equals(input))
    //                return;
    //            else
    //                System.out.println("You typed '" + input + "'.");
//
    //        }
    //    } catch (IOException e) {
    //        e.printStackTrace();
    //    }
//
//
    //}



}
