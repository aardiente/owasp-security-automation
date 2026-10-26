package utilities.JuiceShop;

import automation.ZapManager;
import org.openqa.selenium.json.Json;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public final class JuiceShopApi {

    private final String baseUrl;
    private final HttpClient client;
    private final Json json = new Json();

    public record JuiceSession(String token, int basketId, int userId) {}

    public JuiceShopApi(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;

        URI proxyUri = URI.create("http://" + ZapManager.getProxyAddress());
        ProxySelector proxy = ProxySelector.of(new InetSocketAddress(proxyUri.getHost(), proxyUri.getPort()));

        client = HttpClient.newBuilder()
                .proxy(proxy)
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return sendApi("GET", path, null, null);
    }

    public HttpResponse<String> get(String path, String token) throws IOException, InterruptedException {
        return sendApi("GET", path, null, token);
    }

    public HttpResponse<String> post(String path, Object body) throws IOException, InterruptedException {
        return sendApi("POST", path, json.toJson(body), null);
    }

    public HttpResponse<String> post(String path, Object body, String token) throws IOException, InterruptedException {
        return sendApi("POST", path, json.toJson(body), token);
    }

    public HttpResponse<String> put(String path, Object body, String token) throws IOException, InterruptedException {
        return sendApi("PUT", path, json.toJson(body), token);
    }

    public HttpResponse<String> delete(String path, String token) throws IOException, InterruptedException {
        return sendApi("DELETE", path, null, token);
    }

    public HttpResponse<String> sendApi(String method, String path, String body, String token)
            throws IOException, InterruptedException {

        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        URI uri = URI.create(baseUrl + normalizedPath);

        HttpRequest.Builder request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(15))
                .header("User-Agent", "JuiceShopSecurityAutomation/1.0")
                .header("Accept", "application/json");

        if (token != null && !token.isBlank()) request.header("Authorization", "Bearer " + token);
        if (body != null) request.header("Content-Type", "application/json");

        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8);

        HttpResponse<String> response = client.send(
                request.method(method.toUpperCase(), publisher).build(),
                HttpResponse.BodyHandlers.ofString()
        );

        System.out.println("[JUICE-SHOP API] " + method.toUpperCase() + " " + normalizedPath + " -> " + response.statusCode());
        return response;
    }

    public JuiceSession login() throws IOException, InterruptedException {
        return login("admin@juice-sh.op", "admin123");
    }

    @SuppressWarnings("unchecked")
    public JuiceSession login(String email, String password) throws IOException, InterruptedException {
        HttpResponse<String> response = post("/rest/user/login", Map.of("email", email, "password", password));

        if (response.statusCode() != 200) {
            throw new IllegalStateException("Juice Shop API login failed. HTTP " + response.statusCode());
        }

        Map<String, Object> result = json.toType(response.body(), Map.class);
        Map<String, Object> authentication = (Map<String, Object>) result.get("authentication");

        if (authentication == null) throw new IllegalStateException("Authentication response is missing.");

        String token = (String) authentication.get("token");
        if (token == null || token.isBlank()) throw new IllegalStateException("Authentication token is missing.");

        Object basketValue = authentication.get("bid");
        if (!(basketValue instanceof Number)) throw new IllegalStateException("Authentication basket ID is missing.");

        int basketId = ((Number) basketValue).intValue();
        int userId = extractUserId(token);

        System.out.println("[JUICE-SHOP API] Authenticated user ID: " + userId);
        return new JuiceSession(token, basketId, userId);
    }

    @SuppressWarnings("unchecked")
    private int extractUserId(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) throw new IllegalStateException("Unexpected authentication token format.");

        String decoded = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        Map<String, Object> claims = json.toType(decoded, Map.class);
        Map<String, Object> user = (Map<String, Object>) claims.get("data");

        if (user == null || !(user.get("id") instanceof Number)) {
            throw new IllegalStateException("Authenticated user ID is missing from token.");
        }

        return ((Number) user.get("id")).intValue();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getFeedbackRecords(String responseBody) {
        Map<String, Object> response = json.toType(responseBody, Map.class);
        Object records = response.get("data");

        if (!(records instanceof List<?>)) {
            throw new IllegalStateException("Feedback API did not return a record list.");
        }

        return (List<Map<String, Object>>) records;
    }

    public boolean isChallengeSolved(String challengeName) throws IOException, InterruptedException {
        HttpResponse<String> response = get("/api/Challenges");

        if (response.statusCode() != 200) {
            throw new IllegalStateException("Unable to retrieve Juice Shop challenges. HTTP " + response.statusCode());
        }

        return isChallengeSolved(response.body(), challengeName);
    }

    @SuppressWarnings("unchecked")
    private boolean isChallengeSolved(String responseBody, String challengeName) {
        Map<String, Object> response = json.toType(responseBody, Map.class);
        Object data = response.get("data");

        if (!(data instanceof List<?>)) {
            throw new IllegalStateException("Challenge API did not return a challenge list.");
        }

        for (Object item : (List<?>) data) {
            if (!(item instanceof Map<?, ?> challenge)) continue;
            if (challengeName.equals(challenge.get("name"))) return Boolean.TRUE.equals(challenge.get("solved"));
        }

        throw new IllegalArgumentException("Challenge not found: " + challengeName);
    }
}
