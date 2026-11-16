
package tests.JuiceShop;

import automation.AppConfig;
import automation.ZapManager;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import utilities.Docker.JuiceShopEnvironment;

import java.time.Duration;

/**
 * Provides the Juice Shop TestNG lifecycle: Docker container, ZAP proxy, browser, and suite-specific report export.
 */
public abstract class JuiceShopBaseTest {

    protected static JuiceShopEnvironment container;
    protected static String baseUrl;
    protected String payloadSource = "";

    /**
     * Selects the output filename for the Juice Shop suite's ZAP report; standalone subclasses may override it.
     *
     * @return the HTML report filename selected for Juice Shop suite teardown
     */
    protected String zapReportName() {
        return AppConfig.zapPassiveReportName();
    }

    /**
     * TestNG suite setup: starts the Juice Shop container and initializes ZAP.
     */
    @BeforeSuite
    public void startServer() {
        if (container != null) return;

        container = new JuiceShopEnvironment();
        container.startServer();
        baseUrl = container.getInternalURL();

        ZapManager.initZap();
    }

    /**
     * TestNG suite teardown: exports the selected Juice Shop report and shuts down ZAP and the application.
     */
    @AfterSuite(alwaysRun = true)
    public void stopServer() {
        if (container == null) return;

        try {
            ZapManager.generateReport(zapReportName());
        } finally {
            try {
                ZapManager.quitZap();
            } finally {
                container.stopEnvironment();
                container = null;
            }
        }
    }

    /**
     * TestNG method setup: opens a fresh ZAP-proxied browser with an immediate implicit wait.
     */
    @BeforeMethod
    public void initBrowser() {
        ZapManager.initZapDriver();
        ZapManager.getDriver().manage().timeouts().implicitlyWait(Duration.ZERO);
    }

    /**
     * TestNG method teardown: closes the current ZAP-proxied browser.
     */
    @AfterMethod(alwaysRun = true)
    public void closeBrowser() {
        ZapManager.quitZapDriver();
    }
}
