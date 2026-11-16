package pom.JuiceShop;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * Juice Shop page object exposing Selenium interactions for the shop dashboard view.
 */
public class ShopDashboardPage extends BasePage
{

    // 💡 Locators
    private final By searchValueHeader = By.cssSelector("span[id='searchValue']");
    private final By firstProductCardTitle = By.cssSelector(".mat-card .item-name");

     /**
      * Creates the shop dashboard page component and initializes the dependencies required by its methods.
      *
      * <p>Creates a shop dashboard page instance and initializes its dependencies.</p>
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
      * Reads the search term displayed in the Juice Shop dashboard header.
      *
      * <p>Reads the search term echoed in the Juice Shop dashboard header.</p>
      *
      * @return the visible search term from the search-value header
      */
    public String getActiveSearchValueText() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(searchValueHeader)).getText();
    }

     /**
      * Reads the title text of the first product card currently displayed.
      *
      * <p>Reads the first product title displayed in the Juice Shop search results.</p>
      *
      * @return the visible title of the first search-result product
      */
    public String getFirstProductTitle() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(firstProductCardTitle)).getText();
    }
}
