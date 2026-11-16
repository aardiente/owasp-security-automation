
package pom.JuiceShop;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * Juice Shop page object exposing Selenium interactions for the search view.
 */
public class SearchPage extends BasePage
{
    private final By searchComponent = By.cssSelector("app-mat-search-bar#searchQuery");
    private final By searchToggle = By.cssSelector("#searchQuery button.search-toggle");
    private final By searchInput = By.cssSelector("#searchQuery input");
    private final By searchValue = By.id("searchValue");
    private final By productCards = By.cssSelector("app-product.product");

    /**
     * Creates the search page component and initializes the dependencies required by its methods.
     *
     * <p>Creates a search page instance and initializes its dependencies.</p>
     *
     * @param driver the Selenium WebDriver controlling the application browser
     */
    public SearchPage(WebDriver driver)
    {
        super(driver);
    }

    /**
     * Opens the Juice Shop product search interface.
     */
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

    /**
     * Enters a keyword in the current product search interface.
     *
     * @param payload the test input submitted to the intentionally vulnerable lab application
     */
    public void search(String payload)
    {
        openSearch();

        WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(searchInput));

        input.clear();
        input.sendKeys(payload);
        input.sendKeys(Keys.ENTER);
    }

    /**
     * Opens the storefront and searches for a product keyword.
     *
     * @param baseUrl the base URL of the containerized application under test
     * @param payload the test input submitted to the intentionally vulnerable lab application
     */
    public void searchFromHome(String baseUrl, String payload) {
        driver.get(baseUrl);
        dismissInitialOverlays();
        search(payload);
    }

    /**
     * Waits for the embedded SoundCloud player used in the related challenge.
     */
    public void waitForSoundCloudPlayer() {
        new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(15))
                .until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("iframe[src*='soundcloud.com/player']")));
    }

    // Keep compatibility with your existing tests.
    /**
     * Uses the search control to find a product by keyword.
     *
     * @param productName the product name to search for
     */
    public void searchForProduct(String productName) {
        search(productName);
    }

    /**
     * Reads the current product search text from the search field.
     *
     * @return the text currently entered in the Juice Shop product search field
     */
    public String getSearchValue() {
        return wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        searchValue
                )
        ).getText();
    }

    /**
     * Checks whether products displayed is present or meets the expected condition.
     *
     * @return {@code true} when a product result is visible after searching
     */
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
