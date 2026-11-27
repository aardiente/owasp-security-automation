package pom.Dvwa;

import automation.ZapManager;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents DVWA's stored cross-site scripting practice page. Provides access to the guestbook and collects results for the related tests.
 */
public class XssStoredPage extends BasePage {
    /**
     * Guestbook field identifier used to associate a spreadsheet case with the intended lab field.
     */
    public enum ProbeField { NAME, MESSAGE }

    private static final String ENDPOINT = "/vulnerabilities/xss_s/";
    private final By nameInput = By.name("txtName");
    private final By messageInput = By.name("mtxMessage");
    private final By signButton = By.name("btnSign");

    /**
     * Sets up the DVWA xss stored page with the provided browser session.
     *
     * @param driver current test browser
     */
    public XssStoredPage(WebDriver driver) { super(driver); }

    /**
     * Opens the guestbook and waits for the message field.
     *
     * @param baseUrl running application URL
     */
    public void open(String baseUrl) {
        driver.get(baseUrl + ENDPOINT);
        wait.until(ExpectedConditions.visibilityOfElementLocated(messageInput));
    }

    /**
     * Submits a spreadsheet-based, self-marking probe with a unique plain-text identifier.
     */
    public void submitMarkerProbe(String markerId, ProbeField field, String payloadTemplate, String baseUrl) {
        validateMarker(markerId);
        if (field == null) throw new IllegalArgumentException("Probe field cannot be null");
        if (payloadTemplate == null || !payloadTemplate.contains("{{MARKER}}"))
            throw new IllegalArgumentException("Stored-XSS payload must include {{MARKER}}");
        String payload = payloadTemplate.replace("{{MARKER}}", markerId);

        if (field == ProbeField.NAME) {
            // DVWA's Name input has a client-side maxlength of 10.
            // The authenticated request checks the server's actual validation through ZAP.
            submitNameProbe(baseUrl, payload, markerId);
        } else {
            WebElement name = wait.until(ExpectedConditions.visibilityOfElementLocated(nameInput));
            name.clear();
            name.sendKeys(markerId);
            WebElement message = wait.until(ExpectedConditions.visibilityOfElementLocated(messageInput));
            message.clear();
            message.sendKeys(payload);
            if (!payload.equals(message.getDomProperty("value")))
                throw new IllegalStateException("Guestbook Message payload was truncated for marker " + markerId + ": " + payload);
            wait.until(ExpectedConditions.elementToBeClickable(signButton)).click();
            // Confirm the submitted entry is present before navigating away from the POST response.
            // If the server rejected it, the test still records evidence and fails on persistence.
            try { wait.until(ExpectedConditions.presenceOfElementLocated(entryLocator(markerId, field))); }
            catch (TimeoutException ignored) { /* Persistence is checked after the reload. */ }
        }
    }

    /**
     * Routes a Name-field probe through ZAP with the current authenticated browser session.
     */
    private void submitNameProbe(String baseUrl, String payload, String markerId) {
        String cookieHeader = driver.manage().getCookies().stream()
                .map(cookie -> cookie.getName() + "=" + cookie.getValue())
                .collect(Collectors.joining("; "));
        if (cookieHeader.isBlank()) throw new IllegalStateException("DVWA session cookies are missing");

        String form = "txtName=" + encode(payload) + "&mtxMessage=" + encode(markerId)
                + "&btnSign=" + encode("Sign Guestbook");
        List<WebElement> tokens = driver.findElements(By.name("user_token"));
        if (!tokens.isEmpty()) form += "&user_token=" + encode(tokens.get(0).getDomProperty("value"));

        URI proxyUri = URI.create("http://" + ZapManager.getProxyAddress());
        HttpClient client = HttpClient.newBuilder()
                .proxy(ProxySelector.of(new InetSocketAddress(proxyUri.getHost(), proxyUri.getPort())))
                .connectTimeout(Duration.ofSeconds(10)).build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + ENDPOINT))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Cookie", cookieHeader)
                .POST(HttpRequest.BodyPublishers.ofString(form)).build();
        try {
            int status = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            if (status < 200 || status >= 400)
                throw new IllegalStateException("Guestbook Name probe POST returned HTTP " + status);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted submitting stored-XSS Name probe", e);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to submit stored-XSS Name probe through ZAP", e);
        }
    }

    /**
     * Encodes an existing form value for HTTP submission.
     *
     * @param value form field value
     * @return URL-encoded value
     */
    private String encode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }

    /**
     * Reloads to distinguish stored execution from the immediate submission response.
     */
    public void reloadStoredEntries(String baseUrl) { open(baseUrl); }

    /**
     * Locates the exact guestbook entry by its plain-text identifier, not by old probes.
     */
    private By entryLocator(String markerId, ProbeField field) {
        validateMarker(markerId);
        if (field == null) throw new IllegalArgumentException("Probe field cannot be null");
        String text = (field == ProbeField.NAME ? "Message: " : "Name: ") + markerId;
        return By.xpath("//div[@id='guestbook_comments'][contains(., '" + text + "')]");
    }

    /**
     * Checks whether the uniquely identified guestbook entry is present.
     *
     * @param markerId identifier of this test run
     * @param field guestbook field under assessment
     * @return whether the entry was found
     */
    public boolean entryWasPersisted(String markerId, ProbeField field) {
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(entryLocator(markerId, field)));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * Checks the current probe's DOM execution signal instead of matching earlier entries.
     */
    public boolean didStoredProbeExecute(String markerId, ProbeField field) {
        By locator = entryLocator(markerId, field);
        try {
            return Boolean.TRUE.equals(new WebDriverWait(driver, Duration.ofSeconds(3)).until(d -> {
                List<WebElement> entries = d.findElements(locator);
                if (entries.isEmpty()) return false;
                return entries.get(0).findElements(By.id(markerId)).stream()
                        .anyMatch(element -> "1".equals(element.getDomAttribute("data-p")));
            }));
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * Scrolls the browser to the current test's entry before ScreenshotManager captures it.
     */
    public void focusStoredEntry(String markerId, ProbeField field) {
        WebElement entry = wait.until(ExpectedConditions.presenceOfElementLocated(entryLocator(markerId, field)));
        List<WebElement> markers = entry.findElements(By.id(markerId));
        new Actions(driver).moveToElement(markers.isEmpty() ? entry : markers.get(0)).perform();
    }

    /**
     * Rejects malformed test markers before they are used in selectors.
     *
     * @param markerId identifier to validate
     */
    private void validateMarker(String markerId) {
        if (markerId == null || !markerId.matches("p[a-f0-9]{8}"))
            throw new IllegalArgumentException("Marker must match p followed by eight hex digits");
    }
}
