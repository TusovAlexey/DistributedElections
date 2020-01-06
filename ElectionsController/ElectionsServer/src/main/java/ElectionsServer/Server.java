package ElectionsServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class Server {
    public static void main(String argv[]) {
        System.getProperties().put( "server.port", Integer.parseInt(System.getenv("DOCKER_ELECTIONS_REST_PORT")));
        //System.getProperties().put( "server.port", 8088);
        SpringApplication.run(Server.class, argv);
    }
}
