package tests.JuiceShop.api;

import org.testng.Assert;
import org.testng.annotations.Test;
import tests.JuiceShop.JuiceShopChallengeTestBase;

/** HTTP-driven challenge checks with shared response evidence. */
public class JuiceShopStateApiTest extends JuiceShopChallengeTestBase {

    /**
     * Exercises the Juice Shop Repetitive Registration challenge in the isolated lab and records the test outcome.
     *
     * <p>Calls {@code registerWithMismatchedPasswords} through the shared challenge-action helper.</p>
     * <p>Checks the Score Board challenge state for {@code Repetitive Registration}.</p>
     * <p>Captures a rendered HTTP response as screenshot evidence.</p>
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
     * Exercises the Juice Shop Empty User Registration challenge in the isolated lab and records the test outcome.
     *
     * <p>Calls {@code registerEmptyUser} through the shared challenge-action helper.</p>
     * <p>Checks the Score Board challenge state for {@code Empty User Registration}.</p>
     * <p>Captures a rendered HTTP response as screenshot evidence.</p>
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
     * Exercises the Juice Shop Forged Feedback challenge in the isolated lab and records the test outcome.
     *
     * <p>Calls {@code submitForgedFeedback} through the shared challenge-action helper.</p>
     * <p>Checks the Score Board challenge state for {@code Forged Feedback}.</p>
     * <p>Captures a rendered HTTP response as screenshot evidence.</p>
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
     * Exercises the Juice Shop Five-Star Feedback challenge in the isolated lab and records the test outcome.
     *
     * <p>Calls {@code removeFiveStarFeedback} through the shared challenge-action helper.</p>
     * <p>Checks the Score Board challenge state for {@code Five-Star Feedback}.</p>
     * <p>Captures a rendered HTTP response as screenshot evidence.</p>
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
     * Exercises the Juice Shop Admin Registration challenge in the isolated lab and records the test outcome.
     *
     * <p>Calls {@code registerWithAdminRole} through the shared challenge-action helper.</p>
     * <p>Checks the Score Board challenge state for {@code Admin Registration}.</p>
     * <p>Captures a rendered HTTP response as screenshot evidence.</p>
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
     * Exercises the Juice Shop Zero Stars challenge in the isolated lab and records the test outcome.
     *
     * <p>Calls {@code submitZeroStarFeedback} through the shared challenge-action helper.</p>
     * <p>Checks the Score Board challenge state for {@code Zero Stars}.</p>
     * <p>Captures a rendered HTTP response as screenshot evidence.</p>
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
     * Exercises the Juice Shop Forged Review challenge in the isolated lab and records the test outcome.
     *
     * <p>Calls {@code submitForgedProductReview} through the shared challenge-action helper.</p>
     * <p>Checks the Score Board challenge state for {@code Forged Review}.</p>
     * <p>Captures a rendered HTTP response as screenshot evidence.</p>
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
     * Exercises the Juice Shop Weird Crypto challenge in the isolated lab and records the test outcome.
     *
     * <p>Calls {@code submitCryptoComplaint} through the shared challenge-action helper.</p>
     * <p>Checks the Score Board challenge state for {@code Weird Crypto}.</p>
     * <p>Captures a rendered HTTP response as screenshot evidence.</p>
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
     * Exercises the Juice Shop Vulnerable Library challenge in the isolated lab and records the test outcome.
     *
     * <p>Calls {@code submitVulnerableLibraryComplaint} through the shared challenge-action helper.</p>
     * <p>Checks the Score Board challenge state for {@code Vulnerable Library}.</p>
     * <p>Captures a rendered HTTP response as screenshot evidence.</p>
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
     * Exercises the Juice Shop Product Tampering challenge in the isolated lab and records the test outcome.
     *
     * <p>Calls {@code tamperWithProductDescription}, {@code waitForChallenge} through the shared challenge-action helper.</p>
     * <p>Checks the Score Board challenge state for {@code Product Tampering}.</p>
     * <p>Captures a rendered HTTP response as screenshot evidence.</p>
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
     * Exercises the Juice Shop NoSQL Manipulation challenge in the isolated lab and records the test outcome.
     *
     * <p>Calls {@code updateAllReviewsViaNoSql} through the shared challenge-action helper.</p>
     * <p>Checks the Score Board challenge state for {@code NoSQL Manipulation}.</p>
     * <p>Captures a rendered HTTP response as screenshot evidence.</p>
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
     * Exercises the Juice Shop CAPTCHA Bypass challenge in the isolated lab and records the test outcome.
     *
     * <p>Calls {@code submitRapidFeedback} through the shared challenge-action helper.</p>
     * <p>Checks the Score Board challenge state for {@code CAPTCHA Bypass}.</p>
     * <p>Captures a rendered HTTP response as screenshot evidence.</p>
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
     * Exercises the Juice Shop Multiple Likes challenge in the isolated lab and records the test outcome.
     *
     * <p>Calls {@code likeReviewConcurrently} through the shared challenge-action helper.</p>
     * <p>Checks the Score Board challenge state for {@code Multiple Likes}.</p>
     * <p>Captures a rendered HTTP response as screenshot evidence.</p>
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
     * Exercises the Juice Shop Upload Type challenge in the isolated lab and records the test outcome.
     *
     * <p>Calls {@code uploadUnexpectedFileType} through the shared challenge-action helper.</p>
     * <p>Checks the Score Board challenge state for {@code Upload Type}.</p>
     * <p>Captures a rendered HTTP response as screenshot evidence.</p>
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
     * Exercises the Juice Shop Upload Size challenge in the isolated lab and records the test outcome.
     *
     * <p>Calls {@code uploadOversizedPdf} through the shared challenge-action helper.</p>
     * <p>Checks the Score Board challenge state for {@code Upload Size}.</p>
     * <p>Captures a rendered HTTP response as screenshot evidence.</p>
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
     * Exercises the Juice Shop Deprecated Interface challenge in the isolated lab and records the test outcome.
     *
     * <p>Calls {@code useDeprecatedXmlUpload} through the shared challenge-action helper.</p>
     * <p>Checks the Score Board challenge state for {@code Deprecated Interface}.</p>
     * <p>Captures a rendered HTTP response as screenshot evidence.</p>
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
