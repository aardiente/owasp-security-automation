
package pom.JuiceShop;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class ScoreBoardPage extends BasePage {
    private final By scoreBoard = By.cssSelector("app-score-board");
    private final By challengeCards = By.cssSelector("challenge-card");

    public ScoreBoardPage(WebDriver driver) {
        super(driver);
    }

    public void open(String baseUrl) {
        driver.get(baseUrl + "#/score-board");
        var pageWait = new WebDriverWait(driver, Duration.ofSeconds(15));
        pageWait.until(ExpectedConditions.visibilityOfElementLocated(scoreBoard));
        pageWait.until(ExpectedConditions.visibilityOfElementLocated(challengeCards));
    }

    public boolean isOpen() {
        return driver.getCurrentUrl().contains("#/score-board");
    }
}
