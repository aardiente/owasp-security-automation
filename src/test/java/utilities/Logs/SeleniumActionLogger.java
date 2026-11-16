package utilities.Logs;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.events.WebDriverListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Utility component for the selenium action logger functionality in the security automation framework.
 */
public class SeleniumActionLogger implements WebDriverListener {

    // Matches the "org.openqa.selenium" logger in log4j2.xml -> writes to logs/selenium.log
    private static final Logger log = LoggerFactory.getLogger("org.openqa.selenium");

    /**
     * Handles the pre-action event for get.
     *
     * @param driver the Selenium WebDriver controlling the application browser
     * @param url the URL to navigate to, inspect, or request
     */
    @Override
    public void beforeGet(WebDriver driver, String url) {
        log.info("Navigating to URL: {}", url);
    }

    /**
     * Handles the pre-action event for find element.
     *
     * @param driver the Selenium WebDriver controlling the application browser
     * @param locator the Selenium selector identifying the target element
     */
    @Override
    public void beforeFindElement(WebDriver driver, By locator) {
        log.debug("Locating element: {}", locator);
    }

    /**
     * Handles the pre-action event for click.
     *
     * @param element the Selenium page element being inspected or acted upon
     */
    @Override
    public void beforeClick(WebElement element) {
        log.info("Clicking element: {}", element);
    }

    /**
     * Handles the pre-action event for send keys.
     *
     * @param element the Selenium page element being inspected or acted upon
     * @param keysToSend the keystrokes provided to the selected Selenium element
     */
    @Override
    public void beforeSendKeys(WebElement element, CharSequence... keysToSend) {
        log.info("Sending keys {} to element: {}", Arrays.toString(keysToSend), element);
    }

    /**
     * Handles the event for error.
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
}
