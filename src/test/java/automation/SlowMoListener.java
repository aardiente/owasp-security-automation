
package automation;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.events.WebDriverListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Observes Selenium actions and optionally displays an overlay and delays interactions for demonstrations.
 */
public class SlowMoListener implements WebDriverListener {

    private static final Logger log = LoggerFactory.getLogger("org.openqa.selenium");
    private final WebDriver driver;

    /**
     * Creates the slow mo listener component and initializes the dependencies required by its methods.
     *
     * <p>Creates a slow mo listener instance and initializes its dependencies.</p>
     *
     * @param driver the Selenium WebDriver controlling the application browser
     */
    public SlowMoListener(WebDriver driver) {
        this.driver = driver;
    }

    /**
     * Provides a SlowMo presentation cue before navigating to a URL.
     *
     * @param driver the Selenium WebDriver controlling the application browser
     * @param url the URL to navigate to, inspect, or request
     */
    @Override
    public void beforeGet(WebDriver driver, String url) {
        log.info("Navigating to URL: {}", url);
    }

    /**
     * Waits for the configured presentation delay after a page navigation.
     *
     * @param driver the Selenium WebDriver controlling the application browser
     * @param url the URL to navigate to, inspect, or request
     */
    @Override
    public void afterGet(WebDriver driver, String url) {
        if (!AppConfig.isSlowMoEnabled() || AppConfig.isHeadless()) return;

        // Internal challenge verification isn't a presentation screen.
        if (url != null && url.contains("/api/Challenges")) return;

        PresentationOverlay.show(this.driver, "NAVIGATE", "driver.get(url);", "Page loaded");
        pauseForNavigation();
    }

    /**
     * Records or highlights the element search about to be performed.
     *
     * @param driver the Selenium WebDriver controlling the application browser
     * @param locator the Selenium selector identifying the target element
     */
    @Override
    public void beforeFindElement(WebDriver driver, By locator) {
        log.debug("Finding element: {}", locator);
    }

    /**
     * Highlights the element and pauses before Selenium clicks it.
     *
     * @param element the Selenium page element being inspected or acted upon
     */
    @Override
    public void beforeClick(WebElement element) {
        log.info("Clicking element: {}", element);

        if (AppConfig.isSlowMoEnabled()) {
            PresentationOverlay.show(driver, "CLICK", "element.click();", "Clicking page element");
            highlightAndPause(element);
        }
    }

    /**
     * Highlights the input field and previews the content before typing.
     *
     * @param element the Selenium page element being inspected or acted upon
     * @param keysToSend the keystrokes provided to the selected Selenium element
     */
    @Override
    public void beforeSendKeys(WebElement element, CharSequence... keysToSend) {
        log.debug("Sending keys to element: {}", element);

        if (AppConfig.isSlowMoEnabled()) {
            PresentationOverlay.show(driver, "TYPE", "element.sendKeys(...);", inputPreview(element, keysToSend));
            highlightAndPause(element);
        }
    }

    /**
     * Displays a cue before accepting a browser alert.
     *
     * @param alert the ZAP alert entry being converted into a report finding
     */
    @Override
    public void beforeAccept(Alert alert) {
        log.info("Accepting JavaScript alert");
        AppConfig.pauseForResults();
    }

    /**
     * Displays a cue before dismissing a browser alert.
     *
     * @param alert the ZAP alert entry being converted into a report finding
     */
    @Override
    public void beforeDismiss(Alert alert) {
        log.info("Dismissing JavaScript alert");
        AppConfig.pauseForResults();
    }

    /**
     * Handles a Selenium listener error without altering the underlying test assertion.
     *
     * @param target the configured application/report target
     * @param method the HTTP request method or reflected Java method being processed
     * @param args the entry-point arguments (not currently used)
     * @param e the Selenium listener exception associated with the failed action
     */
    @Override
    public void onError(Object target, Method method, Object[] args, InvocationTargetException e) {
        log.error("Selenium error during [{}]: {}", method.getName(), e.getTargetException().getMessage());
    }

    /**
     * Builds a display-safe preview of the field and typed characters.
     *
     * @param element the Selenium page element being inspected or acted upon
     * @param keys the keystrokes that will be abbreviated for display
     * @return an abbreviated, presentation-safe preview of the keyboard input
     */
    private String inputPreview(WebElement element, CharSequence... keys) {
        try {
            if ("password".equalsIgnoreCase(element.getDomAttribute("type"))) return "Input: [password hidden]";

            StringBuilder text = new StringBuilder();
            for (CharSequence key : keys) text.append(key);

            String value = text.toString().replace("\r", "\\r").replace("\n", "\\n");
            return "Input: " + (value.length() > 100 ? value.substring(0, 100) + "…" : value);
        } catch (RuntimeException e) {
            return "Entering input";
        }
    }

    /**
     * Waits for the configured presentation delay when SlowMo is enabled.
     */
    private void pauseForNavigation() {
        try {
            int delay = AppConfig.slowMoDelayMs();
            if (delay > 0) Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Highlights a browser element and waits before the next action.
     *
     * @param element the Selenium page element being inspected or acted upon
     */
    private void highlightAndPause(WebElement element) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript("arguments[0].style.border='4px solid red'", element);
            Thread.sleep(AppConfig.slowMoDelayMs());
            js.executeScript("arguments[0].style.border=''", element);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.debug("Skipped highlight on detached/stale element: {}", e.getMessage());
        }
    }
}
