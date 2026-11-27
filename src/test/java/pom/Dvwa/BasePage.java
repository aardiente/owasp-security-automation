
package pom.Dvwa;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import automation.AppConfig;
import pom.common.BrowserPage;

import java.time.Duration;

/**
 * Provides common browser controls and navigation for DVWA pages. Shares browser waits and menu interactions across page classes.
 */
public abstract class BasePage extends BrowserPage {

    /**
     * Sets up the DVWA base page with the provided browser session. Uses the configured site-specific wait inherited from the base page.
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
     * Opens the DVWA Brute Force view through its navigation menu, waiting for the menu interaction to succeed.
     */
    public void navigateToBruteForce() {
        wait.until(ExpectedConditions.elementToBeClickable(bruteForceMenu)).click();
    }

    /**
     * Opens the DVWA Command Injection view through its navigation menu, waiting for the menu interaction to succeed.
     */
    public void navigateToCommandInjection() {
        wait.until(ExpectedConditions.elementToBeClickable(commandInjectionMenu)).click();
    }

    /**
     * Opens the DVWA SQL Injection view through its navigation menu, waiting for the menu interaction to succeed.
     */
    public void navigateToSqlInjection() {
        wait.until(ExpectedConditions.elementToBeClickable(sqlInjectionMenu)).click();
    }

    /**
     * Opens the DVWA Dvwa Security view through its navigation menu, waiting for the menu interaction to succeed.
     */
    public void navigateToDvwaSecurity() {
        wait.until(ExpectedConditions.elementToBeClickable(dvwaSecurityMenu)).click();
    }

    /**
     * Opens the DVWA XSS Reflected view through its navigation menu, waiting for the menu interaction to succeed.
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
     * Opens the DVWA XSS Stored view through its navigation menu, waiting for the menu interaction to succeed.
     */
    public void navigateToXssStored() {
        wait.until(ExpectedConditions.elementToBeClickable(xssStoredMenu)).click();
    }

    /**
     * Opens the DVWA CSRF view through its navigation menu, waiting for the menu interaction to succeed.
     */
    public void navigateToCsrf() {
        wait.until(ExpectedConditions.elementToBeClickable(csrfMenu)).click();
    }

    /**
     * Opens the DVWA File Inclusion view through its navigation menu, waiting for the menu interaction to succeed.
     */
    public void navigateToFileInclusion() {
        wait.until(ExpectedConditions.elementToBeClickable(fileInclusionMenu)).click();
    }

    /**
     * Opens the DVWA CSP Bypass view through its navigation menu, waiting for the menu interaction to succeed.
     */
    public void navigateToCspBypass() {
        wait.until(ExpectedConditions.elementToBeClickable(cspBypassMenu)).click();
    }

    /**
     * Opens the DVWA SQL Injection Blind view through its navigation menu, waiting for the menu interaction to succeed.
     */
    public void navigateToSqlInjectionBlind() {
        wait.until(ExpectedConditions.elementToBeClickable(sqlInjectionBlindMenu)).click();
    }

    /**
     * Opens the DVWA Weak Session IDs view through its navigation menu, waiting for the menu interaction to succeed.
     */
    public void navigateToWeakSessionIds() {
        wait.until(ExpectedConditions.elementToBeClickable(weakSessionMenu)).click();
    }

    /**
     * Opens the DVWA JavaScript view through its navigation menu, waiting for the menu interaction to succeed.
     */
    public void navigateToJavaScript() {
        wait.until(ExpectedConditions.elementToBeClickable(javascriptMenu)).click();
    }

    /**
     * Opens the DVWA PHP Info view through its navigation menu, waiting for the menu interaction to succeed.
     */
    public void navigateToPhpInfo() {
        wait.until(ExpectedConditions.elementToBeClickable(phpInfoMenu)).click();
    }

    /**
     * Opens the DVWA About view through its navigation menu, waiting for the menu interaction to succeed.
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
