package utilities.reporting;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.openqa.selenium.json.Json;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
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
    private final List<Target> targets;
    private final Json json = new Json();

    private record Target(String id, String title, String site, String packagePrefix,
                          String zapFile, String screenshotFolder) {}
    private record TestData(List<Map<String, Object>> tests, List<Map<String, Object>> evidence) {}

    private SecurityAssessmentSpa() throws IOException {
        config = ReportingConfiguration.load();
        webDirectory = resolved(config.required("report.spa.assetDirectory"));
        if (!Files.isDirectory(webDirectory))
            throw new IOException("SPA asset directory missing: " + webDirectory);

        targets = new ArrayList<>();
        for (String item : config.required("targets").split(",")) {
            String id = item.trim();
            if (!id.matches("[A-Za-z0-9_-]+")) throw new IOException("Invalid target ID: " + id);
            String prefix = "target." + id + ".";
            targets.add(new Target(id, config.required(prefix + "title"),
                    config.required(prefix + "site"), config.required(prefix + "package"),
                    config.required(prefix + "zapFile"), config.required(prefix + "screenshotFolder")));
        }
    }

    public static void main(String[] args) throws IOException {
        SecurityAssessmentSpa app = new SecurityAssessmentSpa();
        String host = app.config.required("report.spa.host");
        int port;
        try { port = Integer.parseInt(app.config.required("report.spa.port")); }
        catch (NumberFormatException e) { throw new IOException("Invalid SPA port", e); }
        if (port < 1 || port > 65535) throw new IOException("SPA port out of range");
        // Bind only to loopback. Reports can contain sensitive request and test evidence.
        if (!java.net.InetAddress.getByName(host).isLoopbackAddress())
            throw new IOException("SPA host must be a loopback address: " + host);

        HttpServer server = HttpServer.create(new InetSocketAddress(host, port), 0);
        server.createContext("/", app::handle);
        server.start();
        System.out.println("[SECURITY SPA] Running at http://" + host + ":" + port + "/");
        System.out.println("[SECURITY SPA] Reads existing reports only; stop the process to shut down.");
    }

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
                Target target = findTarget(id);
                if (target == null) { sendText(exchange, 404, "Unknown assessment"); return; }
                sendJson(exchange, 200, assessment(target));
            } else if (path.startsWith("/api/screenshots/")) {
                serveScreenshot(exchange, path.substring("/api/screenshots/".length()));
            } else {
                serveStatic(exchange, path);
            }
        } catch (Exception e) {
            System.err.println("[SECURITY SPA] " + e.getMessage());
            sendJson(exchange, 500, Map.of("error", "Unable to load assessment data"));
        } finally {
            exchange.close();
        }
    }

    private List<Map<String, Object>> targetList() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Target target : targets) {
            result.add(Map.of("id", target.id(), "title", target.title(), "site", target.site()));
        }
        return result;
    }

    private Map<String, Object> assessment(Target target) throws IOException {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("target", Map.of("id", target.id(), "title", target.title(), "site", target.site()));
        result.put("scan", loadScan(target));
        TestData tests = loadTests(target);
        result.put("tests", tests.tests());
        result.put("evidence", tests.evidence());
        result.put("loadedAt", Instant.now().toString());
        return result;
    }

    private Map<String, Object> loadScan(Target target) throws IOException {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> findings = new ArrayList<>();
        result.put("findings", findings);
        result.put("generated", "");
        result.put("status", "missing");
        result.put("excludedInstances", 0);
        Path source = config.zapFile(target.zapFile());
        if (!Files.isRegularFile(source)) return result;

        Map<?, ?> report = json.toType(Files.readString(source), Json.MAP_TYPE);
        result.put("generated", str(report.get("@generated")));
        result.put("modifiedAt", Files.getLastModifiedTime(source).toInstant().toString());
        FindingFilter filter = new FindingFilter(config, target.id(), target.site());
        int excluded = 0;
        boolean targetSeen = false;
        if (report.get("site") instanceof List<?> sites) {
            for (Object siteRaw : sites) {
                if (!(siteRaw instanceof Map<?, ?> site)) continue;
                if (filter.matchesOrigin(str(site.get("@name")))) targetSeen = true;
                if (!(site.get("alerts") instanceof List<?> alerts)) continue;
                for (Object alertRaw : alerts) {
                    if (!(alertRaw instanceof Map<?, ?> alert)) continue;
                    String name = str(alert.get("name"));
                    if (name.isBlank()) name = str(alert.get("alert"));
                    String pluginId = str(alert.get("pluginid"));
                    if (!(alert.get("instances") instanceof List<?> instances)) continue;
                    for (Object instanceRaw : instances) {
                        if (!(instanceRaw instanceof Map<?, ?> instance)) continue;
                        String url = str(instance.get("uri"));
                        FindingFilter.Classification classification = filter.classify(url, pluginId, name);
                        if (classification.decision() == FindingFilter.Decision.OUT_OF_SCOPE) {
                            excluded++;
                            continue;
                        }
                        Map<String, Object> finding = new LinkedHashMap<>();
                        finding.put("name", name);
                        finding.put("pluginId", pluginId);
                        finding.put("severity", severity(str(alert.get("riskcode"))));
                        finding.put("confidence", confidence(str(alert.get("confidence"))));
                        finding.put("status", classification.decision().name());
                        finding.put("reason", classification.reason());
                        finding.put("url", url);
                        finding.put("method", str(instance.get("method")));
                        finding.put("parameter", str(instance.get("param")));
                        finding.put("evidence", str(instance.get("evidence")));
                        finding.put("testInput", str(instance.get("attack")));
                        finding.put("otherInfo", cleanMarkup(str(instance.get("otherinfo"))));
                        finding.put("description", cleanMarkup(str(alert.get("desc"))));
                        finding.put("recommendation", cleanMarkup(str(alert.get("solution"))));
                        finding.put("cwe", str(alert.get("cweid")));
                        findings.add(finding);
                    }
                }
            }
        }
        findings.sort(Comparator.comparingInt((Map<String, Object> f) -> severityOrder(str(f.get("severity"))))
                .thenComparing(f -> str(f.get("name"))).thenComparing(f -> str(f.get("url"))));
        result.put("status", targetSeen ? "loaded" : "target-not-present");
        result.put("excludedInstances", excluded);
        return result;
    }

    private TestData loadTests(Target target) throws IOException {
        Path csv = config.reportDirectory().resolve(config.testResultsFile());
        if (!Files.isRegularFile(csv)) return new TestData(List.of(), List.of());
        List<List<String>> rows = parseCsv(Files.readString(csv, StandardCharsets.UTF_8));
        if (rows.size() < 2) return new TestData(List.of(), List.of());
        List<String> headers = rows.get(0);
        List<Map<String, Object>> tests = new ArrayList<>();
        for (int r = 1; r < rows.size(); r++) {
            List<String> cells = rows.get(r);
            Map<String, String> row = new LinkedHashMap<>();
            for (int c = 0; c < headers.size(); c++)
                row.put(headers.get(c), c < cells.size() ? cells.get(c) : "");
            String className = row.getOrDefault("Class", "");
            if (!className.startsWith(target.packagePrefix())) continue;
            String method = row.getOrDefault("Method", "");
            Map<String, Object> test = new LinkedHashMap<>();
            test.put("suite", row.getOrDefault("Suite", ""));
            test.put("className", className);
            test.put("method", method);
            test.put("status", row.getOrDefault("Status", ""));
            test.put("durationMs", row.getOrDefault("DurationMs", ""));
            test.put("error", row.getOrDefault("Error", ""));
            test.put("screenshots", List.of());
            test.put("ambiguousEvidence", false);
            tests.add(test);
        }
        tests.sort(Comparator.comparing(t -> str(t.get("method"))));
        // The TestNG CSV omits invocation parameters. Only attach screenshots to an
        // individual execution if that class/method appears exactly once.
        Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        for (Map<String, Object> test : tests) {
            String key = test.get("className") + "#" + test.get("method");
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(test);
        }
        List<Map<String, Object>> evidence = new ArrayList<>();
        for (List<Map<String, Object>> executions : grouped.values()) {
            Map<String, Object> first = executions.get(0);
            String className = str(first.get("className"));
            String method = str(first.get("method"));
            List<String> links = screenshotLinks(target, className, method);
            evidence.add(Map.of("className", className, "method", method, "screenshots", links));
            if (executions.size() == 1) first.put("screenshots", links);
            else for (Map<String, Object> test : executions)
                test.put("ambiguousEvidence", !links.isEmpty());
        }
        return new TestData(tests, evidence);
    }

    private List<String> screenshotLinks(Target target, String className, String method) throws IOException {
        if (!className.startsWith(target.packagePrefix())) return List.of();
        if (!method.matches("[A-Za-z0-9_-]+")) return List.of();
        String classShort = className.substring(className.lastIndexOf('.') + 1);
        if (!classShort.matches("[A-Za-z0-9_-]+")) return List.of();
        Path folder = config.screenshotDirectory().resolve(target.screenshotFolder()).resolve(classShort);
        if (!Files.isDirectory(folder)) return List.of();
        List<Path> matched;
        try (Stream<Path> files = Files.list(folder)) {
            matched = files.filter(Files::isRegularFile)
                    .filter(p -> {
                        String n = p.getFileName().toString();
                        return n.equals(method + ".png") || (n.startsWith(method + "-") && n.endsWith(".png"));
                    })
                    .sorted((a, b) -> Long.compare(modified(b), modified(a)))
                    .limit(24).toList();
        }
        List<String> links = new ArrayList<>();
        for (Path p : matched) {
            links.add("/api/screenshots/" + encode(target.id()) + "/" +
                    encode(classShort) + "/" + encode(p.getFileName().toString()));
        }
        return links;
    }

    private void serveScreenshot(HttpExchange exchange, String remaining) throws IOException {
        int slash = remaining.indexOf('/');
        if (slash < 0) { sendText(exchange, 404, "Not found"); return; }
        Target target = findTarget(remaining.substring(0, slash));
        if (target == null) { sendText(exchange, 404, "Unknown target"); return; }
        String subpath = URLDecoder.decode(remaining.substring(slash + 1).replace("+", "%2B"), StandardCharsets.UTF_8);
        Path base = config.screenshotDirectory().resolve(target.screenshotFolder()).toAbsolutePath().normalize();
        Path file = base.resolve(subpath).normalize();
        if (!Files.isDirectory(base) || !Files.isRegularFile(file) ||
                !file.toRealPath().startsWith(base.toRealPath()) ||
                !file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png")) {
            sendText(exchange, 404, "Screenshot not found"); return;
        }
        sendBytes(exchange, 200, "image/png", Files.readAllBytes(file));
    }

    private void serveStatic(HttpExchange exchange, String rawPath) throws IOException {
        String path = rawPath.equals("/") ? config.required("report.spa.indexFile")
                : URLDecoder.decode(rawPath.substring(1).replace("+", "%2B"), StandardCharsets.UTF_8);
        Path file = webDirectory.resolve(path).normalize();
        if (!Files.isRegularFile(file) || !file.toRealPath().startsWith(webDirectory.toRealPath())) {
            sendText(exchange, 404, "Asset not found"); return;
        }
        String lower = file.getFileName().toString().toLowerCase(Locale.ROOT);
        String type = lower.endsWith(".html") ? "text/html; charset=utf-8" :
                lower.endsWith(".css") ? "text/css; charset=utf-8" :
                lower.endsWith(".js") ? "text/javascript; charset=utf-8" : "text/plain; charset=utf-8";
        sendBytes(exchange, 200, type, Files.readAllBytes(file));
    }

    private Target findTarget(String id) {
        for (Target target : targets) if (target.id().equals(id)) return target;
        return null;
    }

    private static void setHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("X-Frame-Options", "DENY");
        exchange.getResponseHeaders().set("Referrer-Policy", "no-referrer");
        exchange.getResponseHeaders().set("Content-Security-Policy",
                "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self'; connect-src 'self'; " +
                        "object-src 'none'; base-uri 'none'; frame-ancestors 'none'");
    }

    private void sendJson(HttpExchange exchange, int status, Object value) throws IOException {
        sendBytes(exchange, status, "application/json; charset=utf-8", json.toJson(value).getBytes(StandardCharsets.UTF_8));
    }

    private static void sendText(HttpExchange exchange, int status, String message) throws IOException {
        sendBytes(exchange, status, "text/plain; charset=utf-8", message.getBytes(StandardCharsets.UTF_8));
    }

    private static void sendBytes(HttpExchange exchange, int status, String type, byte[] data) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.sendResponseHeaders(status, data.length);
        try (var output = exchange.getResponseBody()) { output.write(data); }
    }

    private static String str(Object value) { return value == null ? "" : String.valueOf(value); }

    private static String cleanMarkup(String value) {
        return value.replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>", " ")
                .replaceAll("(?s)<[^>]*>", " ").replace("&nbsp;", " ")
                .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                .replaceAll("\\s+", " ").trim();
    }

    private static String severity(String risk) {
        return switch (risk) { case "3" -> "High"; case "2" -> "Medium";
            case "1" -> "Low"; default -> "Informational"; };
    }

    private static int severityOrder(String value) {
        return switch (value) { case "High" -> 0; case "Medium" -> 1;
            case "Low" -> 2; default -> 3; };
    }

    private static String confidence(String value) {
        return switch (value) { case "0" -> "False positive"; case "1" -> "Low";
            case "2" -> "Medium"; case "3" -> "High"; case "4" -> "Confirmed"; default -> "Unknown"; };
    }

    private static long modified(Path path) {
        try { return Files.getLastModifiedTime(path).toMillis(); }
        catch (IOException e) { return 0L; }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static Path resolved(String name) {
        Path path = Path.of(name);
        return (path.isAbsolute() ? path : Path.of("").toAbsolutePath().resolve(path)).normalize();
    }

    /** RFC 4180 CSV parser; handles quoted commas and escaped quotes. */
    private static List<List<String>> parseCsv(String content) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < content.length() && content.charAt(i + 1) == '"') {
                    field.append('"'); i++;
                } else quoted = !quoted;
            } else if (c == ',' && !quoted) {
                row.add(field.toString()); field.setLength(0);
            } else if ((c == '\n' || c == '\r') && !quoted) {
                if (c == '\r' && i + 1 < content.length() && content.charAt(i + 1) == '\n') i++;
                row.add(field.toString()); field.setLength(0);
                if (!(row.size() == 1 && row.get(0).isEmpty())) rows.add(row);
                row = new ArrayList<>();
            } else field.append(c);
        }
        if (!row.isEmpty() || field.length() > 0) { row.add(field.toString()); rows.add(row); }
        return rows;
    }
}
