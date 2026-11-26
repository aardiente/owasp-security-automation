
package pom.Dvwa;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import automation.AppConfig;
import pom.common.BrowserPage;

import java.time.Duration;

/**
 * DVWA page object exposing Selenium interactions for the base module.
 */
public abstract class BasePage extends BrowserPage {

    /**
     * Creates the base page component and initializes the dependencies required by its methods.
     *
     * <p>Creates a base page instance and initializes its dependencies.</p>
     *
     * @param driver the Selenium WebDriver controlling the application browser
     */
    public BasePage(WebDriver driver) {
        super(driver, Duration.ofSeconds(AppConfig.dvwaPageWaitSeconds()));
    }

    private final By logoutLink = By.linkText("Logout");
    private final By bruteForceMenu = By.linkText("Brute Force");
    private final By commandInjectionMenu = By.linkText("Command Injection");
    private final By csrfMenu = By.linkText("CSRF");
    private final By fileInclusionMenu = By.linkText("File Inclusion");
    private final By sqlInjectionMenu = By.linkText("SQL Injection");
    private final By xssReflectedMenu = By.linkText("XSS (Reflected)");
    private final By xssStoredMenu = By.linkText("XSS (Stored)");
    private final By dvwaSecurityMenu = By.linkText("DVWA Security");
    private final By cspBypassMenu = By.linkText("CSP Bypass");
    private final By sqlInjectionBlindMenu = By.linkText("SQL Injection (Blind)");
    private final By weakSessionMenu = By.linkText("Weak Session IDs");
    private final By javascriptMenu = By.linkText("JavaScript");
    private final By phpInfoMenu = By.linkText("PHP Info");
    private final By aboutMenu = By.linkText("About");

    /**
     * Logs out of DVWA using the navigation bar logout control.
     */
    public void clickLogout() {
        wait.until(ExpectedConditions.elementToBeClickable(logoutLink)).click();
    }

    /**
     * Navigates to the brute force page using the application page object.
     */
    public void navigateToBruteForce() {
        wait.until(ExpectedConditions.elementToBeClickable(bruteForceMenu)).click();
    }

    /**
     * Navigates to the command injection page using the application page object.
     */
    public void navigateToCommandInjection() {
        wait.until(ExpectedConditions.elementToBeClickable(commandInjectionMenu)).click();
    }

    /**
     * Navigates to the SQL injection page using the application page object.
     */
    public void navigateToSqlInjection() {
        wait.until(ExpectedConditions.elementToBeClickable(sqlInjectionMenu)).click();
    }

    /**
     * Navigates to the DVWA security page using the application page object.
     */
    public void navigateToDvwaSecurity() {
        wait.until(ExpectedConditions.elementToBeClickable(dvwaSecurityMenu)).click();
    }

    /**
     * Navigates to the XSS reflected page using the application page object.
     */
    public void navigateToXssReflected() {
        wait.until(d -> {
            try {
                d.findElement(xssReflectedMenu).click();
                return true;
            } catch (StaleElementReferenceException e) {
                return false;
            }
        });
    }

    /**
     * Navigates to the XSS stored page using the application page object.
     */
    public void navigateToXssStored() {
        wait.until(ExpectedConditions.elementToBeClickable(xssStoredMenu)).click();
    }

    /**
     * Navigates to the csrf page using the application page object.
     */
    public void navigateToCsrf() {
        wait.until(ExpectedConditions.elementToBeClickable(csrfMenu)).click();
    }

    /**
     * Navigates to the file inclusion page using the application page object.
     */
    public void navigateToFileInclusion() {
        wait.until(ExpectedConditions.elementToBeClickable(fileInclusionMenu)).click();
    }

    /**
     * Navigates to the CSP bypass page using the application page object.
     */
    public void navigateToCspBypass() {
        wait.until(ExpectedConditions.elementToBeClickable(cspBypassMenu)).click();
    }

    /**
     * Navigates to the SQL injection blind page using the application page object.
     */
    public void navigateToSqlInjectionBlind() {
        wait.until(ExpectedConditions.elementToBeClickable(sqlInjectionBlindMenu)).click();
    }

    /**
     * Navigates to the weak session IDS page using the application page object.
     */
    public void navigateToWeakSessionIds() {
        wait.until(ExpectedConditions.elementToBeClickable(weakSessionMenu)).click();
    }

    /**
     * Navigates to the java script page using the application page object.
     */
    public void navigateToJavaScript() {
        wait.until(ExpectedConditions.elementToBeClickable(javascriptMenu)).click();
    }

    /**
     * Navigates to the PHP info page using the application page object.
     */
    public void navigateToPhpInfo() {
        wait.until(ExpectedConditions.elementToBeClickable(phpInfoMenu)).click();
    }

    /**
     * Navigates to the about page using the application page object.
     */
    public void navigateToAbout() {
        wait.until(ExpectedConditions.elementToBeClickable(aboutMenu)).click();
    }

    /**
     * Waits for DVWA to navigate to the specified application URL.
     *
     * @param url the URL to navigate to, inspect, or request
     */
    public void waitForPageLoad(String url) {
        wait.until(ExpectedConditions.urlContains(url));
    }
}
