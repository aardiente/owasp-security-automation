package utilities.JuiceShop;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.json.Json;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;

/** Application-specific browser actions relocated from the original challenge helper. */
public final class BrowserActions extends ActionContext {
    /**
     * Uses the shared per-test HTTP client and JSON serializer.
     * @param api configured application client
     * @param json shared JSON serializer
     */
    BrowserActions(JuiceShopApi api, Json json) { super(api, json); }

    /**
     * Performs the dismiss challenge notifications action against the isolated Juice Shop test application.
     *
     * @param driver the Selenium WebDriver controlling the application browser
     */
    public void dismissChallengeNotifications(WebDriver driver) {
        By notifications = By.cssSelector(
                "app-challenge-solved-notification .challenge-solved-toast mat-card.accent-notification");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        wait.withMessage("Mass Dispel requires at least two visible Challenge solved notifications.");
        wait.until(d -> d.findElements(notifications).stream()
                .filter(WebElement::isDisplayed).count() >= 2);

        long count = driver.findElements(notifications).stream()
                .filter(WebElement::isDisplayed).count();

        By closeButton = By.cssSelector(
                "app-challenge-solved-notification .challenge-solved-toast button#closeButton");

        WebElement button = wait.until(ExpectedConditions.elementToBeClickable(closeButton));

        new org.openqa.selenium.interactions.Actions(driver)
                .keyDown(Keys.SHIFT).click(button).keyUp(Keys.SHIFT).perform();

        System.out.println("[MASS DISPEL] Shift-clicked X with "
                + count + " challenge notifications visible.");
    }

    /**
     * Builds the cross-site imaging URL used in the corresponding Juice Shop challenge.
     *
     * @param baseUrl the base URL of the containerized application under test
     * @return the URL constructed for the Cross-Site Imaging challenge
     */
    public String crossSiteImagingUrl(String baseUrl) {
        String target = "../../../../redirect?to=https://cataas.com/cat"
                + "?x=https://github.com/juice-shop/juice-shop";
        return baseUrl + "/#/deluxe-membership?testDecal=" + encode(target);
    }
}
