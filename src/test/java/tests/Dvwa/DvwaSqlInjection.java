
package tests.Dvwa;

import automation.AppConfig;
import automation.ZapManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import pom.Dvwa.LoginPage;
import pom.Dvwa.SecurityPage;
import pom.Dvwa.SqlInjectionPage;
import utilities.ExcelFileReader;
import utilities.ScreenshotManager;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DvwaSqlInjection extends BaseDvwaTest {

    private final String payloadSource = "src/test/java/data/sqli_payloads.xlsx";
    private final String[] securityLevels = {"low"};

    private static final Pattern RESULT_ROW = Pattern.compile(
            "(?m)First name:[ \\t]*([^\\r\\n]*)\\r?\\n[ \\t]*Surname:[ \\t]*([^\\r\\n]*)"
    );

    private static final Pattern PASSWORD_HASH = Pattern.compile("(?i)^[a-f0-9]{32}$");

    private static final Pattern SQL_STRUCTURE = Pattern.compile(
            "(?i)\\bUNION\\s+SELECT\\b|\\bORDER\\s+BY\\b|\\bOR\\b\\s*(?:'|\\d|\\()"
    );

    private record ResultRow(String firstName, String surname) {}

    private record ProbeResult(
            int recordCount,
            boolean sqlError,
            boolean credentialsExposed,
            List<ResultRow> rows
    ) {}

    @DataProvider(name = "securityAndPayloads")
    public Object[][] getSecurityAndPayloads() {
        Object[][] payloads = ExcelFileReader.extractData(payloadSource, null, false);
        Object[][] testData = new Object[payloads.length * securityLevels.length][2];

        int index = 0;
        for (String level : securityLevels) {
            for (Object[] row : payloads) {
                testData[index][0] = level;
                testData[index][1] = row[0].toString();
                index++;
            }
        }

        return testData;
    }

    private void prepareSqlInjectionPage(WebDriver driver) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        driver.get(baseUrl + LoginPage.loginEndpoint);

        LoginPage loginPage = new LoginPage(driver);
        loginPage.login("admin", "password");

        driver.get(baseUrl + SecurityPage.securityEndpoint);

        SecurityPage securityPage = new SecurityPage(driver);
        securityPage.setSecurityLevel(SecurityPage.SecurityLevel.LOW);

        driver.get(baseUrl + SqlInjectionPage.endpoint);

        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("id")));
        } catch (TimeoutException e) {
            String url = driver.getCurrentUrl();
            String title = driver.getTitle();
            String body = driver.findElement(By.tagName("body")).getText();

            System.err.println("[DVWA SQLi] Expected SQL injection input was not found.");
            System.err.println("[DVWA SQLi] URL: " + url);
            System.err.println("[DVWA SQLi] Title: " + title);
            System.err.println("[DVWA SQLi] Page contents:\n" + body);

            throw new IllegalStateException(
                    "DVWA SQL Injection page did not contain input[name='id']. URL: "
                            + url + " | Title: " + title
                            + " | Page contents: " + body.substring(0, Math.min(body.length(), 1000)), e);
        }
    }

    private String normalizePayload(String payload) {
        String trimmed = payload.stripLeading();

        if (trimmed.startsWith("?id=")) {
            String value = trimmed.substring(4);
            int nextParameter = value.indexOf('&');

            if (nextParameter >= 0) {
                value = value.substring(0, nextParameter);
            }

            System.out.println("Converted URL-style payload to ID input value.");
            return value;
        }

        return payload;
    }

    private List<ResultRow> extractRows(String response) {
        List<ResultRow> rows = new ArrayList<>();
        Matcher matcher = RESULT_ROW.matcher(response);

        while (matcher.find()) {
            rows.add(new ResultRow(
                    matcher.group(1).trim(),
                    matcher.group(2).trim()
            ));
        }

        return rows;
    }

    private boolean containsPasswordHash(List<ResultRow> rows) {
        for (ResultRow row : rows) {
            if (PASSWORD_HASH.matcher(row.surname()).matches()) return true;
        }

        return false;
    }

    private ProbeResult submitProbe(WebDriver driver, String payload) {
        driver.get(baseUrl + SqlInjectionPage.endpoint);

        SqlInjectionPage page = new SqlInjectionPage(driver);
        page.injectPayload(payload);

        String response = page.getInjectionResultText();
        List<ResultRow> rows = extractRows(response);

        ProbeResult result = new ProbeResult(
                page.getResultCount(),
                page.hasSqlError(),
                containsPasswordHash(rows),
                rows
        );

        System.out.println("Payload: " + payload);
        System.out.println("Records returned: " + result.recordCount());
        System.out.println("SQL error: " + result.sqlError());
        System.out.println("Credential hash detected: " + result.credentialsExposed());

        return result;
    }

    @Test(dataProvider = "securityAndPayloads")
    public void sqlInjection_Low(String level, String originalPayload) {
        WebDriver driver = ZapManager.getDriver();
        prepareSqlInjectionPage(driver);

        String payload = normalizePayload(originalPayload);
        Assert.assertFalse(payload.isBlank(), "SQL injection payload cannot be blank.");

        boolean numericLeading = payload.stripLeading().matches("^\\d+.*");
        String controlId = numericLeading ? "1" : "999999999";

        ProbeResult control = submitProbe(driver, controlId);
        Assert.assertFalse(control.sqlError(), "Normal control request returned a SQL error.");

        ProbeResult injected = submitProbe(driver, payload);

        ScreenshotManager.captureSqlResults(driver);

        // Keep the final response visible during presentation mode.
        AppConfig.pauseForResults();

        // Finding 1: Database error disclosure.
        if (injected.sqlError()) {
            System.out.println("RESULT: SQL_ERROR_DISCLOSURE");

            Assert.fail("SQL ERROR DISCLOSURE detected for payload: " + originalPayload
                    + ". MariaDB exposed a query error. Successful SQL injection "
                    + "execution is not confirmed.");
        }

        // Finding 2: Unexpected database records.
        boolean unexpectedRecords = injected.recordCount() > control.recordCount();
        boolean differentData = false;

        for (ResultRow row : injected.rows()) {
            if (!control.rows().contains(row)) {
                differentData = true;
                break;
            }
        }

        // Finding 3: Credential hash disclosure.
        boolean credentialDisclosure = injected.credentialsExposed()
                && !control.credentialsExposed();

        boolean injectionConfirmed = unexpectedRecords
                || differentData
                || credentialDisclosure;

        // Finding 4: SQL-like input accepted as a valid database lookup.
        // This is a validation finding, not proof that the SQL keywords executed.
        boolean unsafeSqlInputAccepted = SQL_STRUCTURE.matcher(payload).find()
                && injected.recordCount() > 0
                && !injectionConfirmed;

        if (credentialDisclosure) {
            System.out.println("RESULT: CREDENTIAL_DISCLOSURE_CONFIRMED");
        } else if (injectionConfirmed) {
            System.out.println("RESULT: SQL_INJECTION_DETECTED");
        } else if (unsafeSqlInputAccepted) {
            System.out.println("RESULT: UNSAFE_SQL_INPUT_ACCEPTED");
        } else {
            System.out.println("RESULT: NO_CONFIRMED_FINDING");
        }

        Assert.assertFalse(injectionConfirmed,
                "SQL INJECTION DETECTED for payload: " + originalPayload
                        + ". Unexpected records: " + unexpectedRecords
                        + ". Different data: " + differentData
                        + ". Credential disclosure: " + credentialDisclosure
                        + ". Control records: " + control.recordCount()
                        + ", injected records: " + injected.recordCount());

        Assert.assertFalse(unsafeSqlInputAccepted,
                "UNSAFE SQL INPUT ACCEPTED for payload: " + originalPayload
                        + ". The application returned database data for input containing SQL commands."
                        + " This indicates insufficient input validation, but does not"
                        + " independently confirm execution of those commands.");
    }
}
