
package automation;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.safari.SafariDriver;
import org.openqa.selenium.safari.SafariOptions;
import org.openqa.selenium.Proxy;

/**
 * Creates Selenium browser instances and applies viewport or ZAP proxy configuration for test execution.
 */
public class BrowserFactory {

    private static final String headless = "--headless=new";
    private static final String zapOptions = "--proxy-bypass-list=<-loopback>";

    /**
     * Formats the configured browser viewport as a browser startup window-size argument.
     *
     * @return the viewport setting used to initialize a new browser
     */
    private static String windowSize() {
        return "--window-size=" + AppConfig.viewportWidth() + "," + AppConfig.viewportHeight();
    }

    /**
     * Applies the configured viewport dimensions to an existing browser session.
     *
     * @param driver the Selenium WebDriver controlling the application browser
     */
    public static void setViewport(WebDriver driver) {
        driver.manage().window().setSize(AppConfig.viewport());
    }

    /**
     * Creates a browser driver of the selected type using the configured headless and viewport settings.
     *
     * @param brow the browser implementation selected for the driver
     * @return a new WebDriver for the requested browser type
     */
    public static WebDriver createDriver(BrowserType brow) {
        switch (brow) {
            case CHROME:
                ChromeOptions chromeOp = new ChromeOptions();
                chromeOp.addArguments(windowSize(), zapOptions);

                if (AppConfig.isHeadless()) chromeOp.addArguments(headless);

                return new ChromeDriver(chromeOp);

            case FIREFOX:
                FirefoxOptions foxOp = new FirefoxOptions();
                foxOp.addArguments("--width=" + AppConfig.viewportWidth());
                foxOp.addArguments("--height=" + AppConfig.viewportHeight());

                if (AppConfig.isHeadless()) foxOp.addArguments("-headless");

                return new FirefoxDriver(foxOp);

            case EDGE:
                EdgeOptions edgeOp = new EdgeOptions();
                edgeOp.addArguments(windowSize(), zapOptions);

                if (AppConfig.isHeadless()) edgeOp.addArguments(headless);

                return new EdgeDriver(edgeOp);

            case SAFARI:
                if (AppConfig.isHeadless()) {
                    throw new IllegalArgumentException("Headless mode is not supported for Safari");
                }

                SafariOptions safariOp = new SafariOptions();
                return new SafariDriver(safariOp);

            default:
                throw new IllegalArgumentException("Unsupported Browser: " + brow);
        }
    }

    /**
     * Creates a Chrome driver configured to send browser traffic through the supplied ZAP proxy.
     *
     * @param zapProxyAddress the host and port of the ZAP proxy used by Selenium
     * @return a new WebDriver configured to route HTTP(S) traffic through ZAP
     */
    public static WebDriver createZapProxy(String zapProxyAddress) {
        Proxy prox = new Proxy();
        prox.setHttpProxy(zapProxyAddress);
        prox.setSslProxy(zapProxyAddress);

        ChromeOptions options = new ChromeOptions();
        options.setProxy(prox);
        options.setAcceptInsecureCerts(true);
        options.addArguments(windowSize(), zapOptions);

        if (AppConfig.isHeadless()) options.addArguments(headless);

        return new ChromeDriver(options);
    }
}
