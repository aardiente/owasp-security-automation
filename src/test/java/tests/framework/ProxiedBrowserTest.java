package tests.framework;

import automation.ZapManager;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;

/** Common per-invocation browser lifecycle for tests using the ZAP proxy. */
public abstract class ProxiedBrowserTest {
    /** Opens a fresh proxy-backed browser and applies the target-specific settings. */
    @BeforeMethod
    public void initBrowser() {
        ZapManager.initZapDriver();
        configureBrowser(ZapManager.getDriver());
    }

    /**
     * Allows application-specific browser settings without duplicating setup and teardown.
     * @param driver active proxy-backed WebDriver
     */
    protected void configureBrowser(WebDriver driver) { }

    /** Closes the current test's browser regardless of its outcome. */
    @AfterMethod(alwaysRun = true)
    public void closeBrowser() {
        ZapManager.quitZapDriver();
    }
}
