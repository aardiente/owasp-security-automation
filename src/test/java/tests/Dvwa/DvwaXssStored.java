package tests.Dvwa;

import automation.ZapManager;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import pom.Dvwa.LoginPage;
import pom.Dvwa.SecurityPage;
import pom.Dvwa.XssStoredPage;
import utilities.ScreenshotManager;

import java.util.Arrays;
import java.util.UUID;

/** Tests stored XSS using an inert SVG marker persisted in the DVWA guestbook. */
public class DvwaXssStored extends BaseDvwaTest {
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
    public void storedXssIsBlocked(SecurityPage.SecurityLevel level) {
        WebDriver driver = ZapManager.getDriver();
        driver.get(baseUrl + LoginPage.loginEndpoint);
        new LoginPage(driver).login("admin", "password");
        driver.get(baseUrl + SecurityPage.securityEndpoint);
        new SecurityPage(driver).setSecurityLevel(level);

        XssStoredPage page = new XssStoredPage(driver);
        String marker = "p" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        page.open(baseUrl);
        page.submitMarkerProbe(marker);
        page.reloadStoredEntries(baseUrl);
        boolean executed = page.didStoredProbeExecute(marker);
        ScreenshotManager.capture(driver);
        Assert.assertFalse(executed, "STORED XSS CONFIRMED at " + level + ": persisted browser event handler executed.");
    }
}
