
package tests.ZapScan;

import automation.AppConfig;
import automation.ZapActiveScan;
import automation.ZapManager;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.Test;
import pom.Dvwa.LoginPage;
import pom.Dvwa.SecurityPage;
import tests.Dvwa.BaseDvwaTest;
import utilities.ScreenshotManager;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

/**
 * Standalone DVWA security assessment: seeds browser routes then launches an explicit ZAP spider/active scan.
 */
public class DvwaZapActiveScanTest extends BaseDvwaTest {

    //@Override
    /**
     * Selects the DVWA active-scan report filename rather than the regular DVWA report.
     *
     * @return the dedicated DVWA active-scan HTML report filename
     */
    protected String zapReportName() {
        return AppConfig.zapDvwaActiveReportName();
    }

    /**
     * Visits authenticated DVWA routes through the proxy, then runs a bounded standalone ZAP active scan.
     *
     * <p>Seeds selected DVWA routes through the proxied browser before launching the bounded ZAP scanner. This alone does not configure scan-as-user authentication.</p>
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test
    public void testDvwaActiveScan() throws Exception {
        WebDriver driver = ZapManager.getDriver();

        LoginPage loginPage = prepareSecurityLevel(SecurityPage.SecurityLevel.LOW);

        List<Consumer<LoginPage>> pages = List.of(
                LoginPage::navigateToSqlInjection,
                LoginPage::navigateToSqlInjectionBlind,
                LoginPage::navigateToXssReflected,
                LoginPage::navigateToXssStored,
                LoginPage::navigateToCommandInjection,
                LoginPage::navigateToFileInclusion,
                LoginPage::navigateToCsrf,
                LoginPage::navigateToBruteForce
        );

        for (Consumer<LoginPage> navigate : pages) {
            driver.get(baseUrl);
            navigate.accept(loginPage);

            String currentUrl = driver.getCurrentUrl();

            Assert.assertTrue(isSameOrigin(baseUrl, currentUrl),
                    "DVWA navigation left the target application. Expected origin: "
                            + baseUrl + " | Actual URL: " + currentUrl);

            System.out.println("[DVWA ZAP] Discovered: " + currentUrl);
        }

        ScreenshotManager.capture(driver);

        boolean completed = ZapActiveScan.run(
                baseUrl,
                Duration.ofMinutes(AppConfig.zapDvwaActiveMaxMinutes()),
                AppConfig.zapDvwaActiveStatusOutput()
        );

        Assert.assertTrue(completed,
                "DVWA active scan did not complete. Check the active-scan status JSON.");
    }

    /**
     * Checks whether two URLs resolve to the same scheme, host, and effective port.
     *
     * @param expectedUrl the expected DVWA application URL
     * @param actualUrl the URL obtained from the browser
     * @return {@code true} when scheme, host, and effective port match; otherwise {@code false}
     */
    private static boolean isSameOrigin(String expectedUrl, String actualUrl) {
        try {
            URI expected = URI.create(expectedUrl);
            URI actual = URI.create(actualUrl);

            return expected.getScheme() != null
                    && actual.getScheme() != null
                    && expected.getHost() != null
                    && actual.getHost() != null
                    && expected.getScheme().equalsIgnoreCase(actual.getScheme())
                    && expected.getHost().equalsIgnoreCase(actual.getHost())
                    && effectivePort(expected) == effectivePort(actual);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Normalizes explicit and implicit URI ports for same-origin comparisons.
     *
     * <p>Returns the explicit URL port or the default HTTP(S) port.</p>
     *
     * @param uri the URI whose scheme, host and port are being examined
     * @return the URI port, default 80/443 for HTTP(S), or {@code -1} for unsupported schemes
     */
    private static int effectivePort(URI uri) {
        if (uri.getPort() >= 0) return uri.getPort();
        if ("http".equalsIgnoreCase(uri.getScheme())) return 80;
        if ("https".equalsIgnoreCase(uri.getScheme())) return 443;
        return -1;
    }
}
