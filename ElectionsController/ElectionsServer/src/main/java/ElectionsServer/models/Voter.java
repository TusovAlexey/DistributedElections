package ElectionsServer.models;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
@Data
public class Voter {
    private Integer id;
    private String state;
    private Integer vote;

    public Voter(@JsonProperty(value = "Id", required = true) Integer id,
                 @JsonProperty(value = "State", required = true) String state,
                 @JsonProperty(value = "Vote", required = true) Integer vote){
        this.id = id;
        this.state = state;
        this.vote = vote;
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
