
package pom.JuiceShop;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/** Browser presentation of raw resources (FTP listings, logs, text files, JSON). */
public class ResourcePage extends BasePage {
    private final By body = By.tagName("body");

    public ResourcePage(WebDriver driver) {
        super(driver);
    }

    public void open(String url) {
        driver.get(url);
    }

    public String bodyText() {
        return driver.findElement(body).getText();
    }

    public void waitForBodyText(String text, int seconds) {
        new WebDriverWait(driver, Duration.ofSeconds(seconds))
                .until(ExpectedConditions.textToBePresentInElementLocated(body, text));
    }

    public String findLinkContaining(String fragment, int seconds) {
        By link = By.cssSelector("a[href*='" + fragment + "']");
        return new WebDriverWait(driver, Duration.ofSeconds(seconds))
                .until(ExpectedConditions.presenceOfElementLocated(link)).getAttribute("href");
    }
}
