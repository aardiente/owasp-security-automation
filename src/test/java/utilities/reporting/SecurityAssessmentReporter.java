
package utilities.reporting;

import org.openqa.selenium.json.Json;
import org.testng.IReporter;
import org.testng.ISuite;
import org.testng.ISuiteResult;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.xml.XmlSuite;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;
import java.util.stream.Stream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.EnumMap;

public class SecurityAssessmentReporter implements IReporter {

    private record Target(String id, String title, String packagePrefix, String siteUrl, String zapFile) {}
    private record TestCase(String method, String className, String variant, String status,
                            long durationMs, long startMs, String error, List<String> screenshots) {}
    private record Finding(String name, int risk, String confidence, String url, String method,
                           String evidence, String description, String solution, String cwe, String reason) {}
    private record Scan(String status, String generated, List<Finding> findings,
                        List<Finding> review, List<Finding> suppressed, int excludedInstances) {}
    private record Summary(Target target, long passed, long failed, long skipped, String scanStatus,
                           int included, int review, int suppressed, int excludedInstances) {}

    @Override
    public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectory) {
        try {
            ReportingConfiguration config = ReportingConfiguration.load();
            List<Target> targets = loadTargets(config);
            Files.createDirectories(config.reportDirectory());
            List<Path> assets = copyAssets(config);

            List<Summary> summaries = new ArrayList<>();
            for (Target target : targets) {
                List<TestCase> tests = collectTests(suites, target.packagePrefix(), config);
                if (tests.isEmpty()) continue;

                tests.sort(Comparator.comparingLong(TestCase::startMs)
                        .thenComparing(TestCase::method).thenComparing(TestCase::variant));
                Scan scan = loadScan(target, tests, config);
                Path output = config.assessmentFile(target.id());
                Files.writeString(output, buildReport(target, tests, scan, config, assets),
                        StandardCharsets.UTF_8);

                long passed = tests.stream().filter(t -> t.status().equals("PASSED")).count();
                long failed = tests.stream().filter(t -> t.status().equals("FAILED")).count();
                long skipped = tests.stream().filter(t -> t.status().equals("SKIPPED")).count();
                summaries.add(new Summary(target, passed, failed, skipped, scan.status(),
                        scan.findings().size(), scan.review().size(), scan.suppressed().size(),
                        scan.excludedInstances()));
                System.out.println("[SECURITY REPORT] HTML saved: " + output.toAbsolutePath());
            }

            if (summaries.isEmpty()) {
                System.out.println("[SECURITY REPORT] No tests matched configured reporting targets.");
                return;
            }

            Path index = config.reportDirectory().resolve(config.indexFile());
            Files.writeString(index, buildIndex(summaries, config, assets), StandardCharsets.UTF_8);
            System.out.println("[SECURITY REPORT] Assessment index saved: " + index.toAbsolutePath());
        } catch (Exception e) {
            System.err.println("[SECURITY REPORT] HTML generation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static List<Target> loadTargets(ReportingConfiguration config) throws IOException {
        List<Target> targets = new ArrayList<>();

        for (String raw : config.required("targets").split(",")) {
            String id = raw.trim();
            if (!id.matches("[a-zA-Z0-9_-]+"))
                throw new IOException("Invalid reporting target ID: " + id);

            String key = "target." + id + ".";
            targets.add(new Target(id, config.required(key + "title"),
                    config.required(key + "package"), config.required(key + "site"),
                    config.required(key + "zapFile")));
        }

        return targets;
    }

    private static List<TestCase> collectTests(List<ISuite> suites, String packagePrefix,
                                               ReportingConfiguration config) {
        List<TestCase> tests = new ArrayList<>();

        for (ISuite suite : suites) {
            for (ISuiteResult suiteResult : suite.getResults().values()) {
                ITestContext context = suiteResult.getTestContext();
                collect(tests, context.getPassedTests().getAllResults(), "PASSED", packagePrefix, config);
                collect(tests, context.getFailedTests().getAllResults(), "FAILED", packagePrefix, config);
                collect(tests, context.getSkippedTests().getAllResults(), "SKIPPED", packagePrefix, config);
            }
        }

        return tests;
    }

    private static void collect(List<TestCase> tests, Collection<ITestResult> results,
                                String status, String packagePrefix, ReportingConfiguration config) {
        for (ITestResult result : results) {
            String className = result.getTestClass().getName();
            if (!className.startsWith(packagePrefix)) continue;

            String error = result.getThrowable() == null ? "" : result.getThrowable().toString();
            long duration = Math.max(0, result.getEndMillis() - result.getStartMillis());
            Object[] parameters = result.getParameters();
            String variant = "";

            if (parameters != null && parameters.length > 0) {
                String candidate = String.valueOf(parameters[0]);
                if (candidate.matches("[A-Za-z0-9_-]{1,32}")) variant = candidate;
            }

            tests.add(new TestCase(result.getMethod().getMethodName(), className, variant, status,
                    duration, result.getStartMillis(), error, screenshotLinks(result, config)));
        }
    }

    private static List<String> screenshotLinks(ITestResult result, ReportingConfiguration config) {
        String fullClassName = result.getTestClass().getName();
        String[] parts = fullClassName.split("\\.");
        if (parts.length < 2) return List.of();

        String application = parts[0].equalsIgnoreCase("tests") && parts.length > 2
                ? parts[1] : parts[parts.length - 2];

        Path folder = config.screenshotDirectory()
                .resolve(sanitize(application.toLowerCase(Locale.ROOT)))
                .resolve(sanitize(result.getTestClass().getRealClass().getSimpleName()));
        if (!Files.isDirectory(folder)) return List.of();

        String method = sanitize(result.getMethod().getMethodName());
        Object[] parameters = result.getParameters();
        if (parameters == null) parameters = new Object[0];
        String filename = parameters.length == 0 ? method + ".png"
                : method + "-" + sanitize(String.valueOf(parameters[0])) +
                "-" + parameterHash(parameters) + ".png";

        List<Path> matches = new ArrayList<>();
        Path exact = folder.resolve(filename);

        if (Files.isRegularFile(exact) && capturedDuring(exact, result)) matches.add(exact);

        // Unknown future applications can use timestamped filenames, but only for
        // non-parameterized tests; never guess which payload produced an image.
        if (matches.isEmpty() && parameters.length == 0) {
            try (Stream<Path> files = Files.list(folder)) {
                matches.addAll(files.filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().startsWith(method + "-"))
                        .filter(p -> p.getFileName().toString().endsWith(".png"))
                        .filter(p -> capturedDuring(p, result))
                        .sorted().toList());
            } catch (IOException e) {
                System.err.println("[SECURITY REPORT] Screenshot lookup failed: " + e.getMessage());
            }
        }

        List<String> links = new ArrayList<>();
        for (Path match : matches) {
            try {
                String relative = config.reportDirectory().toAbsolutePath().normalize()
                        .relativize(match.toAbsolutePath().normalize()).toString().replace('\\', '/');
                links.add(new URI(null, null, relative, null, null).toASCIIString());
            } catch (Exception e) {
                System.err.println("[SECURITY REPORT] Screenshot link failed: " + e.getMessage());
            }
        }

        return links;
    }

    private static boolean capturedDuring(Path image, ITestResult result) {
        long start = result.getStartMillis();
        long end = result.getEndMillis();
        if (start <= 0 || end <= 0 || end < start) return false;

        try {
            long modified = Files.getLastModifiedTime(image).toMillis();
            return modified >= start - 2000 && modified <= end + 2000;
        } catch (IOException e) {
            return false;
        }
    }

    private static String parameterHash(Object[] parameters) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(Arrays.deepToString(parameters).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(Arrays.copyOf(digest, 6));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable.", e);
        }
    }

    private static String sanitize(String text) {
        return text.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static Scan emptyScan(String status, String generated) {
        return new Scan(status, generated, List.of(), List.of(), List.of(), 0);
    }

    private static Scan loadScan(Target target, List<TestCase> tests, ReportingConfiguration config) {
        Path source;
        try {
            source = config.zapFile(target.zapFile());
        } catch (IOException e) {
            return emptyScan("Invalid ZAP report configuration: " + e.getMessage(), "Unavailable");
        }
        if (!Files.isRegularFile(source))
            return emptyScan("ZAP JSON unavailable: " + target.zapFile(), "Unavailable");

        FindingFilter filter;
        try {
            filter = new FindingFilter(config, target.id(), target.siteUrl());
        } catch (IOException e) {
            return emptyScan("Finding filter configuration invalid: " + e.getMessage(), "Unavailable");
        }

        try {
            Map<?, ?> root = new Json().toType(Files.readString(source), Json.MAP_TYPE);
            String generated = value(root.get("@generated"));
            String created = value(root.get("created"));

            long earliestTest = tests.stream().mapToLong(TestCase::startMs)
                    .filter(timestamp -> timestamp > 0).min().orElse(0);
            if (!created.isBlank() && earliestTest > 0 &&
                    Instant.parse(created).isBefore(Instant.ofEpochMilli(earliestTest).minus(Duration.ofMinutes(2))))
                return emptyScan("ZAP JSON predates this run; old findings excluded", generated);

            List<Finding> included = new ArrayList<>();
            List<Finding> review = new ArrayList<>();
            List<Finding> suppressed = new ArrayList<>();
            int excludedInstances = 0;
            boolean foundTarget = false;
            Object sites = root.get("site");

            if (sites instanceof List<?> siteList) {
                for (Object siteObject : siteList) {
                    if (!(siteObject instanceof Map<?, ?> site)) continue;
                    String siteUrl = value(site.get("@name"));
                    if (filter.matchesOrigin(siteUrl)) foundTarget = true;
                    Object rawAlerts = site.get("alerts");
                    if (!(rawAlerts instanceof List<?> alerts)) continue;

                    for (Object alertObject : alerts) {
                        if (!(alertObject instanceof Map<?, ?> alert)) continue;
                        String pluginId = value(alert.get("pluginid"));
                        Map<FindingFilter.Decision, List<Map<?, ?>>> grouped =
                                new EnumMap<>(FindingFilter.Decision.class);
                        Map<FindingFilter.Decision, String> reasons =
                                new EnumMap<>(FindingFilter.Decision.class);
                        Object rawInstances = alert.get("instances");
                        if (!(rawInstances instanceof List<?> instances)) continue;

                        for (Object rawInstance : instances) {
                            if (!(rawInstance instanceof Map<?, ?> instance)) continue;
                            FindingFilter.Classification c = filter.classify(value(instance.get("uri")), pluginId);
                            if (c.decision() == FindingFilter.Decision.OUT_OF_SCOPE) {
                                excludedInstances++;
                                continue;
                            }
                            grouped.computeIfAbsent(c.decision(), ignored -> new ArrayList<>()).add(instance);
                            reasons.putIfAbsent(c.decision(), c.reason());
                        }

                        for (var group : grouped.entrySet()) {
                            Finding finding = createFinding(alert, group.getValue(), reasons.get(group.getKey()));
                            switch (group.getKey()) {
                                case INCLUDE -> included.add(finding);
                                case REVIEW -> review.add(finding);
                                case SUPPRESS -> suppressed.add(finding);
                                default -> { }
                            }
                        }
                    }
                }
            }
            if (!foundTarget)
                return new Scan("Target site absent from ZAP JSON: " + target.siteUrl(),
                        generated, List.of(), List.of(), List.of(), excludedInstances);

            Comparator<Finding> order = Comparator.comparingInt(Finding::risk).reversed()
                    .thenComparing(Finding::name);
            included.sort(order);
            review.sort(order);
            suppressed.sort(order);
            return new Scan("ZAP findings loaded", generated, included, review, suppressed, excludedInstances);
        } catch (Exception e) {
            return emptyScan("ZAP JSON parsing failed: " + e.getMessage(), "Unavailable");
        }
    }

    private static Finding createFinding(Map<?, ?> alert, List<Map<?, ?>> instances, String reason) {
        Map<?, ?> first = instances.get(0);
        return new Finding(value(alert.get("name")), Integer.parseInt(value(alert.get("riskcode"))),
                confidenceLabel(value(alert.get("confidence"))), value(first.get("uri")),
                value(first.get("method")), buildEvidence(instances),
                plain(value(alert.get("desc"))), plain(value(alert.get("solution"))),
                value(alert.get("cweid")), reason == null ? "" : reason);
    }

    private static String buildEvidence(List<Map<?, ?>> instances) {
        if (instances.isEmpty())
            return "ZAP recorded this alert without individual request observations.";

        StringBuilder details = new StringBuilder();
        boolean literalEvidence = false;
        int limit = Math.min(instances.size(), 5);

        for (int i = 0; i < limit; i++) {
            Map<?, ?> instance = instances.get(i);

            if (details.length() > 0) details.append("\n\n");
            details.append("Observation ").append(i + 1).append("\n");

            appendLine(details, "Request",
                    value(instance.get("method")) + " " + value(instance.get("uri")));

            appendLine(details, "Parameter/Header", value(instance.get("param")));

            String matched = value(instance.get("evidence"));
            if (!matched.isBlank()) {
                appendLine(details, "Matched Evidence", matched);
                literalEvidence = true;
            }

            appendLine(details, "Test Input", value(instance.get("attack")));
            appendLine(details, "ZAP Details", plain(value(instance.get("otherinfo"))));
        }

        if (instances.size() > limit)
            details.append("\n").append(instances.size() - limit)
                    .append(" more ZAP observations not displayed.");

        if (!literalEvidence)
            details.append("\n\nValidation note: ZAP did not include a matched response excerpt. ")
                    .append("Review the associated HTTP response before marking this finding confirmed.");

        return details.toString().trim();
    }

    private static void appendLine(StringBuilder out, String label, String value) {
        if (!value.isBlank())
            out.append(label).append(": ").append(value.trim()).append("\n");
    }

    private static String buildReport(Target target, List<TestCase> tests, Scan scan,
                                      ReportingConfiguration config, List<Path> assets) {
        long passed = tests.stream().filter(t -> t.status().equals("PASSED")).count();
        long failed = tests.stream().filter(t -> t.status().equals("FAILED")).count();
        long skipped = tests.stream().filter(t -> t.status().equals("SKIPPED")).count();
        long screenshots = tests.stream().mapToLong(t -> t.screenshots().size()).sum();

        StringBuilder html = new StringBuilder(35000);
        html.append(pageStart(target.title() + " Security Assessment", true, assets, config.assetFolderName()));

        html.append("<header><h1>").append(escape(target.title()))
                .append(" Security Assessment</h1>")
                .append("<p>Automated TestNG execution and OWASP ZAP security findings.</p>")
                .append("<p class=\"note\">ZAP scan: ").append(escape(scan.generated()))
                .append(" | Target: ").append(escape(target.siteUrl())).append("</p>")
                .append("<div class=\"cards\">");

        card(html, tests.size(), "Executed tests");
        card(html, passed, "Passed");
        card(html, failed, "Failed");
        card(html, skipped, "Skipped");
        card(html, screenshots, "Evidence images");

        html.append("</div><p><a href=\"").append(escape(config.indexFile()))
                .append("\">All assessments</a></p></header>");

        html.append("<section><h2>Security Findings</h2><p class=\"note\">")
                .append(escape(scan.status()))
                .append(". Scanner findings are not automatically confirmed exploits.</p>");

        if (scan.status().equals("ZAP findings loaded")) {
            html.append("<div class=\"cards\">");
            card(html, count(scan.findings(), 3), "High");
            card(html, count(scan.findings(), 2), "Medium");
            card(html, count(scan.findings(), 1), "Low");
            card(html, count(scan.findings(), 0), "Informational");
            html.append("</div><div class=\"cards\">");
            card(html, scan.review().size(), "Needs validation");
            card(html, scan.suppressed().size(), "Suppressed by rule");
            card(html, scan.excludedInstances(), "Out-of-scope instances");
            html.append("</div><p class=\"note\">Severity counts cover in-scope, unsuppressed alert groups only. " +
                    "Filtering is request-level; the original ZAP JSON is retained. " +
                    "Needs validation and suppressed findings are not certified false positives.</p>");
        }

        html.append("""
                <div class="finding-controls">
                    <input id="finding-search" type="search" placeholder="Search findings, endpoints, CWE...">
                    <select id="risk-filter" aria-label="Filter by severity">
                        <option value="all">All severities</option>
                        <option value="high">High</option>
                        <option value="medium">Medium</option>
                        <option value="low">Low</option>
                        <option value="info">Informational</option>
                    </select>
                    <button type="button" id="expand-all">Expand All</button>
                    <button type="button" id="collapse-all">Collapse All</button>
                </div>
                <div class="finding-list">
                """);

        for (Finding finding : scan.findings())
            appendFinding(html, finding);

        html.append("</div><p id=\"finding-empty\" class=\"empty-state\" hidden>")
                .append("No matching findings.</p>");

        html.append("<details class=\"filter-audit\"><summary><strong>Needs validation (" +
                        scan.review().size() + ")</strong> — lower-signal findings</summary>")
                .append("<p class=\"note\">These observations match configured review rules. " +
                        "They remain available for manual validation, not marked false positives.</p>")
                .append("<div class=\"finding-list\">");
        for (Finding finding : scan.review()) appendFinding(html, finding);
        html.append("</div></details>");

        html.append("<details class=\"filter-audit\"><summary><strong>Suppressed by rule (" +
                        scan.suppressed().size() + ")</strong></summary>")
                .append("<p class=\"note\">These findings were hidden from the main severity totals " +
                        "by explicit configuration, but remain available for audit here.</p>")
                .append("<div class=\"finding-list\">");
        for (Finding finding : scan.suppressed()) appendFinding(html, finding);
        html.append("</div></details></section>");

        html.append("""
                <section>
                    <h2>Automated Test Results &amp; Screenshots</h2>
                    <p class="note">Expand a test to view screenshot evidence captured during
                    that execution. A passed test or screenshot does not independently prove
                    vulnerability exploitability or challenge completion.</p>
                    <div class="test-result-list">
                """);

        for (TestCase test : tests)
            appendTestResult(html, test);

        html.append("</div></section>")
                .append("<p class=\"note\">Generated automatically. External ZAP sites are excluded.</p>")
                .append("</body></html>");

        return html.toString();
    }

    private static void appendTestResult(StringBuilder html, TestCase test) {
        html.append("<details class=\"test-result\"><summary class=\"test-result-summary\">")
                .append("<span class=\"test-status ").append(test.status().toLowerCase())
                .append("\">").append(test.status()).append("</span>")
                .append("<span class=\"test-title\">").append(escape(test.method()));

        if (!test.variant().isBlank())
            html.append(" <span class=\"test-variant\">(")
                    .append(escape(test.variant())).append(")</span>");

        html.append("</span><span class=\"test-duration\">")
                .append(test.durationMs()).append(" ms</span>")
                .append("<span class=\"test-toggle\">Evidence</span>")
                .append("</summary><div class=\"test-result-body\">")
                .append("<p class=\"note\"><strong>Test class:</strong> ")
                .append(escape(test.className())).append("</p>");

        if (!test.error().isBlank()) {
            html.append("<h3>Test Error</h3><pre class=\"test-error\">")
                    .append(escape(test.error())).append("</pre>");
        }

        if (test.screenshots().isEmpty()) {
            html.append("<p class=\"note\">No screenshot captured during this test execution.</p>");
        } else {
            html.append("<div class=\"test-evidence-grid\">");
            for (String screenshot : test.screenshots()) {
                html.append("<figure class=\"test-screenshot\"><a href=\"")
                        .append(escape(screenshot))
                        .append("\" target=\"_blank\" rel=\"noopener noreferrer\">")
                        .append("<img src=\"").append(escape(screenshot))
                        .append("\" loading=\"lazy\" alt=\"Screenshot evidence for ")
                        .append(escape(test.method())).append("\"></a>")
                        .append("<figcaption>View full-size screenshot</figcaption></figure>");
            }
            html.append("</div>");
        }

        html.append("</div></details>");
    }

    private static String buildIndex(List<Summary> summaries, ReportingConfiguration config,
                                     List<Path> assets) throws IOException {
        StringBuilder html = new StringBuilder(4000);
        html.append(pageStart("Security Assessment Index", false, assets, config.assetFolderName()));

        html.append("<header><h1>Security Assessments</h1>")
                .append("<p>Automated TestNG and ZAP results by application.</p></header>");

        for (Summary summary : summaries) {
            String assessmentName = config.assessmentFile(summary.target().id()).getFileName().toString();
            html.append("<section><h2><a href=\"").append(escape(assessmentName))
                    .append("\">").append(escape(summary.target().title())).append("</a></h2>")
                    .append("<p class=\"note\">")
                    .append(escape(summary.target().siteUrl())).append("</p>")
                    .append("<div class=\"cards\">");

            card(html, summary.passed(), "Passed");
            card(html, summary.failed(), "Failed");
            card(html, summary.skipped(), "Skipped");
            card(html, summary.included(), "In-scope findings");
            card(html, summary.review(), "Needs validation");
            card(html, summary.suppressed(), "Suppressed");
            card(html, summary.excludedInstances(), "Out-of-scope instances");

            html.append("</div><p class=\"note\">")
                    .append(escape(summary.scanStatus()))
                    .append("</p></section>");
        }

        html.append("</body></html>");
        return html.toString();
    }

    private static String pageStart(String title, boolean interactive, List<Path> assets,
                                    String assetFolder) {
        StringBuilder head = new StringBuilder();
        head.append("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">")
                .append("<title>").append(escape(title)).append("</title>");

        for (Path asset : assets) {
            String filename = asset.getFileName().toString();
            String location = escape(assetFolder) + "/" + escape(filename);

            if (filename.endsWith(".css"))
                head.append("<link rel=\"stylesheet\" href=\"").append(location).append("\">");
            else if (interactive && filename.endsWith(".js"))
                head.append("<script src=\"").append(location).append("\" defer></script>");
        }

        return head.append("</head><body>").toString();
    }

    private static void appendFinding(StringBuilder html, Finding finding) {
        String severity = severity(finding.risk());
        String type = severity.toLowerCase();

        html.append("<details class=\"finding\" data-risk=\"").append(type)
                .append("\"><summary class=\"finding-summary\">")
                .append("<span class=\"severity risk-").append(type).append("\">")
                .append(severity).append("</span>")
                .append("<span class=\"finding-title\">")
                .append(escape(finding.name())).append("</span>")
                .append("<span class=\"finding-confidence\">Confidence: ")
                .append(escape(finding.confidence())).append("</span>")
                .append("<span class=\"finding-toggle\">Details</span>")
                .append("</summary><div class=\"finding-body\"><div class=\"evidence-grid\">");

        field(html, "Affected URL", finding.url());
        field(html, "CWE", finding.cwe());
        field(html, "HTTP Method", finding.method());
        field(html, "Confidence", finding.confidence());

        html.append("</div>");
        if (!finding.reason().isBlank()) section(html, "Filter decision", finding.reason());
        section(html, "Description", finding.description());

        html.append("<div class=\"evidence-section\"><h3>ZAP Observations</h3><pre>")
                .append(escape(finding.evidence()))
                .append("</pre></div>");

        section(html, "Recommendation", finding.solution());
        html.append("</div></details>");
    }

    private static void field(StringBuilder html, String label, String value) {
        html.append("<div class=\"evidence-field\"><span class=\"field-label\">")
                .append(escape(label))
                .append("</span><span class=\"field-value\">")
                .append(escape(value.isBlank() ? "Not provided" : value))
                .append("</span></div>");
    }

    private static void section(StringBuilder html, String title, String content) {
        html.append("<div class=\"evidence-section\"><h3>")
                .append(escape(title)).append("</h3><p>")
                .append(escape(content.isBlank() ? "Not provided" : content))
                .append("</p></div>");
    }

    private static void card(StringBuilder html, long number, String label) {
        html.append("<div class=\"card\"><strong>").append(number)
                .append("</strong><span>").append(escape(label))
                .append("</span></div>");
    }

    private static long count(List<Finding> findings, int risk) {
        return findings.stream().filter(f -> f.risk() == risk).count();
    }

    private static String severity(int risk) {
        return switch (risk) {
            case 3 -> "High";
            case 2 -> "Medium";
            case 1 -> "Low";
            default -> "Info";
        };
    }

    private static String confidenceLabel(String confidence) {
        return switch (confidence) {
            case "0" -> "False Positive";
            case "1" -> "Low";
            case "2" -> "Medium";
            case "3" -> "High";
            case "4" -> "Confirmed";
            default -> "Unknown";
        };
    }

    private static String value(Object object) {
        return object == null ? "" : object.toString();
    }

    private static String plain(String value) {
        return value.replaceAll("(?s)<[^>]*>", " ")
                .replace("&nbsp;", " ").replace("&amp;", "&")
                .replace("&lt;", "<").replace("&gt;", ">")
                .replaceAll("\\s+", " ").trim();
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static List<Path> copyAssets(ReportingConfiguration config) throws IOException {
        List<Path> assets = config.assets();
        Path destination = config.reportDirectory().resolve(config.assetFolderName());
        Files.createDirectories(destination);

        for (Path source : assets)
            Files.copy(source, destination.resolve(source.getFileName()),
                    StandardCopyOption.REPLACE_EXISTING);

        return assets;
    }
}
