package tests.Dvwa;

import automation.ZapManager;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import pom.Dvwa.LoginPage;
import pom.Dvwa.SecurityPage;
import pom.Dvwa.CommandInjectionPage;
import utilities.ScreenshotManager;

import java.util.Arrays;


/** Checks whether command injection is demonstrable in each DVWA security mode. */
public class DvwaCommandInjection extends BaseDvwaTest {
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
    public void commandInjectionIsBlocked(SecurityPage.SecurityLevel level) {
        WebDriver driver = ZapManager.getDriver();
        driver.get(baseUrl + LoginPage.loginEndpoint);
        new LoginPage(driver).login("admin", "password");
        driver.get(baseUrl + SecurityPage.securityEndpoint);
        new SecurityPage(driver).setSecurityLevel(level);

        CommandInjectionPage page = new CommandInjectionPage(driver);
        page.open(baseUrl);
        String marker = "DVWA_COMMAND_PROBE";
        page.submitMarkerProbe(marker);
        boolean executed = page.hasExecutedMarker(marker);
        ScreenshotManager.capture(driver);
        Assert.assertFalse(executed, "COMMAND INJECTION CONFIRMED at " + level + ": injected echo marker was executed.");
    }
}
