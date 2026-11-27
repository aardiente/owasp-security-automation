package tests.JuiceShop.ui;

import automation.ZapManager;
import org.openqa.selenium.Alert;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Test;
import pom.JuiceShop.AdministrationPage;
import pom.JuiceShop.LoginPage;
import pom.JuiceShop.PrivacyPolicyPage;
import pom.JuiceShop.ResourcePage;
import pom.JuiceShop.ScoreBoardPage;
import pom.JuiceShop.SearchPage;
import utilities.ScreenshotManager;
import java.net.URI;
import java.time.Duration;
import tests.JuiceShop.JuiceShopChallengeTestBase;

/**
 * Contains Juice Shop challenges tested through the browser. Uses shared page controls and captures evidence of challenge results.
 */
public class JuiceShopUiChallengeTest extends JuiceShopChallengeTestBase {

    /**
     * Checks authentication input validation in the Juice Shop lab. Uses the shared browser test tools to verify and report the recorded outcome.
     */
    @Test(priority = 1, groups = "juice-challenge")
    public void testSqlInjectionLoginBypass() {
        var driver = ZapManager.getDriver();
        LoginPage loginPage = new LoginPage(driver);
        loginPage.loginFromHome(baseUrl, "admin@juice-sh.op'--", "anything");

        ScreenshotManager.capture(driver);
        Assert.assertTrue(loginPage.isUserLoggedIn(), "SQL Injection authentication bypass failed: user is not logged in.");
    }

    /**
     * Checks browser-side input handling in the Juice Shop lab. Uses the shared browser test tools to verify and report the recorded outcome.
     */
    @Test(priority = 2, groups = "juice-challenge")
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

    /**
     * Checks access restrictions on service metrics in the Juice Shop lab. Uses the shared browser test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 4, groups = "juice-challenge")
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

    /**
     * Checks administration-view authorization in the Juice Shop lab. Uses the shared browser test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 5, groups = "juice-challenge")
    public void testAdminSectionChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        new LoginPage(driver).loginFromHome(baseUrl, "admin@juice-sh.op'--", "anything");
        new AdministrationPage(driver).open(baseUrl);

        ScreenshotManager.capture(driver);
        assertChallengeSolved("Admin Section");
    }

    /**
     * Checks availability of security policy information in the Juice Shop lab. Uses the shared browser test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 6, groups = "juice-challenge")
    public void testSecurityPolicyChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        ResourcePage resource = new ResourcePage(driver);
        resource.open(URI.create(baseUrl).resolve("/.well-known/security.txt").toString());
        Assert.assertTrue(resource.bodyText().contains("Contact:"), "Security policy was not retrieved.");

        ScreenshotManager.capture(driver);
        assertChallengeSolved("Security Policy");
    }

    /**
     * Checks browser handling of encoded resource information in the Juice Shop lab. Uses the shared browser test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 7, groups = "juice-challenge")
    public void testMissingEncodingChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        String path = "/assets/public/images/uploads/" +
                "%E1%93%9A%E1%98%8F%E1%97%A2-%23zatschi-%23whoneedsfourlegs-1572600969477.jpg";
        new ResourcePage(driver).open(URI.create(baseUrl).resolve(path).toString());
        Assert.assertTrue(driver.getCurrentUrl().contains("%23zatschi"), "Encoded image request was not made.");

        ScreenshotManager.capture(driver);
        assertChallengeSolved("Missing Encoding");
    }

    /**
     * Checks the application's extra-language challenge in the Juice Shop lab. Uses the shared browser test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 8, groups = "juice-challenge")
    public void testExtraLanguageChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        new ResourcePage(driver).open(URI.create(baseUrl).resolve("/assets/i18n/tlh_AA.json").toString());

        ScreenshotManager.capture(driver);
        assertChallengeSolved("Extra Language");
    }

    /**
     * Checks availability of the privacy policy in the Juice Shop lab. Uses the shared browser test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 9, groups = "juice-challenge")
    public void testPrivacyPolicyChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        new PrivacyPolicyPage(driver).open(baseUrl);

        ScreenshotManager.capture(driver);
        assertChallengeSolved("Privacy Policy");
    }

    /**
     * Checks application error handling in the Juice Shop lab. Uses the shared browser test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 11, groups = "juice-challenge")
    public void testErrorHandlingChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        ResourcePage resource = new ResourcePage(driver);
        resource.open(URI.create(baseUrl).resolve("/rest/products/search?q=%27%3B").toString());
        resource.waitForBodyText("SQLITE_ERROR", 10);

        ScreenshotManager.capture(driver);
        assertChallengeSolved("Error Handling");
    }

    /**
     * Checks lab account password strength in the Juice Shop lab. Uses the shared browser test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 12, groups = "juice-challenge")
    public void testPasswordStrengthChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        LoginPage loginPage = new LoginPage(driver);
        loginPage.loginFromHome(baseUrl, "admin@juice-sh.op", "admin123");
        Assert.assertTrue(loginPage.isUserLoggedIn(), "Administrator login failed.");

        ScreenshotManager.capture(driver);
        assertChallengeSolved("Password Strength");
    }

    /**
     * Checks authentication protection for a designated lab account in the Juice Shop lab. Uses the shared browser test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 13, groups = "juice-challenge")
    public void testLoginBenderChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        LoginPage loginPage = new LoginPage(driver);
        loginPage.loginFromHome(baseUrl, "bender@juice-sh.op'--", "anything");
        Assert.assertTrue(loginPage.isUserLoggedIn(), "Bender login bypass failed.");

        ScreenshotManager.capture(driver);
        assertChallengeSolved("Login Bender");
    }

    /**
     * Checks authentication protection for a designated lab account in the Juice Shop lab. Uses the shared browser test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 14, groups = "juice-challenge")
    public void testLoginJimChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        LoginPage loginPage = new LoginPage(driver);
        loginPage.loginFromHome(baseUrl, "jim@juice-sh.op'--", "anything");
        Assert.assertTrue(loginPage.isUserLoggedIn(), "Jim login bypass failed.");

        ScreenshotManager.capture(driver);
        assertChallengeSolved("Login Jim");
    }

    /**
     * Checks access restrictions on operational logs in the Juice Shop lab. Uses the shared browser test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 15, groups = "juice-challenge")
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

    /**
     * Checks protection of infrastructure configuration files in the Juice Shop lab. Uses the shared browser test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 16, groups = "juice-challenge")
    public void testMisplacedIaCFilesChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        ResourcePage resource = new ResourcePage(driver);
        resource.open(baseUrl + "/infrastructure/terraform/main.tf");
        resource.waitForBodyText("provider", 15);
        Assert.assertTrue(resource.bodyText().contains("resource"), "Terraform configuration was not exposed.");

        ScreenshotManager.capture(driver);
        assertChallengeSolved("Misplaced IaC Files");
    }

    /**
     * Checks the application's additional input-handling challenge in the Juice Shop lab. Uses the shared browser test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 17, groups = "juice-challenge")
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

    /**
     * Checks the application's Easter Egg completion state in the Juice Shop lab. Uses the shared browser test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 18, groups = "juice-challenge")
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

    /**
     * Checks resource input validation in the Juice Shop lab. Uses the shared browser test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 19, groups = "juice-challenge")
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

    /**
     * Checks the application's privacy-policy inspection challenge in the Juice Shop lab. Uses the shared browser test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 23, groups = "juice-challenge")
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

    /**
     * Checks availability of the sandbox view in the Juice Shop lab. Uses the shared browser test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 29, groups = "juice-challenge")
    public void testWeb3SandboxChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        driver.get(baseUrl + "/#/web3-sandbox");
        ScreenshotManager.capture(driver);
        assertChallengeSolved("Web3 Sandbox");
    }

    /**
     * Checks authentication protection for a designated lab account in the Juice Shop lab. Uses the shared browser test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 33, groups = "juice-challenge")
    public void testLoginMcSafeSearchChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        new LoginPage(driver).loginFromHome(baseUrl, "mc.safesearch@juice-sh.op", "Mr. N00dles");
        ScreenshotManager.capture(driver);
        assertChallengeSolved("Login MC SafeSearch");
    }

    /**
     * Checks authentication protection for a designated lab account in the Juice Shop lab. Uses the shared browser test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 34, groups = "juice-challenge")
    public void testLoginAmyChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        new LoginPage(driver).loginFromHome(baseUrl, "amy@juice-sh.op", "K1f.....................");
        ScreenshotManager.capture(driver);
        assertChallengeSolved("Login Amy");
    }

    /**
     * Checks protection of password-derived information in the Juice Shop lab. Uses the shared browser test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 35, groups = "juice-challenge")
    public void testPasswordHashLeakChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        new LoginPage(driver).loginFromHome(baseUrl, "admin@juice-sh.op", "admin123");
        ResourcePage resource = new ResourcePage(driver);
        resource.open(baseUrl + "/rest/user/whoami?fields=password");
        ScreenshotManager.capture(driver);
        assertChallengeSolved("Password Hash Leak");
    }

    /**
     * Checks the application's blockchain-related challenge in the Juice Shop lab. Uses the shared browser test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 41, groups = "juice-challenge")
    public void testBlockchainHypeChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        driver.get(baseUrl + "/#/tokensale-ico-ea");
        ScreenshotManager.capture(driver);
        assertChallengeSolved("Blockchain Hype");
    }

    /**
     * Checks browser handling of cross-site image resources in the Juice Shop lab. Uses the shared browser test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 61, groups = "juice-challenge")
    public void testCrossSiteImagingChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        new LoginPage(driver).loginFromHome(baseUrl, "admin@juice-sh.op", "admin123");

        driver.get(actions.browser().crossSiteImagingUrl(baseUrl));

        actions.waitForChallenge("Cross-Site Imaging");
        ScreenshotManager.capture(driver);
        assertChallengeSolved("Cross-Site Imaging");
    }

    /**
     * Checks authentication protection for a designated lab account in the Juice Shop lab. Uses the shared browser test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 62, groups = "juice-challenge")
    public void testLoginBjoernChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        new LoginPage(driver).loginFromHome(baseUrl, "bjoern.kimminich@gmail.com", "bW9jLmxpYW1nQGhjaW5pbW1pay5ucmVvamI=");
        ScreenshotManager.capture(driver);
        assertChallengeSolved("Login Bjoern");
    }

    /**
     * Checks authentication protection for a designated lab account in the Juice Shop lab. Uses the shared browser test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 63, groups = "juice-challenge")
    public void testLoginSupportTeamChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        new LoginPage(driver).loginFromHome(baseUrl, "support@juice-sh.op", "J6aVjTgOpRs@?5l!Zkq2AYnCE@RF$P");
        ScreenshotManager.capture(driver);
        assertChallengeSolved("Login Support Team");
    }

    /**
     * Checks account-data erasure behavior in the Juice Shop lab. Uses the shared browser test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 64, groups = "juice-challenge")
    public void testGdprDataErasureChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        new LoginPage(driver).loginFromHome(baseUrl, "chris.pike@juice-sh.op'--", "anything");
        ScreenshotManager.capture(driver);
        assertChallengeSolved("GDPR Data Erasure");
    }

    /**
     * Checks account authentication integrity in the Juice Shop lab. Uses the shared browser test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 65, groups = "juice-challenge")
    public void testEphemeralAccountantChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        new LoginPage(driver).loginFromHome(baseUrl, actions.account().ephemeralAccountantLogin(), "anything");
        ScreenshotManager.capture(driver);
        assertChallengeSolved("Ephemeral Accountant");
    }
}
