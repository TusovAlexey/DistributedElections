package ElectionsServer;
import ElectionsServer.models.StateServer;
import ElectionsServer.models.Voter;
import ElectionsServer.service.ElectionsManager;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;


@SpringBootApplication(exclude={DataSourceAutoConfiguration.class})
public class Server {

    @Component
    public class StateServerRESTCallback{
        ElectionsManager electionsManager;

        void init(ElectionsManager electionsManager){
            this.electionsManager = electionsManager;
        }

        public String voteHandle(Voter vote) throws Exception {
            return this.electionsManager.proceedVoteFromClient(vote);
        }

        public List<Voter> listVotersHandle(){
            return this.electionsManager.getAllVoters();
        }

    }








    public static void main(String argv[]) {
        String REST0 = System.getenv("DOCKER_ELECTIONS_REST0");
        String REST1 = System.getenv("DOCKER_ELECTIONS_REST1");
        String REST2 = System.getenv("DOCKER_ELECTIONS_REST2");
        String state = System.getenv("DOCKER_ELECTIONS_STATE");
        System.setProperty("java.rmi.server.hostname", "server" + state);
        StateServer stateServer0 = new StateServer(state, REST0, "0");
        StateServer stateServer1 = new StateServer(state, REST1, "1");
        StateServer stateServer2 = new StateServer(state, REST2, "2");

        RunnableServer server0 = new RunnableServer(stateServer0);
        RunnableServer server1 = new RunnableServer(stateServer1);
        RunnableServer server2 = new RunnableServer(stateServer2);
        Thread thread0 = new Thread(server0);
        Thread thread1 = new Thread(server1);
        Thread thread2 = new Thread(server2);
        thread0.start();
        thread1.start();
        thread2.start();
    }


}


class RunnableServer implements Runnable{
    StateServer stateServer;
    ElectionsManager electionsManager;
    private ConfigurableApplicationContext context;
    public RunnableServer(StateServer stateServer){
        this.stateServer = stateServer;
    }

    @Override
    public void run() {
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("server.port", Integer.parseInt(stateServer.getRESTport()));
        this.context = new SpringApplicationBuilder()
                .sources(Server.class).properties(properties).run("");
        Server.StateServerRESTCallback callback = context.getBean(Server.StateServerRESTCallback.class);
        try {
            this.electionsManager = new ElectionsManager(this.stateServer);
        } catch (RemoteException e) {
            e.printStackTrace();
            return;
        }
        callback.init(this.electionsManager);
    }
}


