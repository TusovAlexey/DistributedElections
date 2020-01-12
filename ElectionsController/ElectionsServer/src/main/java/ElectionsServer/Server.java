package ElectionsServer;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class Server{
    String stateName = "";
    String hostName = "";
    String RESTPort = "";
    String RMIPort = "";
    String gRPCPort = "";

    public static void main(String argv[]) {
        String stateName = System.getenv("DOCKER_ELECTIONS_STATE");
        String RESTPortInst0 = System.getenv("DOCKER_ELECTIONS_REST_PORT");
        String gRPCPortInst0 = System.getenv("DOCKER_ELECTIONS_GRPC_PORT");
        String RMIPortInst0 = System.getenv("DOCKER_ELECTIONS_RMI_PORT");
        String hostName = System.getenv("DOCKER_ELECTIONS_HOSTNAME");
        String RESTPortInst1 = String.valueOf(Integer.parseInt(RESTPortInst0) + 10);
        String RESTPortInst2 = String.valueOf(Integer.parseInt(RESTPortInst0) + 20);
        String RMIPortInst1 = String.valueOf(Integer.parseInt(RMIPortInst0) + 10);
        String RMIPortInst2 = String.valueOf(Integer.parseInt(RMIPortInst0) + 20);
        String gRPCPortInst1 = String.valueOf(Integer.parseInt(gRPCPortInst0) + 10);
        String gRPCPortInst2 = String.valueOf(Integer.parseInt(gRPCPortInst0) + 20);


        new SpringApplicationBuilder(Server.class).run("alexey");
        //SpringApplication.run(Server.class, argv);
        //new SpringApplicationBuilder(Server.class).properties("server.port=${other.port:" + RESTPortInst0 +"}")
        //        .run(stateName, RESTPortInst0, RMIPortInst0, gRPCPortInst0, hostName, "0");
        //new SpringApplicationBuilder(Server.class).properties("server.port=${other.port:" + RESTPortInst1 +"}")
        //        .run(stateName, RESTPortInst1, RMIPortInst1, gRPCPortInst1, hostName, "1");
        //new SpringApplicationBuilder(Server.class).properties("server.port=${other.port:" + RESTPortInst2 +"}")
        //        .run(stateName, RESTPortInst2, RMIPortInst2, gRPCPortInst2, hostName, "2");
    }




}
