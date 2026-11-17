
package utilities.reporting;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * Loads reporting configuration and resolves directories used by
 * the HTML assessment generator and Security Assessment SPA.
 *
 * <p>Configuration is loaded from the test classpath by default.
 * An external properties file can be selected using the
 * security.reporting.config JVM property.</p>
 */
final class ReportingConfiguration {

    private final Properties settings = new Properties();
    private final Path reportDirectory;
    private final Path screenshotDirectory;
    private final Path zapDirectory;
    private final Path assetSourceDirectory;
    private final String indexFile;
    private final String assessmentPattern;
    private final String testResultsFile;

    /**
     * Validates loaded settings and resolves report and asset
     * directories relative to the project root.
     *
     * @param properties settings loaded from the classpath or
     *                   an explicitly selected file
     * @throws IOException if a required property is missing or
     *                     a configured filename is invalid
     */
    private ReportingConfiguration(Properties properties) throws IOException {
        settings.putAll(properties);

        Path projectRoot = Path.of("").toAbsolutePath().normalize();
        reportDirectory = resolve(projectRoot, required("report.outputDirectory"));
        screenshotDirectory = resolve(projectRoot, required("report.screenshotDirectory"));
        zapDirectory = resolve(projectRoot, required("report.zapDirectory"));
        assetSourceDirectory = resolve(projectRoot, required("report.assetDirectory"));
        indexFile = fileName(required("report.indexFile"));
        assessmentPattern = required("report.assessmentPattern");
        testResultsFile = fileName(required("report.testResultsFile"));

        if (!assessmentPattern.contains("{id}"))
            throw new IOException("report.assessmentPattern must contain {id}.");

        fileName(assessmentPattern.replace("{id}", "example"));
    }

    /**
     * Loads reporting configuration from the test classpath or an
     * explicitly selected external file.
     *
     * <p>The external file takes precedence when the JVM property
     * security.reporting.config is supplied.</p>
     *
     * @return validated reporting configuration
     * @throws IOException if configuration cannot be loaded or validated
     */
    static ReportingConfiguration load() throws IOException {
        Properties loaded = new Properties();
        String override = System.getProperty("security.reporting.config");

        if (override != null && !override.isBlank()) {
            Path path = Path.of(override).toAbsolutePath().normalize();

            if (!Files.isRegularFile(path))
                throw new IOException("Reporting configuration not found: " + path);

            try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                loaded.load(reader);
            }
        } else {
            String resourceName = System.getProperty(
                    "security.reporting.resource", "report-targets.properties");

            try (InputStream stream = ReportingConfiguration.class
                    .getClassLoader().getResourceAsStream(resourceName)) {

                if (stream == null)
                    throw new IOException(
                            "Reporting configuration resource not found: " + resourceName);

                loaded.load(stream);
            }
        }

        return new ReportingConfiguration(loaded);
    }

    /**
     * Retrieves a required configuration property.
     *
     * @param key the property name
     * @return the trimmed property value
     * @throws IOException if the property is absent or blank
     */
    String required(String key) throws IOException {
        String value = settings.getProperty(key, "").trim();

        if (value.isEmpty())
            throw new IOException("Missing reporting property: " + key);

        return value;
    }

    /**
     * Retrieves an optional configuration property.
     *
     * @param key the property name
     * @param fallback value used when the property is absent
     * @return the configured value or fallback
     */
    String optional(String key, String fallback) {
        return settings.getProperty(key, fallback).trim();
    }

    /**
     * Returns the directory where generated security assessment reports
     * are stored.
     *
     * @return directory containing generated assessment reports
     */
    Path reportDirectory() {
        return reportDirectory;
    }

    /**
     * Returns the directory containing screenshots captured during
     * automated security tests.
     *
     * @return directory containing screenshot evidence
     */
    Path screenshotDirectory() {
        return screenshotDirectory;
    }

    /**
     * Returns the configured directory containing CSS and JavaScript
     * assets for generated security reports.
     *
     * @return configured source directory for report assets
     */
    Path assetSourceDirectory() {
        return assetSourceDirectory;
    }

    /**
     * Returns the name of the directory used to store generated
     * report assets.
     *
     * @return name of the generated report assets directory
     */
    String assetFolderName() {
        return assetSourceDirectory.getFileName().toString();
    }

    /**
     * Returns the configured filename for the security assessment
     * index page.
     *
     * @return assessment index filename
     */
    String indexFile() {
        return indexFile;
    }

    /**
     * Returns the configured filename used to store TestNG execution
     * results as CSV.
     *
     * @return configured TestNG results filename
     */
    String testResultsFile() {
        return testResultsFile;
    }

    /**
     * Resolves the generated HTML report path for an application.
     *
     * @param id configured application identifier
     * @return path to the application's assessment report
     * @throws IOException if the application identifier or filename is invalid
     */
    Path assessmentFile(String id) throws IOException {
        if (!id.matches("[A-Za-z0-9_-]+"))
            throw new IOException("Invalid reporting target identifier: " + id);

        return reportDirectory.resolve(
                fileName(assessmentPattern.replace("{id}", id)));
    }

    /**
     * Resolves a ZAP report filename within the configured report directory.
     *
     * @param fileName name of the report
     * @return path to the ZAP report
     * @throws IOException if the filename is invalid
     */
    Path zapFile(String fileName) throws IOException {
        return zapDirectory.resolve(fileName(fileName));
    }

    /**
     * Retrieves the CSS and JavaScript assets used by generated reports.
     *
     * @return sorted list of report asset paths
     * @throws IOException if the asset directory is unavailable
     */
    List<Path> assets() throws IOException {
        if (!Files.isDirectory(assetSourceDirectory))
            throw new IOException(
                    "Reporting assets directory not found: " + assetSourceDirectory);

        try (Stream<Path> files = Files.list(assetSourceDirectory)) {
            List<Path> assets = new ArrayList<>(
                    files.filter(Files::isRegularFile)
                            .filter(p -> {
                                String name = p.getFileName().toString().toLowerCase();
                                return name.endsWith(".css") || name.endsWith(".js");
                            }).toList());

            assets.sort(Comparator.comparing(
                    p -> p.getFileName().toString()));

            return assets;
        }
    }

    /**
     * Resolves a relative or absolute path.
     *
     * @param base base directory for relative paths
     * @param path configured path
     * @return normalized absolute path
     */
    private static Path resolve(Path base, String path) {
        Path value = Path.of(path);

        return (value.isAbsolute()
                ? value
                : base.resolve(value)).toAbsolutePath().normalize();
    }

    /**
     * Validates a filename to prevent path traversal or absolute paths.
     *
     * @param value filename to validate
     * @return validated filename
     * @throws IOException if the value contains directory components
     */
    private static String fileName(String value) throws IOException {
        Path path = Path.of(value);

        if (path.isAbsolute() ||
                path.getNameCount() != 1 ||
                !path.getFileName().toString().equals(value) ||
                value.equals(".") ||
                value.equals(".."))
            throw new IOException(
                    "Expected a filename without directories: " + value);

        return value;
    }
}
