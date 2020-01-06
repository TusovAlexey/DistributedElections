
package ElectionsClient;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Voter {
    @JsonProperty(value = "Id", required = true)
    private Integer id;
    @JsonProperty(value = "State", required = true)
    private String state;
    @JsonProperty(value = "Vote", required = true)
    private Integer vote;

    public Voter(Integer id, String state, Integer vote){
        this.id = id;
        this.vote = vote;
        this.state = state;
    }

    Voter(Voter voter){
        if(this == voter){
            return;
        }

        this.id = voter.id;
        this.state = voter.state;
        this.vote = voter.vote;
    }

    public Voter setId(Integer id){
        this.id = id;
        return this;
    }

    public Voter setVote(Integer vote){
        this.vote = vote;
        return this;
    }

    public Voter setState(String state){
        this.state = state;
        return this;
    }

    public Integer getId(){return this.id;}
    public Integer getVote(){return this.vote;}
    public String getState(){return this.state;}

}
