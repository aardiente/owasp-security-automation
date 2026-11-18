package tests.Dvwa;

import automation.ZapManager;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import pom.Dvwa.FileUploadPage;
import pom.Dvwa.LoginPage;
import pom.Dvwa.SecurityPage;
import utilities.ScreenshotManager;
import utilities.ExcelFileReader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

/**
 * Evaluates DVWA file-upload validation using a dedicated Excel workbook.
 * Each workbook case runs independently at its selected security levels; a confirmed
 * non-image upload intentionally fails, while a valid image acts as a control.
 */
public class DvwaFileUpload extends BaseDvwaTest {

    /**
     * Represents a workbook-defined upload probe and the levels at which to run it.
     *
     * @param id descriptive identifier used in TestNG reporting
     * @param levels security modes selected by the workbook
     * @param extension the temporary file's extension
     * @param encoding UTF8 or BASE64
     * @param payload harmless test-file bytes, expressed in the selected encoding
     * @param kind NON_IMAGE or VALID_IMAGE
     */
    private record UploadCase(String id, EnumSet<SecurityPage.SecurityLevel> levels,
                              String extension, String encoding, String payload, String kind) {
        /**
         * Returns a concise test-case name for TestNG and screenshot filenames.
         *
         * @return the case identifier
         */
        @Override public String toString() { return id; }
    }

    /**
     * Reads the workbook from the same project data directory used by the existing DVWA tests,
     * then expands each case across its selected security modes.
     *
     * @return TestNG parameter rows of security level and workbook case
     */
    @DataProvider(name = "fileUploadCases")
    public Object[][] fileUploadCases() {
        List<UploadCase> cases = readCases();
        List<Object[]> invocations = new ArrayList<>();
        for (SecurityPage.SecurityLevel level : SecurityPage.SecurityLevel.values()) {
            for (UploadCase probe : cases) {
                if (probe.levels().contains(level)) invocations.add(new Object[]{level, probe});
            }
        }
        if (invocations.isEmpty()) throw new IllegalStateException("File Upload Excel workbook produced no test cases");
        return invocations.toArray(Object[][]::new);
    }

    /**
     * Uploads one workbook-defined file and fails if DVWA stores invalid non-image content.
     * A legitimate image control is expected to be accepted at every security level.
     *
     * @param level the currently assessed DVWA security mode
     * @param probe the selected workbook test case
     */
    @Test(dataProvider = "fileUploadCases")
    public void fileUploadValidation(SecurityPage.SecurityLevel level, UploadCase probe) {
        WebDriver driver = ZapManager.getDriver();
        driver.get(baseUrl + LoginPage.loginEndpoint);
        new LoginPage(driver).login("admin", "password");
        driver.get(baseUrl + SecurityPage.securityEndpoint);
        new SecurityPage(driver).setSecurityLevel(level);

        FileUploadPage page = new FileUploadPage(driver);
        Path temporaryFile = createTemporaryFile(probe);
        boolean screenshotTaken = false;
        try {
            page.open(baseUrl);
            FileUploadPage.UploadResult result = page.submitFile(temporaryFile);
            ScreenshotManager.capture(driver);
            screenshotTaken = true;

            if (probe.kind().equals("NON_IMAGE")) {
                Assert.assertNotEquals(result.status(), FileUploadPage.Status.ACCEPTED,
                        "INVALID FILE UPLOAD CONFIRMED at " + level + " for workbook case " + probe.id()
                                + ": " + result.evidence());
            } else {
                Assert.assertEquals(result.status(), FileUploadPage.Status.ACCEPTED,
                        "VALID IMAGE CONTROL FAILED at " + level + " for workbook case " + probe.id()
                                + ": " + result.evidence());
            }
        } catch (RuntimeException | AssertionError e) {
            if (!screenshotTaken) ScreenshotManager.capture(driver);
            throw e;
        } finally {
            try { Files.deleteIfExists(temporaryFile); }
            catch (IOException e) { throw new IllegalStateException("Cannot remove temporary test upload", e); }
        }
    }

    /**
     * Materializes workbook payload content in a uniquely named temporary file for browser upload.
     *
     * @param probe the workbook case containing the file content and extension
     * @return the temporary file to submit through Selenium
     */
    private static Path createTemporaryFile(UploadCase probe) {
        byte[] bytes = switch (probe.encoding()) {
            case "UTF8" -> probe.payload().getBytes(StandardCharsets.UTF_8);
            case "BASE64" -> Base64.getDecoder().decode(probe.payload());
            default -> throw new IllegalStateException("Unsupported workbook payload encoding: " + probe.encoding());
        };
        try {
            Path file = Files.createTempFile("dvwa-" + probe.id() + "-", probe.extension());
            Files.write(file, bytes);
            return file;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create temporary upload for case " + probe.id(), e);
        }
    }

    /**
     * Loads cases from the established src/test/java/data workbook directory using the shared reader.
     * The workbook begins with: case_id, levels, extension, encoding, payload, kind.
     *
     * @return validated test cases from the Excel workbook
     */
    private static List<UploadCase> readCases() {
        String workbook = Path.of("src", "test", "java", "data", DvwaFileUpload.class.getSimpleName() + ".xlsx").toString();
        Object[][] rows = ExcelFileReader.extractData(workbook, null);
        List<UploadCase> cases = new ArrayList<>();

        for (int i = 0; i < rows.length; i++) {
            Object[] row = rows[i];
            int excelRow = i + 2;
            if (row == null || row.length != 6)
                throw new IllegalStateException("Expected six columns in upload workbook row " + excelRow);

            String[] fields = new String[6];
            for (int j = 0; j < fields.length; j++)
                fields[j] = row[j] == null ? "" : row[j].toString().trim();

            String id = fields[0];
            String extension = fields[2];
            String encoding = fields[3].toUpperCase(Locale.ROOT);
            String kind = fields[5].toUpperCase(Locale.ROOT);

            if (!id.matches("[A-Za-z0-9_-]{1,60}") || !extension.matches("\\.[A-Za-z0-9]{1,8}"))
                throw new IllegalStateException("Invalid case identifier or extension in workbook row " + excelRow);
            if (!encoding.equals("UTF8") && !encoding.equals("BASE64"))
                throw new IllegalStateException("Unsupported encoding in workbook row " + excelRow);
            if (!kind.equals("NON_IMAGE") && !kind.equals("VALID_IMAGE"))
                throw new IllegalStateException("Unsupported file kind in workbook row " + excelRow);
            if (fields[4].isEmpty())
                throw new IllegalStateException("Missing upload content in workbook row " + excelRow);

            EnumSet<SecurityPage.SecurityLevel> levels = EnumSet.noneOf(SecurityPage.SecurityLevel.class);
            if (fields[1].equalsIgnoreCase("ALL")) {
                levels = EnumSet.allOf(SecurityPage.SecurityLevel.class);
            } else {
                for (String value : fields[1].split("\\|")) {
                    try { levels.add(SecurityPage.SecurityLevel.valueOf(value.trim().toUpperCase(Locale.ROOT))); }
                    catch (IllegalArgumentException e) {
                        throw new IllegalStateException("Unknown security level in workbook row " + excelRow, e);
                    }
                }
            }
            if (levels.isEmpty())
                throw new IllegalStateException("Missing security levels in workbook row " + excelRow);

            cases.add(new UploadCase(id, levels, extension, encoding, fields[4], kind));
        }
        if (cases.isEmpty()) throw new IllegalStateException("The File Upload workbook contains no test cases");
        return cases;
    }
}
