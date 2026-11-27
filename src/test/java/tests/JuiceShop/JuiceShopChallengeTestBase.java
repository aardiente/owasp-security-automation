package tests.JuiceShop;

import automation.ZapManager;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import utilities.JuiceShop.JuiceShopApi;
import utilities.JuiceShop.JuiceShopChallengeActions;
import utilities.ScreenshotManager;
import java.net.http.HttpResponse;

/**
 * Provides shared tools for Juice Shop challenge tests. Checks challenge completion and captures evidence without repeating the same setup in every test.
 */
public abstract class JuiceShopChallengeTestBase extends JuiceShopBaseTest {
    protected JuiceShopApi api;
    protected JuiceShopChallengeActions actions;

    /**
     * Creates fresh test helpers for each test run without sharing mutable sessions.
     */
    @BeforeMethod
    public void prepareChallengeUtilities() {
        api = new JuiceShopApi(baseUrl);
        actions = new JuiceShopChallengeActions(api);
    }

    /**
     * Verifies a named challenge against the application scoreboard.
     *
     * @param challengeName exact name in the lab challenge catalog
     * @throws Exception if the catalog cannot be retrieved
     */
    protected void assertChallengeSolved(String challengeName) throws Exception {
        Assert.assertTrue(api.isChallengeSolved(challengeName), "Challenge not completed: " + challengeName);
        System.out.println("[SOLVED] " + challengeName);
    }

    /**
     * Displays an HTTP response using the configured evidence browser, without changing the API test's result checks.
     *
     * @param response HTTP response to preserve as visual evidence
     */
    protected void captureResponseEvidence(HttpResponse<String> response) {
        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
    }
}
