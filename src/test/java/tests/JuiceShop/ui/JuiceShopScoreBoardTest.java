package tests.JuiceShop.ui;

import automation.ZapManager;
import org.testng.Assert;
import org.testng.annotations.Test;
import pom.JuiceShop.LoginPage;
import pom.JuiceShop.ScoreBoardPage;
import utilities.ScreenshotManager;
import tests.JuiceShop.JuiceShopChallengeTestBase;

/** Browser-driven and mixed-channel challenge checks. */
public class JuiceShopScoreBoardTest extends JuiceShopChallengeTestBase {

    /**
     * Dismisses Juice Shop notifications and verifies completion of the Mass Dispel challenge.
     *
     * <p>Calls {@code dismissChallengeNotifications}, {@code waitForChallenge} through the shared challenge-action helper.</p>
     * <p>Checks the Score Board challenge state for {@code Mass Dispel}.</p>
     * <p>Saves a browser screenshot for evidence.</p>
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
     * Verifies the Juice Shop Score Board after the other challenge tests and exports challenge status JSON.
     *
     * <p>Uses the LoginPage, ScoreBoardPage page objects for browser interaction.</p>
     * <p>Checks the Score Board challenge state for {@code Score Board}.</p>
     * <p>Saves a browser screenshot for evidence.</p>
     * <p>Runs at the final TestNG priority and exports challenge completion data after taking the Score Board screenshot.</p>
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
