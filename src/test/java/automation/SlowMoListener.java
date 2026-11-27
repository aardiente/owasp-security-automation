
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
 * Makes browser demonstrations easier to follow by adding optional pauses and visual highlights to browser actions.
 */
public class SlowMoListener implements WebDriverListener {

    private static final Logger log = LoggerFactory.getLogger("org.openqa.selenium");
    private final WebDriver driver;

    /**
     * Retains the underlying WebDriver used by the listener to display presentation cues without requiring callers to supply a driver for every event.
     *
     * @param driver the Selenium WebDriver controlling the application browser
     */
    public SlowMoListener(WebDriver driver) {
        this.driver = driver;
    }

    /**
     * Logs the requested navigation URL before Selenium starts loading the page.
     *
     * @param driver the Selenium WebDriver controlling the application browser
     * @param url the URL to navigate to, inspect, or request
     */
    @Override
    public void beforeGet(WebDriver driver, String url) {
        log.info("Navigating to URL: {}", url);
    }

    /**
     * After a visible demonstration navigation, displays the navigation overlay and applies the current configured delay; internal challenge verification requests are excluded.
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
     * Logs the requested Selenium locator at debug level before element lookup.
     *
     * @param driver the Selenium WebDriver controlling the application browser
     * @param locator the Selenium selector identifying the target element
     */
    @Override
    public void beforeFindElement(WebDriver driver, By locator) {
        log.debug("Finding element: {}", locator);
    }

    /**
     * Logs the click and, when presentation mode is enabled, shows its overlay and highlights the element before the action.
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
     * Logs the input action and, in presentation mode, shows an input preview and applies the configured highlight delay.
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
     * Logs acceptance of a browser alert and invokes the configured presentation result pause.
     *
     * @param alert browser JavaScript alert linked to this event
     */
    @Override
    public void beforeAccept(Alert alert) {
        log.info("Accepting JavaScript alert");
        AppConfig.pauseForResults();
    }

    /**
     * Logs dismissal of a browser alert and invokes the configured presentation result pause.
     *
     * @param alert browser JavaScript alert linked to this event
     */
    @Override
    public void beforeDismiss(Alert alert) {
        log.info("Dismissing JavaScript alert");
        AppConfig.pauseForResults();
    }

    /**
     * Handles a Selenium listener error without altering the underlying test result check.
     *
     * @param target the configured application/report target
     * @param method the HTTP request method or reflected Java method being processed
     * @param args the entry-point arguments (not currently used)
     * @param e the Selenium listener exception linked to the failed action
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
