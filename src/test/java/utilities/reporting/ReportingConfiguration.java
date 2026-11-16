
package utilities.reporting;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * Loads reporting paths and per-target settings, and resolves files used by the HTML and SPA reporters.
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
     * Creates the reporting configuration component and initializes the dependencies required by its methods.
     *
     * <p>Reads and validates reporting paths and filenames from the selected properties file.</p>
     *
     * @param configFile the reporting properties file from which settings are loaded
     * @throws IOException if a required file or HTTP stream cannot be read or written
     */
    private ReportingConfiguration(Path configFile) throws IOException {
        if (!Files.isRegularFile(configFile))
            throw new IOException("Reporting configuration not found: " + configFile);

        try (var reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            settings.load(reader);
        }

        Path projectRoot = Path.of("").toAbsolutePath().normalize();
        reportDirectory = resolve(projectRoot, required("report.outputDirectory"));
        screenshotDirectory = resolve(projectRoot, required("report.screenshotDirectory"));
        zapDirectory = resolve(projectRoot, required("report.zapDirectory"));
        assetSourceDirectory = resolve(configFile.getParent(), required("report.assetDirectory"));
        indexFile = fileName(required("report.indexFile"));
        assessmentPattern = required("report.assessmentPattern");
        testResultsFile = fileName(required("report.testResultsFile"));

        if (!assessmentPattern.contains("{id}"))
            throw new IOException("report.assessmentPattern must contain {id}.");
        fileName(assessmentPattern.replace("{id}", "example"));
    }

    /**
     * Locates and loads reporting configuration, with support for a JVM property override.
     *
     * @return reporting settings loaded from the bundled configuration file
     * @throws IOException if a required file or HTTP stream cannot be read or written
     */
    static ReportingConfiguration load() throws IOException {
        String override = System.getProperty("security.reporting.config");
        Path configFile;

        if (override != null && !override.isBlank()) {
            configFile = Path.of(override).toAbsolutePath().normalize();
        } else {
            Path sourceRoot = Path.of(System.getProperty("security.reporting.sourceRoot", "src/test/java"));
            Path packageDirectory = sourceRoot.resolve(
                            ReportingConfiguration.class.getPackageName().replace('.', '/'))
                    .toAbsolutePath().normalize();

            if (!Files.isDirectory(packageDirectory))
                throw new IOException("Reporting package directory not found: " + packageDirectory);

            List<Path> candidates;
            try (Stream<Path> files = Files.list(packageDirectory)) {
                candidates = files.filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().endsWith(".properties"))
                        .sorted().toList();
            }

            List<Path> matches = new ArrayList<>();
            for (Path candidate : candidates) {
                Properties properties = new Properties();
                try (var reader = Files.newBufferedReader(candidate, StandardCharsets.UTF_8)) {
                    properties.load(reader);
                }
                if (properties.containsKey("targets") && properties.containsKey("report.outputDirectory"))
                    matches.add(candidate);
            }

            if (matches.size() != 1)
                throw new IOException("Expected one reporting configuration in " + packageDirectory +
                        " but found " + matches.size() + ". Use -Dsecurity.reporting.config to select one.");

            configFile = matches.get(0);
        }

        return new ReportingConfiguration(configFile);
    }

    /**
     * Returns a required reporting property or raises an error if it is missing.
     *
     * @param key the configuration property name to look up
     * @return the nonblank required property value
     * @throws IOException if a required file or HTTP stream cannot be read or written
     */
    String required(String key) throws IOException {
        String value = settings.getProperty(key, "").trim();
        if (value.isEmpty()) throw new IOException("Missing reporting property: " + key);
        return value;
    }

    /**
     * Returns a reporting property, falling back when it is not configured.
     *
     * @param key the configuration property name to look up
     * @param fallback the default value returned when an optional setting is absent
     * @return the configured value, or the fallback when absent
     */
    String optional(String key, String fallback) { return settings.getProperty(key, fallback).trim(); }

    /**
     * Returns the configured directory for generated report artifacts.
     *
     * @return the resolved output directory for security reports
     */
    Path reportDirectory() { return reportDirectory; }
    /**
     * Returns the configured directory for screenshot evidence.
     *
     * @return the resolved directory containing screenshot evidence
     */
    Path screenshotDirectory() { return screenshotDirectory; }
    /**
     * Returns the source directory containing report assets.
     *
     * @return the directory containing frontend CSS/JavaScript assets
     */
    Path assetSourceDirectory() { return assetSourceDirectory; }
    /**
     * Returns the output subfolder name used for copied CSS and JavaScript assets.
     *
     * @return the configured output folder name for assets
     */
    String assetFolderName() { return assetSourceDirectory.getFileName().toString(); }
    /**
     * Returns the configured HTML assessment index filename.
     *
     * @return the configured index page filename
     */
    String indexFile() { return indexFile; }
    /**
     * Returns the configured TestNG CSV filename.
     *
     * @return the configured TestNG CSV filename
     */
    String testResultsFile() { return testResultsFile; }

    /**
     * Builds and validates the report path for a target application.
     *
     * @param id the configured identifier to look up
     * @return the generated per-target HTML assessment path
     * @throws IOException if a required file or HTTP stream cannot be read or written
     */
    Path assessmentFile(String id) throws IOException {
        if (!id.matches("[A-Za-z0-9_-]+"))
            throw new IOException("Invalid reporting target identifier: " + id);
        return reportDirectory.resolve(fileName(assessmentPattern.replace("{id}", id)));
    }

    /**
     * Resolves a validated ZAP report filename within the configured report directory.
     *
     * @param fileName the configured source or output filename
     * @return the resolved path to the requested ZAP JSON file
     * @throws IOException if a required file or HTTP stream cannot be read or written
     */
    Path zapFile(String fileName) throws IOException {
        return zapDirectory.resolve(fileName(fileName));
    }

    /**
     * Lists the CSS and JavaScript assets used by generated report pages.
     *
     * @return the configured asset source paths
     * @throws IOException if a required file or HTTP stream cannot be read or written
     */
    List<Path> assets() throws IOException {
        if (!Files.isDirectory(assetSourceDirectory))
            throw new IOException("Reporting assets directory not found: " + assetSourceDirectory);

        try (Stream<Path> files = Files.list(assetSourceDirectory)) {
            List<Path> assets = new ArrayList<>(files.filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString().toLowerCase();
                        return name.endsWith(".css") || name.endsWith(".js");
                    }).toList());
            assets.sort(Comparator.comparing(p -> p.getFileName().toString()));
            return assets;
        }
    }

    /**
     * Resolves a configured relative or absolute path to a normalized absolute path.
     *
     * @param base the directory used to resolve the requested relative path
     * @param path the path or URL passed to the request or file operation
     * @return the file path for resolve
     */
    private static Path resolve(Path base, String path) {
        Path value = Path.of(path);
        return (value.isAbsolute() ? value : base.resolve(value)).toAbsolutePath().normalize();
    }

    /**
     * Validates that a configured report name is a filename without directory traversal.
     *
     * @param value the input value to convert or display
     * @return a validated filename without path traversal components
     * @throws IOException if a required file or HTTP stream cannot be read or written
     */
    private static String fileName(String value) throws IOException {
        Path path = Path.of(value);
        if (path.isAbsolute() || path.getNameCount() != 1 ||
                !path.getFileName().toString().equals(value) || value.equals(".") || value.equals(".."))
            throw new IOException("Expected a filename without directories: " + value);
        return value;
    }
}
