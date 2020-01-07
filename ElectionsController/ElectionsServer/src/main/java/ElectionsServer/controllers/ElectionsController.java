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

    // Learn how to invoke with different state name using springboot
    ElectionsController(){
        // Need to find out how to pass arguments to manager constructor
        this.electionsManager = new ElectionsManager();
        Integer rmiPort = Integer.parseInt(System.getenv("DOCKER_RMI_PORT"));
        System.setProperty("java.rmi.server.hostname", "DOCKER_HOST_NAME");

        // Bind to registry for RMI
        try {
            ElectionsManager stub = (ElectionsManager) UnicastRemoteObject.exportObject(this.electionsManager, 0);
            Registry registry = LocateRegistry.createRegistry(rmiPort);
            registry.rebind("ElectionsRMI", stub);
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
