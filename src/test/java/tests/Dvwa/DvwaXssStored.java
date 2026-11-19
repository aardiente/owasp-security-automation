package tests.Dvwa;

import automation.ZapManager;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import pom.Dvwa.LoginPage;
import pom.Dvwa.SecurityPage;
import pom.Dvwa.XssStoredPage;
import pom.Dvwa.XssStoredPage.ProbeField;
import utilities.ExcelFileReader;
import utilities.ScreenshotManager;

import java.nio.file.Path;
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
        @Override public String toString() { return id + "-" + field; }
    }

    @DataProvider(name = "storedXssPayloads")
    public Object[][] storedXssPayloads() {
        String workbook = Path.of("src", "test", "java", "data", DvwaXssStored.class.getSimpleName() + ".xlsx").toString();
        Object[][] rows = ExcelFileReader.extractData(workbook, "Payloads");
        List<XssCase> cases = new ArrayList<>();
        Set<String> ids = new HashSet<>();

        for (int i = 0; i < rows.length; i++) {
            Object[] row = rows[i];
            int excelRow = i + 2;
            if (row == null || row.length != 4)
                throw new IllegalStateException("Expected case_id, levels, field, payload in workbook row " + excelRow);

            String id = value(row[0]);
            String levelText = value(row[1]);
            String fieldText = value(row[2]);
            String payload = value(row[3]);
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

            EnumSet<SecurityPage.SecurityLevel> levels = EnumSet.noneOf(SecurityPage.SecurityLevel.class);
            if (levelText.equalsIgnoreCase("ALL")) levels = EnumSet.allOf(SecurityPage.SecurityLevel.class);
            else for (String part : levelText.split("\\|")) {
                try { levels.add(SecurityPage.SecurityLevel.valueOf(part.trim().toUpperCase(Locale.ROOT))); }
                catch (IllegalArgumentException e) {
                    throw new IllegalStateException("Unknown security level in workbook row " + excelRow, e);
                }
            }
            if (levels.isEmpty()) throw new IllegalStateException("No security levels in workbook row " + excelRow);
            cases.add(new XssCase(id, levels, field, payload));
        }

        List<Object[]> invocations = new ArrayList<>();
        for (SecurityPage.SecurityLevel level : SecurityPage.SecurityLevel.values())
            for (XssCase probe : cases)
                if (probe.levels().contains(level)) invocations.add(new Object[]{level, probe});
        if (invocations.isEmpty()) throw new IllegalStateException("Stored-XSS workbook contains no enabled test cases");
        return invocations.toArray(Object[][]::new);
    }

    /** A successfully executed, persisted event handler is a confirmed finding and fails the test. */
    @Test(dataProvider = "storedXssPayloads")
    public void storedXssIsBlocked(SecurityPage.SecurityLevel level, XssCase probe) {
        WebDriver driver = ZapManager.getDriver();
        driver.get(baseUrl + LoginPage.loginEndpoint);
        new LoginPage(driver).login("admin", "password");
        driver.get(baseUrl + SecurityPage.securityEndpoint);
        new SecurityPage(driver).setSecurityLevel(level);

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

    private static String value(Object cell) { return cell == null ? "" : cell.toString().trim(); }
}
