package utilities.Docker;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import java.time.Duration;

/**
 * Starts and stops OWASP Juice Shop for isolated test execution.
 */
public class JuiceShopEnvironment extends DockerContainer
{
    /**
     * Creates the juice shop environment component and initializes the dependencies required by its methods.
     *
     * <p>Creates the Docker environment for Juice Shop.</p>
     */
    public JuiceShopEnvironment()
    {
        super("bkimminich/juice-shop:v20.3.0", 3000);
    }

    /**
     * Starts the isolated Juice Shop application container.
     */
    public void startServer()
    {
        if(container == null)
        {
            container = new GenericContainer<>(containerName)
                    .withNetwork(SHARED_NETWORK)
                    .withNetworkAliases(networkAlias)
                    .withExposedPorts(exposedPort)
                    .withLogConsumer(outputFrame -> System.out.print("[JUICE-SHOP] " + outputFrame.getUtf8String()))
                    .waitingFor(Wait.forHttp("/rest/admin/application-version").forStatusCode(200).withStartupTimeout(Duration.ofMinutes(3)));

            container.start();
        }
    }

    /**
     * Stops the Juice Shop application container.
     */
    @Override
    public void stopEnvironment()
    {
        super.stopEnvironment();
    }
}