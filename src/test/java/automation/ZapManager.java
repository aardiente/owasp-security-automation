
package automation;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.events.EventFiringDecorator;
import org.zaproxy.clientapi.core.ApiResponseElement;
import org.zaproxy.clientapi.core.ClientApi;
import utilities.Docker.OWASPZapEnvironment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

public class ZapManager {

    private static final ThreadLocal<OWASPZapEnvironment> zapEnvironment = new ThreadLocal<>();
    private static final ThreadLocal<ClientApi> zapClient = new ThreadLocal<>();
    private static final ThreadLocal<WebDriver> zapDriver = new ThreadLocal<>();

    private ZapManager() {}

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

    public static WebDriver getDriver() {
        if (zapDriver.get() == null) {
            initZapDriver();
        }

        return zapDriver.get();
    }

    public static void quitZapDriver() {
        if (zapDriver.get() != null) {
            zapDriver.get().quit();
            zapDriver.remove();
        }
    }

    public static ClientApi getZapClient() {
        if (zapClient.get() == null) {
            initZap();
        }

        return zapClient.get();
    }

    public static String getProxyAddress() {
        if (zapEnvironment.get() == null) {
            initZap();
        }

        return zapEnvironment.get().getProxyAddress();
    }

    public static void generateReport() {
        generateReport("zap-report.html");
    }

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
            Path htmlPath = Paths.get("target", fileName);

            Files.createDirectories(htmlPath.getParent());
            Files.write(htmlPath, htmlReport);

            System.out.println("ZAP report generated: " + htmlPath.toAbsolutePath());

            String jsonFileName = fileName.endsWith(".html")
                    ? fileName.substring(0, fileName.length() - 5) + ".json"
                    : fileName + ".json";

            Path jsonPath = Paths.get("target", "security-reports", jsonFileName);

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

    public static void quitZap() {
        quitZapDriver();

        if (zapEnvironment.get() != null) {
            zapEnvironment.get().stopEnvironment();
            zapEnvironment.remove();
            zapClient.remove();
        }
    }
}
