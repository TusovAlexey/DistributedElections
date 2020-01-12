package ElectionsServer.controllers;

import ElectionsServer.models.StateServer;
import ElectionsServer.models.Voter;
import ElectionsServer.service.ElectionsManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.util.EventListener;
import java.util.List;

@RestController
public class ElectionsController{// implements ApplicationRunner {
    // My state information (voters collection, state name, servers list, electors number)
    ElectionsManager electionsManager;
    //String stateName = "";
    //String hostName = "";
    //String RESTPort = "";
    //String RMIPort = "";
    //String gRPCPort = "";
    //StateServer stateServer;
    //String instance = "";
    //String zookeeperServer = "zookeeper1";

    @Autowired
    private org.springframework.boot.ApplicationArguments applicationArguments;

    ElectionsController(){
        //System.out.println(applicationArguments.getSourceArgs());
        //this.stateServer = new StateServer(stateName, hostName, RESTPort, gRPCPort, RMIPort, zookeeperServer, instance);

        //try {
        //    this.electionsManager = new ElectionsManager(stateServer);
        //} catch (RemoteException e) {
        //    e.printStackTrace();
        //}


        //Integer rmiPort = Integer.parseInt(System.getenv(RMIPort));
        //if(Integer.parseInt(instance)==0){
        //    System.setProperty("java.rmi.server.hostname", System.getenv("DOCKER_ELECTIONS_HOSTNAME"));
        //}
//
        //// Bind to registry for RMI
        //try {
        //    Registry registry = LocateRegistry.createRegistry(rmiPort);
        //    //ElectionsManager stub = (ElectionsManager) UnicastRemoteObject.exportObject(this.electionsManager, 0);
        //    registry.rebind("ElectionsRMI" + instance , this.electionsManager);
        //    System.out.println("RMI stub initialized on port " + rmiPort);
        //} catch (RemoteException e) {
        //    e.printStackTrace();
        //}

        //electionsManager.syncSystemUp();
        //electionsManager.waitElectionsOpen();
    }

    @GetMapping("/elections")
    List<Voter> all(){return this.electionsManager.getAllVoters();}

    @PostMapping("/elections")
    String newVote(@RequestBody Voter newVote){
        System.out.println(applicationArguments.getSourceArgs());
        try {
            return this.electionsManager.proceedVoteFromClient(newVote);
        }catch (Exception e){
            e.printStackTrace();
            return "";
        }
    }

    @PutMapping("/elections/{id}")
    String replaceVote(@RequestBody Voter newVote, @PathVariable Integer id){
        try {
            return this.electionsManager.proceedVoteFromClient(newVote);
        }catch (Exception e){
            e.printStackTrace();
            return "";
        }
    }


    //@Override
    //public void run(ApplicationArguments applicationArguments) throws Exception{
    //    String[] args = applicationArguments.getSourceArgs();
    //    this.stateName = args[0];
    //    this.RESTPort = args[1];
    //    this.RMIPort = args[2];
    //    this.gRPCPort = args[3];
    //    this.hostName = args[4];
    //    this.instance = args[5];
    //}

}
