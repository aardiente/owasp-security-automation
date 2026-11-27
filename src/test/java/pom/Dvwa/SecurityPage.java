package pom.Dvwa;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

/**
 * Represents DVWA's security settings page. Lets tests select a security level and read the current settings.
 */
public class SecurityPage extends BasePage {

    public static final String securityEndpoint = "/security.php";

    private static final By SECURITY_LEVEL_DROPDOWN = By.name("security");
    private static final By SUBMIT_BUTTON = By.name("seclev_submit");
    private static final By TXT_INPUT_FIELD = By.cssSelector("input[type='text'], input[name='txt']");
    private static final By ACTION_SUBMIT_BUTTON = By.cssSelector("input[type='submit'], button[type='submit']");
    private static final By SECURITY_STATUS_LABEL = By.xpath("//p[contains(., 'Security level is currently:')]");
    private static final By PHPIDS_STATUS_LABEL = By.xpath("//p[contains(., 'PHPIDS is currently:')]");
    private static final By PHPIDS_TOGGLE_BUTTON = By.name("phpids_submit");
    private static final By SIMULATE_ATTACK_LINK = By.linkText("Simulate attack");
    private static final By VIEW_IDS_LOG_LINK = By.linkText("View IDS log");

    /**
     * Sets up the DVWA security page with the provided browser session.
     *
     * @param driver the Selenium WebDriver controlling the application browser
     */
    public SecurityPage(WebDriver driver) {
        super(driver);
    }

    /**
     * Changes the DVWA security level and waits for the submitted settings page to reload.
     *
     * @param lvl the selected DVWA security setting
     */
    public void setSecurityLevel(SecurityLevel lvl) {
        if (lvl == null) throw new IllegalArgumentException("Security level cannot be null");

        String expectedLevel = lvl.getSecurityLevel();
        wait.until(ExpectedConditions.urlContains(securityEndpoint));

        WebElement dropdown = wait.until(ExpectedConditions.visibilityOfElementLocated(SECURITY_LEVEL_DROPDOWN));
        new Select(dropdown).selectByValue(expectedLevel);

        wait.until(ExpectedConditions.elementToBeClickable(SUBMIT_BUTTON)).click();

        try {
            // An existing cookie can already match the requested level before the form reloads.
            wait.until(ExpectedConditions.stalenessOf(dropdown));
            wait.until(ExpectedConditions.urlContains(securityEndpoint));
            wait.until(d -> {
                Cookie cookie = d.manage().getCookieNamed("security");
                if (cookie == null || !expectedLevel.equalsIgnoreCase(cookie.getValue())) return false;

                WebElement currentDropdown = ExpectedConditions.visibilityOfElementLocated(SECURITY_LEVEL_DROPDOWN).apply(d);
                if (currentDropdown == null) return false;
                try {
                    return expectedLevel.equalsIgnoreCase(new Select(currentDropdown)
                            .getFirstSelectedOption().getAttribute("value"));
                } catch (org.openqa.selenium.StaleElementReferenceException e) {
                    return false;
                }
            });
        } catch (TimeoutException e) {
            Cookie cookie = driver.manage().getCookieNamed("security");
            String actualLevel = cookie == null ? "<missing>" : cookie.getValue();

            throw new IllegalStateException(
                    "DVWA security settings did not finish loading or the level was not saved. Expected: " + expectedLevel +
                            ", actual: " + actualLevel + ", URL: " + driver.getCurrentUrl(), e
            );
        }

        System.out.println("DVWA security level verified: " + expectedLevel);
    }

    /**
     * Types the provided text into the DVWA security module input.
     *
     * @param payload the test input submitted to the intentionally vulnerable lab application
     */
    public void enterPayload(String payload) {
        WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(TXT_INPUT_FIELD));
        element.clear();
        element.sendKeys(payload);
    }

    /**
     * Submits the payload currently entered in the security module.
     */
    public void clickSubmitPayload() {
        wait.until(ExpectedConditions.elementToBeClickable(ACTION_SUBMIT_BUTTON)).click();
    }

    /**
     * Toggles the DVWA PHP IDS setting through its control.
     */
    public void clickTogglePhpIds() {
        wait.until(ExpectedConditions.elementToBeClickable(PHPIDS_TOGGLE_BUTTON)).click();
    }

    /**
     * Triggers the DVWA IDS attack simulation control.
     */
    public void clickSimulateAttack() {
        wait.until(ExpectedConditions.elementToBeClickable(SIMULATE_ATTACK_LINK)).click();
    }

    /**
     * Opens the DVWA IDS log using its page control.
     */
    public void clickViewIdsLog() {
        wait.until(ExpectedConditions.elementToBeClickable(VIEW_IDS_LOG_LINK)).click();
    }

    /**
     * Reads the DVWA security level currently displayed on the settings page.
     *
     * @return the security-level label currently displayed by DVWA
     */
    public String getCurrentSecurityLevelText() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(SECURITY_STATUS_LABEL)).getText();
    }

    /**
     * Reads the PHP IDS protection status currently shown on the DVWA page.
     *
     * @return the current PHP IDS status text displayed by DVWA
     */
    public String getCurrentPhpIdsStatusText() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(PHPIDS_STATUS_LABEL)).getText();
    }

    /**
     * Enumeration of the four DVWA security configurations used by test data and the settings page.
     */
    public enum SecurityLevel {
        LOW("low"),
        MEDIUM("medium"),
        HIGH("high"),
        IMPOSSIBLE("impossible");

        private final String descriptor;

        /**
         * Stores the lowercase setting value represented by this security-level constant.
         *
         * @param str the descriptive value to store for the enum constant
         */
        SecurityLevel(String str) {
            descriptor = str;
        }

        /**
         * Returns the lowercase setting value used by the DVWA security form.
         *
         * @return the lower-case security level string
         */
        public String getSecurityLevel() {
            return descriptor;
        }

        /**
         * Returns the same lowercase setting value for display and test run details.
         *
         * @return the lower-case security level string
         */
        @Override
        public String toString() {
            return descriptor;
        }
    }
}