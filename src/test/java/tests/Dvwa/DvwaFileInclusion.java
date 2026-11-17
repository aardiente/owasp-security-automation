package tests.Dvwa;

import automation.ZapManager;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import pom.Dvwa.LoginPage;
import pom.Dvwa.SecurityPage;
import pom.Dvwa.FileInclusionPage;
import utilities.ScreenshotManager;

import java.util.Arrays;


/** Tests unintended local file inclusion using DVWA's benign PHP information page. */
public class DvwaFileInclusion extends BaseDvwaTest {
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
    public void fileInclusionIsBlocked(SecurityPage.SecurityLevel level) {
        WebDriver driver = ZapManager.getDriver();
        driver.get(baseUrl + LoginPage.loginEndpoint);
        new LoginPage(driver).login("admin", "password");
        driver.get(baseUrl + SecurityPage.securityEndpoint);
        new SecurityPage(driver).setSecurityLevel(level);

        FileInclusionPage page = new FileInclusionPage(driver);
        page.requestLocalInformationPage(baseUrl);
        boolean included = page.renderedLocalInformation();
        ScreenshotManager.capture(driver);
        Assert.assertFalse(included, "LOCAL FILE INCLUSION CONFIRMED at " + level + ": phpinfo output was included.");
    }
}
