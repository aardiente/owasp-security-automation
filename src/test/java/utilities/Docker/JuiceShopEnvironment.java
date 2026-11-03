package utilities.Docker;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import java.time.Duration;

public class JuiceShopEnvironment extends DockerContainer
{
    public JuiceShopEnvironment()
    {
        super("bkimminich/juice-shop:v20.3.0", 3000);
    }

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

    @Override
    public void stopEnvironment()
    {
        super.stopEnvironment();
    }
}