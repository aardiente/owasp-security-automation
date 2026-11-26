
package utilities.reporting;

import org.testng.IReporter;
import org.testng.ISuite;
import org.testng.xml.XmlSuite;
import utilities.ScreenshotManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Exports per-suite TestNG execution records to CSV without launching new security scans.
 */
public class TestNgResultsReporter implements IReporter {

    /**
     * Writes a CSV summary of executed TestNG methods to the configured report location.
     *
     * <p>Writes a CSV with execution status, duration, error summary, screenshot filename, and security level; a suite-level parameter can select an alternate output name.</p>
     *
     * @param xmlSuites the TestNG XML suite definitions supplied to the reporter
     * @param suites the TestNG suite results supplied to the reporter
     * @param outputDirectory the default TestNG reporter output directory
     */
    @Override
    public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectory) {
        List<String> rows = new ArrayList<>();
        rows.add("Suite,Test,Class,Method,Status,DurationMs,Error,Screenshot,SecurityLevel");

        for (TestExecutions.Execution execution : TestExecutions.from(suites)) {
            Object captured = execution.result().getAttribute(ScreenshotManager.EVIDENCE_ATTRIBUTE);
            String screenshot = captured instanceof String filename ? filename : "";
            rows.add(String.join(",", ReportCsv.quote(execution.suite()), ReportCsv.quote(execution.test()),
                    ReportCsv.quote(execution.className()), ReportCsv.quote(execution.method()),
                    ReportCsv.quote(execution.status().name()), String.valueOf(execution.durationMs()),
                    ReportCsv.quote(execution.error()), ReportCsv.quote(screenshot),
                    ReportCsv.quote(execution.securityLevel())));
        }

        try {
            ReportingConfiguration config = ReportingConfiguration.load();
            String filename = config.testResultsFile();

            for (XmlSuite suite : xmlSuites) {
                String override = suite.getParameter("security.results.file");
                if (override == null || override.isBlank()) continue;

                if (!override.matches("[A-Za-z0-9][A-Za-z0-9._-]*\\.csv"))
                    throw new IOException("Invalid TestNG results filename: " + override);

                filename = override;
                break;
            }

            Path report = config.reportDirectory().resolve(filename);
            Files.createDirectories(report.getParent());
            Files.write(report, rows, StandardCharsets.UTF_8);

            System.out.println("[SECURITY REPORT] TestNG results saved: " +
                    report.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("[SECURITY REPORT] Failed to write TestNG results: " +
                    e.getMessage());
        }
    }
}
