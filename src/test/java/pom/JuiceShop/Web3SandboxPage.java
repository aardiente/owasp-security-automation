
package pom.JuiceShop;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class Web3SandboxPage extends BasePage {

    private static final String ROUTE = "#/web3-sandbox";
    private final By application = By.tagName("app-root");

    public Web3SandboxPage(WebDriver driver) {
        super(driver);
    }

    public void open(String baseUrl) {
        driver.get(baseUrl + ROUTE);

        WebDriverWait pageWait = new WebDriverWait(driver, Duration.ofSeconds(15));
        pageWait.until(ExpectedConditions.urlContains(ROUTE));
        pageWait.until(ExpectedConditions.visibilityOfElementLocated(application));
    }

    public boolean isOpen() {
        return driver.getCurrentUrl().contains(ROUTE);
    }
}
