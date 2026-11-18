package pom.Dvwa;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Models benign local-file inclusion attempts and classifies the observed DVWA response. */
public class FileInclusionPage extends BasePage {
    private static final String ENDPOINT = "/vulnerabilities/fi/";

    /** Classifies the result of a local information-page inclusion probe. */
    public enum InclusionResult { INCLUDED, REJECTED, INCONCLUSIVE }

    /**
     * Creates the DVWA file-inclusion page object.
     * @param driver active DVWA browser
     */
    public FileInclusionPage(WebDriver driver) { super(driver); }

    /**
     * Requests DVWA's own benign PHP-information page using a mode-appropriate relative path.
     * The Medium probe checks whether path-filtering normalizes traversal before or after filtering.
     * No operating-system files, credentials, or external URLs are requested.
     *
     * @param baseUrl DVWA application's base URL
     * @param level current DVWA security mode
     */
    public void requestLocalInformationPage(String baseUrl, SecurityPage.SecurityLevel level) {
        if (level == null) throw new IllegalArgumentException("Security level cannot be null");
        String sample = switch (level) {
            case LOW -> "../../phpinfo.php";
            case MEDIUM -> "....//....//phpinfo.php";
            case HIGH, IMPOSSIBLE -> "file:///var/www/html/phpinfo.php";
        };
        driver.get(baseUrl + ENDPOINT + "?page=" + URLEncoder.encode(sample, StandardCharsets.UTF_8));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
    }

    /**
     * Distinguishes included PHP diagnostic output from explicit rejection and empty results.
     * Empty/ambiguous output must not be treated as proof that file inclusion was blocked.
     *
     * @return observed inclusion result
     */
    public InclusionResult inclusionResult() {
        String text = driver.findElement(By.tagName("body")).getText().toLowerCase(Locale.ROOT);
        if (text.contains("php version")) return InclusionResult.INCLUDED;
        if (text.contains("error: file not found") || text.contains("file not found"))
            return InclusionResult.REJECTED;
        return InclusionResult.INCONCLUSIVE;
    }
}
