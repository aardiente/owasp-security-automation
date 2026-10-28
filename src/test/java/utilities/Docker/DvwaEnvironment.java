package utilities.Docker;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;

public class DvwaEnvironment extends DockerContainer {

    public DvwaEnvironment() {
        super("vulnerables/web-dvwa:latest", 80);
    }

    @Override
    public void startEnvironment() {
        if (container != null && container.isRunning()) return;

        container = new GenericContainer<>(containerName)
                .withNetwork(SHARED_NETWORK)
                .withNetworkAliases(networkAlias)
                .withExposedPorts(exposedPort)
                .waitingFor(Wait.forHttp("/setup.php")
                        .forPort(exposedPort)
                        .forStatusCode(200)
                        .withStartupTimeout(Duration.ofMinutes(2)));

        container.start();
    }
}