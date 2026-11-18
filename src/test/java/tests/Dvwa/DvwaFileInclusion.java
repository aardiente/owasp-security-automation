package tests.Dvwa;

import automation.ZapManager;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import pom.Dvwa.FileInclusionPage;
import pom.Dvwa.LoginPage;
import pom.Dvwa.SecurityPage;
import utilities.ScreenshotManager;

import java.util.Arrays;

/** Verifies whether DVWA includes an unintended but harmless local PHP-information page. */
public class DvwaFileInclusion extends BaseDvwaTest {
    /**
     * Enumerates the four DVWA security modes.
     * @return one independent invocation per mode
     */
    @DataProvider(name = "securityModes")
    public Object[][] securityModes() {
        return Arrays.stream(SecurityPage.SecurityLevel.values())
                .map(level -> new Object[]{level}).toArray(Object[][]::new);
    }

    /**
     * Fails on confirmed file inclusion, passes on explicit rejection, and skips ambiguous output.
     * @param level DVWA security mode to assess
     */
    @Test(dataProvider = "securityModes")
    public void fileInclusionIsBlocked(SecurityPage.SecurityLevel level) {
        WebDriver driver = ZapManager.getDriver();
        driver.get(baseUrl + LoginPage.loginEndpoint);
        new LoginPage(driver).login("admin", "password");
        driver.get(baseUrl + SecurityPage.securityEndpoint);
        new SecurityPage(driver).setSecurityLevel(level);

        FileInclusionPage page = new FileInclusionPage(driver);
        page.requestLocalInformationPage(baseUrl, level);
        FileInclusionPage.InclusionResult result = page.inclusionResult();
        ScreenshotManager.capture(driver);
        if (result == FileInclusionPage.InclusionResult.INCONCLUSIVE)
            throw new SkipException("File-inclusion response was blank or ambiguous at " + level
                    + "; this probe cannot establish that the input was rejected.");
        Assert.assertNotEquals(result, FileInclusionPage.InclusionResult.INCLUDED,
                "LOCAL FILE INCLUSION CONFIRMED at " + level + ": phpinfo output was rendered.");
    }
}
