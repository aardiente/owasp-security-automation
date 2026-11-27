package pom.Dvwa;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * Represents DVWA's session ID practice page. Provides access to its controls and results for the related tests.
 */
public class WeakSessionIdsPage extends BasePage {
    private static final String ENDPOINT = "/vulnerabilities/weak_id/";
    private static final String SESSION_COOKIE = "dvwaSession";
    private final By generateButton = By.name("generate");

    /**
     * Sets up the DVWA weak session ids page with the provided browser session.
     *
     * @param driver the active DVWA WebDriver
     */
    public WeakSessionIdsPage(WebDriver driver) { super(driver); }

    /**
     * Opens the weak-session-ID lab.
     *
     * @param baseUrl the running DVWA application's base URL
     */
    public void open(String baseUrl) {
        driver.get(baseUrl + ENDPOINT);
        wait.until(ExpectedConditions.elementToBeClickable(generateButton));
    }

    /**
     * Uses the page's Generate control and returns the browser's resulting DVWA session token.
     *
     * @return the newly issued dvwaSession cookie value
     */
    public String generateToken() {
        wait.until(ExpectedConditions.elementToBeClickable(generateButton)).click();
        Cookie cookie = driver.manage().getCookieNamed(SESSION_COOKIE);
        if (cookie == null || cookie.getValue().isBlank())
            throw new IllegalStateException("DVWA did not issue the expected dvwaSession cookie");
        return cookie.getValue();
    }
}
