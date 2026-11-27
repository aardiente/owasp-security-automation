
package pom.JuiceShop;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Represents the Juice Shop Web3 sandbox. Opens the page and checks that the expected view is displayed.
 */
public class Web3SandboxPage extends BasePage {

    private static final String ROUTE = "#/web3-sandbox";
    private final By application = By.tagName("app-root");

    /**
     * Sets up the Juice Shop web3 sandbox page with the provided browser session.
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
     * Checks whether the expected web3 sandbox view is currently open.
     *
     * @return {@code true} when the Web3 Sandbox page has loaded
     */
    public boolean isOpen() {
        return driver.getCurrentUrl().contains(ROUTE);
    }
}
