package tests.Dvwa;

import automation.ZapManager;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import pom.Dvwa.LoginPage;
import pom.Dvwa.SecurityPage;
import pom.Dvwa.XssStoredPage;
import utilities.ScreenshotManager;

import java.util.Arrays;
import java.util.UUID;

/** Tests harmless stored-XSS execution after verifying the entry was actually persisted. */
public class DvwaXssStored extends BaseDvwaTest {
    /**
     * Enumerates the four DVWA security modes.
     * @return independent TestNG invocations for each security level
     */
    @DataProvider(name = "securityModes")
    public Object[][] securityModes() {
        return Arrays.stream(SecurityPage.SecurityLevel.values())
                .map(level -> new Object[]{level}).toArray(Object[][]::new);
    }

    /**
     * Fails on confirmed stored XSS and skips runs where the test entry was not saved.
     * @param level DVWA security level under test
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
        boolean persisted = page.entryWasPersisted(marker);
        boolean executed = persisted && page.didStoredProbeExecute(marker);
        ScreenshotManager.capture(driver);
        if (!persisted)
            throw new SkipException("Stored-XSS probe was not saved at " + level
                    + "; a missing guestbook entry is not proof of XSS protection.");
        Assert.assertFalse(executed, "STORED XSS CONFIRMED at " + level
                + ": the persisted browser event handler executed.");
    }
}
