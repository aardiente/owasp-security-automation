
package automation;

import org.openqa.selenium.Dimension;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Manages the project's settings. Reads the configuration file and allows settings to be overridden while the program is running.
 */
public final class AppConfig {

    private static final Properties properties = loadProperties();

    /**
     * Prevents this shared configuration class from being created as an object.
     */
    private AppConfig() {}

    /**
     * Loads the configuration file when the class starts. Reports an error if the file cannot be read.
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
     * Reads a required setting, using a runtime override when one is available. Rejects missing or empty settings.
     *
     * @param key the configuration property name to look up
     * @return the trimmed setting after applying any runtime override
     */
    private static String getProperty(String key) {
        String value = System.getProperty(key, properties.getProperty(key));
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("Missing configuration property: " + key);
        return value.trim();
    }

    /**
     * Reads a true or false setting and rejects invalid values.
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
     * Reads a whole-number setting and checks that it meets the required minimum.
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
     * Checks whether tests should run without a visible browser window.
     *
     * @return {@code true} when the {@code browser.headless} setting is enabled; otherwise {@code false}
     */
    public static boolean isHeadless() {
        return getBoolean("browser.headless");
    }

    /**
     * Returns the configured browser width in pixels.
     *
     * @return the configured viewport width in pixels
     */
    public static int viewportWidth() {
        return getInt("browser.viewport.width", 1);
    }

    /**
     * Returns the configured browser height in pixels.
     *
     * @return the configured viewport height in pixels
     */
    public static int viewportHeight() {
        return getInt("browser.viewport.height", 1);
    }

    /**
     * Combines the configured width and height for browser sizing.
     *
     * @return the width and height used for the browser window
     */
    public static Dimension viewport() {
        return new Dimension(viewportWidth(), viewportHeight());
    }

    /**
     * Returns the implicit wait time used by new browser sessions.
     *
     * @return the implicit wait duration in seconds
     */
    public static int implicitWaitSeconds() {
        return getInt("browser.implicitWaitSeconds", 0);
    }

    /**
     * Determines whether demonstration timing and visual presentation features are enabled.
     *
     * @return {@code true} if slow-motion demonstration mode is enabled
     */
    public static boolean isSlowMoEnabled() {
        return getBoolean("browser.slowMo.enabled");
    }

    /**
     * Checks whether visible demonstration effects should run.
     *
     * @return true only when slow motion is enabled and the browser is visible
     */
    public static boolean isPresentationEnabled() {
        return !isHeadless() && isSlowMoEnabled();
    }

    /**
     * Returns the current interaction delay in milliseconds. The live presentation controller can override this value without restarting the suite.
     *
     * @return the pause duration in milliseconds
     */
    public static int slowMoDelayMs() {
        return getInt("browser.slowMo.delayMs", 0);
    }

    /**
     * Returns the current duration in milliseconds for holding a visible result before execution continues or the browser closes.
     *
     * @return the result display duration in milliseconds
     */
    public static int showResultsMs() {
        return getInt("browser.showResultsMs", 0);
    }

    /**
     * Reads the older active-scan setting. Standalone scan suites do not depend on this setting.
     *
     * @return {@code true} when the legacy active-scan property is enabled
     */
    public static boolean zapActiveEnabled() {
        return getBoolean("zap.active.enabled");
    }

    /**
     * Returns the time limit for the standalone Juice Shop active scan, in minutes.
     *
     * @return the maximum allowed Juice Shop scan duration in minutes
     */
    public static int zapActiveMaxMinutes() {
        return getInt("zap.active.maxMinutes", 1);
    }

    /**
     * Finds the configured Juice Shop active-scan status JSON destination.
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
     * Returns the time limit for the standalone DVWA active scan, in minutes.
     *
     * @return the maximum allowed DVWA scan duration in minutes
     */
    public static int zapDvwaActiveMaxMinutes() {
        return getInt("zap.dvwa.active.maxMinutes", 1);
    }

    /**
     * Finds the configured destination for DVWA active-scan status details.
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
     * Keeps the current result visible for the configured time when slow-motion mode is enabled. Skips the pause when the delay is zero and handles interruptions without losing their status.
     */
    public static void pauseForResults() {
        if (!isPresentationEnabled()) return;

        int delay = showResultsMs();
        if (delay == 0) return;

        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Result display pause interrupted", e);
        }
    }

    /**
     * Returns the configured output path for the final Juice Shop challenge catalog.
     */
    public static Path juiceShopChallengesOutput() { return Path.of(getProperty("juiceshop.challenges.output")); }

    /**
     * Returns the configured local DVWA lab username.
     */
    public static String dvwaUsername() { return getProperty("dvwa.username"); }

    /**
     * Returns the configured local DVWA lab password.
     */
    public static String dvwaPassword() { return getProperty("dvwa.password"); }

    /**
     * Returns the configured Juice Shop Docker image.
     */
    public static String juiceShopImage() { return getProperty("juiceshop.image"); }

    /**
     * Returns the configured Juice Shop internal service port.
     */
    public static int juiceShopPort() { return getInt("juiceshop.port", 1); }

    /**
     * Returns the configured DVWA Docker image.
     */
    public static String dvwaImage() { return getProperty("dvwa.image"); }

    /**
     * Returns the configured DVWA internal service port.
     */
    public static int dvwaPort() { return getInt("dvwa.port", 1); }

    /**
     * Returns the default data directory for external spreadsheet overrides.
     */
    public static Path testDataDirectory() { return Path.of(getProperty("test.data.directory")); }

    /**
     * Finds the default spreadsheet or an explicitly configured override.
     *
     * @param key system property for the specific spreadsheet
     * @return path or project resources resource name consumed by ExcelFileReader
     */
    public static String testWorkbook(String key) {
        String override = System.getProperty(key);
        if (override != null && !override.isBlank()) return override.trim();
        return testDataDirectory().resolve(getProperty(key)).toString().replace(java.io.File.separatorChar, '/');
    }

    /**
     * Returns the configured base wait duration for Juice Shop pages, in seconds.
     */
    public static int juiceShopPageWaitSeconds() { return getInt("juiceshop.page.waitSeconds", 1); }

    /**
     * Returns the configured base wait duration for DVWA pages, in seconds.
     */
    public static int dvwaPageWaitSeconds() { return getInt("dvwa.page.waitSeconds", 1); }

    /**
     * Returns the configured default ZAP report filename.
     */
    public static String zapDefaultReportName() { return getProperty("zap.default.reportName"); }

    /**
     * Returns the output directory for ZAP HTML reports.
     */
    public static Path zapHtmlReportDirectory() { return Path.of(getProperty("zap.htmlReportDirectory")); }

    /**
     * Returns the connection timeout used by the existing Juice Shop API client.
     */
    public static int juiceShopApiConnectSeconds() { return getInt("juiceshop.api.connectSeconds", 1); }

    /**
     * Returns the timeout for existing Juice Shop JSON requests.
     */
    public static int juiceShopApiRequestSeconds() { return getInt("juiceshop.api.requestSeconds", 1); }

    /**
     * Returns the timeout for existing Juice Shop multipart requests.
     */
    public static int juiceShopApiUploadSeconds() { return getInt("juiceshop.api.uploadSeconds", 1); }

    /**
     * Returns the user-agent value shared by existing Juice Shop requests.
     */
    public static String juiceShopApiUserAgent() { return getProperty("juiceshop.api.userAgent"); }
}
