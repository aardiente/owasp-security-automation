
package automation;

import org.openqa.selenium.Dimension;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Reads browser, presentation, and ZAP settings from the bundled configuration, with system-property overrides. Invalid required values are rejected early.
 */
public final class AppConfig {

    private static final Properties properties = loadProperties();

    /**
     * Prevents direct instantiation of this utility class.
     */
    private AppConfig() {}

    /**
     * Loads the bundled application settings and rejects a missing or unreadable configuration resource.
     *
     * @return the loaded key/value configuration, including defaults from the bundled resource
     */
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

    /**
     * Resolves a required setting, allowing a JVM system property to override its configured value.
     *
     * <p>Reads system properties first and falls back to the bundled properties file; blank or missing values are rejected.</p>
     *
     * @param key the configuration property name to look up
     * @return the trimmed setting after applying any system-property override
     */
    private static String getProperty(String key) {
        String value = System.getProperty(key, properties.getProperty(key));
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("Missing configuration property: " + key);
        return value.trim();
    }

    /**
     * Reads a required boolean setting and rejects values other than true or false.
     *
     * @param key the configuration property name to look up
     * @return the validated boolean value of the requested setting
     */
    private static boolean getBoolean(String key) {
        String value = getProperty(key);
        if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false"))
            throw new IllegalArgumentException("Invalid boolean value for " + key + ": " + value);
        return Boolean.parseBoolean(value);
    }

    /**
     * Reads a required integer setting and enforces the caller-supplied minimum.
     *
     * @param key the configuration property name to look up
     * @param minimum the lowest accepted integer value for the setting
     * @return the configured integer after validating its minimum
     */
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

    /**
     * Reports whether Selenium should run without a visible browser window.
     *
     * <p>Checks whether headless is present or meets the expected condition.</p>
     *
     * @return {@code true} when the {@code browser.headless} setting is enabled; otherwise {@code false}
     */
    public static boolean isHeadless() {
        return getBoolean("browser.headless");
    }

    /**
     * Reads the configured browser viewport width in pixels.
     *
     * <p>Returns the configured browser viewport width in pixels.</p>
     *
     * @return the configured viewport width in pixels
     */
    public static int viewportWidth() {
        return getInt("browser.viewport.width", 1);
    }

    /**
     * Reads the configured browser viewport height in pixels.
     *
     * <p>Returns the configured browser viewport height in pixels.</p>
     *
     * @return the configured viewport height in pixels
     */
    public static int viewportHeight() {
        return getInt("browser.viewport.height", 1);
    }

    /**
     * Builds the Selenium Dimension used for the configured browser viewport.
     *
     * @return the width and height used for the browser window
     */
    public static Dimension viewport() {
        return new Dimension(viewportWidth(), viewportHeight());
    }

    /**
     * Reads the configured Selenium implicit wait interval in seconds.
     *
     * <p>Returns the configured Selenium implicit wait in seconds.</p>
     *
     * @return the implicit wait duration in seconds
     */
    public static int implicitWaitSeconds() {
        return getInt("browser.implicitWaitSeconds", 0);
    }

    /**
     * Reports whether the optional presentation delays and overlays are enabled.
     *
     * <p>Checks whether slow mo enabled is present or meets the expected condition.</p>
     *
     * @return {@code true} if slow-motion demonstration mode is enabled
     */
    public static boolean isSlowMoEnabled() {
        return getBoolean("browser.slowMo.enabled");
    }

    /**
     * Reads the configured pause between demonstrated browser actions.
     *
     * <p>Returns the configured delay applied between SlowMo interactions.</p>
     *
     * @return the pause duration in milliseconds
     */
    public static int slowMoDelayMs() {
        return getInt("browser.slowMo.delayMs", 0);
    }

    /**
     * Reads the configured time to display an action result during a demonstration.
     *
     * <p>Returns the duration for displaying selected results during a SlowMo demonstration.</p>
     *
     * @return the result display duration in milliseconds
     */
    public static int showResultsMs() {
        return getInt("browser.showResultsMs", 0);
    }

    /**
     * Reads the legacy active-scan enable switch; standalone suites explicitly start scans independently.
     *
     * <p>Returns the legacy active-scan flag; standalone TestNG scanners run independently of this setting.</p>
     *
     * @return {@code true} when the legacy active-scan property is enabled
     */
    public static boolean zapActiveEnabled() {
        return getBoolean("zap.active.enabled");
    }

    /**
     * Reads the Juice Shop standalone active-scan limit in minutes.
     *
     * <p>Returns the configured maximum Juice Shop active-scan duration in minutes.</p>
     *
     * @return the maximum allowed Juice Shop scan duration in minutes
     */
    public static int zapActiveMaxMinutes() {
        return getInt("zap.active.maxMinutes", 1);
    }

    /**
     * Resolves the configured Juice Shop active-scan status JSON destination.
     *
     * @return the configured path for the Juice Shop active-scan status JSON
     */
    public static Path zapActiveStatusOutput() {
        return Path.of(getProperty("zap.active.statusOutput"));
    }

    /**
     * Returns the report filename for the regular Juice Shop suite.
     *
     * @return the HTML report filename for a normal Juice Shop run
     */
    public static String zapPassiveReportName() {
        return getProperty("zap.passive.reportName");
    }

    /**
     * Returns the report filename for the standalone Juice Shop active scan.
     *
     * @return the HTML report filename for the standalone Juice Shop active run
     */
    public static String zapActiveReportName() {
        return getProperty("zap.active.reportName");
    }

    /**
     * Reads the DVWA standalone active-scan limit in minutes.
     *
     * <p>Returns the maximum duration for a standalone DVWA active scan.</p>
     *
     * @return the maximum allowed DVWA scan duration in minutes
     */
    public static int zapDvwaActiveMaxMinutes() {
        return getInt("zap.dvwa.active.maxMinutes", 1);
    }

    /**
     * Resolves the configured destination for DVWA active-scan status metadata.
     *
     * @return the configured path for the DVWA active-scan status JSON
     */
    public static Path zapDvwaActiveStatusOutput() {
        return Path.of(getProperty("zap.dvwa.active.statusOutput"));
    }

    /**
     * Returns the report filename for DVWA active findings.
     *
     * @return the HTML report filename for the standalone DVWA active run
     */
    public static String zapDvwaActiveReportName() {
        return getProperty("zap.dvwa.active.reportName");
    }

    /**
     * Returns the report filename for regular DVWA test traffic.
     *
     * @return the HTML report filename for a normal DVWA run
     */
    public static String zapDvwaPassiveReportName() {
        return getProperty("zap.dvwa.passive.reportName");
    }

    /**
     * Reports whether DVWA-only development container reuse is enabled.
     *
     * @return true when the DVWA development container should persist between JVM runs
     */
    public static boolean dvwaDevelopmentReuse() {
        return getBoolean("dvwa.development.reuse");
    }

    /**
     * Reads the Docker container name reserved for reusable DVWA development runs.
     *
     * @return the configurable DVWA development container name
     */
    public static String dvwaDevelopmentContainerName() {
        return getProperty("dvwa.development.containerName");
    }

    /**
     * Determines whether development mode should intentionally reset DVWA's database on this run.
     *
     * @return true when the DVWA setup step should run even for an existing development container
     */
    public static boolean dvwaDevelopmentResetDatabase() {
        return getBoolean("dvwa.development.resetDatabase");
    }

    /**
     * Pauses the current thread to show results when slow-motion mode is enabled.
     *
     * <p>Pauses supported presentation results only when SlowMo mode is enabled; preserves interruption.</p>
     */
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
