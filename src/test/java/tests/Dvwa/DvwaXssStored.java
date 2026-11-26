package tests.Dvwa;

import automation.ZapManager;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import pom.Dvwa.SecurityPage;
import pom.Dvwa.XssStoredPage;
import pom.Dvwa.XssStoredPage.ProbeField;
import utilities.ScreenshotManager;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Data-driven DVWA stored-XSS tests. The workbook chooses payloads, fields, and security levels. */
public class DvwaXssStored extends BaseDvwaTest {

    private record XssCase(String id, EnumSet<SecurityPage.SecurityLevel> levels,
                           ProbeField field, String payload) {
        /**
         * Returns a stable case and field identifier for evidence naming.
         * @return case and field identifier
         */
        @Override public String toString() { return id + "-" + field; }
    }

    /**
     * Loads and validates stored-XSS workbook cases and expands their configured security levels.
     * @return security-level and case pairs for TestNG
     */
    @DataProvider(name = "storedXssPayloads")
    public Object[][] storedXssPayloads() {
        Object[][] rows = DvwaCases.load("dvwa.storedXss.workbook", "Payloads");
        List<XssCase> cases = new ArrayList<>();
        Set<String> ids = new HashSet<>();

        for (int i = 0; i < rows.length; i++) {
            Object[] row = rows[i];
            int excelRow = i + 2;
            if (row == null || row.length != 4)
                throw new IllegalStateException("Expected case_id, levels, field, payload in workbook row " + excelRow);

            String id = DvwaCases.cell(row, 0);
            String levelText = DvwaCases.cell(row, 1);
            String fieldText = DvwaCases.cell(row, 2);
            String payload = DvwaCases.cell(row, 3);
            if (!id.matches("[A-Za-z0-9_-]{1,60}") || !ids.add(id))
                throw new IllegalStateException("Missing, invalid, or duplicate case_id in workbook row " + excelRow);
            if (!payload.contains("{{MARKER}}") || !payload.contains("this.dataset.p=1"))
                throw new IllegalStateException("Payload in workbook row " + excelRow
                        + " must include {{MARKER}} and the harmless this.dataset.p=1 execution signal");

            ProbeField field;
            try { field = ProbeField.valueOf(fieldText.toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException e) {
                throw new IllegalStateException("Unknown probe field in workbook row " + excelRow, e);
            }

            EnumSet<SecurityPage.SecurityLevel> levels = DvwaCases.levels(levelText, excelRow);
            cases.add(new XssCase(id, levels, field, payload));
        }

        return DvwaCases.levelFirst(cases, XssCase::levels, "Stored-XSS");
    }

    /** A successfully executed, persisted event handler is a confirmed finding and fails the test. */
    @Test(dataProvider = "storedXssPayloads")
    public void storedXssIsBlocked(SecurityPage.SecurityLevel level, XssCase probe) {
        WebDriver driver = ZapManager.getDriver();
        prepareSecurityLevel(level);

        XssStoredPage page = new XssStoredPage(driver);
        String marker = "p" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        page.open(baseUrl);
        page.submitMarkerProbe(marker, probe.field(), probe.payload(), baseUrl);
        page.reloadStoredEntries(baseUrl);

        boolean persisted = page.entryWasPersisted(marker, probe.field());
        boolean executed = persisted && page.didStoredProbeExecute(marker, probe.field());
        if (persisted) page.focusStoredEntry(marker, probe.field());
        Reporter.log("STORED XSS | case=" + probe.id() + " | level=" + level + " | field=" + probe.field()
                + " | marker=" + marker + " | persisted=" + persisted + " | eventExecuted=" + executed, true);
        ScreenshotManager.capture(driver);

        Assert.assertTrue(persisted, "Stored-XSS probe was not saved for " + probe.id() + " at " + level
                + "; an unsaved payload is not evidence that XSS is blocked.");
        Assert.assertFalse(executed, "STORED XSS CONFIRMED at " + level + " in " + probe.field()
                + " for workbook case " + probe.id() + " (marker=" + marker + ").");
    }

}
