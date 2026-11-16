package utilities.Docker;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

/**
 * Common lifecycle and addressing helpers for containerized lab services.
 */
public abstract class DockerContainer
{
    protected static final Network SHARED_NETWORK = Network.newNetwork();

    protected GenericContainer<?> container = null;
    protected String containerName = null;
    protected Integer exposedPort;
    protected String networkAlias;

    /**
     * Creates the docker container component and initializes the dependencies required by its methods.
     *
     * <p>Configures a reusable Docker container wrapper for an isolated test service.</p>
     *
     * @param name the finding, resource, or application name to resolve
     * @param port the container port exposing the service to the test network
     */
    protected DockerContainer(String name, Integer port)
    {
        containerName = name;
        exposedPort = port;
        networkAlias = name.replaceAll(".*/", "").replaceAll(":.*", "");
    }

    /**
     * Starts the configured Docker test environment and waits for the container to be available.
     */
    public void startEnvironment()
    {
        if(container == null)
        {
            container = new GenericContainer<>(containerName)
                    .withNetwork(SHARED_NETWORK)
                    .withNetworkAliases(networkAlias)
                    .withExposedPorts(exposedPort);
            container.start();
        }
    }

    /**
     * Stops the managed Docker container and releases its resources.
     */
    public void stopEnvironment()
    {
        if(container != null && container.isRunning())
        {
            container.stop();
            container = null;
        }
    }

    /**
     * Returns the externally mapped URL used to reach the container.
     *
     * @return the URL used to access the running container from the host
     */
    public String getURL()
    {
        if(container == null || !container.isRunning())
            throw new IllegalStateException("Container: " + containerName + " is not running.");

        return "http://" + container.getHost() + ":" + container.getMappedPort(exposedPort);
    }

    /**
     * Returns the container's internal Docker-network URL for test and scan traffic.
     *
     * @return the service URL available to other containers on the Docker network
     */
    public String getInternalURL()
    {
        if(container == null || !container.isRunning())
            throw new IllegalStateException("Container: " + containerName + " is not running.");

        return "http://" + networkAlias + ":" + exposedPort;
    }

    /**
     * Returns the host-mapped port for a container service.
     *
     * @return the container port exposing the application service
     */
    public Integer getExposedPort()
    {
        return exposedPort;
    }
}