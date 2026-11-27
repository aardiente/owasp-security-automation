package pom.JuiceShop;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * Represents the Juice Shop storefront. Reads the displayed search label and first product title for tests.
 */
public class ShopDashboardPage extends BasePage
{

    // 💡 Locators
    private final By searchValueHeader = By.cssSelector("span[id='searchValue']");
    private final By firstProductCardTitle = By.cssSelector(".mat-card .item-name");

     /**
      * Sets up the Juice Shop shop dashboard page with the provided browser session.
      *
      * @param driver the Selenium WebDriver controlling the application browser
      */
    public ShopDashboardPage(WebDriver driver)
    {
        super(driver);
    }

    /**
     * Fetches the reflecting text snippet at the top of a search result page.
     * Essential for validating Reflected XSS exploits.
     */
     /**
     * Reads the displayed search value from the storefront header for UI verification.
     *
     * @return the visible search term from the search-value header
     */
    public String getActiveSearchValueText() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(searchValueHeader)).getText();
    }

     /**
      * Reads the first visible product title for storefront result checks.
      *
      * @return the visible title of the first search-result product
      */
    public String getFirstProductTitle() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(firstProductCardTitle)).getText();
    }
}
