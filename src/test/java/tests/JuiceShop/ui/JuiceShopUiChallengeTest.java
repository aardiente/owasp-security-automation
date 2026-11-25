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

/** Browser-driven and mixed-channel challenge checks. */
public class JuiceShopUiChallengeTest extends JuiceShopChallengeTestBase {

    /**
     * Tests whether an SQL injection login input bypasses Juice Shop authentication in the isolated lab.
     *
     * <p>Uses the LoginPage page object for browser interaction.</p>
     * <p>Evaluates assertTrue assertion against the observed response or page state.</p>
     * <p>Saves a browser screenshot for evidence.</p>
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
     * Exercises DOM-based XSS in the isolated Juice Shop lab and verifies the resulting browser alert.
     *
     * <p>Uses the SearchPage page object for browser interaction.</p>
     * <p>Evaluates assertEquals assertion against the observed response or page state.</p>
     * <p>Saves a browser screenshot for evidence.</p>
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
     * Exercises the Juice Shop Exposed Metrics challenge in the isolated lab and records the test outcome.
     *
     * <p>Requests {@code /metrics} through the API client.</p>
     * <p>Checks the Score Board challenge state for {@code Exposed Metrics}.</p>
     * <p>Saves a browser screenshot for evidence.</p>
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
     * Exercises the Juice Shop Admin Section challenge in the isolated lab and records the test outcome.
     *
     * <p>Uses the LoginPage, AdministrationPage page objects for browser interaction.</p>
     * <p>Checks the Score Board challenge state for {@code Admin Section}.</p>
     * <p>Saves a browser screenshot for evidence.</p>
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
     * Exercises the Juice Shop Security Policy challenge in the isolated lab and records the test outcome.
     *
     * <p>Uses the ResourcePage page object for browser interaction.</p>
     * <p>Checks the Score Board challenge state for {@code Security Policy}.</p>
     * <p>Saves a browser screenshot for evidence.</p>
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
     * Exercises the Juice Shop Missing Encoding challenge in the isolated lab and records the test outcome.
     *
     * <p>Uses the ResourcePage page object for browser interaction.</p>
     * <p>Checks the Score Board challenge state for {@code Missing Encoding}.</p>
     * <p>Saves a browser screenshot for evidence.</p>
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
     * Exercises the Juice Shop Extra Language challenge in the isolated lab and records the test outcome.
     *
     * <p>Uses the ResourcePage page object for browser interaction.</p>
     * <p>Checks the Score Board challenge state for {@code Extra Language}.</p>
     * <p>Saves a browser screenshot for evidence.</p>
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
     * Exercises the Juice Shop Privacy Policy challenge in the isolated lab and records the test outcome.
     *
     * <p>Uses the PrivacyPolicyPage page object for browser interaction.</p>
     * <p>Checks the Score Board challenge state for {@code Privacy Policy}.</p>
     * <p>Saves a browser screenshot for evidence.</p>
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
     * Exercises the Juice Shop Error Handling challenge in the isolated lab and records the test outcome.
     *
     * <p>Uses the ResourcePage page object for browser interaction.</p>
     * <p>Checks the Score Board challenge state for {@code Error Handling}.</p>
     * <p>Saves a browser screenshot for evidence.</p>
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
     * Exercises the Juice Shop Password Strength challenge in the isolated lab and records the test outcome.
     *
     * <p>Uses the LoginPage page object for browser interaction.</p>
     * <p>Checks the Score Board challenge state for {@code Password Strength}.</p>
     * <p>Saves a browser screenshot for evidence.</p>
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
     * Exercises the Juice Shop Login Bender challenge in the isolated lab and records the test outcome.
     *
     * <p>Uses the LoginPage page object for browser interaction.</p>
     * <p>Checks the Score Board challenge state for {@code Login Bender}.</p>
     * <p>Saves a browser screenshot for evidence.</p>
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
     * Exercises the Juice Shop Login Jim challenge in the isolated lab and records the test outcome.
     *
     * <p>Uses the LoginPage page object for browser interaction.</p>
     * <p>Checks the Score Board challenge state for {@code Login Jim}.</p>
     * <p>Saves a browser screenshot for evidence.</p>
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
     * Exercises the Juice Shop Access Log challenge in the isolated lab and records the test outcome.
     *
     * <p>Uses the ResourcePage page object for browser interaction.</p>
     * <p>Checks the Score Board challenge state for {@code Access Log}.</p>
     * <p>Saves a browser screenshot for evidence.</p>
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
     * Exercises the Juice Shop Misplaced IaC Files challenge in the isolated lab and records the test outcome.
     *
     * <p>Uses the ResourcePage page object for browser interaction.</p>
     * <p>Checks the Score Board challenge state for {@code Misplaced IaC Files}.</p>
     * <p>Saves a browser screenshot for evidence.</p>
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
     * Exercises the Juice Shop Bonus Payload challenge in the isolated lab and records the test outcome.
     *
     * <p>Uses the SearchPage page object for browser interaction.</p>
     * <p>Checks the Score Board challenge state for {@code Bonus Payload}.</p>
     * <p>Saves a browser screenshot for evidence.</p>
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
     * Exercises the Juice Shop Easter Egg challenge in the isolated lab and records the test outcome.
     *
     * <p>Requests {@code /ftp/eastere.gg%2500.md} through the API client.</p>
     * <p>Checks the Score Board challenge state for {@code Easter Egg}.</p>
     * <p>Captures a rendered HTTP response as screenshot evidence.</p>
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
     * Exercises the Juice Shop Forgotten Developer Backup challenge in the isolated lab and records the test outcome.
     *
     * <p>Requests {@code /ftp/package.json.bak%2500.md} through the API client.</p>
     * <p>Checks the Score Board challenge state for {@code Forgotten Developer Backup}, {@code Poison Null Byte}.</p>
     * <p>Captures a rendered HTTP response as screenshot evidence.</p>
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
     * Exercises the Juice Shop Privacy Policy Inspection challenge in the isolated lab and records the test outcome.
     *
     * <p>Requests {@code /assets/private/thank-you.jpg} through the API client.</p>
     * <p>Checks the Score Board challenge state for {@code Privacy Policy Inspection}.</p>
     * <p>Saves a browser screenshot for evidence.</p>
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
     * Exercises the Juice Shop Web3 Sandbox challenge in the isolated lab and records the test outcome.
     *
     * <p>Checks the Score Board challenge state for {@code Web3 Sandbox}.</p>
     * <p>Saves a browser screenshot for evidence.</p>
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
     * Exercises the Juice Shop Login MC SafeSearch challenge in the isolated lab and records the test outcome.
     *
     * <p>Uses the LoginPage page object for browser interaction.</p>
     * <p>Checks the Score Board challenge state for {@code Login MC SafeSearch}.</p>
     * <p>Saves a browser screenshot for evidence.</p>
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
     * Exercises the Juice Shop Login Amy challenge in the isolated lab and records the test outcome.
     *
     * <p>Uses the LoginPage page object for browser interaction.</p>
     * <p>Checks the Score Board challenge state for {@code Login Amy}.</p>
     * <p>Saves a browser screenshot for evidence.</p>
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
     * Exercises the Juice Shop Password Hash Leak challenge in the isolated lab and records the test outcome.
     *
     * <p>Uses the LoginPage, ResourcePage page objects for browser interaction.</p>
     * <p>Checks the Score Board challenge state for {@code Password Hash Leak}.</p>
     * <p>Saves a browser screenshot for evidence.</p>
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
     * Exercises the Juice Shop Blockchain Hype challenge in the isolated lab and records the test outcome.
     *
     * <p>Checks the Score Board challenge state for {@code Blockchain Hype}.</p>
     * <p>Saves a browser screenshot for evidence.</p>
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
     * Exercises the Juice Shop Cross-Site Imaging challenge in the isolated lab and records the test outcome.
     *
     * <p>Calls {@code crossSiteImagingUrl}, {@code waitForChallenge} through the shared challenge-action helper.</p>
     * <p>Checks the Score Board challenge state for {@code Cross-Site Imaging}.</p>
     * <p>Saves a browser screenshot for evidence.</p>
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
     * Exercises the Juice Shop Login Bjoern challenge in the isolated lab and records the test outcome.
     *
     * <p>Uses the LoginPage page object for browser interaction.</p>
     * <p>Checks the Score Board challenge state for {@code Login Bjoern}.</p>
     * <p>Saves a browser screenshot for evidence.</p>
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
     * Exercises the Juice Shop Login Support Team challenge in the isolated lab and records the test outcome.
     *
     * <p>Uses the LoginPage page object for browser interaction.</p>
     * <p>Checks the Score Board challenge state for {@code Login Support Team}.</p>
     * <p>Saves a browser screenshot for evidence.</p>
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
     * Exercises the Juice Shop GDPR Data Erasure challenge in the isolated lab and records the test outcome.
     *
     * <p>Uses the LoginPage page object for browser interaction.</p>
     * <p>Checks the Score Board challenge state for {@code GDPR Data Erasure}.</p>
     * <p>Saves a browser screenshot for evidence.</p>
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
     * Exercises the Juice Shop Ephemeral Accountant challenge in the isolated lab and records the test outcome.
     *
     * <p>Calls {@code ephemeralAccountantLogin} through the shared challenge-action helper.</p>
     * <p>Checks the Score Board challenge state for {@code Ephemeral Accountant}.</p>
     * <p>Saves a browser screenshot for evidence.</p>
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
