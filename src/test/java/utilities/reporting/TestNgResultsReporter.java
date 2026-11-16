
package utilities.reporting;

import org.testng.IReporter;
import org.testng.ISuite;
import org.testng.ISuiteResult;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.xml.XmlSuite;
import utilities.ScreenshotManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Exports per-suite TestNG execution records to CSV without launching new security scans.
 */
public class TestNgResultsReporter implements IReporter {

    /**
     * Writes a CSV summary of executed TestNG methods to the configured report location.
     *
     * <p>Writes a CSV with execution status, duration, error summary, and screenshot filename; a suite-level parameter can select an alternate output name.</p>
     *
     * @param xmlSuites the TestNG XML suite definitions supplied to the reporter
     * @param suites the TestNG suite results supplied to the reporter
     * @param outputDirectory the default TestNG reporter output directory
     */
    @Override
    public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectory) {
        List<String> rows = new ArrayList<>();
        rows.add("Suite,Test,Class,Method,Status,DurationMs,Error,Screenshot");

        for (ISuite suite : suites) {
            for (ISuiteResult suiteResult : suite.getResults().values()) {
                ITestContext context = suiteResult.getTestContext();
                addResults(rows, suite.getName(), context.getName(),
                        context.getPassedTests().getAllResults(), "PASSED");
                addResults(rows, suite.getName(), context.getName(),
                        context.getFailedTests().getAllResults(), "FAILED");
                addResults(rows, suite.getName(), context.getName(),
                        context.getSkippedTests().getAllResults(), "SKIPPED");
            }
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

    /**
     * Appends normalized CSV rows for a group of TestNG results.
     *
     * @param rows the rows of previously parsed test or CSV data
     * @param suite the TestNG suite label associated with the result
     * @param test the TestNG test label or execution data being recorded
     * @param results the TestNG execution results to include in the report
     * @param status the HTTP status code or test execution status being recorded
     */
    private void addResults(List<String> rows, String suite, String test,
                            Collection<ITestResult> results, String status) {
        for (ITestResult result : results) {
            String className = result.getTestClass().getName();
            String method = result.getMethod().getMethodName();
            long duration = Math.max(0, result.getEndMillis() - result.getStartMillis());
            String error = result.getThrowable() == null ? "" :
                    result.getThrowable().toString();

            Object captured = result.getAttribute(ScreenshotManager.EVIDENCE_ATTRIBUTE);
            String screenshot = captured instanceof String filename ? filename : "";

            rows.add(String.join(",",
                    csv(suite), csv(test), csv(className), csv(method),
                    csv(status), String.valueOf(duration), csv(error), csv(screenshot)));
        }
    }

    /**
     * Quotes and escapes a single CSV value.
     *
     * @param value the input value to convert or display
     * @return the CSV-escaped field, with quotes doubled and line breaks normalized
     */
    private String csv(String value) {
        if (value == null) return "\"\"";
        return "\"" + value.replace("\"", "\"\"")
                .replace("\r", "\\r").replace("\n", "\\n") + "\"";
    }
}
