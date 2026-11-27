
package automation;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.events.EventFiringDecorator;
import org.zaproxy.clientapi.core.ApiResponseElement;
import org.zaproxy.clientapi.core.ClientApi;
import utilities.Docker.OWASPZapEnvironment;

import java.nio.file.Files;
import java.nio.file.Path;
import utilities.reporting.ReportingConfiguration;
import java.time.Duration;

/**
 * Manages the ZAP service and browsers that connect through it. Provides shared setup, report export and cleanup for test suites.
 */
public class ZapManager {

    private static final ThreadLocal<OWASPZapEnvironment> zapEnvironment = new ThreadLocal<>();
    private static final ThreadLocal<ClientApi> zapClient = new ThreadLocal<>();
    private static final ThreadLocal<WebDriver> zapDriver = new ThreadLocal<>();

    /**
     * Prevents direct creation of this utility class.
     */
    private ZapManager() {}

    /**
     * Starts the ZAP container and sets up its API client for the current thread.
     */
    public static void initZap() {
        if (zapEnvironment.get() != null) return;

        OWASPZapEnvironment env = new OWASPZapEnvironment();
        env.startEnvironment();

        ClientApi client = new ClientApi(env.getHost(), env.getMappedPort(), null);
        Exception lastError = null;

        // This was added to resolve issues created by the security reports being added.
        for (int attempt = 0; attempt < 30; attempt++) {
            try {
                client.core.version();
                zapEnvironment.set(env);
                zapClient.set(client);
                return;
            } catch (Exception e) {
                lastError = e;

                try {
                    Thread.sleep(500);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    env.stopEnvironment();
                    throw new IllegalStateException("Interrupted while waiting for ZAP.", interrupted);
                }
            }
        }

        env.stopEnvironment();
        throw new IllegalStateException("ZAP API did not become ready.", lastError);
    }

    /**
     * Creates the current thread's Selenium driver configured with the ZAP proxy.
     */
    public static void initZapDriver() {
        if (zapEnvironment.get() == null) {
            initZap();
        }

        if (zapDriver.get() == null) {
            String proxyAddress = zapEnvironment.get().getProxyAddress();
            WebDriver webDriver = BrowserFactory.createZapProxy(proxyAddress);

            BrowserFactory.setViewport(webDriver);
            webDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(AppConfig.implicitWaitSeconds()));

            SlowMoListener listener = new SlowMoListener(webDriver);
            WebDriver decoratedDriver = new EventFiringDecorator<>(listener).decorate(webDriver);
            zapDriver.set(decoratedDriver);
        }
    }

    /**
     * Returns the ZAP-proxied WebDriver for the current thread.
     *
     * @return the current thread\u2019s Selenium driver configured for the ZAP proxy
     */
    public static WebDriver getDriver() {
        if (zapDriver.get() == null) {
            initZapDriver();
        }

        return zapDriver.get();
    }

    /**
     * Quits and clears the current thread's ZAP-proxied WebDriver.
     */
    public static void quitZapDriver() {
        if (zapDriver.get() != null) {
            try { zapDriver.get().quit(); }
            finally { zapDriver.remove(); }
        }
    }

    /**
     * Returns the ZAP API client, initializing its environment if necessary.
     *
     * @return the API client for the current thread\u2019s ZAP instance
     */
    public static ClientApi getZapClient() {
        if (zapClient.get() == null) {
            initZap();
        }

        return zapClient.get();
    }

    /**
     * Returns the proxy endpoint exposed by the running ZAP environment.
     *
     * @return the host and port used to proxy browser requests through ZAP
     */
    public static String getProxyAddress() {
        if (zapEnvironment.get() == null) {
            initZap();
        }

        return zapEnvironment.get().getProxyAddress();
    }

    /**
     * Waits for pending ZAP passive-scan records, then exports the captured findings as HTML and JSON. Waits for passive processing to finish, then exports both HTML and JSON data from the current ZAP instance.
     */
    public static void generateReport() {
        generateReport(AppConfig.zapDefaultReportName());
    }

    /**
     * Waits for pending ZAP passive-scan records, then exports the captured findings as HTML and JSON. Waits for passive processing to finish, then exports both HTML and JSON data from the current ZAP instance.
     *
     * @param fileName the configured source or output filename
     */
    public static void generateReport(String fileName) {
        ClientApi client = zapClient.get();
        if (client == null) return;

        try {
            int recordsRemaining;

            do {
                Thread.sleep(1000);
                ApiResponseElement response = (ApiResponseElement) client.pscan.recordsToScan();
                recordsRemaining = Integer.parseInt(response.getValue());
            } while (recordsRemaining != 0);

            byte[] htmlReport = client.core.htmlreport();
            Path htmlPath = AppConfig.zapHtmlReportDirectory().resolve(fileName);

            Files.createDirectories(htmlPath.getParent());
            Files.write(htmlPath, htmlReport);

            System.out.println("ZAP report generated: " + htmlPath.toAbsolutePath());

            String jsonFileName = fileName.endsWith(".html")
                    ? fileName.substring(0, fileName.length() - 5) + ".json"
                    : fileName + ".json";

            Path jsonPath = ReportingConfiguration.load().zapFile(jsonFileName);

            try {
                byte[] jsonReport = client.core.jsonreport();
                Files.createDirectories(jsonPath.getParent());
                Files.write(jsonPath, jsonReport);

                System.out.println("[SECURITY REPORT] ZAP JSON saved: " + jsonPath.toAbsolutePath());
            } catch (Exception e) {
                System.err.println("[SECURITY REPORT] ZAP JSON export failed: " + e.getMessage());
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while generating ZAP report.", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate ZAP report: " + fileName, e);
        }
    }

    /**
     * Stops and clears the thread-specific ZAP browser, client, and container.
     */
    public static void quitZap() {
        quitZapDriver();

        if (zapEnvironment.get() != null) {
            zapEnvironment.get().stopEnvironment();
            zapEnvironment.remove();
            zapClient.remove();
        }
    }
}
