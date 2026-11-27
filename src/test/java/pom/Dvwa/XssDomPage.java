package pom.Dvwa;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Represents DVWA's DOM cross-site scripting practice page. Provides browser interactions and observations for the related tests.
 */
public class XssDomPage extends BasePage {
    private static final String ENDPOINT = "/vulnerabilities/xss_d/";

    /**
     * Sets up the DVWA xss dom page with the provided browser session.
     *
     * @param driver the active DVWA WebDriver
     */
    public XssDomPage(WebDriver driver) { super(driver); }

    /**
     * Opens the DOM XSS page with an inert SVG event probe that marks its own element.
     *
     * @param baseUrl the running DVWA application's base URL
     * @param markerId unique identifier of the test's SVG probe
     */
    public void openWithMarkerProbe(String baseUrl, String markerId) {
        validateMarker(markerId);
        String payload = "</option></select><svg id=" + markerId + " onload=\"this.dataset.p=1\">";
        driver.get(baseUrl + ENDPOINT + "?default=" + URLEncoder.encode(payload, StandardCharsets.UTF_8)
                .replace("+", "%20").replace("%2F", "/").replace("%3D", "=").replace("%3A", ":"));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
    }

    /**
     * Checks whether the event handler actually ran, not just whether its HTML was reflected.
     *
     * @param markerId unique identifier of the submitted SVG probe
     * @return true when browser execution set data-p on the inserted SVG
     */
    public boolean didProbeExecute(String markerId) {
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
     * Validates a marker before placing it inside a diagnostic HTML attribute.
     *
     * @param markerId unique alphanumeric marker
     */
    private void validateMarker(String markerId) {
        if (!markerId.matches("[A-Za-z][A-Za-z0-9]{3,24}"))
            throw new IllegalArgumentException("Invalid DOM XSS marker identifier");
    }
}
