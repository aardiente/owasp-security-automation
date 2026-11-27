package tests.framework;

import automation.ZapManager;
import utilities.Docker.DockerContainer;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Manages the shared lab environment for a test suite. Starts the application and scanner, saves reports, and releases resources when finished.
 */
public final class LabLifecycle<T extends DockerContainer> {
    private T environment;
    private String baseUrl;

    /**
     * Starts the lab application once, launches ZAP and runs the application's setup steps.
     *
     * @param factory creates the configured container
     * @param starter starts the container using the target's existing strategy
     * @param initializer prepares the running target, receiving its internal URL
     * @return the internal application URL shared by the suite
     */
    public synchronized String start(Supplier<T> factory, Consumer<T> starter,
                                     BiConsumer<T, String> initializer) {
        if (environment != null) return baseUrl;
        T candidate = factory.get();
        try {
            starter.accept(candidate);
            String url = candidate.getInternalURL();
            ZapManager.initZap();
            initializer.accept(candidate, url);
            environment = candidate;
            baseUrl = url;
            return url;
        } catch (RuntimeException | Error failure) {
            try { ZapManager.quitZap(); }
            catch (RuntimeException | Error cleanup) { failure.addSuppressed(cleanup); }
            try { candidate.stopEnvironment(); }
            catch (RuntimeException | Error cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }

    /**
     * Exports the configured report and releases the scanner and target even if report export fails.
     *
     * @param reportName report filename for the current suite
     */
    public synchronized void stop(String reportName) {
        if (environment == null) return;
        try {
            ZapManager.generateReport(reportName);
        } finally {
            try {
                ZapManager.quitZap();
            } finally {
                try { environment.stopEnvironment(); }
                finally { environment = null; baseUrl = null; }
            }
        }
    }
}
