
package pom.JuiceShop;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import automation.AppConfig;
import pom.common.BrowserPage;

import java.time.Duration;

/**
 * Provides common browser controls and navigation for Juice Shop pages. Shares storefront navigation, dialog handling and product search.
 */
public abstract class BasePage extends BrowserPage {

    private final By dismissBannerButton = By.cssSelector("button[aria-label='Close Welcome Banner']");
    private final By acceptCookiesButton = By.cssSelector("a[aria-label='dismiss cookie message']");
    private final By navAccountMenu = By.id("navbarAccount");
    private final By navLoginButton = By.id("navbarLoginButton");
    private final By searchIcon = By.className("mat-search_icon");
    private final By searchBarInput = By.id("mat-input-0");

    /**
     * Sets up the Juice Shop base page with the provided browser session. Uses the configured site-specific wait inherited from the base page.
     *
     * @param driver the Selenium WebDriver controlling the application browser
     */
    public BasePage(WebDriver driver) {
        super(driver, Duration.ofSeconds(AppConfig.juiceShopPageWaitSeconds()));
    }

    /**
     * Dismisses initial Juice Shop UI overlays that intercept page interactions.
     */
    public void dismissInitialOverlays() {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(dismissBannerButton)).click();
            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(".cdk-overlay-backdrop")));
        } catch (Exception ignored) {}

        try {
            wait.until(ExpectedConditions.elementToBeClickable(acceptCookiesButton)).click();
        } catch (Exception ignored) {}
    }

    /**
     * Opens the storefront account menu and selects the login control using browser waits.
     */
    public void navigateToLoginViaNavbar() {
        wait.until(ExpectedConditions.elementToBeClickable(navAccountMenu)).click();
        wait.until(ExpectedConditions.elementToBeClickable(navLoginButton)).click();
    }

    /**
     * Uses the common shop navigation to search for a product keyword.
     *
     * @param keyword the product search term to submit
     */
    public void searchForProduct(String keyword) {
        wait.until(ExpectedConditions.elementToBeClickable(searchIcon)).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(searchBarInput)).sendKeys(keyword + "\n");
    }
}
