package utilities.JuiceShop;

import org.openqa.selenium.json.Json;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import utilities.JuiceShop.JuiceShopChallengeActions.FeedbackAttempt;
import utilities.JuiceShop.JuiceShopChallengeActions.FeedbackRemoval;

/**
 * Groups feedback and review operations used by the Juice Shop lab tests.
 */
public final class FeedbackActions extends ActionContext {
    /**
     * Uses the shared per-test HTTP client and JSON JSON converter.
     *
     * @param api configured application client
     * @param json shared JSON JSON converter
     */
    FeedbackActions(JuiceShopApi api, Json json) { super(api, json); }

    /**
     * Collects the feedback attribution observation for the related lab challenge.
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
     * Collects the feedback removal observation, including the resulting feedback state.
     *
     * @return the feedback deletion response and details describing the removed record
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
     * Collects the feedback-rating validation observation for the zero-star case.
     *
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> submitZeroStarFeedback() throws IOException, InterruptedException {
        return submitFeedback("Automated zero-star feedback validation", 0);
    }

    /**
     * Collects the product-review attribution observation for the related lab challenge.
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
     * Submits the challenge-related feedback entry about cryptography for the isolated lab.
     *
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> submitCryptoComplaint() throws IOException, InterruptedException {
        return submitFeedback("The MD5 hashing algorithm should not be used for password storage.");
    }

    /**
     * Submits the challenge-related feedback entry about a vulnerable library for the isolated lab.
     *
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> submitVulnerableLibraryComplaint() throws IOException, InterruptedException {
        return submitFeedback("sanitize-html 1.4.2 is a vulnerable dependency.");
    }

    /**
     * Submits a feedback entry to the lab and returns the response for verification.
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
     * Submits a feedback entry to the lab and returns the response for verification.
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
     * Collects the feedback rate-control observation for the related lab challenge.
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
     * Collects the review interaction consistency observation for the related lab challenge.
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
     * Requests and parses the captcha challenge used by feedback actions. Uses the Juice Shop endpoint {@code /rest/captcha/}.
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
}
