package utilities.Docker;

import com.github.dockerjava.api.model.Bind;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import java.time.Duration;

/**
 * Starts the optional local Ollama test dependency.
 */
public class OllamaEnvironment extends DockerContainer
{
    private static final String BASE_MODEL = "qwen2.5:1.5b";
    public static final String CHAT_MODEL = "juiceshop-qwen";

    /**
     * Creates the ollama environment component and initializes the dependencies required by its methods.
     *
     * <p>Creates the optional Docker environment for Ollama.</p>
     */
    public OllamaEnvironment()
    {
        super("ollama/ollama:latest", 11434);
    }

    /**
     * Starts the optional local Ollama service container used by experimental tests.
     */
    @Override
    public void startEnvironment()
    {
        if(container == null)
        {
            container = new GenericContainer<>(containerName)
                    .withNetwork(SHARED_NETWORK)
                    .withNetworkAliases(networkAlias)
                    .withExposedPorts(exposedPort)
                    .withEnv("OLLAMA_HOST", "0.0.0.0")
                    .withEnv("OLLAMA_DEBUG", "1")
                    .withCreateContainerCmdModifier(cmd -> cmd.getHostConfig().withBinds(
                            Bind.parse("ollama-data:/root/.ollama")
                    ))
                    .withLogConsumer(outputFrame -> System.out.print("[OLLAMA] " + outputFrame.getUtf8String()))
                    .waitingFor(Wait.forHttp("/").forStatusCode(200).withStartupTimeout(Duration.ofMinutes(5)));

            container.start();

            try {
                container.execInContainer("ollama", "pull", BASE_MODEL);

                String modelfile = "FROM " + BASE_MODEL + "\n" +
                        "PARAMETER num_ctx 4096\n" +
                        "TEMPLATE \"\"\"{{- range .Messages }}<|im_start|>{{ .Role }}\n{{ .Content }}<|im_end|>\n{{- end }}<|im_start|>assistant\n\"\"\"\n";

                container.execInContainer("sh", "-c", "printf '%s' '" + modelfile.replace("'", "'\\''") + "' > /tmp/Modelfile && ollama create " + CHAT_MODEL + " -f /tmp/Modelfile");
                container.execInContainer("ollama", "run", CHAT_MODEL, "hi");
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize Ollama model: " + CHAT_MODEL, e);
            }
        }
    }
}