package utilities.Docker;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Starts the proxy and API service used by the test framework to collect HTTP evidence.
 */
public class OWASPZapEnvironment extends DockerContainer
{
    // Routes directly to logs/zap.log via log4j2.xml
    private static final Logger log = LoggerFactory.getLogger("zap");

    private static final String ZAP_Image = "ghcr.io/zaproxy/zaproxy:stable";
    private static final int ZAP_Internal_Port = 8080;

    /**
     * Creates the owasp zap environment component and initializes the dependencies required by its methods.
     *
     * <p>Creates the Docker environment for the ZAP scanning service.</p>
     */
    public OWASPZapEnvironment()
    {
        super(ZAP_Image, ZAP_Internal_Port);
    }

    /**
     * Starts ZAP in the Docker test environment and prepares its proxy/API endpoints.
     */
    @Override
    public void startEnvironment()
    {
        if(container != null) {
            log.info("OWASP ZAP container environment is already running.");
            return;
        }
        try
        {
            log.info("Starting OWASP ZAP Docker container [{}]...", ZAP_Image);
            container = new GenericContainer<>(containerName)
                    .withNetwork(SHARED_NETWORK)
                    .withNetworkAliases(networkAlias)
                    .withExposedPorts(exposedPort)
                    .withCommand(
                            "zap.sh", "-daemon",
                            "-host", "0.0.0.0",
                            "-port", String.valueOf(exposedPort),
                            "-config", "api.addrs.addr.name=.*",
                            "-config", "api.addrs.addr.regex=true",
                            "-config", "api.disablekey=true"
                    )
                    .waitingFor(
                            Wait.forLogMessage(".*ZAP is now listening.*", 1)
                                    .withStartupTimeout(Duration.ofMinutes(2))
                    );

            container.start();
            log.info("OWASP ZAP successfully started! Proxy address: {}:{}", getHost(), getMappedPort());
        } catch (Exception e)
        {
            log.error("Failed to start OWASP ZAP container: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the network address Selenium uses to proxy requests through ZAP.
     *
     * @return the proxy hostname and port address used by the browser
     */
    public String getProxyAddress()
    {
        if(container == null || !container.isRunning())
        {
            log.error("Attempted to get proxy address, but ZAP container is not running.");
            throw new IllegalStateException("Container: " + containerName + " is not running.");
        }
        return container.getHost() + ":" + container.getMappedPort(exposedPort);
    }

    /**
     * Returns the ZAP API host.
     *
     * @return the host address of the started ZAP API service
     */
    public String getHost()
    {
        return container.getHost();
    }

    /**
     * Returns the mapped ZAP API port.
     *
     * @return the host port mapped to the ZAP API container
     */
    public int getMappedPort()
    {
        return container.getMappedPort(exposedPort);
    }
}