
package pom.JuiceShop;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class PrivacyPolicyPage extends BasePage {
    private final By policyContainer = By.cssSelector("app-privacy-policy");
    private final By policyContent = By.cssSelector("app-privacy-policy .privacy-policy");
    private final By policyImage = By.cssSelector("app-privacy-policy img[src$='81px.png']");

    public PrivacyPolicyPage(WebDriver driver) {
        super(driver);
    }

    public void open(String baseUrl) {
        driver.get(baseUrl + "#/privacy-security/privacy-policy");
        var pageWait = new WebDriverWait(driver, Duration.ofSeconds(15));
        pageWait.until(ExpectedConditions.visibilityOfElementLocated(policyContent));
        pageWait.until(ExpectedConditions.presenceOfElementLocated(policyImage));
    }

    /** Show an actual image when available; otherwise show the application's existing privacy policy. */
    public void showInspectionEvidence(String baseUrl, boolean imageAvailable) {
        if (imageAvailable) {
            try {
                driver.get(baseUrl + "/assets/private/thank-you.jpg");
                boolean loaded = new WebDriverWait(driver, Duration.ofSeconds(5)).until(d ->
                        d.findElements(By.tagName("img")).stream().anyMatch(img -> {
                            String width = img.getDomProperty("naturalWidth");
                            return width != null && Integer.parseInt(width) > 0;
                        }));
                if (loaded) return;
            } catch (Exception e) {
                System.out.println("[PRIVACY POLICY] Image unavailable: " + e.getMessage());
            }
        }
        System.out.println("[PRIVACY POLICY] Showing the existing Privacy Policy page.");
        driver.get(baseUrl + "#/privacy-security/privacy-policy");
        new WebDriverWait(driver, Duration.ofSeconds(15))
                .until(ExpectedConditions.visibilityOfElementLocated(policyContainer));
    }
}
