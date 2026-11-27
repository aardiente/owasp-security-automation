package utilities.JuiceShop;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.json.Json;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;

/**
 * Groups browser-related tasks used by Juice Shop challenges so they can be shared across tests.
 */
public final class BrowserActions extends ActionContext {
    /**
     * Uses the shared per-test HTTP client and JSON JSON converter.
     *
     * @param api configured application client
     * @param json shared JSON JSON converter
     */
    BrowserActions(JuiceShopApi api, Json json) { super(api, json); }

    /**
     * Dismisses the visible challenge notifications so the presentation and subsequent browser interactions are not obstructed.
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
     * Builds the cross-site imaging URL used in the related Juice Shop challenge.
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
