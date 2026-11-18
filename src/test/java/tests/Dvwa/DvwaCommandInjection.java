package tests.Dvwa;

import automation.ZapManager;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import pom.Dvwa.CommandInjectionPage;
import pom.Dvwa.LoginPage;
import pom.Dvwa.SecurityPage;
import utilities.ScreenshotManager;

import java.util.Arrays;

/** Verifies whether DVWA executes a harmless command marker at each security level. */
public class DvwaCommandInjection extends BaseDvwaTest {
    /**
     * Enumerates the four supported DVWA security levels.
     * @return independent TestNG invocations for all security modes
     */
    @DataProvider(name = "securityModes")
    public Object[][] securityModes() {
        return Arrays.stream(SecurityPage.SecurityLevel.values())
                .map(level -> new Object[]{level}).toArray(Object[][]::new);
    }

    /**
     * Fails when DVWA emits the marker from the additional command.
     * @param level security mode under test
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
        page.submitMarkerProbe(marker, level);
        boolean executed = page.hasExecutedMarker(marker);
        ScreenshotManager.capture(driver);
        Assert.assertFalse(executed, "COMMAND INJECTION CONFIRMED at " + level
                + ": the server executed the additional harmless marker command.");
    }
}
