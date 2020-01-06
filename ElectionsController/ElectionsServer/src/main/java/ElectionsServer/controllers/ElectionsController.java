package ElectionsServer.controllers;

import ElectionsServer.models.State;
import ElectionsServer.models.Voter;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ElectionsController {
    // My state information (voters collection, state name, servers list, electors number)
    State myState;

    // Learn how to invoke with different state name using springboot
    ElectionsController(){
        // Need to find out how to pass arguments to state constructors
        this.myState = new State();
    }

    @GetMapping("/elections")
    List<Voter> all(){return this.myState.getAllVoters();}

    @PostMapping("/elections")
    String newVote(@RequestBody Voter newVote){
        // Check if voter registered in current state
        if(!newVote.getState().equals(this.myState.getName())){
            // Voter not registered in current state, redirect voter to his servers
            return myState.suggestAnotherServerByState(newVote.getState());
        }
        return this.myState.addVote(newVote);
    }

    @PutMapping("/elections/{id}")
    String replaceVote(@RequestBody Voter newVote, @PathVariable Integer id){
        // Check if voter registered in current state
        if(!newVote.getState().equals(this.myState.getName())){
            // Voter not registered in current state, redirect voter to his servers
            return myState.suggestAnotherServerByState(newVote.getState());
        }
        return this.myState.addVote(newVote);
    }

}
