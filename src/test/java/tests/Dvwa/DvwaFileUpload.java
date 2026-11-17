package tests.Dvwa;

import automation.ZapManager;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import pom.Dvwa.LoginPage;
import pom.Dvwa.SecurityPage;
import pom.Dvwa.FileUploadPage;
import utilities.ScreenshotManager;

import java.util.Arrays;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.UUID;

/** Tests whether the DVWA upload restriction accepts an ordinary non-image text file. */
public class DvwaFileUpload extends BaseDvwaTest {
    /**
     * Provides all DVWA security levels as independent TestNG invocations.
     * @return the Low, Medium, High, and Impossible modes
     */
    @DataProvider(name = "securityModes")
    public Object[][] securityModes() {
        return Arrays.stream(SecurityPage.SecurityLevel.values())
                .map(level -> new Object[]{level}).toArray(Object[][]::new);
    }

    /**
     * Checks a harmless diagnostic probe and fails when the vulnerability is demonstrated.
     * @param level the DVWA security level being assessed
     */
    @Test(dataProvider = "securityModes")
    public void nonImageUploadIsRejected(SecurityPage.SecurityLevel level) {
        WebDriver driver = ZapManager.getDriver();
        driver.get(baseUrl + LoginPage.loginEndpoint);
        new LoginPage(driver).login("admin", "password");
        driver.get(baseUrl + SecurityPage.securityEndpoint);
        new SecurityPage(driver).setSecurityLevel(level);

        FileUploadPage page = new FileUploadPage(driver);
        page.open(baseUrl);
        Path file;
        try {
            file = Files.createTempFile("dvwa-evidence-", ".txt");
            Files.writeString(file, "DVWA harmless upload validation: " + UUID.randomUUID(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to prepare a harmless temporary upload file", e);
        }
        try {
            page.submitFile(file);
            boolean accepted = page.hasUploadedFileLink(file.getFileName().toString());
            ScreenshotManager.capture(driver);
            Assert.assertFalse(accepted, "UNRESTRICTED FILE UPLOAD CONFIRMED at " + level + ": non-image text file was stored.");
        } finally {
            try { Files.deleteIfExists(file); }
            catch (IOException e) { System.err.println("Could not delete temporary upload file: " + e.getMessage()); }
        }
    }
}
