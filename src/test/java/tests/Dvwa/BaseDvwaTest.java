
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

/**
 * Provides shared TestNG suite setup and teardown for the Docker-based DVWA tests, including database initialization and ZAP reporting.
 */
public abstract class BaseDvwaTest {

    protected static DvwaEnvironment container;
    protected static String baseUrl;
    protected String payloadSource = "";
    protected String[] securityLevels = {"low", "medium", "high"};

    /**
     * Selects the output filename for the DVWA suite's ZAP report; standalone subclasses may override it.
     *
     * @return the HTML report filename selected for DVWA suite teardown
     */
    protected String zapReportName() {
        return AppConfig.zapDvwaPassiveReportName();
    }

    /**
     * TestNG suite setup: starts the DVWA container, initializes ZAP, and prepares the DVWA database.
     */
    @BeforeSuite
    public void startServer() {
        if (container != null) return;

        container = new DvwaEnvironment();
        container.startEnvironment();
        baseUrl = container.getInternalURL();

        ZapManager.initZap();
        initializeDatabase();
    }

    /**
     * TestNG suite teardown: exports the selected DVWA ZAP report and closes the ZAP and DVWA environments.
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
     * TestNG method setup: creates a fresh ZAP-proxied browser for the upcoming DVWA test.
     */
    @BeforeMethod
    public void initBrowser() {
        ZapManager.initZapDriver();
    }

    /**
     * TestNG method teardown: closes the current ZAP-proxied browser.
     */
    @AfterMethod(alwaysRun = true)
    public void closeBrowser() {
        ZapManager.quitZapDriver();
    }

    /**
     * Opens DVWA setup, creates its database, and performs the initial administrator login.
     *
     * <p>Opens the DVWA setup page, presses the database initialization control, and authenticates to prepare the test suite.</p>
     */
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

    /**
     * Expands Excel payloads into parameterized DVWA security-level test inputs.
     *
     * @return the TestNG data-provider matrix; each row contains the parameters for one test invocation
     */
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
