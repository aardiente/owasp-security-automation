
package utilities.JuiceShop;

import automation.ZapManager;
import automation.AppConfig;
import org.openqa.selenium.json.Json;

import java.io.ByteArrayOutputStream;
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
import java.util.UUID;
import java.nio.file.Path;
import java.nio.file.Files;

/**
 * Wraps Juice Shop HTTP endpoints, login sessions, challenge records, and multipart uploads for the test helpers.
 */
public final class JuiceShopApi {

    private final String baseUrl;
    private final HttpClient client;
    private final Json json = new Json();

    public record JuiceSession(String token, int basketId, int userId) {}

    /**
     * Creates the juice shop api component and initializes the dependencies required by its methods.
     *
     * <p>Creates an API helper bound to the specified Juice Shop base URL.</p>
     *
     * @param baseUrl the base URL of the containerized application under test
     */
    public JuiceShopApi(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;

        URI proxyUri = URI.create("http://" + ZapManager.getProxyAddress());
        ProxySelector proxy = ProxySelector.of(
                new InetSocketAddress(proxyUri.getHost(), proxyUri.getPort())
        );

        client = HttpClient.newBuilder()
                .proxy(proxy)
                .connectTimeout(Duration.ofSeconds(AppConfig.juiceShopApiConnectSeconds()))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    /**
     * Sends a GET request to the specified Juice Shop API path, optionally using a session token.
     *
     * @param path the application-relative HTTP endpoint to request
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return sendApi("GET", path, null, null);
    }

    /**
     * Sends a GET request to the specified Juice Shop API path, optionally using a session token.
     *
     * @param path the application-relative HTTP endpoint to request
     * @param token the optional authentication bearer token
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> get(String path, String token) throws IOException, InterruptedException {
        return sendApi("GET", path, null, token);
    }

    /**
     * Sends a JSON POST request to the specified Juice Shop API path, optionally using a session token.
     *
     * @param path the application-relative HTTP endpoint to request
     * @param body the request payload being serialized and submitted to the API
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> post(String path, Object body) throws IOException, InterruptedException {
        return sendApi("POST", path, json.toJson(body), null);
    }

    /**
     * Sends a JSON POST request to the specified Juice Shop API path, optionally using a session token.
     *
     * @param path the application-relative HTTP endpoint to request
     * @param body the request payload being serialized and submitted to the API
     * @param token the optional authentication bearer token
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> post(String path, Object body, String token)
            throws IOException, InterruptedException {
        return sendApi("POST", path, json.toJson(body), token);
    }

    /**
     * Sends an authenticated JSON PUT request to a Juice Shop API endpoint.
     *
     * @param path the application-relative HTTP endpoint to request
     * @param body the request payload being serialized and submitted to the API
     * @param token the optional authentication bearer token
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> put(String path, Object body, String token)
            throws IOException, InterruptedException {
        return sendApi("PUT", path, json.toJson(body), token);
    }

    /**
     * Sends an authenticated HTTP DELETE request to a Juice Shop API endpoint.
     *
     * @param path the application-relative HTTP endpoint to request
     * @param token the optional authentication bearer token
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> delete(String path, String token) throws IOException, InterruptedException {
        return sendApi("DELETE", path, null, token);
    }

    /**
     * Builds and sends an HTTP request to the Juice Shop API with optional body and authorization token.
     *
     * <p>Builds the HTTP request and optionally attaches the session token; callers receive the unmodified HTTP response.</p>
     *
     * @param method the HTTP request method or reflected Java method being processed
     * @param path the application-relative HTTP endpoint to request
     * @param body the request payload being serialized and submitted to the API
     * @param token the optional authentication bearer token
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> sendApi(String method, String path, String body, String token)
            throws IOException, InterruptedException {

        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        HttpRequest.Builder request = requestBuilder(normalizedPath, token, "application/json",
                Duration.ofSeconds(AppConfig.juiceShopApiRequestSeconds()));

        if (body != null)
            request.header("Content-Type", "application/json");

        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8);

        HttpResponse<String> response = client.send(
                request.method(method.toUpperCase(), publisher).build(),
                HttpResponse.BodyHandlers.ofString()
        );

        System.out.println(
                "[JUICE-SHOP API] " + method.toUpperCase()
                        + " " + normalizedPath + " -> " + response.statusCode()
        );

        return response;
    }

    /**
     * Authenticates a Juice Shop user and returns the parsed session details.
     *
     * @return the authenticated session with token and user identifier
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public JuiceSession login() throws IOException, InterruptedException {
        return login("admin@juice-sh.op", "admin123");
    }

    /**
     * Authenticates a Juice Shop user and returns the parsed session details.
     *
     * @param email the email address submitted as the account identifier
     * @param password the password submitted in the login request
     * @return the authenticated session with token and user identifier
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    @SuppressWarnings("unchecked")
    public JuiceSession login(String email, String password) throws IOException, InterruptedException {
        HttpResponse<String> response = post(
                "/rest/user/login",
                Map.of("email", email, "password", password)
        );

        if (response.statusCode() != 200)
            throw new IllegalStateException(
                    "Juice Shop API login failed. HTTP " + response.statusCode()
            );

        Map<String, Object> result = json.toType(response.body(), Map.class);
        Map<String, Object> authentication =
                (Map<String, Object>) result.get("authentication");

        if (authentication == null)
            throw new IllegalStateException("Authentication response is missing.");

        String token = (String) authentication.get("token");

        if (token == null || token.isBlank())
            throw new IllegalStateException("Authentication token is missing.");

        Object basketValue = authentication.get("bid");

        if (!(basketValue instanceof Number))
            throw new IllegalStateException("Authentication basket ID is missing.");

        int basketId = ((Number) basketValue).intValue();
        int userId = extractUserId(token);

        System.out.println("[JUICE-SHOP API] Authenticated user ID: " + userId);

        return new JuiceSession(token, basketId, userId);
    }

    /**
     * Fetches the current Juice Shop Score Board challenge states from the API.
     *
     * <p>Fetches the Juice Shop challenges endpoint.</p>
     *
     * @return the HTTP response containing challenge completion records
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> getChallenges() throws IOException, InterruptedException {
        HttpResponse<String> response = get("/api/Challenges");

        if (response.statusCode() != 200)
            throw new IllegalStateException("Failed to retrieve Juice Shop challenges. HTTP " + response.statusCode());

        return response;
    }

    /**
     * Writes the returned Juice Shop challenge catalog to a destination file.
     *
     * <p>Fetches the challenge status API and writes its JSON response to the requested output file.</p>
     *
     * @param outputFile the destination path for generated output
     * @return the path where the current challenge status JSON was saved
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public Path saveChallenges(Path outputFile) throws IOException, InterruptedException {
        HttpResponse<String> response = get("/api/Challenges");

        if (response.statusCode() != 200)
            throw new IOException("Failed to retrieve challenges. HTTP " + response.statusCode());

        Path destination = outputFile.toAbsolutePath().normalize();
        if (destination.getParent() != null)
            Files.createDirectories(destination.getParent());

        Files.writeString(destination, response.body(), StandardCharsets.UTF_8);
        System.out.println("[JUICE-SHOP] Challenge results saved: " + destination);

        return destination;
    }


    /**
     * Extracts a user ID from a Juice Shop authentication token.
     *
     * @param token the optional authentication bearer token
     * @return the user identifier encoded in the supplied session token
     */
    @SuppressWarnings("unchecked")
    private int extractUserId(String token) {
        String[] parts = token.split("\\.");

        if (parts.length != 3)
            throw new IllegalStateException("Unexpected authentication token format.");

        String decoded = new String(
                Base64.getUrlDecoder().decode(parts[1]),
                StandardCharsets.UTF_8
        );

        Map<String, Object> claims = json.toType(decoded, Map.class);
        Map<String, Object> user = (Map<String, Object>) claims.get("data");

        if (user == null || !(user.get("id") instanceof Number))
            throw new IllegalStateException("Authenticated user ID is missing from token.");

        return ((Number) user.get("id")).intValue();
    }

    /**
     * Parses the submitted API response into feedback objects.
     *
     * <p>Parses feedback records from the supplied API response body.</p>
     *
     * @param responseBody the raw response body returned from Juice Shop
     * @return the feedback records decoded from the response JSON
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getFeedbackRecords(String responseBody) {
        Map<String, Object> response = json.toType(responseBody, Map.class);
        Object records = response.get("data");

        if (!(records instanceof List<?>))
            throw new IllegalStateException("Feedback API did not return a record list.");

        return (List<Map<String, Object>>) records;
    }

    /**
     * Checks the Juice Shop challenge catalog for the named completed challenge.
     *
     * @param challengeName the Score Board name of the Juice Shop challenge to inspect
     * @return {@code true} when the named challenge is marked solved in the scoreboard response
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public boolean isChallengeSolved(String challengeName) throws IOException, InterruptedException {
        HttpResponse<String> response = get("/api/Challenges");

        if (response.statusCode() != 200)
            throw new IllegalStateException(
                    "Unable to retrieve Juice Shop challenges. HTTP " + response.statusCode()
            );

        return isChallengeSolved(response.body(), challengeName);
    }

    /**
     * Checks the Juice Shop challenge catalog for the named completed challenge.
     *
     * @param responseBody the raw response body returned from Juice Shop
     * @param challengeName the Score Board name of the Juice Shop challenge to inspect
     * @return {@code true} when the named challenge is marked solved in the scoreboard response
     */
    @SuppressWarnings("unchecked")
    private boolean isChallengeSolved(String responseBody, String challengeName) {
        Map<String, Object> response = json.toType(responseBody, Map.class);
        Object data = response.get("data");

        if (!(data instanceof List<?>))
            throw new IllegalStateException("Challenge API did not return a challenge list.");

        for (Object item : (List<?>) data) {
            if (!(item instanceof Map<?, ?> challenge)) continue;

            if (challengeName.equals(challenge.get("name")))
                return Boolean.TRUE.equals(challenge.get("solved"));
        }

        throw new IllegalArgumentException("Challenge not found: " + challengeName);
    }

    /**
     * Submits a multipart file upload to the configured Juice Shop endpoint.
     *
     * <p>Formats the multipart request body and returns the server response for assertion in the caller.</p>
     *
     * @param path the application-relative HTTP endpoint to request
     * @param filename the filename associated with a file upload or screenshot
     * @param contentType the uploaded part’s MIME type
     * @param fileContent the bytes uploaded as the multipart file part
     * @param token the optional authentication bearer token
     * @return the HTTP response containing the status code, headers, and response body from the requested endpoint
     * @throws IOException if a required file or HTTP stream cannot be read or written
     * @throws InterruptedException if the current thread is interrupted while waiting for the operation
     */
    public HttpResponse<String> uploadMultipart(
            String path,
            String filename,
            String contentType,
            byte[] fileContent,
            String token
    ) throws IOException, InterruptedException {

        String boundary = "juice-shop-" + UUID.randomUUID();
        ByteArrayOutputStream body = new ByteArrayOutputStream();

        body.write(("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: " + contentType + "\r\n\r\n"
        ).getBytes(StandardCharsets.UTF_8));

        body.write(fileContent);

        body.write(("\r\n--" + boundary + "--\r\n")
                .getBytes(StandardCharsets.UTF_8));

        HttpRequest.Builder builder = requestBuilder(path, token, "*/*",
                Duration.ofSeconds(AppConfig.juiceShopApiUploadSeconds()))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()));

        HttpResponse<String> response = client.send(
                builder.build(),
                HttpResponse.BodyHandlers.ofString()
        );

        System.out.println(
                "[JUICE-SHOP API] multipart POST "
                        + path + " -> " + response.statusCode()
        );

        return response;
    }

    /**
     * Applies common request metadata for the existing JSON and multipart methods.
     * @param path application-relative path
     * @param token optional session token
     * @param accept response media type expected by the caller
     * @param timeout caller-specific request timeout
     * @return configured HTTP request builder
     */
    private HttpRequest.Builder requestBuilder(String path, String token, String accept, Duration timeout) {
        URI uri = URI.create(baseUrl + (path.startsWith("/") ? path : "/" + path));
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(timeout)
                .header("User-Agent", AppConfig.juiceShopApiUserAgent())
                .header("Accept", accept);
        if (token != null && !token.isBlank()) builder.header("Authorization", "Bearer " + token);
        return builder;
    }
}
