package pom.common;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import java.util.function.Function;

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

    /**
     * Waits for a page value or element, retrying when a browser navigation
     * temporarily invalidates an element. Other browser errors are reported.
     * Only use this for reads or repeatable setup actions, not form submissions.
     *
     * @param condition a repeatable check for the required page state
     * @param <T> type of the value returned by the check
     * @return the value from the completed check
     */
    protected <T> T waitForPageValue(Function<WebDriver, T> condition) {
        return wait.until(current -> {
            try {
                return condition.apply(current);
            } catch (WebDriverException e) {
                if (isDetachedElement(e)) return null;
                throw e;
            }
        });
    }

    /**
     * Waits for the old page to be replaced and for a known element to
     * appear on the new page. Prevents reading the previous form as a result.
     *
     * @param previous element from the page before navigation
     * @param readyLocator element expected on the next page
     */
    protected void waitForNewPage(WebElement previous, By readyLocator) {
        wait.until(current -> {
            try {
                previous.isEnabled();
                return false;
            } catch (WebDriverException e) {
                if (isDetachedElement(e)) return true;
                throw e;
            }
        });
        waitForPageValue(ExpectedConditions.visibilityOfElementLocated(readyLocator)::apply);
    }

    /**
     * Recognizes a stale element or the equivalent Chromium error reported
     * when an element belongs to a replaced document.
     *
     * @param error browser exception to inspect
     * @return true when the element is no longer attached to the document
     */
    private static boolean isDetachedElement(WebDriverException error) {
        return error instanceof StaleElementReferenceException
                || (error.getMessage() != null
                    && error.getMessage().contains("Node with given id does not belong to the document"));
    }
}
