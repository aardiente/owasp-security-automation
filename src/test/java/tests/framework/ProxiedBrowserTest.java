package tests.framework;

import automation.AppConfig;
import automation.ZapManager;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;

/**
 * Provides the common per-invocation browser lifecycle
 * for tests using the ZAP proxy.
 */
public abstract class ProxiedBrowserTest {

    /**
     * Opens a fresh proxy-backed browser and applies
     * target-specific browser settings.
     */
    @BeforeMethod
    public void initBrowser() {
        ZapManager.initZapDriver();
        configureBrowser(ZapManager.getDriver());
    }

    /**
     * Allows application-specific browser settings without
     * duplicating browser setup and teardown.
     *
     * @param driver active proxy-backed WebDriver
     */
    protected void configureBrowser(WebDriver driver) {}

    /**
     * Displays the final browser state for the configured
     * result presentation duration before closing the browser.
     *
     * <p>Browser resources are released regardless of the test
     * outcome or interruption of the presentation delay.</p>
     */
    @AfterMethod(alwaysRun = true)
    public void closeBrowser() {
        try {
            if (!AppConfig.isHeadless()) {
                AppConfig.pauseForResults();
            }
        } finally {
            ZapManager.quitZapDriver();
        }
    }
}