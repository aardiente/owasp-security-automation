package pom.Dvwa;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * DVWA page object exposing Selenium interactions for the xss reflected module.
 */
public class XssReflectedPage extends BasePage {

    private static final String xssReflectedEndpoint = "vulnerabilities/xss_r/";
    // 💡 Static final locators that never change
    private static final By NAME_INPUT_FIELD = By.name("name");
    private static final By SUBMIT_BUTTON = By.cssSelector("input[value='Submit']");

     /**
      * Creates the xss reflected page component and initializes the dependencies required by its methods.
      *
      * <p>Creates a XSS reflected page instance and initializes its dependencies.</p>
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

