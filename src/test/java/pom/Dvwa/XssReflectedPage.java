package pom.Dvwa;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Represents DVWA's reflected cross-site scripting practice page. Manages the page controls so tests can focus on checking the results.
 */
public class XssReflectedPage extends BasePage {

    private static final String xssReflectedEndpoint = "vulnerabilities/xss_r/";
    // 💡 Static final locators that never change
    private static final By NAME_INPUT_FIELD = By.name("name");
    private static final By SUBMIT_BUTTON = By.cssSelector("input[value='Submit']");

     /**
      * Sets up the DVWA xss reflected page with the provided browser session.
      *
      * @param driver the Selenium WebDriver controlling the application browser
      */
    public XssReflectedPage(WebDriver driver) {
        super(driver);
    }

     /**
     * Submits a reflected-XSS payload through the DVWA test form.
     *
     * @param payload the test input submitted to the intentionally vulnerable lab application
     */
    public void injectPayload(String payload) {
        driver.findElement(NAME_INPUT_FIELD).clear();
        driver.findElement(NAME_INPUT_FIELD).sendKeys(payload);
        driver.findElement(SUBMIT_BUTTON).click();
    }
}

