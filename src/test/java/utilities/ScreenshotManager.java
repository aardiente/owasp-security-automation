
package utilities;

import automation.AppConfig;
import automation.PresentationOverlay;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

public class ScreenshotManager {

    private static final Path SCREENSHOT_DIR = Path.of("target", "screenshots");
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");
    private static final AtomicLong SEQUENCE = new AtomicLong();

    private ScreenshotManager() {}

    public static void capture(WebDriver driver) {
        Path screenshotPath = nextScreenshotPath();

        try {
            byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            Files.write(screenshotPath, screenshot);
            System.out.println("[SCREENSHOT] Saved: " + screenshotPath.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to save screenshot: " + screenshotPath, e);
        }

        showResult(driver, screenshotPath);
    }

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

            byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            Files.write(screenshotPath, screenshot);

            System.out.println("[SQLi SCREENSHOT] Full webpage saved: " + screenshotPath.toAbsolutePath());

        } catch (Exception e) {
            System.out.println("[SQLi SCREENSHOT] Full-page capture failed: " + e.getMessage());

            try {
                Path fallback = nextScreenshotPath();
                Files.write(fallback, ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES));
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

            System.out.println("[XSS SCREENSHOT] Saved: " + screenshotPath.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to save XSS screenshot.", e);
        }

        showResult(driver, screenshotPath);
    }

    private static Path nextScreenshotPath() {
        ITestResult result = Reporter.getCurrentTestResult();
        if (result == null) throw new IllegalStateException("ScreenshotManager must be called from a TestNG test.");

        Class<?> testClass = result.getTestClass().getRealClass();
        String[] packages = testClass.getPackageName().split("\\.");

        String application = packages.length > 1 && packages[0].equalsIgnoreCase("tests")
                ? packages[1] : packages[packages.length - 1];

        Path directory = SCREENSHOT_DIR.resolve(sanitize(application.toLowerCase(Locale.ROOT)))
                .resolve(sanitize(testClass.getSimpleName()));

        String testName = sanitize(result.getMethod().getMethodName());
        String filename;

        if (application.equalsIgnoreCase("dvwa")) {
            Object[] parameters = result.getParameters();

            if (parameters.length > 0) {
                String level = sanitize(String.valueOf(parameters[0]));
                String hash = parameterHash(parameters);
                filename = testName + "-" + level + "-" + hash + ".png";
            } else {
                filename = testName + ".png";
            }
        } else {
            filename = testName + "-"
                    + LocalDateTime.now().format(TIMESTAMP) + "-"
                    + SEQUENCE.incrementAndGet() + ".png";
        }

        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create screenshot directory: " + directory, e);
        }

        return directory.resolve(filename);
    }

    private static String parameterHash(Object[] parameters) {
        try {
            String values = Arrays.deepToString(parameters);
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(values.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(Arrays.copyOf(digest, 6));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable.", e);
        }
    }

    private static String sanitize(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static void showResult(WebDriver driver, Path screenshotPath) {
        if (AppConfig.isSlowMoEnabled() && !AppConfig.isHeadless()) {
            PresentationOverlay.show(driver, "RESULT",
                    "ScreenshotManager.capture(driver);",
                    "Evidence captured: " + screenshotPath.getFileName());

            System.out.println("[PRESENTATION] Displaying result: " + screenshotPath.getFileName());
            AppConfig.pauseForResults();
        }
    }

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

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }
}
