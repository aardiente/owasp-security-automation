package utilities.JuiceShop;

import org.openqa.selenium.json.Json;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Provides common tools for Juice Shop helpers, including the shared API client and JSON handling.
 */
abstract class ActionContext {
    protected final JuiceShopApi api;
    protected final Json json;

    /**
     * Shares the existing client and JSON converter across the action domains for one test run.
     *
     * @param api configured Juice Shop HTTP client
     * @param json shared JSON JSON converter
     */
    ActionContext(JuiceShopApi api, Json json) {
        this.api = api;
        this.json = json;
    }

    /**
     * URL-encodes a value for use in an application request.
     *
     * @param s the string that will be URL-encoded
     * @return percent-encoded query text suitable for a Juice Shop URL
     */
    protected String encode(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20"); }
}
