package tests.JuiceShop.api;

import org.testng.Assert;
import org.testng.annotations.Test;
import utilities.JuiceShop.JuiceShopApi;
import java.net.URI;
import tests.JuiceShop.JuiceShopChallengeTestBase;

/**
 * Contains API-based Juice Shop tests for shopping baskets, checkout and pricing challenges. Uses shared tools to check completion and capture results.
 */
public class JuiceShopCommerceApiTest extends JuiceShopChallengeTestBase {

    /**
     * Checks basket access authorization in the Juice Shop lab. Uses the shared API test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 26, groups = "juice-challenge")
    public void testViewBasketChallenge() throws Exception {
        var result = actions.basket().viewAnotherBasket();
        System.out.println("[VIEW BASKET] Authenticated basket: " + result.ownBasketId());
        System.out.println("[VIEW BASKET] Requested basket: " + result.targetBasketId());
        Assert.assertNotEquals(result.targetBasketId(), result.ownBasketId(), "Target belongs to the authenticated user.");
        Assert.assertEquals(result.response().statusCode(), 200, "Another user's basket was not accessible.");

        captureResponseEvidence(result.response());
        assertChallengeSolved("View Basket");
    }

    /**
     * Checks basket modification authorization in the Juice Shop lab. Uses the shared API test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 68, groups = "juice-challenge")
    public void testManipulateBasketChallenge() throws Exception {
        var response = actions.basket().addProductToAnotherBasket();

        captureResponseEvidence(response);
        Assert.assertTrue(response.statusCode() == 200 || response.statusCode() == 201,
                "Basket manipulation request failed. HTTP " + response.statusCode());
        assertChallengeSolved("Manipulate Basket");
    }

    /**
     * Checks membership and payment validation in the Juice Shop lab. Uses the shared API test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 89, groups = "juice-challenge")
    public void testDeluxeFraudChallenge() throws Exception {
        String email = "deluxe-test-" + System.nanoTime() + "@example.com";
        String password = "Password123!";
        var registration = api.post("/api/Users/", java.util.Map.of("email", email,
                "password", password, "passwordRepeat", password));
        Assert.assertTrue(registration.statusCode() == 200 || registration.statusCode() == 201,
                "Could not create the standard customer used for the deluxe test.");

        var session = api.login(email, password);
        var response = api.post("/rest/deluxe-membership", java.util.Map.of("paymentMode", ""), session.token());

        captureResponseEvidence(response);
        Assert.assertEquals(response.statusCode(), 200, "Deluxe membership request was rejected.");
        assertChallengeSolved("Deluxe Fraud");
    }

    /**
     * Checks availability rules for retired catalog items in the Juice Shop lab. Uses the shared API test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 90, groups = "juice-challenge")
    public void testChristmasSpecialChallenge() throws Exception {
        var session = api.login();

        var search = api.get("/rest/products/search?q=" + encodePathQuery("'))--"));
        captureResponseEvidence(search);

        Assert.assertEquals(search.statusCode(), 200, "Christmas Special SQL injection request failed.");

        int productId = productIdByName(search.body(), "Christmas Super-Surprise-Box");
        Assert.assertTrue(productId > 0, "Christmas special was not returned by the injected search.");

        addProductToOwnBasket(session, 1, 1);
        var response = addProductToOwnBasket(session, productId, 1);

        Assert.assertTrue(response.statusCode() == 200 || response.statusCode() == 201,
                "Christmas special was not added to the basket. HTTP " + response.statusCode());

        var checkout = api.post("/rest/basket/" + session.basketId() + "/checkout",
                java.util.Map.of(), session.token());

        captureResponseEvidence(checkout);
        Assert.assertTrue(checkout.statusCode() >= 200 && checkout.statusCode() < 300,
                "Christmas special checkout failed. HTTP " + checkout.statusCode());

        assertChallengeSolved("Christmas Special");
    }

    /**
     * Checks checkout price and quantity validation in the Juice Shop lab. Uses the shared API test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 91, groups = "juice-challenge")
    public void testPaybackTimeChallenge() throws Exception {
        var session = api.login();
        var json = new org.openqa.selenium.json.Json();

        var basketResponse = api.get("/rest/basket/" + session.basketId(), session.token());
        Assert.assertEquals(basketResponse.statusCode(), 200, "Could not retrieve the authenticated user's basket.");

        java.util.Map<?, ?> basketResult = json.toType(basketResponse.body(), java.util.Map.class);
        Assert.assertTrue(basketResult.get("data") instanceof java.util.Map<?, ?>, "Basket response contains no data.");

        java.util.Map<?, ?> basket = (java.util.Map<?, ?>) basketResult.get("data");
        Object productsObject = basket.get("Products");
        int basketItemId = -1;

        if (productsObject instanceof java.util.List<?> products) {
            for (Object product : products) {
                if (!(product instanceof java.util.Map<?, ?> entry)) continue;
                if (!(entry.get("BasketItem") instanceof java.util.Map<?, ?> item)) continue;
                if (!(item.get("id") instanceof Number id)) continue;
                basketItemId = id.intValue();
                break;
            }
        }

        if (basketItemId < 1) {
            var catalogResponse = api.get("/api/Products");
            Assert.assertEquals(catalogResponse.statusCode(), 200, "Could not retrieve the product catalog.");

            java.util.Map<?, ?> catalog = json.toType(catalogResponse.body(), java.util.Map.class);
            Assert.assertTrue(catalog.get("data") instanceof java.util.List<?>, "Product catalog is unavailable.");

            int productId = -1;
            for (Object product : (java.util.List<?>) catalog.get("data")) {
                if (product instanceof java.util.Map<?, ?> entry && entry.get("id") instanceof Number id) {
                    productId = id.intValue();
                    break;
                }
            }

            Assert.assertTrue(productId > 0, "No product was available to prepare the basket.");

            var added = addProductToOwnBasket(session, productId, 1);
            captureResponseEvidence(added);
            Assert.assertTrue(added.statusCode() == 200 || added.statusCode() == 201,
                    "Basket preparation failed. HTTP " + added.statusCode());

            basketItemId = recordId(added.body());
        }

        var response = api.put("/api/BasketItems/" + basketItemId,
                java.util.Map.of("quantity", -100), session.token());

        captureResponseEvidence(response);
        Assert.assertEquals(response.statusCode(), 200,
                "Negative basket quantity update failed. HTTP " + response.statusCode());

        var checkout = api.post("/rest/basket/" + session.basketId() + "/checkout",
                java.util.Map.of(), session.token());

        captureResponseEvidence(checkout);
        Assert.assertTrue(checkout.statusCode() >= 200 && checkout.statusCode() < 300,
                "Payback Time checkout failed. HTTP " + checkout.statusCode());

        assertChallengeSolved("Payback Time");
    }

    /**
     * Checks coupon authenticity validation in the Juice Shop lab. Uses the shared API test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 93, groups = "juice-challenge")
    public void testForgedCouponChallenge() throws Exception {
        var session = api.login();
        var json = new org.openqa.selenium.json.Json();

        var basketResponse = api.get("/rest/basket/" + session.basketId(), session.token());
        Assert.assertEquals(basketResponse.statusCode(), 200, "Could not retrieve the authenticated basket.");

        java.util.Map<?, ?> result = json.toType(basketResponse.body(), java.util.Map.class);
        Assert.assertTrue(result.get("data") instanceof java.util.Map<?, ?>, "Basket response is missing data.");

        java.util.Map<?, ?> basket = (java.util.Map<?, ?>) result.get("data");
        Assert.assertTrue(basket.get("Products") instanceof java.util.List<?>, "Basket products are missing.");

        java.util.List<?> products = (java.util.List<?>) basket.get("Products");

        if (products.isEmpty()) {
            var catalogResponse = api.get("/api/Products");
            Assert.assertEquals(catalogResponse.statusCode(), 200, "Product catalog request failed.");

            java.util.Map<?, ?> catalog = json.toType(catalogResponse.body(), java.util.Map.class);
            Assert.assertTrue(catalog.get("data") instanceof java.util.List<?>, "Product catalog is empty or invalid.");

            int productId = -1;
            for (Object product : (java.util.List<?>) catalog.get("data")) {
                if (product instanceof java.util.Map<?, ?> entry && entry.get("id") instanceof Number id) {
                    productId = id.intValue();
                    break;
                }
            }

            Assert.assertTrue(productId > 0, "No valid product was found.");

            var added = addProductToOwnBasket(session, productId, 1);
            captureResponseEvidence(added);
            Assert.assertTrue(added.statusCode() == 200 || added.statusCode() == 201,
                    "Basket preparation failed. HTTP " + added.statusCode());
        }

        String couponPlaintext = java.time.LocalDate.now().format(
                        java.time.format.DateTimeFormatter.ofPattern("MMMyy", java.util.Locale.ENGLISH))
                .toUpperCase(java.util.Locale.ROOT) + "-99";

        String couponCode = z85Encode(couponPlaintext);
        var response = api.put("/rest/basket/" + session.basketId() + "/coupon/" + encodePathQuery(couponCode),
                java.util.Map.of(), session.token());

        captureResponseEvidence(response);
        Assert.assertEquals(response.statusCode(), 200,
                "Forged coupon was rejected. HTTP " + response.statusCode());

        var checkout = api.post("/rest/basket/" + session.basketId() + "/checkout",
                java.util.Map.of(), session.token());

        captureResponseEvidence(checkout);
        Assert.assertTrue(checkout.statusCode() >= 200 && checkout.statusCode() < 300,
                "Forged coupon checkout failed. HTTP " + checkout.statusCode());

        assertChallengeSolved("Forged Coupon");
    }

    /**
     * Checks coupon expiration enforcement in the Juice Shop lab. Uses the shared API test tools to verify and report the recorded outcome.
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 94, groups = "juice-challenge")
    public void testExpiredCouponChallenge() throws Exception {

        String email = "expired-coupon-" + System.nanoTime() + "@example.com";
        String password = "Password123!";

        var registration = api.post("/api/Users/", java.util.Map.of(
                "email", email,
                "password", password,
                "passwordRepeat", password));

        Assert.assertTrue(registration.statusCode() == 200 || registration.statusCode() == 201,
                "Could not register the coupon test account. HTTP " + registration.statusCode());

        var session = api.login(email, password);

        var catalogResponse = api.get("/api/Products");
        Assert.assertEquals(catalogResponse.statusCode(), 200, "Could not retrieve the product catalog.");

        java.util.Map<?, ?> catalog = new org.openqa.selenium.json.Json()
                .toType(catalogResponse.body(), java.util.Map.class);

        Assert.assertTrue(catalog.get("data") instanceof java.util.List<?>,
                "Product catalog did not contain a product list.");

        int productId = -1;
        for (Object product : (java.util.List<?>) catalog.get("data")) {
            if (!(product instanceof java.util.Map<?, ?> entry)) continue;
            if (!(entry.get("id") instanceof Number id)) continue;
            if (entry.get("deletedAt") != null) continue;
            if (entry.get("quantity") instanceof Number quantity && quantity.intValue() <= 0) continue;

            productId = id.intValue();
            break;
        }

        Assert.assertTrue(productId > 0, "No available product was found.");

        var added = addProductToOwnBasket(session, productId, 1);
        Assert.assertTrue(added.statusCode() == 200 || added.statusCode() == 201,
                "Failed to prepare coupon basket. HTTP " + added.statusCode());

        String coupon = "WMNSDY2019";
        long campaignDate = 1551999600000L;

        String couponData = java.util.Base64.getEncoder().encodeToString(
                (coupon + "-" + campaignDate).getBytes(java.nio.charset.StandardCharsets.UTF_8));

        var checkout = api.post("/rest/basket/" + session.basketId() + "/checkout",
                java.util.Map.of("couponData", couponData), session.token());

        captureResponseEvidence(checkout);

        Assert.assertTrue(checkout.statusCode() >= 200 && checkout.statusCode() < 300,
                "Expired coupon checkout failed. HTTP " + checkout.statusCode());

        assertChallengeSolved("Expired Coupon");
    }

    /**
     * Adds a test product to the authenticated user's own basket before the basket challenges.
     *
     * @param session the authenticated Juice Shop session containing the account and bearer token
     * @param productId the numeric product id used by the method
     * @param quantity the numeric quantity used by the method
     * @return the HTTP response linked to the requested Juice Shop action
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    private java.net.http.HttpResponse<String> addProductToOwnBasket(JuiceShopApi.JuiceSession session, int productId, int quantity) throws Exception {
        return api.post("/api/BasketItems/", java.util.Map.of("BasketId", session.basketId(),
                "ProductId", productId, "quantity", quantity), session.token());
    }

    /**
     * Finds the Juice Shop product identifier for a provided product name.
     *
     * @param body the request payload being serialized and submitted to the API
     * @param expectedName the product name whose matching identifier is requested
     * @return the Juice Shop product identifier matching the provided product name
     */
    @SuppressWarnings("unchecked")
    private int productIdByName(String body, String expectedName) {
        java.util.Map<String, Object> result = new org.openqa.selenium.json.Json().toType(body, java.util.Map.class);
        Object data = result.get("data");
        if (!(data instanceof java.util.List<?> products)) return -1;

        for (Object item : products) {
            if (!(item instanceof java.util.Map<?, ?> product)) continue;
            if (!String.valueOf(product.get("name")).contains(expectedName)) continue;
            if (product.get("id") instanceof Number id) return id.intValue();
        }
        return -1;
    }

    /**
     * Extracts an ID from the JSON record used in the challenge test.
     *
     * @param body the request payload being serialized and submitted to the API
     * @return the feedback or review record identifier extracted from the response
     */
    private int recordId(String body) {
        java.util.Map<?, ?> result = new org.openqa.selenium.json.Json().toType(body, java.util.Map.class);
        if (result.get("data") instanceof java.util.Map<?, ?> record && record.get("id") instanceof Number id)
            return id.intValue();
        throw new IllegalStateException("Basket item response did not contain an ID.");
    }

    /**
     * URL-encodes a request path or query segment for challenge probes.
     *
     * @param value the input value to convert or display
     * @return the URI query component generated from the provided request parameter
     */
    private String encodePathQuery(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Encodes input bytes using the Z85 alphabet for a Juice Shop challenge.
     *
     * @param text the input string being standardized, sanitized, or rendered
     * @return the Z85-encoded representation of the input bytes
     */
    private String z85Encode(String text) {
        String alphabet = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ.-:+=^!/*?&<>()[]{}@%$#";
        byte[] bytes = text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (bytes.length % 4 != 0) throw new IllegalArgumentException("Z85 input length must be a multiple of 4.");

        StringBuilder result = new StringBuilder(bytes.length * 5 / 4);
        for (int i = 0; i < bytes.length; i += 4) {
            long value = ((bytes[i] & 0xffL) << 24) | ((bytes[i + 1] & 0xffL) << 16)
                    | ((bytes[i + 2] & 0xffL) << 8) | (bytes[i + 3] & 0xffL);
            char[] encoded = new char[5];
            for (int pos = 4; pos >= 0; pos--) {
                encoded[pos] = alphabet.charAt((int) (value % 85));
                value /= 85;
            }
            result.append(encoded);
        }
        return result.toString();
    }
}
