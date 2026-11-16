package pom.JuiceShop;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Juice Shop page object exposing Selenium interactions for the administration view.
 */
public class AdministrationPage extends BasePage {
    private final By administration = By.cssSelector("app-administration");

    /**
     * Creates the administration page component and initializes the dependencies required by its methods.
     *
     * <p>Creates a administration page instance and initializes its dependencies.</p>
     *
     * @param driver the Selenium WebDriver controlling the application browser
     */
    public AdministrationPage(WebDriver driver) {
        super(driver);
    }

    /**
     * Opens the Juice Shop administration page for the configured application URL.
     *
     * @param baseUrl the base URL of the containerized application under test
     */
    public void open(String baseUrl) {
        driver.get(baseUrl + "#/administration");
        new WebDriverWait(driver, Duration.ofSeconds(15)).until(ExpectedConditions.visibilityOfElementLocated(administration));
    }
}
