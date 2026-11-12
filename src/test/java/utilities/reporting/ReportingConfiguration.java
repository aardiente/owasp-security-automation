
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

final class ReportingConfiguration {

    private final Properties settings = new Properties();
    private final Path reportDirectory;
    private final Path screenshotDirectory;
    private final Path zapDirectory;
    private final Path assetSourceDirectory;
    private final String indexFile;
    private final String assessmentPattern;
    private final String testResultsFile;

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

    String required(String key) throws IOException {
        String value = settings.getProperty(key, "").trim();
        if (value.isEmpty()) throw new IOException("Missing reporting property: " + key);
        return value;
    }

    String optional(String key, String fallback) { return settings.getProperty(key, fallback).trim(); }

    Path reportDirectory() { return reportDirectory; }
    Path screenshotDirectory() { return screenshotDirectory; }
    Path assetSourceDirectory() { return assetSourceDirectory; }
    String assetFolderName() { return assetSourceDirectory.getFileName().toString(); }
    String indexFile() { return indexFile; }
    String testResultsFile() { return testResultsFile; }

    Path assessmentFile(String id) throws IOException {
        if (!id.matches("[A-Za-z0-9_-]+"))
            throw new IOException("Invalid reporting target identifier: " + id);
        return reportDirectory.resolve(fileName(assessmentPattern.replace("{id}", id)));
    }

    Path zapFile(String fileName) throws IOException {
        return zapDirectory.resolve(fileName(fileName));
    }

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

    private static Path resolve(Path base, String path) {
        Path value = Path.of(path);
        return (value.isAbsolute() ? value : base.resolve(value)).toAbsolutePath().normalize();
    }

    private static String fileName(String value) throws IOException {
        Path path = Path.of(value);
        if (path.isAbsolute() || path.getNameCount() != 1 ||
                !path.getFileName().toString().equals(value) || value.equals(".") || value.equals(".."))
            throw new IOException("Expected a filename without directories: " + value);
        return value;
    }
}
