package utilities.Docker;

import automation.AppConfig;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import java.time.Duration;

/**
 * Manages the Juice Shop Docker container using the project's application settings.
 */
public class JuiceShopEnvironment extends DockerContainer
{
    /**
     * Sets up the container wrapper with configured Juice Shop image and service settings.
     */
    public JuiceShopEnvironment()
    {
        super(AppConfig.juiceShopImage(), AppConfig.juiceShopPort());
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