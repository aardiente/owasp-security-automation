
package pom.JuiceShop;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/** Browser presentation of raw resources (FTP listings, logs, text files, JSON). */
public class ResourcePage extends BasePage {
    private final By body = By.tagName("body");

    /**
     * Creates the resource page component and initializes the dependencies required by its methods.
     *
     * <p>Creates a resource page instance and initializes its dependencies.</p>
     *
     * @param driver the Selenium WebDriver controlling the application browser
     */
    public ResourcePage(WebDriver driver) {
        super(driver);
    }

    /**
     * Opens an arbitrary resource page within the configured Juice Shop application.
     *
     * @param url the URL to navigate to, inspect, or request
     */
    public void open(String url) {
        driver.get(url);
    }

    /**
     * Returns visible text from the currently loaded resource page.
     *
     * @return the visible text content of the currently open resource page
     */
    public String bodyText() {
        return driver.findElement(body).getText();
    }

    /**
     * Waits until the resource page contains the supplied text.
     *
     * @param text the input string being normalized, sanitized, or rendered
     * @param seconds the maximum wait duration in seconds
     */
    public void waitForBodyText(String text, int seconds) {
        new WebDriverWait(driver, Duration.ofSeconds(seconds))
                .until(ExpectedConditions.textToBePresentInElementLocated(body, text));
    }

    /**
     * Finds a hyperlink whose visible text includes the specified fragment.
     *
     * @param fragment the substring used to identify a link
     * @param seconds the maximum wait duration in seconds
     * @return the URL of the first link containing the requested fragment
     */
    public String findLinkContaining(String fragment, int seconds) {
        By link = By.cssSelector("a[href*='" + fragment + "']");
        return new WebDriverWait(driver, Duration.ofSeconds(seconds))
                .until(ExpectedConditions.presenceOfElementLocated(link)).getAttribute("href");
    }
}
