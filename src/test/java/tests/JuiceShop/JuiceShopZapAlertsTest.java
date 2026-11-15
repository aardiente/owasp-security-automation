
package tests.JuiceShop;

import automation.ZapManager;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.zaproxy.clientapi.core.ApiResponseElement;
import utilities.JuiceShop.JuiceShopApi;
import utilities.ScreenshotManager;

import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class JuiceShopZapAlertsTest extends JuiceShopBaseTest {

    private JuiceShopApi api;

    @BeforeMethod
    public void prepareZapTests() {
        api = new JuiceShopApi(baseUrl);
    }

    @Test(priority = 1)
    public void testPublicPagesForZap() {
        WebDriver driver = ZapManager.getDriver();
        driver.get(baseUrl);

        Assert.assertTrue(driver.getCurrentUrl().startsWith(baseUrl),
                "Juice Shop homepage did not load.");

        ScreenshotManager.capture(driver);
    }

    @Test(priority = 2)
    public void testPublicProductApiForZap() throws Exception {
        HttpResponse<String> response = api.get("/api/Products");

        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);

        Assert.assertEquals(response.statusCode(), 200,
                "Product API request failed.");
        Assert.assertFalse(response.body().isBlank(),
                "Product API returned an empty response.");
    }

    @Test(priority = 3)
    public void testProductSearchForZap() throws Exception {
        HttpResponse<String> response = api.get("/rest/products/search?q=apple");

        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);

        Assert.assertEquals(response.statusCode(), 200,
                "Product search request failed.");
        Assert.assertFalse(response.body().isBlank(),
                "Product search returned an empty response.");
    }

    @Test(priority = 4)
    public void testExposedMetricsForZap() throws Exception {
        HttpResponse<String> response = api.get("/metrics");

        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);

        Assert.assertEquals(response.statusCode(), 200,
                "Metrics endpoint was not accessible.");
        Assert.assertTrue(response.body().contains("# HELP"),
                "Expected Prometheus metrics were not returned.");
    }

    @Test(priority = 5)
    public void testPublicFileListingForZap() throws Exception {
        HttpResponse<String> response = api.get("/ftp/");

        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);

        Assert.assertEquals(response.statusCode(), 200,
                "Public FTP directory was not accessible.");
        Assert.assertFalse(response.body().isBlank(),
                "FTP directory returned an empty response.");
    }

    @Test(priority = 6)
    public void testPublicDocumentForZap() throws Exception {
        HttpResponse<String> response = api.get("/ftp/acquisitions.md");

        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);

        Assert.assertEquals(response.statusCode(), 200,
                "Publicly accessible document could not be retrieved.");
        Assert.assertFalse(response.body().isBlank(),
                "Document response was empty.");
    }

    @Test(priority = 7)
    public void testFailedAuthenticationForZap() throws Exception {
        HttpResponse<String> response = api.post("/rest/user/login", Map.of(
                "email", "admin@juice-sh.op",
                "password", "invalid-test-password"
        ));

        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);

        Assert.assertEquals(response.statusCode(), 401,
                "Invalid credentials were not rejected.");
    }

    @Test(priority = 8)
    public void testApplicationErrorForZap() throws Exception {
        HttpResponse<String> response = api.get("/rest/products/search?q=%27%3B");

        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);

        Assert.assertTrue(response.statusCode() >= 500,
                "Expected server error for ZAP application-error detection. HTTP "
                        + response.statusCode());
    }

    @Test(priority = 9)
    public void testApplicationConfigurationForZap() throws Exception {
        HttpResponse<String> response = api.get("/rest/admin/application-configuration");

        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);

        Assert.assertEquals(response.statusCode(), 200,
                "Application configuration request failed.");
        Assert.assertFalse(response.body().isBlank(),
                "Application configuration response was empty.");
    }

    @Test(priority = 10)
    public void testApplicationVersionForZap() throws Exception {
        HttpResponse<String> response = api.get("/rest/admin/application-version");

        ScreenshotManager.captureResponse(ZapManager.getDriver(), response);

        Assert.assertEquals(response.statusCode(), 200,
                "Application version request failed.");
        Assert.assertFalse(response.body().isBlank(),
                "Application version response was empty.");
    }

    @Test(priority = 100)
    public void testVerifyZapAlerts() throws Exception {
        var client = ZapManager.getZapClient();
        long deadline = System.nanoTime() + Duration.ofSeconds(90).toNanos();

        while (true) {
            ApiResponseElement pending = (ApiResponseElement) client.pscan.recordsToScan();
            int remaining = Integer.parseInt(pending.getValue());

            if (remaining == 0) break;

            Assert.assertTrue(System.nanoTime() < deadline,
                    "ZAP passive scan did not complete within 90 seconds.");

            Thread.sleep(500);
        }

        ApiResponseElement result =
                (ApiResponseElement) client.alert.numberOfAlerts(baseUrl, null);

        int alertCount = Integer.parseInt(result.getValue());

        System.out.println("[ZAP] Target: " + baseUrl);
        System.out.println("[ZAP] Passive scan completed.");
        System.out.println("[ZAP] Alerts detected for Juice Shop: " + alertCount);

        ScreenshotManager.capture(ZapManager.getDriver());

        Assert.assertTrue(alertCount > 0,
                "ZAP did not report any alerts for Juice Shop.");
    }
}
