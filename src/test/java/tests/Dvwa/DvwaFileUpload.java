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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.UUID;

/** Tests whether DVWA accepts an ordinary text file where it expects an image. */
public class DvwaFileUpload extends BaseDvwaTest {
    /**
     * Enumerates the four DVWA security modes.
     * @return one independent invocation per mode
     */
    @DataProvider(name = "securityModes")
    public Object[][] securityModes() {
        return Arrays.stream(SecurityPage.SecurityLevel.values())
                .map(level -> new Object[]{level}).toArray(Object[][]::new);
    }

    /**
     * Fails when a non-image text file is confirmed stored by the DVWA upload endpoint.
     * @param level DVWA security level to assess
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
            throw new IllegalStateException("Unable to prepare a temporary upload probe", e);
        }

        try {
            page.submitFile(file);
            boolean accepted;
            try {
                accepted = page.nonImageUploadAccepted(file.getFileName().toString());
            } catch (RuntimeException e) {
                ScreenshotManager.capture(driver);
                throw e;
            }
            ScreenshotManager.capture(driver);
            Assert.assertFalse(accepted, "UNRESTRICTED FILE UPLOAD CONFIRMED at " + level
                    + ": DVWA reported successful storage of a non-image text file.");
        } finally {
            try { Files.deleteIfExists(file); }
            catch (IOException e) { System.err.println("Temporary upload cleanup failed: " + e.getMessage()); }
        }
    }
}
