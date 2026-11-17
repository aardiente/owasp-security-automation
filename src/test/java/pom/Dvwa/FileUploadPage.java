package pom.Dvwa;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;

/** Encapsulates DVWA's upload form and verifies the completed server response. */
public class FileUploadPage extends BasePage {
    private static final String ENDPOINT = "/vulnerabilities/upload/";
    private final By fileInput = By.name("uploaded");
    private final By uploadButton = By.name("Upload");
    private String lastUploadResponse;

    /**
     * Creates the DVWA file upload page object.
     * @param driver active DVWA browser
     */
    public FileUploadPage(WebDriver driver) { super(driver); }

    /**
     * Opens the upload exercise and waits for the file input to be present.
     * @param baseUrl root URL of the running DVWA application
     */
    public void open(String baseUrl) {
        lastUploadResponse = null;
        driver.get(baseUrl + ENDPOINT);
        wait.until(ExpectedConditions.presenceOfElementLocated(fileInput));
    }

    /**
     * Uploads the supplied local file and waits for DVWA's POST response.
     * A browser click can return before the response page is available, so
     * the wait examines fresh page source on each poll rather than holding
     * a WebElement across navigation. No JavaScript execution is required.
     *
     * @param file harmless local test file supplied by the TestNG test
     */
    public void submitFile(Path file) {
        lastUploadResponse = null;
        String expectedName = file.getFileName().toString().toLowerCase(Locale.ROOT);
        wait.until(ExpectedConditions.presenceOfElementLocated(fileInput))
                .sendKeys(file.toAbsolutePath().toString());
        wait.until(ExpectedConditions.elementToBeClickable(uploadButton)).click();

        try {
            lastUploadResponse = new WebDriverWait(driver, Duration.ofSeconds(20)).until(d -> {
                String source = d.getPageSource();
                if (source == null || source.isBlank()) return null;
                String text = source.toLowerCase(Locale.ROOT);
                // DVWA 1.9: the success text is misspelled "succesfully" in this image.
                boolean accepted = (text.contains("successfully uploaded") || text.contains("succesfully uploaded")) && text.contains(expectedName);
                // DVWA 1.9: rejected image types produce this explicit server message.
                boolean rejected = text.contains("your image was not uploaded")
                        && (text.contains("jpeg") || text.contains("png"));
                return accepted || rejected ? source : null;
            });
        } catch (TimeoutException e) {
            String source = driver.getPageSource();
            String detail = source == null ? "" : source.replaceAll("(?s)<[^>]*>", " ")
                    .replaceAll("\\s+", " ").trim();
            throw new IllegalStateException("DVWA did not return a recognizable upload result for "
                    + file.getFileName() + " at " + driver.getCurrentUrl() + ". Page excerpt: "
                    + detail.substring(0, Math.min(detail.length(), 300)), e);
        }
    }

    /**
     * Determines whether DVWA confirms that the submitted non-image file was stored.
     * Accepts the original DVWA 1.9 typo "succesfully" as well as "successfully".
     * Rejection is accepted only when DVWA explicitly says that the file type
     * is unsupported. An ambiguous response raises an automation error instead
     * of being reported as a secure result.
     *
     * @param fileName expected uploaded test file basename
     * @return true for a confirmed upload; false for an explicit file-type rejection
     */
    public boolean nonImageUploadAccepted(String fileName) {
        if (lastUploadResponse == null)
            throw new IllegalStateException("No completed upload response captured; call submitFile first.");

        String response = lastUploadResponse.toLowerCase(Locale.ROOT);
        if ((response.contains("successfully uploaded") || response.contains("succesfully uploaded"))
                && response.contains(fileName.toLowerCase(Locale.ROOT))
                && response.contains("hackable/uploads/")) return true;

        if (response.contains("your image was not uploaded")
                && (response.contains("jpeg") || response.contains("png"))) return false;

        throw new IllegalStateException("Unexpected DVWA upload response for " + fileName
                + " at " + driver.getCurrentUrl() + ". The completed page did not confirm upload or rejection.");
    }
}
