
package automation;

import org.openqa.selenium.Dimension;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;

public final class AppConfig {

    private static final Properties properties = loadProperties();

    private AppConfig() {}

    private static Properties loadProperties() {
        Properties config = new Properties();

        try (InputStream stream = AppConfig.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (stream == null)
                throw new IllegalStateException("config.properties not found in src/test/resources");

            config.load(stream);
            return config;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load config.properties", e);
        }
    }

    private static String getProperty(String key) {
        String value = System.getProperty(key, properties.getProperty(key));
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("Missing configuration property: " + key);
        return value.trim();
    }

    private static boolean getBoolean(String key) {
        String value = getProperty(key);
        if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false"))
            throw new IllegalArgumentException("Invalid boolean value for " + key + ": " + value);
        return Boolean.parseBoolean(value);
    }

    private static int getInt(String key, int minimum) {
        int value;
        try {
            value = Integer.parseInt(getProperty(key));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer value for " + key, e);
        }
        if (value < minimum)
            throw new IllegalArgumentException(key + " must be at least " + minimum);
        return value;
    }

    public static boolean isHeadless() {
        return getBoolean("browser.headless");
    }

    public static int viewportWidth() {
        return getInt("browser.viewport.width", 1);
    }

    public static int viewportHeight() {
        return getInt("browser.viewport.height", 1);
    }

    public static Dimension viewport() {
        return new Dimension(viewportWidth(), viewportHeight());
    }

    public static int implicitWaitSeconds() {
        return getInt("browser.implicitWaitSeconds", 0);
    }

    public static boolean isSlowMoEnabled() {
        return getBoolean("browser.slowMo.enabled");
    }

    public static int slowMoDelayMs() {
        return getInt("browser.slowMo.delayMs", 0);
    }

    public static int showResultsMs() {
        return getInt("browser.showResultsMs", 0);
    }

    public static boolean zapActiveEnabled() {
        return getBoolean("zap.active.enabled");
    }

    public static int zapActiveMaxMinutes() {
        return getInt("zap.active.maxMinutes", 1);
    }

    public static Path zapActiveStatusOutput() {
        return Path.of(getProperty("zap.active.statusOutput"));
    }

    public static String zapPassiveReportName() {
        return getProperty("zap.passive.reportName");
    }

    public static String zapActiveReportName() {
        return getProperty("zap.active.reportName");
    }

    public static int zapDvwaActiveMaxMinutes() {
        return getInt("zap.dvwa.active.maxMinutes", 1);
    }

    public static Path zapDvwaActiveStatusOutput() {
        return Path.of(getProperty("zap.dvwa.active.statusOutput"));
    }

    public static String zapDvwaActiveReportName() {
        return getProperty("zap.dvwa.active.reportName");
    }

    public static String zapDvwaPassiveReportName() {
        return getProperty("zap.dvwa.passive.reportName");
    }

    public static void pauseForResults() {
        if (!isSlowMoEnabled()) return;

        int delay = showResultsMs();
        if (delay == 0) return;

        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Result display pause interrupted", e);
        }
    }
}
