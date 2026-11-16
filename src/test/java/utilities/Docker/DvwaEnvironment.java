package utilities.Docker;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;

/**
 * Starts the intentionally vulnerable DVWA service used only in the project test network.
 */
public class DvwaEnvironment extends DockerContainer {

    /**
     * Creates the dvwa environment component and initializes the dependencies required by its methods.
     *
     * <p>Creates the Docker environment for DVWA.</p>
     */
    public DvwaEnvironment() {
        super("vulnerables/web-dvwa:1.9", 80);
    }

    /**
     * Starts the isolated DVWA application and its required Docker environment.
     */
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