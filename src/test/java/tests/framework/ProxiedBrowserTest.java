package tests.framework;

import automation.AppConfig;
import automation.ZapManager;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;

/**
 * Provides common browser setup and cleanup for tests that use ZAP. Keeps final results visible for the configured time before closing the browser.
 */
public abstract class ProxiedBrowserTest {

    /**
     * Opens a fresh proxy-connected browser and applies target-specific browser settings.
     */
    @BeforeMethod
    public void initBrowser() {
        ZapManager.initZapDriver();
        configureBrowser(ZapManager.getDriver());
    }

    /**
     * Allows application-specific browser settings without duplicating browser setup and cleanup.
     *
     * @param driver active proxy-connected WebDriver
     */
    protected void configureBrowser(WebDriver driver) {}

    /**
     * Displays the final browser state for the configured result presentation duration before closing the browser. Browser resources are released regardless of the test outcome or interruption of the presentation delay.
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