package pom.Dvwa;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Models the DVWA local file inclusion page and its rendered include result. */
public class FileInclusionPage extends BasePage {
    private static final String ENDPOINT = "/vulnerabilities/fi/";
    private static final String SAFE_SAMPLE = "../../phpinfo.php";

    /**
     * Creates the file inclusion page object.
     * @param driver the active DVWA WebDriver
     */
    public FileInclusionPage(WebDriver driver) { super(driver); }

    /**
     * Opens the file inclusion lab and requests DVWA's own harmless PHP information page via traversal.
     * @param baseUrl the running DVWA application's base URL
     */
    public void requestLocalInformationPage(String baseUrl) {
        driver.get(baseUrl + ENDPOINT + "?page=" + URLEncoder.encode(SAFE_SAMPLE, StandardCharsets.UTF_8));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
    }

    /**
     * Determines whether the page contains diagnostic PHP information from the included sample.
     * @return true when the local PHP information content is visibly included
     */
    public boolean renderedLocalInformation() {
        return driver.findElement(By.tagName("body")).getText().toLowerCase(Locale.ROOT).contains("php version");
    }
}
