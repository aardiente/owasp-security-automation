
package pom.JuiceShop;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Juice Shop page object exposing Selenium interactions for the web3 sandbox view.
 */
public class Web3SandboxPage extends BasePage {

    private static final String ROUTE = "#/web3-sandbox";
    private final By application = By.tagName("app-root");

    /**
     * Creates the web3 sandbox page component and initializes the dependencies required by its methods.
     *
     * <p>Creates a Web3 sandbox page instance and initializes its dependencies.</p>
     *
     * @param driver the Selenium WebDriver controlling the application browser
     */
    public Web3SandboxPage(WebDriver driver) {
        super(driver);
    }

    /**
     * Navigates to the Juice Shop Web3 sandbox.
     *
     * @param baseUrl the base URL of the containerized application under test
     */
    public void open(String baseUrl) {
        driver.get(baseUrl + ROUTE);

        WebDriverWait pageWait = new WebDriverWait(driver, Duration.ofSeconds(15));
        pageWait.until(ExpectedConditions.urlContains(ROUTE));
        pageWait.until(ExpectedConditions.visibilityOfElementLocated(application));
    }

    /**
     * Checks whether open is present or meets the expected condition.
     *
     * @return {@code true} when the Web3 Sandbox page has loaded
     */
    public boolean isOpen() {
        return driver.getCurrentUrl().contains(ROUTE);
    }
}
