
package tests.Dvwa;

import automation.ZapManager;
import org.openqa.selenium.Alert;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import pom.Dvwa.LoginPage;
import pom.Dvwa.SecurityPage;
import utilities.ExcelFileReader;
import utilities.ScreenshotManager;

import java.time.Duration;

public class DvwaXSSReflection extends BaseDvwaTest {

    @DataProvider(name = "securityAndPayloads")
    public Object[][] getSecurityAndPayloads() {
        Object[][] payloadsFromExcel = ExcelFileReader.extractData("src/test/java/data/xss_payloads.xlsx", null);
        String[] securityLevels = {"low", "medium", "high"};

        Object[][] testData = new Object[payloadsFromExcel.length * securityLevels.length][2];
        int index = 0;

        for (String level : securityLevels) {
            for (Object[] row : payloadsFromExcel) {
                testData[index][0] = level;
                testData[index][1] = row[0].toString();
                index++;
            }
        }

        return testData;
    }

    @Test(dataProvider = "securityAndPayloads")
    public void XSS_Reflected(String levelStr, String payload) {
        var driver = ZapManager.getDriver();

        SecurityPage.SecurityLevel securityLevel = null;
        for (SecurityPage.SecurityLevel lvl : SecurityPage.SecurityLevel.values()) {
            if (lvl.getSecurityLevel().equalsIgnoreCase(levelStr)) {
                securityLevel = lvl;
                break;
            }
        }

        driver.get(baseUrl + LoginPage.loginEndpoint);

        LoginPage loginPage = new LoginPage(driver);
        loginPage.login("admin", "password");

        driver.get(baseUrl + SecurityPage.securityEndpoint);

        SecurityPage securityPage = new SecurityPage(driver);
        securityPage.setSecurityLevel(securityLevel);

        loginPage.navigateToXssReflected();

        WebDriverWait pageSettlementWait = new WebDriverWait(driver, Duration.ofSeconds(5));
        pageSettlementWait.until(ExpectedConditions.urlContains("xss_r"));

        securityPage.enterPayload(payload);
        securityPage.clickSubmitPayload();

        boolean xssExecuted = false;

        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
            wait.until(ExpectedConditions.alertIsPresent());

            Alert alert = driver.switchTo().alert();
            String alertText = alert.getText();
            System.out.println("Visual Verification Confirmed: " + alertText);

            alert.accept();
            xssExecuted = true;
            ScreenshotManager.captureAlert(driver, alertText);

        } catch (org.openqa.selenium.TimeoutException e) {
            System.out.println("Payload execution blocked: " + e.getLocalizedMessage());
        }

        if (!xssExecuted) ScreenshotManager.capture(driver);

        Assert.assertFalse(xssExecuted,
                "Targeted XSS exploit successfully fired under setting [" + levelStr.toUpperCase() + "]");
    }
}
