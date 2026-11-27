package tests.JuiceShop;

import automation.AppConfig;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import tests.framework.LabLifecycle;
import tests.framework.ProxiedBrowserTest;
import utilities.Docker.JuiceShopEnvironment;
import java.time.Duration;

/**
 * Provides common setup and cleanup for Juice Shop tests. Manages the application, scanner and browser sessions.
 */
public abstract class JuiceShopBaseTest extends ProxiedBrowserTest {
    private static final LabLifecycle<JuiceShopEnvironment> LAB = new LabLifecycle<>();
    protected static String baseUrl;

    /**
     * Returns the configured ZAP passive report name; standalone scans may override it.
     */
    protected String zapReportName() { return AppConfig.zapPassiveReportName(); }

    /**
     * Starts the application and its scanner once per sequential suite.
     */
    @BeforeSuite
    public void startServer() {
        baseUrl = LAB.start(JuiceShopEnvironment::new, JuiceShopEnvironment::startServer,
                (environment, url) -> { });
    }

    /**
     * Exports the selected report and stops the shared application environment.
     */
    @AfterSuite(alwaysRun = true)
    public void stopServer() {
        try { LAB.stop(zapReportName()); }
        finally { baseUrl = null; }
    }

    /**
     * Keeps Juice Shop on browser waits, as in the original suite.
     *
     * @param driver current ZAP-browser connected through ZAP
     */
    @Override
    protected void configureBrowser(WebDriver driver) {
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
    }
}
