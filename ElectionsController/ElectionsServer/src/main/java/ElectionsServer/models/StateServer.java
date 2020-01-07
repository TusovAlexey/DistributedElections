package ElectionsServer.models;


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
    private ServerStatus status;
    private UpdateStatus updateStatus;

    public StateServer(String state, String ip, String port, String GRpcPort){
        this.state = state;
        this.ip = ip;
        this.port = port;
        this.GRpcPort = GRpcPort;
    }

    public String getGRpcPort(){return this.GRpcPort;}
    public void setGRpcPort(String GRpcPort){this.GRpcPort=GRpcPort;}
    public ServerStatus getStatus(){return this.status;}
    public void setStatus(ServerStatus status){this.status=status;}
    public UpdateStatus getUpdateStatus(){return this.updateStatus;}
    public void setUpdateStatus(UpdateStatus updateStatus) {this.updateStatus = updateStatus;}

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