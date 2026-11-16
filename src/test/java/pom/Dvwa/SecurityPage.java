
package pom.Dvwa;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

/**
 * DVWA page object exposing Selenium interactions for the security module.
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
     * Creates the security page component and initializes the dependencies required by its methods.
     *
     * <p>Creates a security page instance and initializes its dependencies.</p>
     *
     * @param driver the Selenium WebDriver controlling the application browser
     */
    public SecurityPage(WebDriver driver) {
        super(driver);
    }

    /**
     * Changes the DVWA security level through the security settings form.
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
            wait.until(d -> {
                Cookie cookie = d.manage().getCookieNamed("security");
                return cookie != null && expectedLevel.equalsIgnoreCase(cookie.getValue());
            });
        } catch (TimeoutException e) {
            Cookie cookie = driver.manage().getCookieNamed("security");
            String actualLevel = cookie == null ? "<missing>" : cookie.getValue();

            throw new IllegalStateException(
                    "DVWA security level was not saved. Expected: " + expectedLevel +
                            ", actual: " + actualLevel + ", URL: " + driver.getCurrentUrl(), e
            );
        }

        System.out.println("DVWA security level verified: " + expectedLevel);
    }

    /**
     * Types the supplied text into the DVWA security module input.
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

    public enum SecurityLevel {
        LOW("low"),
        MEDIUM("medium"),
        HIGH("high"),
        IMPOSSIBLE("impossible");

        private final String descriptor;

        /**
         * Creates the security level component and initializes the dependencies required by its methods.
         *
         * <p>Stores the configured display value of a DVWA security level.</p>
         *
         * @param str the descriptive value to store for the enum constant
         */
        SecurityLevel(String str) {
            descriptor = str;
        }

        /**
         * Returns the DVWA security-level code accepted by the settings form.
         *
         * <p>Returns the DVWA security level setting represented by this enum constant.</p>
         *
         * @return the lower-case security level string
         */
        public String getSecurityLevel() {
            return descriptor;
        }

        /**
         * Returns the DVWA security-level code accepted by the settings form.
         *
         * <p>Returns the display value for this DVWA security level.</p>
         *
         * @return the lower-case security level string
         */
        @Override
        public String toString() {
            return descriptor;
        }
    }
}
