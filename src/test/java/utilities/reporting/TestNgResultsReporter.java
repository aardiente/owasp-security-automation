package utilities.reporting;

import org.testng.IReporter;
import org.testng.ISuite;
import org.testng.ISuiteResult;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.xml.XmlSuite;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TestNgResultsReporter implements IReporter {

    private static final Path REPORT = Path.of("target", "security-reports", "testng-results.csv");

    @Override
    public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectory) {
        List<String> rows = new ArrayList<>();
        rows.add("Suite,Test,Class,Method,Status,DurationMs,Error");

        for (ISuite suite : suites) {
            for (ISuiteResult suiteResult : suite.getResults().values()) {
                ITestContext context = suiteResult.getTestContext();
                addResults(rows, suite.getName(), context.getName(), context.getPassedTests().getAllResults(), "PASSED");
                addResults(rows, suite.getName(), context.getName(), context.getFailedTests().getAllResults(), "FAILED");
                addResults(rows, suite.getName(), context.getName(), context.getSkippedTests().getAllResults(), "SKIPPED");
            }
        }

        try {
            Files.createDirectories(REPORT.getParent());
            Files.write(REPORT, rows, StandardCharsets.UTF_8);
            System.out.println("[SECURITY REPORT] TestNG results saved: " + REPORT.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("[SECURITY REPORT] Failed to write TestNG results: " + e.getMessage());
        }
    }

    private void addResults(List<String> rows, String suite, String test,
                            Collection<ITestResult> results, String status) {
        for (ITestResult result : results) {
            String className = result.getTestClass().getName();
            String method = result.getMethod().getMethodName();
            long duration = Math.max(0, result.getEndMillis() - result.getStartMillis());
            String error = result.getThrowable() == null ? "" : result.getThrowable().toString();

            rows.add(String.join(",",
                    csv(suite), csv(test), csv(className), csv(method),
                    csv(status), String.valueOf(duration), csv(error)));
        }
    }

    private String csv(String value) {
        if (value == null) return "\"\"";
        return "\"" + value.replace("\"", "\"\"")
                .replace("\r", "\\r").replace("\n", "\\n") + "\"";
    }
}