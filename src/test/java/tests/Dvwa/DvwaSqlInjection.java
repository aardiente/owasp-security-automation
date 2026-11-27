package tests.Dvwa;

import automation.ZapManager;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import pom.Dvwa.SecurityPage;
import pom.Dvwa.SqlInjectionPage;
import utilities.ScreenshotManager;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Contains DVWA SQL injection tests. Uses spreadsheet cases to evaluate protections at each security level.
 */
public class DvwaSqlInjection extends BaseDvwaTest {
    private static final Pattern RESULT_ROW = Pattern.compile(
            "(?m)First name:[ \\t]*([^\\r\\n]*)\\r?\\n[ \\t]*Surname:[ \\t]*([^\\r\\n]*)");
    private static final Pattern SQL_ERROR = Pattern.compile(
            "(?i)sql syntax|unknown column|mysqli error|sqlstate|error in your sql|sqlite.*error"
                    + "|the used select statements|different number of columns|column count doesn't match"
                    + "|operand should contain|syntax error");
    private static final List<String> SIGNALS = List.of(
            "CANARY", "ROW_EXPANSION", "BOOLEAN_DIFFERENTIAL", "PREFIX_CANARY", "SQL_ERROR", "TIME_DELAY");

    /**
     * Fixed spreadsheet-based SQL injection validation case and expected observation details.
     */
    private record SqlCase(String id, EnumSet<SecurityPage.SecurityLevel> levels, String payload,
                           String signal, String expectedFirst, String expectedLast, String controlPayload,
                           long minDelayMs, String technique) {
        /**
         * Returns the stable spreadsheet case identifier for reports.
         *
         * @return case identifier
         */
        @Override public String toString() { return id; }
    }
    /**
     * Fixed pair of display values from a lab result row.
     */
    private record ResultRow(String firstName, String surname) {}
    /**
     * Fixed observation collected for SQL injection validation, including response text, parsed rows, and error information.
     */
    private record ProbeResult(String text, List<ResultRow> rows, boolean sqlError,
                               HttpResponse<String> response, long durationMs) {}

    /**
     * Loads and validates SQLi spreadsheet cases in the original case-first order.
     *
     * @return security-level and case pairs for TestNG
     */
    @DataProvider(name = "sqliCases")
    public Object[][] sqliCases() {
        Object[][] rows = DvwaCases.load("dvwa.sqli.workbook", null);
        List<Object[]> tests = new ArrayList<>();
        for (int i = 0; i < rows.length; i++) {
            String id = DvwaCases.cell(rows[i], 0), levels = DvwaCases.cell(rows[i], 1), payload = DvwaCases.cell(rows[i], 2);
            String signal = DvwaCases.cell(rows[i], 3).toUpperCase(Locale.ROOT);
            String expectedFirst = DvwaCases.cell(rows[i], 4), expectedLast = DvwaCases.cell(rows[i], 5);
            String control = DvwaCases.cell(rows[i], 6), delayText = DvwaCases.cell(rows[i], 7), technique = DvwaCases.cell(rows[i], 8);
            if (id.isBlank() || levels.isBlank() || payload.isBlank() || !SIGNALS.contains(signal))
                throw new IllegalStateException("Invalid SQLi case at workbook row " + (i + 2) + ": " + id);
            if ((signal.equals("CANARY") || signal.equals("PREFIX_CANARY"))
                    && (expectedFirst.isBlank() || expectedLast.isBlank()))
                throw new IllegalStateException("Case " + id + " requires both expected result markers");
            if ((signal.equals("BOOLEAN_DIFFERENTIAL") || signal.equals("TIME_DELAY")) && control.isBlank())
                throw new IllegalStateException("Case " + id + " requires a comparison/control payload");
            if (control.isBlank()) control = "0";
            long minDelay = delayText.isBlank() ? 1400 : Long.parseLong(delayText);
            if (minDelay < 0 || minDelay > 10000)
                throw new IllegalStateException("Invalid minimum delay for " + id + ": " + minDelay);

            EnumSet<SecurityPage.SecurityLevel> selected = DvwaCases.levels(levels, i + 2);
            SqlCase test = new SqlCase(id, selected, payload, signal, expectedFirst, expectedLast, control,
                    minDelay, technique.isBlank() ? signal : technique);
            DvwaCases.appendCase(tests, test, selected);
        }
        if (tests.isEmpty()) throw new IllegalStateException("SQLi workbook produced no test cases");
        return tests.toArray(Object[][]::new);
    }

    /**
     * Compares the existing test and control observations and asserts the expected protection.
     *
     * @param level DVWA security mode
     * @param test validated spreadsheet case
     */
    @Test(dataProvider = "sqliCases")
    public void sqlInjectionValidation(SecurityPage.SecurityLevel level, SqlCase test) {
        WebDriver driver = ZapManager.getDriver();
        prepareSecurityLevel(level);

        SqlInjectionPage page = new SqlInjectionPage(driver);
        ProbeResult baseline = submit(page, level, "0");
        Assert.assertFalse(baseline.sqlError(), "Control ID 0 produced a SQL error at " + level);
        Assert.assertTrue(baseline.rows().isEmpty(), "Control ID 0 unexpectedly returned rows at " + level);
        ProbeResult comparison = test.controlPayload().equals("0")
                ? baseline : submit(page, level, test.controlPayload());
        Assert.assertFalse(comparison.sqlError(), "Comparison request returned a SQL error for " + test.id());
        ProbeResult injected = submit(page, level, test.payload());

        long firstDeltaMs = injected.durationMs() - comparison.durationMs();
        long repeatDeltaMs = -1;
        boolean confirmed = switch (test.signal()) {
            case "CANARY" -> hasCanary(injected, test, false);
            case "PREFIX_CANARY" -> hasCanary(injected, test, true);
            case "ROW_EXPANSION" -> injected.rows().size() > baseline.rows().size();
            case "BOOLEAN_DIFFERENTIAL" -> comparison.rows().isEmpty()
                    && injected.rows().size() > comparison.rows().size();
            case "SQL_ERROR" -> injected.sqlError();
            case "TIME_DELAY" -> !injected.rows().isEmpty() && injected.rows().equals(comparison.rows())
                    && firstDeltaMs >= test.minDelayMs();
            default -> throw new IllegalStateException("Unsupported SQLi signal: " + test.signal());
        };

        // A second paired observation prevents a single slow UI/navigation response from counting as time-based SQLi.
        ProbeResult evidence = injected;
        if (test.signal().equals("TIME_DELAY") && confirmed) {
            ProbeResult repeatControl = submit(page, level, test.controlPayload());
            ProbeResult repeatInjected = submit(page, level, test.payload());
            repeatDeltaMs = repeatInjected.durationMs() - repeatControl.durationMs();
            confirmed = !repeatControl.sqlError() && !repeatInjected.sqlError()
                    && !repeatInjected.rows().isEmpty() && repeatInjected.rows().equals(repeatControl.rows())
                    && repeatDeltaMs >= test.minDelayMs();
            evidence = repeatInjected;
        }

        boolean maskedError = injected.text().contains("Something went wrong.");
        Reporter.log("SQLi | level=" + level + " | case=" + test.id() + " | technique=" + test.technique()
                + " | signal=" + test.signal() + " | controlRows=" + comparison.rows().size()
                + " | injectedRows=" + injected.rows().size() + " | sqlError=" + injected.sqlError()
                + " | maskedError=" + maskedError + " | delayMs=" + firstDeltaMs
                + " | repeatDelayMs=" + repeatDeltaMs + " | confirmed=" + confirmed, true);

        if (evidence.response() != null)
            ScreenshotManager.captureResponse(driver, evidence.response());
        else
            ScreenshotManager.captureSqlResults(driver);

        // A masked query failure is not evidence that a non-error injection was safely blocked.
        if (injected.text().contains("Something went wrong.") && !test.signal().equals("SQL_ERROR"))
            throw new SkipException("INCONCLUSIVE SQLi probe at " + level + " for " + test.id()
                    + ": DVWA returned a masked SQL error, so the intended " + test.signal()
                    + " signal could not be evaluated.");

        if (test.signal().equals("SQL_ERROR"))
            Assert.assertFalse(confirmed, "SQL ERROR DISCLOSURE at " + level + " for " + test.id()
                    + "; this is a security finding, not by itself confirmation of data retrieval.");
        else {
            Assert.assertFalse(injected.sqlError(), "SQL ERROR DISCLOSURE at " + level + " for " + test.id()
                    + "; intended " + test.signal() + " signal was not assessable.");
            Assert.assertFalse(confirmed, "SQL INJECTION CONFIRMED at " + level + " for " + test.id()
                    + " (" + test.technique() + ", " + test.signal() + "). Control rows="
                    + comparison.rows().size() + ", injected rows=" + injected.rows().size()
                    + (test.signal().equals("TIME_DELAY") ? ", first delay=" + firstDeltaMs
                            + " ms, repeat delay=" + repeatDeltaMs + " ms" : "") + ".");
        }
    }

    /**
     * Compares observed rows to the expected spreadsheet markers.
     *
     * @param result observed result
     * @param test spreadsheet case
     * @param prefix whether a prefix match is requested
     * @return whether a matching row exists
     */
    private boolean hasCanary(ProbeResult result, SqlCase test, boolean prefix) {
        return result.rows().stream().anyMatch(row -> row.surname().equals(test.expectedLast())
                && (prefix ? row.firstName().startsWith(test.expectedFirst())
                    && row.firstName().length() > test.expectedFirst().length()
                    : row.firstName().equals(test.expectedFirst())));
    }

    /**
     * Submits an existing spreadsheet input using the selected page flow and records the observed result.
     *
     * @param page current SQL injection page
     * @param level selected security mode
     * @param payload spreadsheet or control input
     * @return standardized observation and elapsed time
     */
    private ProbeResult submit(SqlInjectionPage page, SecurityPage.SecurityLevel level, String payload) {
        page.open(baseUrl);
        HttpResponse<String> response = null;
        long started = System.nanoTime();
        String text;
        switch (level) {
            case MEDIUM -> {
                response = page.injectMediumPayload(baseUrl, payload);
                text = page.getHttpResultText(response.body());
            }
            case HIGH -> {
                page.injectHighPayload(baseUrl, payload);
                text = page.getInjectionResultText();
            }
            case LOW, IMPOSSIBLE -> {
                page.injectPayload(payload);
                text = page.getInjectionResultText();
            }
            default -> throw new IllegalStateException("Unexpected DVWA level: " + level);
        }
        long durationMs = (System.nanoTime() - started) / 1_000_000;
        List<ResultRow> results = new ArrayList<>();
        Matcher matcher = RESULT_ROW.matcher(text);
        while (matcher.find()) results.add(new ResultRow(matcher.group(1).trim(), matcher.group(2).trim()));
        return new ProbeResult(text, results, SQL_ERROR.matcher(text).find(), response, durationMs);
    }
}
