package utilities.Logs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.util.Arrays;

/**
 * Records when tests start and finish, including whether they pass, fail or are skipped.
 */
public class TestLogListener implements ITestListener {

    // Uses the "tests" logger configured in log4j2.xml so everything writes to logs/tests.log
    private static final Logger log = LoggerFactory.getLogger("tests");

    /**
     * Logs the start of a TestNG test group.
     *
     * @param context the TestNG context for the current suite or test
     */
    @Override
    public void onStart(ITestContext context)
    {
        log.info("=== Starting Test Suite: {} ===", context.getName());
    }

    /**
     * Logs when an individual test starts.
     *
     * @param result the TestNG test result whose status or evidence is recorded
     */
    @Override
    public void onTestStart(ITestResult result)
    {
        Object[] params = result.getParameters();
        if (params.length > 0)
        {
            log.info("STARTING TEST: {}.{} | Payload/Params: {}",
                    result.getTestClass().getRealClass().getSimpleName(),
                    result.getMethod().getMethodName(),
                    Arrays.toString(params));
        } else
        {
            log.info("STARTING TEST: {}.{}",
                    result.getTestClass().getRealClass().getSimpleName(),
                    result.getMethod().getMethodName());
        }
    }

    /**
     * Logs a passed test.
     *
     * @param result the TestNG test result whose status or evidence is recorded
     */
    @Override
    public void onTestSuccess(ITestResult result)
    {
        log.info("PASSED: {}.{} ({} ms)",
                result.getTestClass().getRealClass().getSimpleName(),
                result.getMethod().getMethodName(),
                result.getEndMillis() - result.getStartMillis());
    }

    /**
     * Logs a failed test.
     *
     * @param result the TestNG test result whose status or evidence is recorded
     */
    @Override
    public void onTestFailure(ITestResult result)
    {
        // Logs the exact failure and the full exception stack trace automatically
        log.error("FAILED: {}.{} | Params: {}",
                result.getTestClass().getRealClass().getSimpleName(),
                result.getMethod().getMethodName(),
                Arrays.toString(result.getParameters()),
                result.getThrowable());
    }

    /**
     * Logs a skipped test.
     *
     * @param result the TestNG test result whose status or evidence is recorded
     */
    @Override
    public void onTestSkipped(ITestResult result)
    {
        log.warn("SKIPPED: {}.{}",
                result.getTestClass().getRealClass().getSimpleName(),
                result.getMethod().getMethodName(),
                result.getThrowable());
    }

    /**
     * Logs the end of a TestNG test group.
     *
     * @param context the TestNG context for the current suite or test
     */
    @Override
    public void onFinish(ITestContext context)
    {
        log.info("=== Finished Suite: {} | Passed: {}, Failed: {}, Skipped: {} ===",
                context.getName(),
                context.getPassedTests().size(),
                context.getFailedTests().size(),
                context.getSkippedTests().size());
    }
}