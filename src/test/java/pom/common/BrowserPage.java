package pom.common;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;

/** Shared Selenium context for site-specific page objects. */
public abstract class BrowserPage {
    protected final WebDriver driver;
    protected final WebDriverWait wait;

    /**
     * Initializes explicit waits for the supplied browser without changing implicit wait policy.
     * @param driver current test browser
     * @param timeout target-specific explicit wait duration
     */
    protected BrowserPage(WebDriver driver, Duration timeout) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, timeout);
    }
}
