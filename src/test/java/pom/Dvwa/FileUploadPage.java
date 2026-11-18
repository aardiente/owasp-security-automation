package pom.Dvwa;

import automation.ZapManager;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Encapsulates DVWA's upload form and verifies the completed server response. */
public class FileUploadPage extends BasePage {
    private static final String ENDPOINT = "/vulnerabilities/upload/";
    private final By fileInput = By.name("uploaded");
    private final By uploadButton = By.name("Upload");
    private String lastUploadResponse;
    private URI applicationRoot;
    private String submittedName;
    private String storedName;
    private static final Pattern STORED_FILE = Pattern.compile("hackable/uploads/([A-Za-z0-9._-]+)", Pattern.CASE_INSENSITIVE);

    /** Confirmed decision returned by the DVWA upload form. */
    public enum Status { ACCEPTED, REJECTED }

    /** Result of a submitted upload, including a short server-response excerpt. */
    public record UploadResult(Status status, String evidence) {}

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
        submittedName = null;
        storedName = null;
        applicationRoot = URI.create(baseUrl.endsWith("/") ? baseUrl : baseUrl + "/");
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
     * @return confirmed upload decision and a short server response excerpt
     */
    public UploadResult submitFile(Path file) {
        lastUploadResponse = null;
        storedName = null;
        submittedName = file.getFileName().toString().toLowerCase(Locale.ROOT);
        wait.until(ExpectedConditions.presenceOfElementLocated(fileInput))
                .sendKeys(file.toAbsolutePath().toString());
        wait.until(ExpectedConditions.elementToBeClickable(uploadButton)).click();

        try {
            lastUploadResponse = new WebDriverWait(driver, Duration.ofSeconds(20)).until(d -> {
                String source = d.getPageSource();
                if (source == null || source.isBlank()) return null;
                String text = source.toLowerCase(Locale.ROOT);
                // Impossible can rename valid uploads; use the path DVWA actually returned.
                boolean accepted = hasUploadSuccessText(text) && storedFileName(source) != null;
                // DVWA also has a short rejection message without the JPEG/PNG explanation.
                boolean rejected = text.contains("your image was not uploaded");
                // A token failure is an environment/setup error, never a passing security check.
                boolean tokenError = text.contains("csrf token is incorrect");
                return accepted || rejected || tokenError ? source : null;
            });
        } catch (TimeoutException e) {
            String source = driver.getPageSource();
            String detail = source == null ? "" : source.replaceAll("(?s)<[^>]*>", " ")
                    .replaceAll("\\s+", " ").trim();
            throw new IllegalStateException("DVWA did not return a recognizable upload result for "
                    + file.getFileName() + " at " + driver.getCurrentUrl() + ". Page excerpt: "
                    + detail.substring(0, Math.min(detail.length(), 300)), e);
        }

        if (lastUploadResponse.toLowerCase(Locale.ROOT).contains("csrf token is incorrect"))
            throw new IllegalStateException("DVWA rejected the upload form's CSRF token at " + driver.getCurrentUrl());
        boolean accepted = nonImageUploadAccepted(file.getFileName().toString());
        if (accepted) storedName = storedFileName(lastUploadResponse);
        String evidence = lastUploadResponse.replaceAll("(?s)<[^>]*>", " ")
                .replaceAll("\\s+", " ").trim();
        String normalized = evidence.toLowerCase(Locale.ROOT);
        int matchAt = normalized.indexOf(accepted ? "uploaded" : "not uploaded");
        int start = matchAt < 0 ? 0 : Math.max(0, matchAt - 120);
        int end = Math.min(evidence.length(), start + 240);
        return new UploadResult(accepted ? Status.ACCEPTED : Status.REJECTED, evidence.substring(start, end));
    }

    /**
     * Retrieves the file DVWA confirmed storing, through the same ZAP proxy used by Selenium.
     * This only downloads an uploaded file for inspection and never executes its contents.
     *
     * @return bytes served by the DVWA uploaded-file endpoint
     */
    public byte[] downloadStoredUpload() {
        if (applicationRoot == null || lastUploadResponse == null || submittedName == null)
            throw new IllegalStateException("Submit an upload before requesting the stored file.");
        if (!nonImageUploadAccepted(submittedName) || storedName == null)
            throw new IllegalStateException("Cannot retrieve an upload that DVWA did not confirm storing.");

        // Impossible may replace the supplied name with a random one.
        // Resolve the actual filename reported by DVWA rather than guessing it.
        URI fileUri = applicationRoot.resolve("hackable/uploads/" + storedName);
        URI proxyUri = URI.create("http://" + ZapManager.getProxyAddress());
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .proxy(ProxySelector.of(new InetSocketAddress(proxyUri.getHost(), proxyUri.getPort())))
                .build();
        HttpRequest request = HttpRequest.newBuilder(fileUri).timeout(Duration.ofSeconds(15)).GET().build();
        try {
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200)
                throw new IllegalStateException("DVWA upload retrieval returned HTTP " + response.statusCode()
                        + " for " + fileUri);
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while retrieving the stored upload", e);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Unable to retrieve stored upload through ZAP: " + fileUri, e);
        }
    }

    /**
     * Determines whether DVWA confirmed storing the submitted file.
     * Recognizes either success-message spelling and both long and short upload
     * rejection messages. The accepted file may have a server-generated name.
     * An ambiguous response raises an automation error, not a passing result.
     *
     * @param fileName local basename supplied by the TestNG test
     * @return true for a confirmed upload, false for an explicit rejection
     */
    public boolean nonImageUploadAccepted(String fileName) {
        if (lastUploadResponse == null)
            throw new IllegalStateException("No completed upload response captured; call submitFile first.");
        if (submittedName == null || !submittedName.equalsIgnoreCase(fileName))
            throw new IllegalArgumentException("Requested upload name differs from the submitted file: " + fileName);

        String response = lastUploadResponse.toLowerCase(Locale.ROOT);
        if (hasUploadSuccessText(response) && storedFileName(lastUploadResponse) != null) return true;
        if (response.contains("your image was not uploaded")) return false;
        throw new IllegalStateException("Unexpected DVWA upload response for " + fileName
                + " at " + driver.getCurrentUrl() + ". The completed page did not confirm upload or rejection.");
    }

    /**
     * Recognizes both the standard success wording and DVWA 1.9's misspelling.
     *
     * @param response page source normalized to lowercase
     * @return whether the response contains the server upload-success message
     */
    private static boolean hasUploadSuccessText(String response) {
        return response.contains("successfully uploaded") || response.contains("succesfully uploaded");
    }

    /**
     * Reads the actual stored filename from DVWA's own upload confirmation.
     * Impossible may assign a generated name instead of retaining the local filename.
     *
     * @param response HTML response containing the upload status
     * @return stored basename, or null if DVWA did not report a stored file
     */
    private static String storedFileName(String response) {
        Matcher match = STORED_FILE.matcher(response);
        return match.find() ? match.group(1) : null;
    }
}
