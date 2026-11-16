
package pom.JuiceShop;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Juice Shop page object exposing Selenium interactions for the score board view.
 */
public class ScoreBoardPage extends BasePage {
    private final By scoreBoard = By.cssSelector("app-score-board");
    private final By challengeCards = By.cssSelector("challenge-card");

    /**
     * Creates the score board page component and initializes the dependencies required by its methods.
     *
     * <p>Creates a score board page instance and initializes its dependencies.</p>
     *
     * @param driver the Selenium WebDriver controlling the application browser
     */
    public ScoreBoardPage(WebDriver driver) {
        super(driver);
    }

    /**
     * Opens the Juice Shop Score Board using the provided application URL.
     *
     * @param baseUrl the base URL of the containerized application under test
     */
    public void open(String baseUrl) {
        driver.get(baseUrl + "#/score-board");
        var pageWait = new WebDriverWait(driver, Duration.ofSeconds(15));
        pageWait.until(ExpectedConditions.visibilityOfElementLocated(scoreBoard));
        pageWait.until(ExpectedConditions.visibilityOfElementLocated(challengeCards));
    }

    /**
     * Checks whether open is present or meets the expected condition.
     *
     * @return {@code true} when the Juice Shop Score Board has loaded
     */
    public boolean isOpen() {
        return driver.getCurrentUrl().contains("#/score-board");
    }
}
