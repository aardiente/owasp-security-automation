package pom.Dvwa;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class LoginPage extends BasePage {

    public static final String loginEndpoint = "/login.php";

    private final By usernameField = By.name("username");
    private final By passwordField = By.name("password");
    private final By loginButton = By.name("Login");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

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

    public WebDriver getDriver() {
        return this.driver;
    }
}