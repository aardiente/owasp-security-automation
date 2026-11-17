package pom.Dvwa;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.nio.file.Path;

/** Provides the controls and result inspection for DVWA's file-upload lab. */
public class FileUploadPage extends BasePage {
    private static final String ENDPOINT = "/vulnerabilities/upload/";
    private final By fileInput = By.name("uploaded");
    private final By uploadButton = By.name("Upload");
    private final By uploadedLinks = By.cssSelector(".vulnerable_code_area a[href]");

    /**
     * Creates the file-upload page object.
     * @param driver the active DVWA WebDriver
     */
    public FileUploadPage(WebDriver driver) { super(driver); }

    /**
     * Opens the file-upload lab and locates the input.
     * @param baseUrl the running DVWA application's base URL
     */
    public void open(String baseUrl) {
        driver.get(baseUrl + ENDPOINT);
        wait.until(ExpectedConditions.presenceOfElementLocated(fileInput));
    }

    /**
     * Submits a local benign file through the DVWA upload form.
     * @param file absolute path to the file being evaluated
     */
    public void submitFile(Path file) {
        wait.until(ExpectedConditions.presenceOfElementLocated(fileInput)).sendKeys(file.toAbsolutePath().toString());
        wait.until(ExpectedConditions.elementToBeClickable(uploadButton)).click();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
    }

    /**
     * Checks that DVWA returned a link to a file stored in its uploads directory.
     * @param fileName basename of the submitted file
     * @return true when an uploaded-file link matches that filename
     */
    public boolean hasUploadedFileLink(String fileName) {
        for (WebElement link : driver.findElements(uploadedLinks)) {
            String href = link.getAttribute("href");
            if (href != null && href.contains("hackable/uploads/") && href.endsWith("/" + fileName)) return true;
        }
        return false;
    }
}
