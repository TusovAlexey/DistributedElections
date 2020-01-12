package ElectionsServer.models;


import ElectionsRemoteInterfaceRMI.ElectionsCommitteeInstruction;
import ElectionsRemoteInterfaceRMI.ElectionsCommitteeInstructionRemote;
import org.apache.zookeeper.data.Stat;

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
    private String hostName;
    private String RESTport;
    private String GRpcPort;
    private String RMIport;
    private ServerStatus status;
    private UpdateStatus updateStatus;
    private ElectionsCommitteeInstructionRemote remoteExecutor;
    private boolean isLeader;
    private String zooKeeperServer;
    private String instance;

    public StateServer(String state, String ip, String RESTport, String GRpcPort, String rmiPort, String zooKeeperServer, String instance){
        this.state = state;
        this.hostName = ip + instance;
        this.RESTport = RESTport;
        this.GRpcPort = GRpcPort;
        this.RMIport = rmiPort;
        this.isLeader = false;
        this.zooKeeperServer = zooKeeperServer;
        this.instance = instance;
        this.ip = ip;
    }

    public StateServer(String state, String RESTport, String instance){
        this.state = state;
        this.hostName = "server" + state + instance;
        this.RESTport = RESTport;
        this.GRpcPort = "RESTport";
        this.RMIport = "rmiPort";
        this.isLeader = false;
        this.zooKeeperServer = "zooKeeperServer";
        this.instance = instance;
        this.ip = "";
    }


    public String getInstance(){return this.instance;}
    public String getZooKeeperServer(){return this.zooKeeperServer;}
    public void setZooKeeperServer(String zooKeeperServer){this.zooKeeperServer = zooKeeperServer;}
    public String getGRpcPort(){return this.GRpcPort;}
    public void setGRpcPort(String GRpcPort){this.GRpcPort=GRpcPort;}
    public ServerStatus getStatus(){return this.status;}
    public void setStatus(ServerStatus status){this.status=status;}
    public UpdateStatus getUpdateStatus(){return this.updateStatus;}
    public void setUpdateStatus(UpdateStatus updateStatus) {this.updateStatus = updateStatus;}
    public String getRmiPort(){return this.RMIport;}
    public void setRmiPort(String rmiPort){this.RMIport = rmiPort;}
    public ElectionsCommitteeInstructionRemote getRemoteExecutor(){return this.remoteExecutor;}
    public String getCommonHostname(){return this.ip;}
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
    

    public void takeLeadership(){this.isLeader = true;}

    public void stopLeadership(){this.isLeader = false;}

    public StateServer setState(String state){
        this.state = state;
        return this;
    }

    public StateServer setHostName(String hostName){
        this.hostName = hostName;
        return this;
    }

    public StateServer setPort(String port){
        this.RESTport = port;
        return this;
    }

    public StateServer setRESTport(String port){
        this.RESTport = port;
        return this;
    }

    public String getState(){return this.state;}
    public String getIp(){return this.ip;}
    public String getHostName(){return this.hostName;}
    public String getPort(){return this.RESTport;}
    public String getRESTport(){return this.RESTport;}

    @Override
    public String toString() {
        return hostName;
    }
}