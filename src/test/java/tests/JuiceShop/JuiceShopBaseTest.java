
package tests.JuiceShop;

import automation.AppConfig;
import automation.ZapManager;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import utilities.Docker.JuiceShopEnvironment;

import java.time.Duration;

public abstract class JuiceShopBaseTest {

    protected static JuiceShopEnvironment container;
    protected static String baseUrl;
    protected String payloadSource = "";

    protected String zapReportName() {
        return AppConfig.zapPassiveReportName();
    }

    @BeforeSuite
    public void startServer() {
        if (container != null) return;

        container = new JuiceShopEnvironment();
        container.startServer();
        baseUrl = container.getInternalURL();

        ZapManager.initZap();
    }

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

    @BeforeMethod
    public void initBrowser() {
        ZapManager.initZapDriver();
        ZapManager.getDriver().manage().timeouts().implicitlyWait(Duration.ZERO);
    }

    @AfterMethod(alwaysRun = true)
    public void closeBrowser() {
        ZapManager.quitZapDriver();
    }
}
