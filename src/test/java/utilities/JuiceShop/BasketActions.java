package utilities.JuiceShop;

import org.openqa.selenium.json.Json;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_OK;
import utilities.JuiceShop.JuiceShopChallengeActions.BasketAttempt;

/** Application-specific basket actions relocated from the original challenge helper. */
public final class BasketActions extends ActionContext {
    /**
     * Uses the shared per-test HTTP client and JSON serializer.
     * @param api configured application client
     * @param json shared JSON serializer
     */
    BasketActions(JuiceShopApi api, Json json) { super(api, json); }

    /**
     * Performs the view another basket action against the isolated Juice Shop test application.
     *
     * <p>Uses the Juice Shop endpoint {@code /rest/basket/}.</p>
     *
     * @return the request status and basket identifiers describing the access attempt
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public BasketAttempt viewAnotherBasket() throws IOException, InterruptedException {
        var session = api.login();
        int targetBasketId = session.basketId() == 1 ? 2 : 1;
        return new BasketAttempt(session.basketId(), targetBasketId,
                api.get("/rest/basket/" + targetBasketId, session.token()));
    }

    /**
     * Performs the add products to another basket action against the isolated Juice Shop test application.
     *
     * <p>Uses the Juice Shop endpoint {@code /rest/basket/}, {@code /api/Products/}.</p>
     *
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    @SuppressWarnings("unchecked")
    public HttpResponse<String> addProductsToAnotherBasket() throws IOException, InterruptedException {
        var session = api.login();
        int targetBasketId = session.basketId() == 1 ? 2 : 1;

        var basketResponse = api.get("/rest/basket/" + targetBasketId, session.token());
        if (basketResponse.statusCode() != HTTP_OK)
            throw new IllegalStateException("Target basket HTTP " + basketResponse.statusCode());

        var productsResponse = api.get("/api/Products/", session.token());
        if (productsResponse.statusCode() != HTTP_OK)
            throw new IllegalStateException("Products HTTP " + productsResponse.statusCode());

        Map<String, Object> catalog = json.toType(productsResponse.body(), Map.class);
        if (!(catalog.get("data") instanceof List<?> products) || products.isEmpty())
            throw new IllegalStateException("No products available.");

        Map<String, Object> basketData = json.toType(basketResponse.body(), Map.class);
        Object basket = basketData.get("data");
        List<?> existing = basket instanceof Map<?, ?> b && b.get("Products") instanceof List<?> items
                ? items : List.of();

        int productId = -1;
        for (Object item : products) {
            if (!(item instanceof Map<?, ?> product) || !(product.get("id") instanceof Number id)) continue;
            boolean alreadyPresent = existing.stream().anyMatch(entry ->
                    entry instanceof Map<?, ?> p && p.get("id") instanceof Number existingId
                            && existingId.intValue() == id.intValue());
            if (!alreadyPresent) {
                productId = id.intValue();
                break;
            }
        }
        if (productId == -1) throw new IllegalStateException("No unused product available for target basket.");

        String body = "{\"ProductId\":" + productId
                + ",\"BasketId\":\"" + session.basketId()
                + "\",\"quantity\":1,\"BasketId\":\"" + targetBasketId + "\"}";

        var response = api.sendApi("POST", "/api/BasketItems/", body, session.token());
        if (response.statusCode() != HTTP_OK && response.statusCode() != HTTP_CREATED)
            throw new IllegalStateException("Basket manipulation HTTP " + response.statusCode() + " " + response.body());

        return response;
    }

    /**
     * Performs the add product to another basket action against the isolated Juice Shop test application.
     *
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> addProductToAnotherBasket() throws IOException, InterruptedException {
        return addProductsToAnotherBasket();
    }
}
