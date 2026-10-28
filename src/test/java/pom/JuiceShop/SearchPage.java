
package pom.JuiceShop;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class SearchPage extends BasePage
{
    private final By searchComponent = By.cssSelector("app-mat-search-bar#searchQuery");
    private final By searchToggle = By.cssSelector("#searchQuery button.search-toggle");
    private final By searchInput = By.cssSelector("#searchQuery input");
    private final By searchValue = By.id("searchValue");
    private final By productCards = By.cssSelector("app-product.product");

    public SearchPage(WebDriver driver)
    {
        super(driver);
    }

    public void openSearch()
    {
        try {
            // Ensure the Angular search component exists.
            wait.until(ExpectedConditions.presenceOfElementLocated(searchComponent)
            );

            // Do not close an already expanded search field.
            for (WebElement input : driver.findElements(searchInput))
            {
                if (input.isDisplayed())
                {
                    return;
                }
            }

            // Click the actual button, not its container.
            wait.until(ExpectedConditions.elementToBeClickable(searchToggle)).click();

            // Wait for the input to become visible.
            wait.until(ExpectedConditions.visibilityOfElementLocated(searchInput));

        } catch (TimeoutException e)
        {
            throw new TimeoutException("Search component interaction failed. URL: "
                    + driver.getCurrentUrl()
                    + " | Search components found: "
                    + driver.findElements(searchComponent).size()
                    + " | Toggle buttons found: "
                    + driver.findElements(searchToggle).size(), e);
        }
    }

    public void search(String payload)
    {
        openSearch();

        WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(searchInput));

        input.clear();
        input.sendKeys(payload);
        input.sendKeys(Keys.ENTER);
    }

    public void searchFromHome(String baseUrl, String payload) {
        driver.get(baseUrl);
        dismissInitialOverlays();
        search(payload);
    }

    public void waitForSoundCloudPlayer() {
        new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(15))
                .until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("iframe[src*='soundcloud.com/player']")));
    }

    // Keep compatibility with your existing tests.
    public void searchForProduct(String productName) {
        search(productName);
    }

    public String getSearchValue() {
        return wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        searchValue
                )
        ).getText();
    }

    public boolean areProductsDisplayed() {
        try {
            wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            productCards
                    )
            );
            return true;

        } catch (TimeoutException e) {
            return false;
        }
    }
}
