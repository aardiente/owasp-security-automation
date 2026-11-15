
package tests.Dvwa;

import automation.AppConfig;
import automation.ZapManager;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.*;
import pom.Dvwa.LoginPage;
import utilities.Docker.DvwaEnvironment;
import utilities.ExcelFileReader;

import java.time.Duration;

public abstract class BaseDvwaTest {

    protected static DvwaEnvironment container;
    protected static String baseUrl;
    protected String payloadSource = "";
    protected String[] securityLevels = {"low", "medium", "high"};

    protected String zapReportName() {
        return AppConfig.zapDvwaPassiveReportName();
    }

    @BeforeSuite
    public void startServer() {
        if (container != null) return;

        container = new DvwaEnvironment();
        container.startEnvironment();
        baseUrl = container.getInternalURL();

        ZapManager.initZap();
        initializeDatabase();
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
    }

    @AfterMethod(alwaysRun = true)
    public void closeBrowser() {
        ZapManager.quitZapDriver();
    }

    private void initializeDatabase() {
        ZapManager.initZapDriver();
        WebDriver driver = ZapManager.getDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

        try {
            driver.get(baseUrl + "/setup.php");

            wait.until(webDriver -> {
                try {
                    WebElement button = webDriver.findElement(By.name("create_db"));
                    if (!button.isDisplayed() || !button.isEnabled()) return false;

                    button.click();
                    return true;
                } catch (StaleElementReferenceException e) {
                    return false;
                } catch (WebDriverException e) {
                    String message = e.getMessage();
                    if (message != null &&
                            message.contains("Node with given id does not belong to the document"))
                        return false;

                    throw e;
                }
            });

            driver.get(baseUrl + LoginPage.loginEndpoint);
            new LoginPage(driver).login("admin", "password");

            System.out.println("DVWA database initialized and authenticated successfully");
        } finally {
            ZapManager.quitZapDriver();
        }
    }

    @DataProvider(name = "securityAndPayloads")
    public Object[][] getSecurityAndPayloads() {
        Object[][] payloadsFromExcel = ExcelFileReader.extractData(payloadSource, null);
        Object[][] testData = new Object[payloadsFromExcel.length * securityLevels.length][2];

        int index = 0;

        for (String level : securityLevels) {
            for (Object[] row : payloadsFromExcel) {
                testData[index][0] = level;
                testData[index][1] = row[0].toString();
                index++;
            }
        }

        return testData;
    }
}
