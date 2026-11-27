package pom.common;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;

/**
 * Provides common browser and waiting tools for page objects, avoiding repeated setup in each page class.
 */
public abstract class BrowserPage {
    protected final WebDriver driver;
    protected final WebDriverWait wait;

    /**
     * Sets up the shared browser page with the provided browser session.
     *
     * @param driver current test browser
     * @param timeout target-specific browser wait duration
     */
    protected BrowserPage(WebDriver driver, Duration timeout) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, timeout);
    }
}
