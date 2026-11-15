
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

public class DvwaZapActiveScanTest extends BaseDvwaTest {

    //@Override
    protected String zapReportName() {
        return AppConfig.zapDvwaActiveReportName();
    }

    @Test
    public void testDvwaActiveScan() throws Exception {
        WebDriver driver = ZapManager.getDriver();

        driver.get(baseUrl + LoginPage.loginEndpoint);
        LoginPage loginPage = new LoginPage(driver);
        loginPage.login("admin", "password");

        driver.get(baseUrl + SecurityPage.securityEndpoint);
        new SecurityPage(driver).setSecurityLevel(SecurityPage.SecurityLevel.LOW);

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

    private static int effectivePort(URI uri) {
        if (uri.getPort() >= 0) return uri.getPort();
        if ("http".equalsIgnoreCase(uri.getScheme())) return 80;
        if ("https".equalsIgnoreCase(uri.getScheme())) return 443;
        return -1;
    }
}
