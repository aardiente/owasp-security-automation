package utilities.reporting;

import org.testng.ISuite;
import org.testng.ISuiteResult;
import org.testng.ITestContext;
import org.testng.ITestResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Collects passed, failed and skipped test results in a consistent format for the HTML and CSV reports.
 */
final class TestExecutions {
    /**
     * Prevents creation of the result adapter.
     */
    private TestExecutions() {}

    /**
     * Lists the possible test results: passed, failed or skipped.
     */
    enum Status { PASSED, FAILED, SKIPPED }

    /**
     * Stores one test result and its suite, test group and status. Reads additional details from the original TestNG result.
     */
    record Execution(String suite, String test, Status status, ITestResult result) {
        /**
         * Returns the qualified test class name.
         */
        String className() { return result.getTestClass().getName(); }
        /**
         * Returns the TestNG method name.
         */
        String method() { return result.getMethod().getMethodName(); }
        /**
         * Returns the non-negative elapsed time in milliseconds.
         */
        long durationMs() { return Math.max(0, result.getEndMillis() - result.getStartMillis()); }
        /**
         * Reads the security level from the test's first input, if available. Other test inputs are not included in the report.
         *
         * @return the uppercase security level or an empty string if not available
         */
        String securityLevel() {
            Object[] parameters = result.getParameters();
            if (parameters == null || parameters.length == 0) return "";
            Object first = parameters[0];
            String value = first instanceof Enum<?> level ? level.name()
                    : first instanceof String text ? text : "";
            return switch (value.strip().toUpperCase(Locale.ROOT)) {
                case "LOW", "MEDIUM", "HIGH", "IMPOSSIBLE" -> value.strip().toUpperCase(Locale.ROOT);
                default -> "";
            };
        }
        /**
         * Returns the failure summary, or an empty string.
         */
        String error() { return result.getThrowable() == null ? "" : result.getThrowable().toString(); }
    }

    /**
     * Collects completed tests from the passed, failed and skipped result lists.
     */
    static List<Execution> from(List<ISuite> suites) {
        List<Execution> executions = new ArrayList<>();
        for (ISuite suite : suites) {
            for (ISuiteResult suiteResult : suite.getResults().values()) {
                ITestContext context = suiteResult.getTestContext();
                add(executions, suite.getName(), context.getName(), Status.PASSED,
                        context.getPassedTests().getAllResults());
                add(executions, suite.getName(), context.getName(), Status.FAILED,
                        context.getFailedTests().getAllResults());
                add(executions, suite.getName(), context.getName(), Status.SKIPPED,
                        context.getSkippedTests().getAllResults());
            }
        }
        return List.copyOf(executions);
    }

    /**
     * Adds executions sharing the same suite, test and status.
     */
    private static void add(List<Execution> executions, String suite, String test,
                            Status status, Collection<ITestResult> results) {
        for (ITestResult result : results) executions.add(new Execution(suite, test, status, result));
    }
}
