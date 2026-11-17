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
     * @param driver active DVWA browser
     */
    public XssStoredPage(WebDriver driver) { super(driver); }

    /**
     * Opens the stored-XSS guestbook and waits for the message form.
     * @param baseUrl DVWA application's base URL
     */
    public void open(String baseUrl) {
        driver.get(baseUrl + ENDPOINT);
        wait.until(ExpectedConditions.visibilityOfElementLocated(messageInput));
    }

    /**
     * Submits a harmless SVG event marker and uses its unique ID as the guestbook author.
     * The author name provides independent proof that the entry was saved.
     *
     * @param markerId unique identifier of this test's stored entry
     */
    public void submitMarkerProbe(String markerId) {
        validateMarker(markerId);
        String payload = "<svg id=" + markerId + " onload=\"this.dataset.p=1\">";
        WebElement name = wait.until(ExpectedConditions.visibilityOfElementLocated(nameInput));
        name.clear();
        name.sendKeys(markerId);
        WebElement message = wait.until(ExpectedConditions.visibilityOfElementLocated(messageInput));
        message.clear();
        message.sendKeys(payload);
        if (!payload.equals(message.getDomProperty("value")))
            throw new IllegalStateException("Guestbook message was truncated; cannot assess the XSS probe");
        wait.until(ExpectedConditions.elementToBeClickable(signButton)).click();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
    }

    /**
     * Reloads the guestbook to distinguish persisted execution from immediate reflection.
     * @param baseUrl DVWA application's base URL
     */
    public void reloadStoredEntries(String baseUrl) { open(baseUrl); }

    /**
     * Checks whether the unique guestbook entry actually survived a page reload.
     * The test must not report a pass if it never successfully saved its diagnostic entry.
     *
     * @param markerId identifier saved as the entry author
     * @return true if the reloaded guestbook visibly contains that author
     */
    public boolean entryWasPersisted(String markerId) {
        validateMarker(markerId);
        String content = driver.findElement(By.tagName("body")).getText();
        return content.contains("Name: " + markerId);
    }

    /**
     * Checks whether the saved SVG event handler executed in the browser.
     * @param markerId this test's unique SVG ID
     * @return true if the matching SVG set its own diagnostic data attribute
     */
    public boolean didStoredProbeExecute(String markerId) {
        validateMarker(markerId);
        try {
            return Boolean.TRUE.equals(new WebDriverWait(driver, Duration.ofSeconds(3)).until(d -> {
                var matches = d.findElements(By.id(markerId));
                return !matches.isEmpty() && "1".equals(matches.get(0).getDomAttribute("data-p"));
            }));
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * Rejects malformed marker IDs before including them in the test's HTML attributes.
     * @param markerId marker to validate
     */
    private void validateMarker(String markerId) {
        if (markerId == null || !markerId.matches("p[a-f0-9]{8}"))
            throw new IllegalArgumentException("Marker must match p followed by eight hex digits");
    }
}
