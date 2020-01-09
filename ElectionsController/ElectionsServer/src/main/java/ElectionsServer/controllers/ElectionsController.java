package ElectionsServer.controllers;

import ElectionsServer.models.Voter;
import ElectionsServer.service.ElectionsManager;
import org.springframework.web.bind.annotation.*;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

@RestController
public class ElectionsController {
    // My state information (voters collection, state name, servers list, electors number)
    ElectionsManager electionsManager;

    ElectionsController(){
        // Need to find out how to pass arguments to manager constructor
        try {
            this.electionsManager = new ElectionsManager();
        } catch (RemoteException e) {
            e.printStackTrace();
        }


        Integer rmiPort = Integer.parseInt(System.getenv("DOCKER_RMI_PORT"));
        System.setProperty("java.rmi.server.hostname", System.getenv("DOCKER_HOST_NAME"));

        // Bind to registry for RMI
        try {
            Registry registry = LocateRegistry.createRegistry(rmiPort);
            //ElectionsManager stub = (ElectionsManager) UnicastRemoteObject.exportObject(this.electionsManager, 0);
            registry.rebind("ElectionsRMI", this.electionsManager);
            System.out.println("RMI stub initialized on port " + rmiPort);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/elections")
    List<Voter> all(){return this.electionsManager.getAllVoters();}

    @PostMapping("/elections")
    String newVote(@RequestBody Voter newVote){
        return this.electionsManager.addVoteFromREST(newVote);
    }

    @PutMapping("/elections/{id}")
    String replaceVote(@RequestBody Voter newVote, @PathVariable Integer id){
        return this.electionsManager.addVoteFromREST(newVote);
    }

}
