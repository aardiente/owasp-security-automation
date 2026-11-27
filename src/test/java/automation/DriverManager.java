
package automation;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.events.EventFiringDecorator;

import java.time.Duration;

/**
 * Manages browser sessions that run without the ZAP proxy. Creates a browser when needed and closes it when finished.
 */
public class DriverManager {

    private static final ThreadLocal<WebDriver> driver = new ThreadLocal<>();

    /**
     * Prevents direct creation of this utility class.
     */
    private DriverManager() {}

    /**
     * Sets up the current thread's WebDriver and applies configured browser timeouts.
     *
     * @param brow the browser implementation selected for the driver
     */
    public static void initDriver(BrowserType brow) {
        if (driver.get() == null) {
            WebDriver webDriver = BrowserFactory.createDriver(brow);
            BrowserFactory.setViewport(webDriver);
            webDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(AppConfig.implicitWaitSeconds()));

            SlowMoListener listener = new SlowMoListener(webDriver);
            WebDriver decoratedDriver = new EventFiringDecorator<>(listener).decorate(webDriver);
            driver.set(decoratedDriver);
        }
    }

    /**
     * Returns the current thread's WebDriver, initializing one when needed.
     *
     * @return the WebDriver linked to the current test thread
     */
    public static WebDriver getDriver() {
        if (driver.get() == null) initDriver(BrowserType.CHROME);
        return driver.get();
    }

    /**
     * Quits and clears the WebDriver linked to the current thread.
     */
    public static void quitDriver() {
        if (driver.get() == null) return;

        driver.get().quit();
        driver.remove();
    }
}
