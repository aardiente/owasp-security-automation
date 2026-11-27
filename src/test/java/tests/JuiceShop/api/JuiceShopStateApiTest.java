package tests.JuiceShop.api;

import org.testng.Assert;
import org.testng.annotations.Test;
import tests.JuiceShop.JuiceShopChallengeTestBase;

/**
 * Contains API-based Juice Shop tests for account, feedback, catalog and file upload challenges. Uses shared tools to check completion and capture results.
 */
public class JuiceShopStateApiTest extends JuiceShopChallengeTestBase {

    /**
     * Checks consistency of registration input validation in the Juice Shop lab. Uses the shared API test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 24, groups = "juice-challenge")
    public void testRepetitiveRegistrationChallenge() throws Exception {
        var response = actions.account().registerWithMismatchedPasswords();
        captureResponseEvidence(response);
        assertChallengeSolved("Repetitive Registration");
    }

    /**
     * Checks enforcement of required account fields in the Juice Shop lab. Uses the shared API test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 25, groups = "juice-challenge")
    public void testEmptyUserRegistrationChallenge() throws Exception {
        var response = actions.account().registerEmptyUser();
        captureResponseEvidence(response);
        assertChallengeSolved("Empty User Registration");
    }

    /**
     * Checks feedback attribution integrity in the Juice Shop lab. Uses the shared API test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 27, groups = "juice-challenge")
    public void testForgedFeedbackChallenge() throws Exception {
        var result = actions.feedback().submitForgedFeedback();
        System.out.println("[FORGED FEEDBACK] Authenticated user: " + result.authenticatedUserId());
        System.out.println("[FORGED FEEDBACK] Submitted as user: " + result.forgedUserId());
        Assert.assertNotEquals(result.forgedUserId(), result.authenticatedUserId(), "Feedback must reference another user.");
        Assert.assertEquals(result.response().statusCode(), 201, "Forged feedback was not accepted.");

        captureResponseEvidence(result.response());
        assertChallengeSolved("Forged Feedback");
    }

    /**
     * Checks feedback removal authorization in the Juice Shop lab. Uses the shared API test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 28, groups = "juice-challenge")
    public void testFiveStarFeedbackChallenge() throws Exception {
        var result = actions.feedback().removeFiveStarFeedback();
        Assert.assertFalse(result.fiveStarsRemain(), "Five-star customer feedback still exists.");
        System.out.println("[FIVE-STAR FEEDBACK] Entries removed: " + result.deletedCount());

        captureResponseEvidence(result.response());
        assertChallengeSolved("Five-Star Feedback");
    }

    /**
     * Checks account-role assignment authorization in the Juice Shop lab. Uses the shared API test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 30, groups = "juice-challenge")
    public void testAdminRegistrationChallenge() throws Exception {
        var response = actions.account().registerWithAdminRole();
        captureResponseEvidence(response);
        assertChallengeSolved("Admin Registration");
    }

    /**
     * Checks feedback rating bounds in the Juice Shop lab. Uses the shared API test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 31, groups = "juice-challenge")
    public void testZeroStarsChallenge() throws Exception {
        var response = actions.feedback().submitZeroStarFeedback();
        captureResponseEvidence(response);
        assertChallengeSolved("Zero Stars");
    }

    /**
     * Checks review attribution integrity in the Juice Shop lab. Uses the shared API test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 32, groups = "juice-challenge")
    public void testForgedReviewChallenge() throws Exception {
        var response = actions.feedback().submitForgedProductReview();
        captureResponseEvidence(response);
        assertChallengeSolved("Forged Review");
    }

    /**
     * Checks recognition of weak cryptographic practices in the Juice Shop lab. Uses the shared API test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 37, groups = "juice-challenge")
    public void testWeirdCryptoChallenge() throws Exception {
        var response = actions.feedback().submitCryptoComplaint();
        captureResponseEvidence(response);
        assertChallengeSolved("Weird Crypto");
    }

    /**
     * Checks recognition of vulnerable application dependencies in the Juice Shop lab. Uses the shared API test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 38, groups = "juice-challenge")
    public void testVulnerableLibraryChallenge() throws Exception {
        var response = actions.feedback().submitVulnerableLibraryComplaint();
        captureResponseEvidence(response);
        assertChallengeSolved("Vulnerable Library");
    }

    /**
     * Checks catalog update authorization in the Juice Shop lab. Uses the shared API test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 50, groups = "juice-challenge")
    public void testProductTamperingChallenge() throws Exception {
        var response = actions.catalog().tamperWithProductDescription();

        Assert.assertEquals(response.statusCode(), 200, "Product description update failed.");

        captureResponseEvidence(response);
        actions.waitForChallenge("Product Tampering");
        assertChallengeSolved("Product Tampering");
    }

    /**
     * Checks review-data integrity in the Juice Shop lab. Uses the shared API test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 51, groups = "juice-challenge")
    public void testNoSqlManipulationChallenge() throws Exception {
        var response = actions.catalog().updateAllReviewsViaNoSql();
        captureResponseEvidence(response);
        assertChallengeSolved("NoSQL Manipulation");
    }

    /**
     * Checks feedback abuse-control enforcement in the Juice Shop lab. Uses the shared API test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 55, groups = "juice-challenge")
    public void testCaptchaBypassChallenge() throws Exception {
        var response = actions.feedback().submitRapidFeedback(12);
        captureResponseEvidence(response);
        assertChallengeSolved("CAPTCHA Bypass");
    }

    /**
     * Checks review interaction consistency in the Juice Shop lab. Uses the shared API test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 56, groups = "juice-challenge")
    public void testMultipleLikesChallenge() throws Exception {
        var responses = actions.feedback().likeReviewConcurrently();
        Assert.assertEquals(responses.size(), 3, "Expected three review-like requests.");
        captureResponseEvidence(responses.get(0));
        assertChallengeSolved("Multiple Likes");
    }

    /**
     * Checks accepted file-type policy in the Juice Shop lab. Uses the shared API test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 57, groups = "juice-challenge")
    public void testUploadTypeChallenge() throws Exception {
        var response = actions.uploads().uploadUnexpectedFileType();
        captureResponseEvidence(response);
        assertChallengeSolved("Upload Type");
    }

    /**
     * Checks file-size limits in the Juice Shop lab. Uses the shared API test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 58, groups = "juice-challenge")
    public void testUploadSizeChallenge() throws Exception {
        var response = actions.uploads().uploadOversizedPdf();
        captureResponseEvidence(response);
        assertChallengeSolved("Upload Size");
    }

    /**
     * Checks legacy interface security in the Juice Shop lab. Uses the shared API test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 59, groups = "juice-challenge")
    public void testDeprecatedInterfaceChallenge() throws Exception {
        var response = actions.uploads().useDeprecatedXmlUpload();
        captureResponseEvidence(response);
        assertChallengeSolved("Deprecated Interface");
    }
}
