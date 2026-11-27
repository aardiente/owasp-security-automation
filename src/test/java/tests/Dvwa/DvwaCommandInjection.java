package tests.Dvwa;

import automation.ZapManager;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.Test;
import pom.Dvwa.CommandInjectionPage;
import pom.Dvwa.SecurityPage;
import utilities.ScreenshotManager;


/**
 * Contains DVWA command injection tests. Records how the lab's protections behave at each configured security level.
 */
public class DvwaCommandInjection extends BaseDvwaTest {
    /**
     * Fails when DVWA emits the marker from the additional command.
     *
     * @param level security mode under test
     */
    @Test(dataProvider = "securityModes")
    public void commandInjectionIsBlocked(SecurityPage.SecurityLevel level) {
        WebDriver driver = ZapManager.getDriver();
        prepareSecurityLevel(level);

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
