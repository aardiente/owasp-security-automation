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
 * Records browser actions and errors in the test log to help explain what happened during a run.
 */
public class SeleniumActionLogger implements WebDriverListener {

    // Matches the "org.openqa.selenium" logger in log4j2.xml -> writes to logs/selenium.log
    private static final Logger log = LoggerFactory.getLogger("org.openqa.selenium");

    /**
     * Logs a browser navigation before it begins.
     *
     * @param driver the Selenium WebDriver controlling the application browser
     * @param url the URL to navigate to, inspect, or request
     */
    @Override
    public void beforeGet(WebDriver driver, String url) {
        log.info("Navigating to URL: {}", url);
    }

    /**
     * Logs an element search before it begins.
     *
     * @param driver the Selenium WebDriver controlling the application browser
     * @param locator the Selenium selector identifying the target element
     */
    @Override
    public void beforeFindElement(WebDriver driver, By locator) {
        log.debug("Locating element: {}", locator);
    }

    /**
     * Logs a browser click before it occurs.
     *
     * @param element the Selenium page element being inspected or acted upon
     */
    @Override
    public void beforeClick(WebElement element) {
        log.info("Clicking element: {}", element);
    }

    /**
     * Logs a browser typing action before it occurs.
     *
     * @param element the Selenium page element being inspected or acted upon
     * @param keysToSend the keystrokes provided to the selected Selenium element
     */
    @Override
    public void beforeSendKeys(WebElement element, CharSequence... keysToSend) {
        log.info("Sending keys {} to element: {}", Arrays.toString(keysToSend), element);
    }

    /**
     * Records a browser error in the log.
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
}
