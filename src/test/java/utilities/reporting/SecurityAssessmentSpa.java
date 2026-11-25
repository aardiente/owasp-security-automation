
package utilities.reporting;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.openqa.selenium.json.Json;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/** Local, read-only SPA server. It never starts Docker, Selenium, or ZAP. */
public final class SecurityAssessmentSpa {

    private final ReportingConfiguration config;
    private final Path webDirectory;
    private final List<ReportingTarget> targets;
    private final Json json = new Json();

    private record TestData(List<Map<String, Object>> tests,
                            List<Map<String, Object>> evidence) {}

    /**
     * Creates the security assessment spa component and initializes the dependencies required by its methods.
     *
     * <p>Loads the reporting settings and initializes the configured, read-only SPA targets and asset paths.</p>
     *
     * @throws IOException if a required file or HTTP stream cannot be read or written
     */
    private SecurityAssessmentSpa() throws IOException {
        config = ReportingConfiguration.load();
        webDirectory = config.path("report.spa.assetDirectory");

        if (!Files.isDirectory(webDirectory))
            throw new IOException("SPA asset directory missing: " + webDirectory);

        targets = config.targets();
    }

    /**
     * Starts the read-only security assessment web server on the configured loopback interface.
     *
     * <p>Starts a loopback-only HTTP server; it does not start ZAP, Selenium, Docker, or new scans.</p>
     *
     * @param args the entry-point arguments (not currently used)
     * @throws IOException if a required file or HTTP stream cannot be read or written
     */
    public static void main(String[] args) throws IOException {
        SecurityAssessmentSpa app = new SecurityAssessmentSpa();

        String host = app.config.required("report.spa.host");
        int port;

        try {
            port = Integer.parseInt(app.config.required("report.spa.port"));
        } catch (NumberFormatException e) {
            throw new IOException("Invalid SPA port", e);
        }

        if (port < 1 || port > 65535)
            throw new IOException("SPA port out of range");

        if (!java.net.InetAddress.getByName(host).isLoopbackAddress())
            throw new IOException("SPA host must be a loopback address: " + host);

        HttpServer server = HttpServer.create(new InetSocketAddress(host, port), 0);
        server.createContext("/", app::handle);
        server.start();

        System.out.println("[SECURITY SPA] Running at http://" + host + ":" + port + "/");
        System.out.println("[SECURITY SPA] Reads existing reports only; stop the process to shut down.");
    }

    /**
     * Routes read-only HTTP requests to targets, assessments, screenshots, or static SPA assets.
     *
     * <p>Routes read-only target, assessment, screenshot, and static-asset requests; rejects non-GET methods.</p>
     *
     * @param exchange the incoming HTTP exchange to inspect or respond to
     * @throws IOException if a required file or HTTP stream cannot be read or written
     */
    private void handle(HttpExchange exchange) throws IOException {
        try {
            setHeaders(exchange);

            if (!"GET".equals(exchange.getRequestMethod())) {
                sendText(exchange, 405, "Method not allowed");
                return;
            }

            String path = exchange.getRequestURI().getRawPath();

            if ("/api/targets".equals(path)) {
                sendJson(exchange, 200, targetList());
            } else if (path.startsWith("/api/assessments/")) {
                String id = path.substring("/api/assessments/".length());
                ReportingTarget target = findTarget(id);

                if (target == null) {
                    sendText(exchange, 404, "Unknown assessment");
                    return;
                }

                sendJson(exchange, 200, assessment(target));
            } else if (path.startsWith("/api/screenshots/")) {
                serveScreenshot(exchange,
                        path.substring("/api/screenshots/".length()));
            } else {
                serveStatic(exchange, path);
            }
        } catch (Exception e) {
            System.err.println("[SECURITY SPA] " + e.getMessage());
            sendJson(exchange, 500,
                    Map.of("error", "Unable to load assessment data"));
        } finally {
            exchange.close();
        }
    }

    /**
     * Returns the configured applications exposed by the SPA selector.
     *
     * @return the configured application targets exposed in the SPA dropdown
     */
    private List<Map<String, Object>> targetList() {
        List<Map<String, Object>> result = new ArrayList<>();

        for (ReportingTarget target : targets) {
            result.add(Map.of(
                    "id", target.id(),
                    "title", target.title(),
                    "site", target.siteUrl()
            ));
        }

        return result;
    }

    /**
     * Combines the passive and active ZAP results with tests and screenshots for one application.
     *
     * <p>Includes {@code scan} (passive), {@code activeFindings}, {@code activeScan}, test executions, and evidence for the same target.</p>
     *
     * @param target the configured application/report target
     * @return the JSON-ready assessment containing both scan types, test results, and evidence links for one application
     * @throws IOException if a required file or HTTP stream cannot be read or written
     */
    private Map<String, Object> assessment(ReportingTarget target) throws IOException {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("target", Map.of(
                "id", target.id(),
                "title", target.title(),
                "site", target.siteUrl()
        ));

        // Both reports belong to the same application.
        result.put("scan", loadScan(target, target.zapFile()));

        result.put("activeFindings", loadScan(target,
                target.activeZapFile()));

        result.put("activeScan", loadActiveScan(target));

        TestData tests = loadTests(target);
        result.put("tests", tests.tests());
        result.put("evidence", tests.evidence());
        result.put("loadedAt", Instant.now().toString());

        return result;
    }

    /**
     * Reads saved active-scan status metadata without starting another scan.
     *
     * <p>Reads the scan status JSON separately from the findings file and represents missing files explicitly.</p>
     *
     * @param target the configured application/report target
     * @return the active-scan status fields, or a missing/not-configured status
     * @throws IOException if a required file or HTTP stream cannot be read or written
     */
    private Map<String, Object> loadActiveScan(ReportingTarget target) throws IOException {
        String fileName = target.activeScanFile();

        if (fileName.isBlank())
            return Map.of("status", "not_configured");

        Path source = config.zapFile(fileName);

        if (!Files.isRegularFile(source))
            return Map.of("status", "missing");

        Map<?, ?> saved = json.toType(
                Files.readString(source, StandardCharsets.UTF_8),
                Json.MAP_TYPE
        );

        Map<String, Object> result = new LinkedHashMap<>();

        for (String key : List.of(
                "status", "target", "scanId", "progress",
                "startedAt", "finishedAt", "durationMs", "error")) {
            result.put(key, saved.containsKey(key) ? saved.get(key) : "");
        }

        return result;
    }

    /**
     * Parses a selected ZAP findings file, filtering each alert instance against the application origin and review rules.
     *
     * <p>Separates out-of-scope alert instances from in-scope security findings without modifying the original ZAP JSON.</p>
     *
     * @param target the configured application/report target
     * @param fileName the ZAP findings JSON filename configured for the selected assessment mode
     * @return separately classified in-scope and out-of-scope ZAP observations with report metadata
     * @throws IOException if a required file or HTTP stream cannot be read or written
     */
    private Map<String, Object> loadScan(ReportingTarget target, String fileName)
            throws IOException {

        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> findings = new ArrayList<>();
        List<Map<String, Object>> outOfScopeFindings = new ArrayList<>();

        result.put("findings", findings);
        result.put("outOfScopeFindings", outOfScopeFindings);
        result.put("generated", "");
        result.put("status", "missing");
        result.put("excludedInstances", 0);

        if (fileName.isBlank()) {
            result.put("status", "not_configured");
            return result;
        }

        Path source = config.zapFile(fileName);

        if (!Files.isRegularFile(source))
            return result;

        Map<?, ?> report = json.toType(
                Files.readString(source, StandardCharsets.UTF_8),
                Json.MAP_TYPE
        );

        result.put("generated", str(report.get("@generated")));
        result.put("modifiedAt",
                Files.getLastModifiedTime(source).toInstant().toString());

        FindingFilter filter = new FindingFilter(
                config, target.id(), target.siteUrl());

        int excluded = 0;
        boolean targetSeen = false;

        if (report.get("site") instanceof List<?> sites) {
            for (Object siteRaw : sites) {
                if (!(siteRaw instanceof Map<?, ?> site)) continue;

                if (filter.matchesOrigin(str(site.get("@name"))))
                    targetSeen = true;

                if (!(site.get("alerts") instanceof List<?> alerts))
                    continue;

                for (Object alertRaw : alerts) {
                    if (!(alertRaw instanceof Map<?, ?> alert)) continue;

                    String name = str(alert.get("name"));
                    if (name.isBlank()) name = str(alert.get("alert"));

                    String pluginId = str(alert.get("pluginid"));

                    if (!(alert.get("instances") instanceof List<?> instances))
                        continue;

                    for (Object instanceRaw : instances) {
                        if (!(instanceRaw instanceof Map<?, ?> instance))
                            continue;

                        String url = str(instance.get("uri"));

                        FindingFilter.Classification classification =
                                filter.classify(url, pluginId, name);

                        Map<String, Object> finding = new LinkedHashMap<>();

                        finding.put("name", name);
                        finding.put("pluginId", pluginId);
                        finding.put("severity",
                                severity(str(alert.get("riskcode"))));
                        finding.put("confidence",
                                confidence(str(alert.get("confidence"))));
                        finding.put("status",
                                classification.decision().name());
                        finding.put("reason", classification.reason());
                        finding.put("url", url);
                        finding.put("method", str(instance.get("method")));
                        finding.put("parameter", str(instance.get("param")));
                        finding.put("evidence", str(instance.get("evidence")));
                        finding.put("testInput", str(instance.get("attack")));
                        finding.put("otherInfo",
                                cleanMarkup(str(instance.get("otherinfo"))));
                        finding.put("description",
                                cleanMarkup(str(alert.get("desc"))));
                        finding.put("recommendation",
                                cleanMarkup(str(alert.get("solution"))));
                        finding.put("cwe", str(alert.get("cweid")));

                        if (classification.decision() == FindingFilter.Decision.OUT_OF_SCOPE) {
                            outOfScopeFindings.add(finding);
                            excluded++;
                        } else {
                            findings.add(finding);
                        }
                    }
                }
            }
        }

        Comparator<Map<String, Object>> order = Comparator.comparingInt(
                        (Map<String, Object> f) -> severityOrder(str(f.get("severity"))))
                .thenComparing(f -> str(f.get("name")))
                .thenComparing(f -> str(f.get("url")));
        findings.sort(order);
        outOfScopeFindings.sort(order);

        result.put("status",
                targetSeen ? "loaded" : "target-not-present");
        result.put("excludedInstances", excluded);

        return result;
    }

    /**
     * Loads TestNG CSV rows and resolves matching screenshot evidence for the selected application.
     *
     * <p>Associates TestNG CSV entries and screenshot evidence without assuming ambiguous parameterized-test images belong to one run.</p>
     *
     * @param target the configured application/report target
     * @return the matching TestNG tests and screenshot references for one application
     * @throws IOException if a required file or HTTP stream cannot be read or written
     */
    private TestData loadTests(ReportingTarget target) throws IOException {
        Path csv = config.reportDirectory().resolve(
                config.testResultsFile());

        if (!Files.isRegularFile(csv))
            return new TestData(List.of(), List.of());

        List<List<String>> rows = ReportCsv.parse(
                Files.readString(csv, StandardCharsets.UTF_8));

        if (rows.size() < 2)
            return new TestData(List.of(), List.of());

        List<String> headers = rows.get(0);
        List<Map<String, Object>> tests = new ArrayList<>();

        for (int r = 1; r < rows.size(); r++) {
            List<String> cells = rows.get(r);
            Map<String, String> row = new LinkedHashMap<>();

            for (int c = 0; c < headers.size(); c++) {
                row.put(headers.get(c),
                        c < cells.size() ? cells.get(c) : "");
            }

            String className = row.getOrDefault("Class", "");

            if (!className.startsWith(target.packagePrefix()))
                continue;

            String method = row.getOrDefault("Method", "");
            Map<String, Object> test = new LinkedHashMap<>();

            test.put("suite", row.getOrDefault("Suite", ""));
            test.put("className", className);
            test.put("method", method);
            test.put("status", row.getOrDefault("Status", ""));
            test.put("securityLevel", row.getOrDefault("SecurityLevel", ""));
            test.put("durationMs", row.getOrDefault("DurationMs", ""));
            test.put("error", row.getOrDefault("Error", ""));
            test.put("screenshots", List.of());
            test.put("ambiguousEvidence", false);

            if (headers.contains("Screenshot")) {
                String screenshot = row.getOrDefault("Screenshot", "");
                String link = screenshotLink(
                        target, className, method, screenshot);

                if (link != null)
                    test.put("screenshots", List.of(link));
            }

            tests.add(test);
        }

        tests.sort(Comparator.comparing(
                t -> str(t.get("method"))));

        Map<String, List<Map<String, Object>>> grouped =
                new LinkedHashMap<>();

        for (Map<String, Object> test : tests) {
            String key = test.get("className") + "#" + test.get("method");
            grouped.computeIfAbsent(
                    key, ignored -> new ArrayList<>()).add(test);
        }

        List<Map<String, Object>> evidence = new ArrayList<>();

        for (List<Map<String, Object>> executions : grouped.values()) {
            Map<String, Object> first = executions.get(0);
            String className = str(first.get("className"));
            String method = str(first.get("method"));

            List<String> links = screenshotLinks(
                    target, className, method);

            evidence.add(Map.of(
                    "className", className,
                    "method", method,
                    "screenshots", links
            ));

            if (!headers.contains("Screenshot")) {
                if (executions.size() == 1) {
                    first.put("screenshots", links);
                } else {
                    for (Map<String, Object> test : executions)
                        test.put("ambiguousEvidence", !links.isEmpty());
                }
            }
        }

        return new TestData(tests, evidence);
    }

    /**
     * Verifies and builds a safe screenshot URL for a specific test execution.
     *
     * @param target the configured application/report target
     * @param className the fully-qualified name of the TestNG test class
     * @param method the HTTP request method or reflected Java method being processed
     * @param filename the filename associated with a file upload or screenshot
     * @return the safe SPA screenshot URL, or {@code null} when no valid file matches
     * @throws IOException if a required file or HTTP stream cannot be read or written
     */
    private String screenshotLink(ReportingTarget target, String className,
                                  String method, String filename)
            throws IOException {

        if (filename == null ||
                !filename.matches("[A-Za-z0-9._-]+\\.png"))
            return null;

        if (!className.startsWith(target.packagePrefix()))
            return null;

        if (!method.matches("[A-Za-z0-9_-]+"))
            return null;

        String classShort =
                className.substring(className.lastIndexOf('.') + 1);

        if (!classShort.matches("[A-Za-z0-9_-]+"))
            return null;

        if (!filename.equals(method + ".png") &&
                !filename.startsWith(method + "-"))
            return null;

        Path base = config.screenshotDirectory()
                .resolve(target.screenshotFolder())
                .resolve(classShort);

        Path image = base.resolve(filename).normalize();

        if (!Files.isRegularFile(image) ||
                !image.toRealPath().startsWith(base.toRealPath()))
            return null;

        return "/api/screenshots/" + encode(target.id()) + "/" +
                encode(classShort) + "/" + encode(filename);
    }

    /**
     * Locates screenshot files associated with a test method.
     *
     * @param target the configured application/report target
     * @param className the fully-qualified name of the TestNG test class
     * @param method the HTTP request method or reflected Java method being processed
     * @return the newest matching screenshot URLs associated with a test method
     * @throws IOException if a required file or HTTP stream cannot be read or written
     */
    private List<String> screenshotLinks(ReportingTarget target, String className,
                                         String method) throws IOException {

        if (!className.startsWith(target.packagePrefix()))
            return List.of();

        if (!method.matches("[A-Za-z0-9_-]+"))
            return List.of();

        String classShort =
                className.substring(className.lastIndexOf('.') + 1);

        if (!classShort.matches("[A-Za-z0-9_-]+"))
            return List.of();

        Path folder = config.screenshotDirectory()
                .resolve(target.screenshotFolder())
                .resolve(classShort);

        if (!Files.isDirectory(folder))
            return List.of();

        List<Path> matched;

        try (Stream<Path> files = Files.list(folder)) {
            matched = files.filter(Files::isRegularFile)
                    .filter(p -> {
                        String n = p.getFileName().toString();
                        return n.equals(method + ".png") ||
                                (n.startsWith(method + "-") &&
                                        n.endsWith(".png"));
                    })
                    .sorted((a, b) ->
                            Long.compare(modified(b), modified(a)))
                    .limit(24)
                    .toList();
        }

        List<String> links = new ArrayList<>();

        for (Path p : matched) {
            links.add("/api/screenshots/" + encode(target.id()) + "/" +
                    encode(classShort) + "/" +
                    encode(p.getFileName().toString()));
        }

        return links;
    }

    /**
     * Returns a screenshot only when its normalized real path is inside the configured evidence directory.
     *
     * <p>Normalizes screenshot paths and rejects files outside the configured evidence directory.</p>
     *
     * @param exchange the incoming HTTP exchange to inspect or respond to
     * @param remaining the screenshot path segment after the API route prefix
     * @throws IOException if a required file or HTTP stream cannot be read or written
     */
    private void serveScreenshot(HttpExchange exchange, String remaining)
            throws IOException {

        int slash = remaining.indexOf('/');

        if (slash < 0) {
            sendText(exchange, 404, "Not found");
            return;
        }

        ReportingTarget target = findTarget(remaining.substring(0, slash));

        if (target == null) {
            sendText(exchange, 404, "Unknown target");
            return;
        }

        String subpath = URLDecoder.decode(
                remaining.substring(slash + 1).replace("+", "%2B"),
                StandardCharsets.UTF_8);

        Path base = config.screenshotDirectory()
                .resolve(target.screenshotFolder())
                .toAbsolutePath()
                .normalize();

        Path file = base.resolve(subpath).normalize();

        if (!Files.isDirectory(base) ||
                !Files.isRegularFile(file) ||
                !file.toRealPath().startsWith(base.toRealPath()) ||
                !file.getFileName().toString()
                        .toLowerCase(Locale.ROOT).endsWith(".png")) {
            sendText(exchange, 404, "Screenshot not found");
            return;
        }

        sendBytes(exchange, 200, "image/png",
                Files.readAllBytes(file));
    }

    /**
     * Serves frontend HTML, CSS, and JS only from the configured SPA asset directory.
     *
     * <p>Serves only files rooted within the configured SPA frontend directory.</p>
     *
     * @param exchange the incoming HTTP exchange to inspect or respond to
     * @param rawPath the request URI path used to locate a frontend asset
     * @throws IOException if a required file or HTTP stream cannot be read or written
     */
    private void serveStatic(HttpExchange exchange, String rawPath)
            throws IOException {

        String path = rawPath.equals("/")
                ? config.required("report.spa.indexFile")
                : URLDecoder.decode(
                rawPath.substring(1).replace("+", "%2B"),
                StandardCharsets.UTF_8);

        Path file = webDirectory.resolve(path).normalize();

        if (!Files.isRegularFile(file) ||
                !file.toRealPath().startsWith(webDirectory.toRealPath())) {
            sendText(exchange, 404, "Asset not found");
            return;
        }

        String lower =
                file.getFileName().toString().toLowerCase(Locale.ROOT);

        String type =
                lower.endsWith(".html") ? "text/html; charset=utf-8" :
                        lower.endsWith(".css") ? "text/css; charset=utf-8" :
                                lower.endsWith(".js") ? "text/javascript; charset=utf-8" :
                                        "text/plain; charset=utf-8";

        sendBytes(exchange, 200, type, Files.readAllBytes(file));
    }

    /**
     * Looks up the configured target application with the requested identifier.
     *
     * @param id the configured identifier to look up
     * @return the matching configured application, or {@code null} if not found
     */
    private ReportingTarget findTarget(String id) {
        for (ReportingTarget target : targets)
            if (target.id().equals(id)) return target;

        return null;
    }

    /**
     * Sets HTTP response headers to reduce caching, framing, and cross-origin risks.
     *
     * @param exchange the incoming HTTP exchange to inspect or respond to
     */
    private static void setHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("X-Frame-Options", "DENY");
        exchange.getResponseHeaders().set("Referrer-Policy", "no-referrer");

        exchange.getResponseHeaders().set(
                "Content-Security-Policy",
                "default-src 'self'; script-src 'self'; style-src 'self'; " +
                        "img-src 'self'; connect-src 'self'; object-src 'none'; " +
                        "base-uri 'none'; frame-ancestors 'none'"
        );
    }

    /**
     * Serializes a value as JSON and sends an HTTP response.
     *
     * @param exchange the incoming HTTP exchange to inspect or respond to
     * @param status the HTTP status code or test execution status being recorded
     * @param value the input value to convert or display
     * @throws IOException if a required file or HTTP stream cannot be read or written
     */
    private void sendJson(HttpExchange exchange, int status, Object value)
            throws IOException {

        sendBytes(exchange, status,
                "application/json; charset=utf-8",
                json.toJson(value).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Sends a UTF-8 plain-text HTTP response.
     *
     * @param exchange the incoming HTTP exchange to inspect or respond to
     * @param status the HTTP status code or test execution status being recorded
     * @param message the plain-text body of the HTTP response
     * @throws IOException if a required file or HTTP stream cannot be read or written
     */
    private static void sendText(HttpExchange exchange, int status,
                                 String message) throws IOException {

        sendBytes(exchange, status, "text/plain; charset=utf-8",
                message.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Writes the response headers and bytes to the HTTP exchange.
     *
     * @param exchange the incoming HTTP exchange to inspect or respond to
     * @param status the HTTP status code or test execution status being recorded
     * @param type the response MIME type
     * @param data the response data written to the HTTP exchange
     * @throws IOException if a required file or HTTP stream cannot be read or written
     */
    private static void sendBytes(HttpExchange exchange, int status,
                                  String type, byte[] data)
            throws IOException {

        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.sendResponseHeaders(status, data.length);

        try (var output = exchange.getResponseBody()) {
            output.write(data);
        }
    }

    /**
     * Converts a nullable JSON value to a non-null string.
     *
     * @param value the input value to convert or display
     * @return the string representation of a value, or an empty string for {@code null}
     */
    private static String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /**
     * Removes HTML markup from ZAP report text before exposing it in the SPA.
     *
     * @param value the input value to convert or display
     * @return plain text with markup and excess whitespace removed
     */
    private static String cleanMarkup(String value) {
        return value
                .replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>", " ")
                .replaceAll("(?s)<[^>]*>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Maps a ZAP risk code to a readable severity label.
     *
     * @param risk the numeric ZAP alert risk level
     * @return the severity label mapped from a ZAP risk code
     */
    private static String severity(String risk) {
        return switch (risk) {
            case "3" -> "High";
            case "2" -> "Medium";
            case "1" -> "Low";
            default -> "Informational";
        };
    }

    /**
     * Returns a sort order for ZAP severity levels.
     *
     * @param value the input value to convert or display
     * @return the numeric sort order used to rank ZAP severities
     */
    private static int severityOrder(String value) {
        return switch (value) {
            case "High" -> 0;
            case "Medium" -> 1;
            case "Low" -> 2;
            default -> 3;
        };
    }

    /**
     * Maps a ZAP confidence code to a readable confidence label.
     *
     * @param value the input value to convert or display
     * @return the display label mapped from a ZAP confidence code
     */
    private static String confidence(String value) {
        return switch (value) {
            case "0" -> "False positive";
            case "1" -> "Low";
            case "2" -> "Medium";
            case "3" -> "High";
            case "4" -> "Confirmed";
            default -> "Unknown";
        };
    }

    /**
     * Reads a file modification timestamp, returning a fallback on I/O failure.
     *
     * @param path the path or URL passed to the request or file operation
     * @return the file modification timestamp in milliseconds, or zero on read failure
     */
    private static long modified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    /**
     * Encodes a path component for an HTTP URL.
     *
     * @param value the input value to convert or display
     * @return URL-encoded text suitable for a link path component
     */
    private static String encode(String value) {
        return URLEncoder.encode(
                value, StandardCharsets.UTF_8).replace("+", "%20");
    }

}
