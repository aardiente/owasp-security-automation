package pom.Dvwa;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * Represents the DVWA sign-in page. Enters the lab credentials and provides access to the browser used by tests.
 */
public class LoginPage extends BasePage {

    public static final String loginEndpoint = "/login.php";

    private final By usernameField = By.name("username");
    private final By passwordField = By.name("password");
    private final By loginButton = By.name("Login");

    /**
     * Sets up the DVWA login page with the provided browser session.
     *
     * @param driver the Selenium WebDriver controlling the application browser
     */
    public LoginPage(WebDriver driver) {
        super(driver);
    }

    /**
     * Enters application credentials and submits the DVWA login form.
     *
     * @param username the account name used by the login form
     * @param password the password submitted in the login request
     */
    public void login(String username, String password) {
        WebElement user = wait.until(
                ExpectedConditions.visibilityOfElementLocated(usernameField));
        user.clear();
        user.sendKeys(username);

        WebElement pass = wait.until(
                ExpectedConditions.visibilityOfElementLocated(passwordField));
        pass.clear();
        pass.sendKeys(password);

        wait.until(
                ExpectedConditions.elementToBeClickable(loginButton)).click();

        // Leaving login.php can also mean a setup or error redirect.
        // Require evidence of an authenticated page.
        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.linkText("Logout")));
    }

    /**
     * Returns the WebDriver backing this DVWA login page object.
     *
     * @return the browser driver used by this test execution
     */
    public WebDriver getDriver() {
        return this.driver;
    }
}