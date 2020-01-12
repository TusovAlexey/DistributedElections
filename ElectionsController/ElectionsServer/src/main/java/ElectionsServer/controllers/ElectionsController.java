package ElectionsServer.controllers;

import ElectionsServer.Server;
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

    Server.StateServerRESTCallback callback;

    ElectionsController(Server.StateServerRESTCallback callback){
        this.callback = callback;
    }

    @GetMapping("/elections")
    List<Voter> all(){return this.callback.listVotersHandle();}

    @PostMapping("/elections")
    String newVote(@RequestBody Voter newVote){
        try {
            return this.callback.voteHandle(newVote);
        }catch (Exception e){
            e.printStackTrace();
            return "";
        }
    }

    @PutMapping("/elections/{id}")
    String replaceVote(@RequestBody Voter newVote, @PathVariable Integer id){
        try {
            return this.callback.voteHandle(newVote);
        }catch (Exception e){
            e.printStackTrace();
            return "";
        }
    }

}
