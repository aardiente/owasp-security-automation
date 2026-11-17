package pom.Dvwa;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/** Encapsulates DVWA guestbook entry and harmless stored-XSS execution evidence. */
public class XssStoredPage extends BasePage {
    private static final String ENDPOINT = "/vulnerabilities/xss_s/";
    private final By nameInput = By.name("txtName");
    private final By messageInput = By.name("mtxMessage");
    private final By signButton = By.name("btnSign");

    /**
     * Creates the stored-XSS guestbook page object.
     * @param driver the active DVWA WebDriver
     */
    public XssStoredPage(WebDriver driver) { super(driver); }

    /**
     * Opens the guestbook page and waits for its form.
     * @param baseUrl the running DVWA application's base URL
     */
    public void open(String baseUrl) {
        driver.get(baseUrl + ENDPOINT);
        wait.until(ExpectedConditions.visibilityOfElementLocated(messageInput));
    }

    /**
     * Submits a short, inert SVG event probe via the guestbook's message field.
     * The probe only sets an attribute on its own element and makes no network requests.
     * @param markerId unique identifier for this individual test run
     */
    public void submitMarkerProbe(String markerId) {
        if (!markerId.matches("p[a-f0-9]{8}")) throw new IllegalArgumentException("Expected p followed by 8 hex digits");
        String payload = "<svg id=" + markerId + " onload=\"this.dataset.p=1\">";
        WebElement name = wait.until(ExpectedConditions.visibilityOfElementLocated(nameInput));
        name.clear();
        name.sendKeys("dvwa-test");
        WebElement message = wait.until(ExpectedConditions.visibilityOfElementLocated(messageInput));
        message.clear();
        message.sendKeys(payload);
        if (!payload.equals(message.getDomProperty("value")))
            throw new IllegalStateException("Guestbook message was truncated; cannot validate stored XSS probe");
        wait.until(ExpectedConditions.elementToBeClickable(signButton)).click();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
    }

    /**
     * Reloads the guestbook to confirm execution comes from a persisted entry.
     * @param baseUrl the running DVWA application's base URL
     */
    public void reloadStoredEntries(String baseUrl) { open(baseUrl); }

    /**
     * Checks for an SVG marker whose event handler actually ran on the reloaded page.
     * @param markerId identifier of this test's guestbook entry
     * @return true when the stored handler executed in the browser
     */
    public boolean didStoredProbeExecute(String markerId) {
        try {
            return Boolean.TRUE.equals(new WebDriverWait(driver, Duration.ofSeconds(3)).until(d -> {
                var matches = d.findElements(By.id(markerId));
                return !matches.isEmpty() && "1".equals(matches.get(0).getDomAttribute("data-p"));
            }));
        } catch (TimeoutException e) {
            return false;
        }
    }
}
