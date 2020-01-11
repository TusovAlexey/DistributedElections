package ElectionsServer.models;
import java.io.Serializable;

public class Candidate implements Serializable {
    //private static final long serialVersionUID = 1L;


    private String name;
    private Integer index;
    private Integer electors;
    private Integer votes;

    public Candidate(String name, Integer index, Integer electors, Integer votes){
        this.name = name;
        this.index = index;
        this.electors = electors;
        this.votes = votes;
    }

    public Candidate(String name, Integer index){
        this(name, index, 0, 0);
    }

    public Candidate(Candidate candidate){
        if (this==candidate){
            return;
        }
        this.name = candidate.getName();
        this.index = candidate.getIndex();
        this.electors = candidate.getElectors();
    }

    public String getName(){return this.name;}
    public void setName(String name){this.name = name;}

    public Integer getIndex(){return this.index;}
    public void setIndex(Integer index){this.index = index;}

    public Integer getElectors(){return this.electors;}
    public void setElectors(Integer electors){this.electors = electors;}

    public Integer getVotes(){return this.votes;}
    public void setVotes(Integer votes){this.votes = votes;}

    @Override
    public String toString() {
        return "Candidate " + this.name + " received " + this.votes + " votes, " +
                "and got " + this.electors + " electors.";
    }
}
