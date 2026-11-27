
package utilities;

import automation.AppConfig;
import automation.PresentationOverlay;
import utilities.reporting.ReportingConfiguration;
import utilities.reporting.EvidencePolicy;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;
import org.testng.Reporter;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Captures and saves screenshots during tests. Associates each image with its test result and can display it during demonstrations.
 */
public class ScreenshotManager {

    public static final String EVIDENCE_ATTRIBUTE = "screenshot.evidenceFile";

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");
    private static final AtomicLong SEQUENCE = new AtomicLong();
    private static final Set<Path> CLEANED_DIRECTORIES = ConcurrentHashMap.newKeySet();
    private static final Pattern LEGACY_TIMESTAMP_NAME = Pattern.compile(".+-\\d{8}-\\d{6}-\\d{3}-\\d+\\.png");

    /**
     * Prevents direct creation of this utility class.
     */
    private ScreenshotManager() {}

    /**
     * Uses the same screenshot folder and target details as both report readers.
     */
    private static ReportingConfiguration reportingConfig() {
        try {
            return ReportingConfiguration.load();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to resolve screenshot directory", e);
        }
    }

    /**
     * Takes a screenshot of the browser and saves it with the current test result.
     *
     * @param driver the Selenium WebDriver controlling the application browser
     */
    public static void capture(WebDriver driver) {
        Path screenshotPath = nextScreenshotPath();

        try {
            writeBrowserEvidence(driver, screenshotPath);
            System.out.println("[SCREENSHOT] Saved: " + screenshotPath.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to save screenshot: " + screenshotPath, e);
        }

        showResult(driver, screenshotPath);
    }

    /**
     * Captures the SQL injection result view as screenshot evidence.
     *
     * @param driver the Selenium WebDriver controlling the application browser
     */
    public static void captureSqlResults(WebDriver driver) {
        Dimension originalSize = null;

        try {
            Path screenshotPath = nextScreenshotPath();
            originalSize = driver.manage().window().getSize();

            JavascriptExecutor js = (JavascriptExecutor) driver;

            Number pageWidth = (Number) js.executeScript(
                    "return Math.max(document.body.scrollWidth, document.documentElement.scrollWidth, document.documentElement.clientWidth);");

            Number pageHeight = (Number) js.executeScript(
                    "return Math.max(document.body.scrollHeight, document.documentElement.scrollHeight, document.documentElement.clientHeight);");

            int width = Math.max(originalSize.getWidth(), pageWidth.intValue() + 30);
            int height = Math.max(originalSize.getHeight(), pageHeight.intValue() + 150);

            driver.manage().window().setSize(new Dimension(width, height));

            writeBrowserEvidence(driver, screenshotPath);

            System.out.println("[SQLi SCREENSHOT] Full webpage saved: " + screenshotPath.toAbsolutePath());

        } catch (Exception e) {
            System.out.println("[SQLi SCREENSHOT] Full-page capture failed: " + e.getMessage());

            try {
                Path fallback = nextScreenshotPath();
                writeBrowserEvidence(driver, fallback);
                System.out.println("[SQLi SCREENSHOT] Browser fallback saved: " + fallback.toAbsolutePath());
            } catch (Exception fallbackError) {
                System.out.println("[SQLi SCREENSHOT] Unable to save screenshot: " + fallbackError.getMessage());
            }

        } finally {
            if (originalSize != null) {
                try {
                    driver.manage().window().setSize(originalSize);
                } catch (Exception e) {
                    System.out.println("[SQLi SCREENSHOT] Unable to restore browser size: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Captures visual evidence linked to a browser alert and its message.
     *
     * @param driver the Selenium WebDriver controlling the application browser
     * @param alertText the message displayed in the JavaScript alert being captured
     */
    public static void captureAlert(WebDriver driver, String alertText) {
        Path screenshotPath = nextScreenshotPath();

        try {
            byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            BufferedImage page = ImageIO.read(new ByteArrayInputStream(screenshot));
            if (page == null) throw new IOException("Invalid browser screenshot.");

            int bannerHeight = 110;
            BufferedImage evidence = new BufferedImage(
                    page.getWidth(), page.getHeight() + bannerHeight, BufferedImage.TYPE_INT_RGB);

            Graphics2D graphics = evidence.createGraphics();

            try {
                graphics.setColor(new Color(30, 41, 59));
                graphics.fillRect(0, 0, page.getWidth(), bannerHeight);

                graphics.setColor(Color.WHITE);
                graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
                graphics.drawString("DVWA Reflected XSS - Alert Detected", 20, 35);

                graphics.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 16));
                String message = "Alert: " + alertText.replace("\n", "\\n").replace("\r", "\\r");

                while (message.length() > 1
                        && graphics.getFontMetrics().stringWidth(message) > page.getWidth() - 40) {
                    message = message.substring(0, message.length() - 1);
                }

                graphics.drawString(message, 20, 70);
                graphics.drawImage(page, 0, bannerHeight, null);
            } finally {
                graphics.dispose();
            }

            if (!ImageIO.write(evidence, "png", screenshotPath.toFile())) {
                throw new IOException("PNG image writer unavailable.");
            }

            finalizeEvidence(screenshotPath);
            System.out.println("[XSS SCREENSHOT] Saved: " + screenshotPath.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to save XSS screenshot.", e);
        }

        showResult(driver, screenshotPath);
    }

    /**
     * Saves a browser viewport screenshot and associates it with the current test.
     *
     * @param driver browser providing screenshot bytes
     * @param destination resolved evidence path
     * @throws IOException if the screenshot cannot be written
     */
    private static void writeBrowserEvidence(WebDriver driver, Path destination) throws IOException {
        Files.write(destination, ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
        finalizeEvidence(destination);
    }

    /**
     * Applies the common evidence cleanup and TestNG details policy after a successful write.
     *
     * @param destination saved evidence path
     */
    private static void finalizeEvidence(Path destination) {
        cleanupLegacyScreenshots(destination);
        recordEvidence(destination);
    }

    /**
     * Associates an existing screenshot path with the current TestNG execution.
     *
     * @param screenshotPath the generated screenshot file to associate with the test
     */
    private static void recordEvidence(Path screenshotPath) {
        ITestResult result = Reporter.getCurrentTestResult();
        if (result != null) result.setAttribute(EVIDENCE_ATTRIBUTE, screenshotPath.getFileName().toString());
    }

    /**
     * Chooses where to save the screenshot using the configured naming rules and the current test details.
     *
     * @return the generated evidence path for the currently executing TestNG method
     */
    private static Path nextScreenshotPath() {
        ITestResult result = Reporter.getCurrentTestResult();
        if (result == null) throw new IllegalStateException("ScreenshotManager must be called from a TestNG test.");

        Class<?> testClass = result.getTestClass().getRealClass();
        ReportingConfiguration config = reportingConfig();
        EvidencePolicy policy;
        try {
            policy = config.evidencePolicyFor(testClass);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to resolve screenshot naming policy", e);
        }
        Path directory = config.screenshotDirectory().resolve(policy.folder())
                .resolve(EvidenceNames.sanitize(testClass.getSimpleName()));
        String method = result.getMethod().getMethodName();
        String filename = EvidenceNames.deterministicName(policy.naming(), method, result.getParameters());
        if (filename == null) {
            filename = EvidenceNames.sanitize(method) + "-" + LocalDateTime.now().format(TIMESTAMP)
                    + "-" + SEQUENCE.incrementAndGet() + ".png";
        }

        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create screenshot directory: " + directory, e);
        }

        return directory.resolve(filename);
    }

    /**
     * Removes old timestamped copies only for targets that opt into legacy cleanup.
     *
     * @param screenshotPath the generated screenshot file to associate with the test
     */
    private static void cleanupLegacyScreenshots(Path screenshotPath) {
        ITestResult result = Reporter.getCurrentTestResult();
        if (result == null) return;
        ReportingConfiguration config = reportingConfig();
        EvidencePolicy policy;
        try {
            policy = config.evidencePolicyFor(result.getTestClass().getRealClass());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to resolve legacy screenshot policy", e);
        }
        if (!policy.cleanupLegacy() || policy.naming() == EvidenceNames.Mode.TIMESTAMP) return;
        Path evidenceDirectory = config.screenshotDirectory().resolve(policy.folder()).toAbsolutePath().normalize();
        if (!screenshotPath.toAbsolutePath().normalize().startsWith(evidenceDirectory)
                || !CLEANED_DIRECTORIES.add(evidenceDirectory)) return;

        try (Stream<Path> files = Files.walk(evidenceDirectory)) {
            files.filter(Files::isRegularFile)
                    .filter(file -> LEGACY_TIMESTAMP_NAME.matcher(file.getFileName().toString()).matches())
                    .forEach(file -> {
                        try { Files.delete(file); }
                        catch (IOException e) {
                            System.err.println("[SCREENSHOT] Could not remove old screenshot "
                                    + file + ": " + e.getMessage());
                        }
                    });
        } catch (IOException e) {
            System.err.println("[SCREENSHOT] Could not clean old screenshots: " + e.getMessage());
        }
    }

    /**
     * Displays the captured screenshot in the optional presentation overlay.
     *
     * @param driver the Selenium WebDriver controlling the application browser
     * @param screenshotPath the generated screenshot file to associate with the test
     */
    private static void showResult(WebDriver driver, Path screenshotPath) {
        if (AppConfig.isPresentationEnabled()) {
            PresentationOverlay.show(driver, "RESULT",
                    "ScreenshotManager.capture(driver);",
                    "Evidence captured: " + screenshotPath.getFileName());

            System.out.println("[PRESENTATION] Displaying result: " + screenshotPath.getFileName());
            AppConfig.pauseForResults();
        }
    }

    /**
     * Displays an API response in a temporary browser page and captures a screenshot for the report.
     *
     * @param driver the Selenium WebDriver controlling the application browser
     * @param response the HTTP response to display as screenshot evidence
     */
    public static void captureResponse(WebDriver driver, HttpResponse<String> response) {
        String body = response.body() == null ? "" : response.body();

        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { background: #161616; color: #e5e5e5; padding: 24px; font-family: Consolas, monospace; }
                        h2 { color: #70cfff; }
                        pre { white-space: pre-wrap; overflow-wrap: anywhere; line-height: 1.5; }
                    </style>
                </head>
                <body>
                    <h2>HTTP Response</h2>
                    <p>Status: %d</p>
                    <p>URL: %s</p>
                    <hr>
                    <pre>%s</pre>
                </body>
                </html>
                """.formatted(response.statusCode(), escape(response.request().uri().toString()), escape(body));

        Path preview = null;

        try {
            preview = Files.createTempFile("http-response-", ".html");
            Files.writeString(preview, html, StandardCharsets.UTF_8);

            driver.get(preview.toUri().toString());
            capture(driver);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create HTTP response preview.", e);
        } finally {
            if (preview != null) {
                try {
                    Files.deleteIfExists(preview);
                } catch (IOException ignored) {}
            }
        }
    }

    /**
     * Escapes characters before embedding text in presentation markup.
     *
     * @param value the input value to convert or display
     * @return HTML-escaped text suitable for the screenshot preview
     */
    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }
}
