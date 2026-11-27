package utilities.Docker;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

/**
 * Provides shared Docker container settings and startup tools for lab services. Gives related services access to a common network and their connection addresses.
 */
public abstract class DockerContainer
{
    protected static final Network SHARED_NETWORK = Network.newNetwork();

    protected GenericContainer<?> container = null;
    protected String containerName = null;
    protected Integer exposedPort;
    protected String networkAlias;

    /**
     * Stores the Docker image and service port and creates a shared-network alias from the image name.
     *
     * @param name Docker image name, including its configured tag if present
     * @param port service port inside the container
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
     * Returns the configured container-side service port, not the dynamically mapped host port.
     *
     * @return configured container-side service port
     */
    public Integer getExposedPort()
    {
        return exposedPort;
    }
}