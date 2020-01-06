package ElectionsServer.models;


public class StateServer {
    private String state;
    private String ip;
    private String port;

    public StateServer(String state, String ip, String port){
        this.state = state;
        this.ip = ip;
        this.port = port;
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