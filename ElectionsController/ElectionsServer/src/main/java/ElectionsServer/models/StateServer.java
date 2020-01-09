package ElectionsServer.models;


import ElectionsRemoteInterfaceRMI.ElectionsCommitteeInstruction;
import ElectionsRemoteInterfaceRMI.ElectionsCommitteeInstructionRemote;

import java.rmi.RemoteException;

public class StateServer {
    public enum ServerStatus{
        ALIVE,
        DIED
    }

    public enum UpdateStatus{
        PENDING,
        ACK
    }

    private String state;
    private String ip;
    private String port;
    private String GRpcPort;
    private String rmiPort;
    private ServerStatus status;
    private UpdateStatus updateStatus;
    private ElectionsCommitteeInstructionRemote remoteExecutor;

    public StateServer(String state, String ip, String port, String GRpcPort, String rmiPort){
        this.state = state;
        this.ip = ip;
        this.port = port;
        this.GRpcPort = GRpcPort;
        this.rmiPort = rmiPort;
    }

    public String getGRpcPort(){return this.GRpcPort;}
    public void setGRpcPort(String GRpcPort){this.GRpcPort=GRpcPort;}
    public ServerStatus getStatus(){return this.status;}
    public void setStatus(ServerStatus status){this.status=status;}
    public UpdateStatus getUpdateStatus(){return this.updateStatus;}
    public void setUpdateStatus(UpdateStatus updateStatus) {this.updateStatus = updateStatus;}
    public String getRmiPort(){return this.rmiPort;}
    public void setRmiPort(String rmiPort){this.rmiPort = rmiPort;}
    public ElectionsCommitteeInstructionRemote getRemoteExecutor(){return this.remoteExecutor;}
    public void setRemoteExecutor(ElectionsCommitteeInstructionRemote remoteExecutor){
        this.remoteExecutor = remoteExecutor;
    }
    public ElectionsCommitteeInstruction remoteRMI(ElectionsCommitteeInstruction instructionRequest){
        if (this.status!=ServerStatus.ALIVE){
            instructionRequest.setInstructionStatus(ElectionsCommitteeInstruction.ElectionsCommitteeInstructionStatus.ERROR);
            System.out.println("+++++++++++++++++  this.status!=ServerStatus.ALIVE, Status is " + this.status + "++++++++++++++++");
            return instructionRequest;
        }
        try {
            return this.remoteExecutor.executeTask(instructionRequest);
        } catch (RemoteException e) {
            this.status = ServerStatus.DIED;
            instructionRequest.setInstructionStatus(ElectionsCommitteeInstruction.ElectionsCommitteeInstructionStatus.ERROR);
            System.out.println("+++++++++++++++++  RemoteException   ++++++++++++++++");
            e.printStackTrace();
            return instructionRequest;
        }
    }


    public StateServer setState(String state){
        this.state = state;
        return this;
    }

    public StateServer setIp(String ip){
        this.ip = ip;
        return this;
    }

    public StateServer setPort(String port){
        this.port = port;
        return this;
    }

    public String getState(){return this.state;}
    public String getIp(){return this.ip;}
    public String getPort(){return this.port;}

    @Override
    public String toString() {
        return ip + ":" + port;
    }
}