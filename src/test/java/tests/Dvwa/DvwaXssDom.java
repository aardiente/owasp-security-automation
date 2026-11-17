package tests.Dvwa;

import automation.ZapManager;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import pom.Dvwa.LoginPage;
import pom.Dvwa.SecurityPage;
import pom.Dvwa.XssDomPage;
import utilities.ScreenshotManager;

import java.util.Arrays;
import java.util.UUID;

/** Tests DOM XSS by detecting a harmless browser-executed event marker. */
public class DvwaXssDom extends BaseDvwaTest {
    /**
     * Provides all DVWA security levels as independent TestNG invocations.
     * @return the Low, Medium, High, and Impossible modes
     */
    @DataProvider(name = "securityModes")
    public Object[][] securityModes() {
        return Arrays.stream(SecurityPage.SecurityLevel.values())
                .map(level -> new Object[]{level}).toArray(Object[][]::new);
    }

    /**
     * Checks a harmless diagnostic probe and fails when the vulnerability is demonstrated.
     * @param level the DVWA security level being assessed
     */
    @Test(dataProvider = "securityModes")
    public void domXssIsBlocked(SecurityPage.SecurityLevel level) {
        WebDriver driver = ZapManager.getDriver();
        driver.get(baseUrl + LoginPage.loginEndpoint);
        new LoginPage(driver).login("admin", "password");
        driver.get(baseUrl + SecurityPage.securityEndpoint);
        new SecurityPage(driver).setSecurityLevel(level);

        XssDomPage page = new XssDomPage(driver);
        String marker = "p" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        page.openWithMarkerProbe(baseUrl, marker);
        boolean executed = page.didProbeExecute(marker);
        ScreenshotManager.capture(driver);
        Assert.assertFalse(executed, "DOM XSS CONFIRMED at " + level + ": injected browser event handler executed.");
    }
}
