
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
import pom.Dvwa.SecurityPage;
import tests.framework.LabLifecycle;
import tests.framework.ProxiedBrowserTest;

import java.time.Duration;

/**
 * Provides shared TestNG suite setup and teardown for the Docker-based DVWA tests, including database initialization and ZAP reporting.
 */
public abstract class BaseDvwaTest extends ProxiedBrowserTest {

    private static final LabLifecycle<DvwaEnvironment> LAB = new LabLifecycle<>();
    protected static String baseUrl;

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
        boolean reuse = AppConfig.dvwaDevelopmentReuse();
        baseUrl = LAB.start(() -> reuse
                        ? new DvwaEnvironment(AppConfig.dvwaDevelopmentContainerName())
                        : new DvwaEnvironment(),
                DvwaEnvironment::startEnvironment,
                (environment, url) -> {
                    if (!reuse || environment.isNewDevelopmentContainer() || AppConfig.dvwaDevelopmentResetDatabase()) {
                        initializeDatabase(url);
                    } else {
                        System.out.println("[DVWA DEV] Reusing initialized DVWA container; database setup skipped.");
                    }
                });
    }

    /** Exports the DVWA report and releases the shared target and scanner. */
    @AfterSuite(alwaysRun = true)
    public void stopServer() {
        try { LAB.stop(zapReportName()); }
        finally { baseUrl = null; }
    }

    /**
     * Opens DVWA setup, creates its database, and performs the initial administrator login.
     *
     * <p>Opens the DVWA setup page, presses the database initialization control, and authenticates to prepare the test suite.</p>
     * @param baseUrl running application URL
     */
    private void initializeDatabase(String baseUrl) {
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
            new LoginPage(driver).login(AppConfig.dvwaUsername(), AppConfig.dvwaPassword());

            System.out.println("DVWA database initialized and authenticated successfully");
        } finally {
            ZapManager.quitZapDriver();
        }
    }

    /**
     * Logs in and selects the requested DVWA mode using the original page objects.
     * @param level security level for this invocation
     * @return authenticated login page for subsequent navigation
     */
    protected LoginPage prepareSecurityLevel(SecurityPage.SecurityLevel level) {
        WebDriver driver = ZapManager.getDriver();
        driver.get(baseUrl + LoginPage.loginEndpoint);
        LoginPage loginPage = new LoginPage(driver);
        loginPage.login(AppConfig.dvwaUsername(), AppConfig.dvwaPassword());
        driver.get(baseUrl + SecurityPage.securityEndpoint);
        new SecurityPage(driver).setSecurityLevel(level);
        return loginPage;
    }

    /** Enumerates all existing DVWA modes in the established order. */
    @DataProvider(name = "securityModes")
    public Object[][] securityModes() {
        return java.util.Arrays.stream(SecurityPage.SecurityLevel.values())
                .map(level -> new Object[]{level}).toArray(Object[][]::new);
    }
}
