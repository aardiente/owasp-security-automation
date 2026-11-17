package utilities.Docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.ContainerNetwork;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;
import java.util.Map;

/**
 * Starts DVWA in its normal isolated Testcontainers environment or attaches an explicitly named development container.
 *
 * <p>Development reuse is DVWA-specific. The persistent lab container has no published host ports, is verified
 * by an ownership label, and joins the current Testcontainers network so the normal ZAP proxy can still reach it.
 * The network itself and ZAP remain managed by the existing per-suite lifecycle.</p>
 */
public class DvwaEnvironment extends DockerContainer {

    private static final String DEVELOPMENT_LABEL = "org.owasp-security-automation.dvwa.development";
    private final String developmentName;
    private String developmentId;
    private boolean newDevelopmentContainer;

    /**
     * Creates the standard DVWA environment, preserving the existing clean-start behavior.
     */
    public DvwaEnvironment() {
        this(null);
    }

    /**
     * Creates a DVWA environment that can reuse a development container across independent JVM runs.
     *
     * @param developmentName the configured, persistent Docker container name, or null for clean-start mode
     */
    public DvwaEnvironment(String developmentName) {
        super("vulnerables/web-dvwa:1.9", 80);
        if (developmentName != null && !developmentName.matches("[A-Za-z0-9][A-Za-z0-9_.-]*"))
            throw new IllegalArgumentException("Invalid DVWA development container name: " + developmentName);
        this.developmentName = developmentName;
    }

    /**
     * Starts a fresh DVWA container or reuses the explicitly named development container when configured.
     */
    @Override
    public void startEnvironment() {
        if (developmentName != null) {
            startDevelopmentEnvironment();
            return;
        }
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

    /**
     * Attaches to a lab container only when it bears this framework's DVWA development ownership label.
     * A newly created lab container is kept between test runs and connected to the current suite's ZAP network.
     */
    private void startDevelopmentEnvironment() {
        if (developmentId != null) return;

        DockerClient client = DockerClientFactory.instance().client();
        InspectContainerResponse existing = null;
        try {
            existing = client.inspectContainerCmd(developmentName).exec();
        } catch (NotFoundException ignored) {
            // Only a missing container authorizes creating the named development environment.
        }

        if (existing == null) {
            CreateContainerResponse created;
            try {
                created = client.createContainerCmd(containerName)
                        .withName(developmentName)
                        .withLabels(Map.of(DEVELOPMENT_LABEL, "true"))
                        .exec();
            } catch (NotFoundException e) {
                throw new IllegalStateException("DVWA image is not available locally: " + containerName
                        + ". Run a normal DVWA suite once to pull the lab image before enabling reuse.", e);
            }
            developmentId = created.getId();
            client.startContainerCmd(developmentId).exec();
            newDevelopmentContainer = true;
            System.out.println("[DVWA DEV] Created persistent DVWA container: " + developmentName);
        } else {
            String image = existing.getConfig() == null ? null : existing.getConfig().getImage();
            String label = existing.getConfig() == null || existing.getConfig().getLabels() == null
                    ? null : existing.getConfig().getLabels().get(DEVELOPMENT_LABEL);
            if (!"true".equals(label) || !containerName.equals(image))
                throw new IllegalStateException("Refusing to reuse container '" + developmentName
                        + "': its ownership label or Docker image does not match this DVWA environment.");

            developmentId = existing.getId();
            if (existing.getState() == null || !Boolean.TRUE.equals(existing.getState().getRunning())) {
                client.startContainerCmd(developmentId).exec();
            }
            System.out.println("[DVWA DEV] Reusing persistent DVWA container: " + developmentName);
        }

        String currentNetworkId = SHARED_NETWORK.getId();
        InspectContainerResponse info = client.inspectContainerCmd(developmentId).exec();
        boolean alreadyConnected = info.getNetworkSettings() != null
                && info.getNetworkSettings().getNetworks() != null
                && info.getNetworkSettings().getNetworks().values().stream()
                .anyMatch(network -> currentNetworkId.equals(network.getNetworkID()));

        if (!alreadyConnected) {
            client.connectToNetworkCmd()
                    .withContainerId(developmentId)
                    .withNetworkId(currentNetworkId)
                    .withContainerNetwork(new ContainerNetwork().withAliases(networkAlias))
                    .exec();
        }
    }

    /**
     * Stops the normal managed DVWA container. For development reuse, disconnects the lab from this run's
     * temporary Testcontainers network without stopping or deleting the lab container or its database.
     */
    @Override
    public void stopEnvironment() {
        if (developmentName == null) {
            super.stopEnvironment();
            return;
        }
        if (developmentId == null) return;

        try {
            DockerClient client = DockerClientFactory.instance().client();
            String currentNetworkId = SHARED_NETWORK.getId();
            InspectContainerResponse info = client.inspectContainerCmd(developmentId).exec();
            boolean connected = info.getNetworkSettings() != null
                    && info.getNetworkSettings().getNetworks() != null
                    && info.getNetworkSettings().getNetworks().values().stream()
                    .anyMatch(network -> currentNetworkId.equals(network.getNetworkID()));
            if (connected) {
                client.disconnectFromNetworkCmd()
                        .withContainerId(developmentId)
                        .withNetworkId(currentNetworkId)
                        .exec();
            }
        } finally {
            developmentId = null;
        }
    }

    /**
     * Returns the DVWA URL accessible from the ZAP proxy on the current test network.
     *
     * @return the internal lab URL
     */
    @Override
    public String getInternalURL() {
        if (developmentName == null) return super.getInternalURL();
        if (developmentId == null)
            throw new IllegalStateException("DVWA development container was not started.");
        return "http://" + networkAlias + ":" + exposedPort;
    }

    /**
     * Indicates whether development mode created a new container that requires database initialization.
     *
     * @return true only on the first creation of a persistent development container
     */
    public boolean isNewDevelopmentContainer() {
        return newDevelopmentContainer;
    }
}
