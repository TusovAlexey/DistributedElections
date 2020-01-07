package ElectionsRemoteInterfaceRMI;

import ElectionsServer.models.Candidate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ElectionsCommitteeInstruction implements ElectionsCommitteeTask<ElectionsCommitteeInstruction>, Serializable {
    public enum ElectionCommitteeInstructionType{
        SYSTEM_UP,
        SYSTEM_UP_RESPONSE,
        SYSTEM_DOWN,
        SYSTEM_DOWN_RESPONSE,
        START_ELECTIONS,
        START_ELECTIONS_RESPONSE,
        STOP_ELECTIONS,
        STOP_ELECTIONS_RESPONSE,
        GET_RESULTS,
        GET_RESULTS_RESPONSE
    }

    public enum ElectionsCommitteeInstructionStatus{
        PENDING,
        SUCCESS,
        ERROR
    }

    private ElectionCommitteeInstructionType instructionType;
    private ElectionsCommitteeInstructionStatus instructionStatus;
    private HashMap<Integer, Candidate> candidatesResults;

    public void setInstructionStatus(ElectionsCommitteeInstructionStatus status){this.instructionStatus = status;}
    public void setInstructionType(ElectionCommitteeInstructionType type){this.instructionType = type;}


    public ElectionsCommitteeInstruction(ElectionCommitteeInstructionType instructionType,
                                         ElectionsCommitteeInstructionStatus instructionStatus,
                                         HashMap<Integer, Candidate> candidatesResults){
        this.instructionType = instructionType;
        this.instructionStatus = instructionStatus;
        this.candidatesResults = candidatesResults;
    }

    public ElectionsCommitteeInstruction(ElectionCommitteeInstructionType instructionType,
                                         ElectionsCommitteeInstructionStatus instructionStatus){
        this(instructionType, instructionStatus, new HashMap<>());
    }

    public ElectionsCommitteeInstruction(ElectionCommitteeInstructionType instructionType){
        this(instructionType, ElectionsCommitteeInstructionStatus.PENDING);
    }

    public ElectionCommitteeInstructionType getInstructionType(){return this.instructionType;}
    public ElectionsCommitteeInstructionStatus getInstructionStatus(){return this.instructionStatus;}
    public HashMap<Integer, Candidate> getResults(){return this.candidatesResults;}

    @Override
    public ElectionsCommitteeInstruction execute() {
        this.instructionStatus = ElectionsCommitteeInstructionStatus.ERROR;
        return this;
    }

    @Override
    public ElectionsCommitteeInstruction responseStartElections() {
        this.instructionStatus = ElectionsCommitteeInstructionStatus.SUCCESS;
        this.instructionType = ElectionCommitteeInstructionType.START_ELECTIONS_RESPONSE;
        return this;
    }

    @Override
    public ElectionsCommitteeInstruction responseStopElections() {
        this.instructionStatus = ElectionsCommitteeInstructionStatus.SUCCESS;
        this.instructionType = ElectionCommitteeInstructionType.STOP_ELECTIONS_RESPONSE;
        return this;
    }

    @Override
    public ElectionsCommitteeInstruction responseGetResults(HashMap<Integer, Candidate> results) {
        this.instructionType = ElectionCommitteeInstructionType.GET_RESULTS_RESPONSE;
        this.instructionStatus = ElectionsCommitteeInstructionStatus.SUCCESS;
        this.candidatesResults = results;
        return this;
    }

    @Override
    public ElectionsCommitteeInstruction responseSystemUp() {
        this.instructionStatus = ElectionsCommitteeInstructionStatus.SUCCESS;
        this.instructionType = ElectionCommitteeInstructionType.SYSTEM_UP_RESPONSE;
        return this;
    }

    @Override
    public ElectionsCommitteeInstruction responseSystemDown() {
        this.instructionStatus = ElectionsCommitteeInstructionStatus.SUCCESS;
        this.instructionType = ElectionCommitteeInstructionType.SYSTEM_DOWN_RESPONSE;
        return this;
    }


}
