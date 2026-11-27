package tests.JuiceShop.ui;

import automation.ZapManager;
import org.testng.Assert;
import org.testng.annotations.Test;
import pom.JuiceShop.LoginPage;
import pom.JuiceShop.ScoreBoardPage;
import utilities.ScreenshotManager;
import tests.JuiceShop.JuiceShopChallengeTestBase;

/**
 * Contains the final Juice Shop notification and Score Board checks. Saves the challenge overview for the report.
 */
public class JuiceShopScoreBoardTest extends JuiceShopChallengeTestBase {

    /**
     * Verifies the Mass Dispel challenge using the final notification state of the Juice Shop lab.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 999, dependsOnGroups = "juice-challenge", alwaysRun = true)
    public void testMassDispelChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        driver.get(baseUrl);
        new LoginPage(driver).dismissInitialOverlays();

        actions.browser().dismissChallengeNotifications(driver);
        actions.waitForChallenge("Mass Dispel");

        ScreenshotManager.capture(driver);
        assertChallengeSolved("Mass Dispel");
    }

    /**
     * Opens the final Score Board, checks its recorded completion state, and saves the challenge catalog for reporting.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 1000, dependsOnMethods = "testMassDispelChallenge", alwaysRun = true)
    public void testScoreBoardChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        driver.get(baseUrl);
        new LoginPage(driver).dismissInitialOverlays();

        ScoreBoardPage scoreBoard = new ScoreBoardPage(driver);
        scoreBoard.open(baseUrl);
        Assert.assertTrue(scoreBoard.isOpen(), "Score Board page did not load.");

        ScreenshotManager.capture(driver);
        assertChallengeSolved("Score Board");

        var outputFile = automation.AppConfig.juiceShopChallengesOutput();

        api.saveChallenges(outputFile);
    }
}
