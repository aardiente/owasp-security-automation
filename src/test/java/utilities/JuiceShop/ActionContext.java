package utilities.JuiceShop;

import org.openqa.selenium.json.Json;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Holds common dependencies for the application-specific action domains. */
abstract class ActionContext {
    protected final JuiceShopApi api;
    protected final Json json;

    /**
     * Shares the existing client and serializer across the action domains for one invocation.
     * @param api configured Juice Shop HTTP client
     * @param json shared JSON serializer
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
