
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
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class SecurityAssessmentReporter implements IReporter {

    private static final Path REPORT_DIR = Path.of("target", "security-reports");
    private static final Path CONFIG = Path.of("src", "test", "java", "utilities", "reporting", "report-targets.properties");
    private static final Path ASSETS = Path.of("src", "test", "java", "utilities", "reporting", "assets");

    private record Target(String id, String title, String packagePrefix, String siteUrl, String zapFile) {}
    private record TestCase(String method, String status, long durationMs, long startMs, String error) {}
    private record Finding(String name, int risk, String confidence, String url, String method,
                           String evidence, String description, String solution, String cwe) {}
    private record Scan(String status, String generated, List<Finding> findings) {}
    private record Summary(Target target, long passed, long failed, long skipped, String scanStatus) {}

    @Override
    public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectory) {
        try {
            List<Target> targets = loadTargets();
            Files.createDirectories(REPORT_DIR);
            copyAsset("security-assessment.css");
            copyAsset("security-assessment.js");

            List<Summary> summaries = new ArrayList<>();
            for (Target target : targets) {
                List<TestCase> tests = collectTests(suites, target.packagePrefix());
                if (tests.isEmpty()) continue;

                tests.sort(Comparator.comparing(TestCase::method));
                Scan scan = loadScan(target, tests);
                Path output = REPORT_DIR.resolve("SecurityAssessment-" + target.id() + ".html");
                Files.writeString(output, buildReport(target, tests, scan), StandardCharsets.UTF_8);

                long passed = tests.stream().filter(t -> t.status().equals("PASSED")).count();
                long failed = tests.stream().filter(t -> t.status().equals("FAILED")).count();
                long skipped = tests.stream().filter(t -> t.status().equals("SKIPPED")).count();
                summaries.add(new Summary(target, passed, failed, skipped, scan.status()));
                System.out.println("[SECURITY REPORT] HTML saved: " + output.toAbsolutePath());
            }

            if (summaries.isEmpty()) {
                System.out.println("[SECURITY REPORT] No tests matched configured reporting targets.");
                return;
            }

            Path index = REPORT_DIR.resolve("SecurityAssessment.html");
            Files.writeString(index, buildIndex(summaries), StandardCharsets.UTF_8);
            System.out.println("[SECURITY REPORT] Assessment index saved: " + index.toAbsolutePath());
        } catch (Exception e) {
            System.err.println("[SECURITY REPORT] HTML generation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static List<Target> loadTargets() throws IOException {
        if (!Files.isRegularFile(CONFIG))
            throw new IOException("Missing reporting configuration: " + CONFIG.toAbsolutePath());

        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(CONFIG, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }

        List<Target> targets = new ArrayList<>();
        for (String raw : required(properties, "targets").split(",")) {
            String id = raw.trim();
            if (!id.matches("[a-zA-Z0-9_-]+"))
                throw new IOException("Invalid reporting target ID: " + id);

            String key = "target." + id + ".";
            targets.add(new Target(id, required(properties, key + "title"),
                    required(properties, key + "package"), required(properties, key + "site"),
                    required(properties, key + "zapFile")));
        }

        return targets;
    }

    private static String required(Properties properties, String key) throws IOException {
        String value = properties.getProperty(key, "").trim();
        if (value.isEmpty()) throw new IOException("Missing reporting property: " + key);
        return value;
    }

    private static List<TestCase> collectTests(List<ISuite> suites, String packagePrefix) {
        List<TestCase> tests = new ArrayList<>();

        for (ISuite suite : suites) {
            for (ISuiteResult suiteResult : suite.getResults().values()) {
                ITestContext context = suiteResult.getTestContext();
                collect(tests, context.getPassedTests().getAllResults(), "PASSED", packagePrefix);
                collect(tests, context.getFailedTests().getAllResults(), "FAILED", packagePrefix);
                collect(tests, context.getSkippedTests().getAllResults(), "SKIPPED", packagePrefix);
            }
        }

        return tests;
    }

    private static void collect(List<TestCase> tests, Collection<ITestResult> results,
                                String status, String packagePrefix) {
        for (ITestResult result : results) {
            if (!result.getTestClass().getName().startsWith(packagePrefix)) continue;

            String error = result.getThrowable() == null ? "" : result.getThrowable().toString();
            long duration = Math.max(0, result.getEndMillis() - result.getStartMillis());

            tests.add(new TestCase(result.getMethod().getMethodName(), status, duration,
                    result.getStartMillis(), error));
        }
    }

    private static Scan loadScan(Target target, List<TestCase> tests) {
        Path source = REPORT_DIR.resolve(target.zapFile());
        if (!Files.isRegularFile(source))
            return new Scan("ZAP JSON unavailable: " + target.zapFile(), "Unavailable", List.of());

        try {
            Map<?, ?> root = new Json().toType(Files.readString(source), Json.MAP_TYPE);
            String generated = value(root.get("@generated"));
            String created = value(root.get("created"));

            long earliestTest = tests.stream().mapToLong(TestCase::startMs)
                    .filter(timestamp -> timestamp > 0).min().orElse(0);

            if (!created.isBlank() && earliestTest > 0 &&
                    Instant.parse(created).isBefore(Instant.ofEpochMilli(earliestTest).minus(Duration.ofMinutes(2))))
                return new Scan("ZAP JSON predates this run; old findings excluded", generated, List.of());

            List<Finding> findings = new ArrayList<>();
            boolean foundTarget = false;
            Object sites = root.get("site");

            if (sites instanceof List<?> siteList) {
                for (Object siteObject : siteList) {
                    if (!(siteObject instanceof Map<?, ?> site)) continue;
                    if (!sameSite(value(site.get("@name")), target.siteUrl())) continue;

                    foundTarget = true;
                    Object rawAlerts = site.get("alerts");
                    if (!(rawAlerts instanceof List<?> alerts)) continue;

                    for (Object alertObject : alerts) {
                        if (!(alertObject instanceof Map<?, ?> alert)) continue;

                        Map<?, ?> first = firstInstance(alert);
                        findings.add(new Finding(
                                value(alert.get("name")),
                                Integer.parseInt(value(alert.get("riskcode"))),
                                confidenceLabel(value(alert.get("confidence"))),
                                value(first.get("uri")),
                                value(first.get("method")),
                                buildEvidence(alert),
                                plain(value(alert.get("desc"))),
                                plain(value(alert.get("solution"))),
                                value(alert.get("cweid"))
                        ));
                    }
                }
            }

            if (!foundTarget)
                return new Scan("Target site absent from ZAP JSON: " + target.siteUrl(),
                        generated, List.of());

            findings.sort(Comparator.comparingInt(Finding::risk).reversed()
                    .thenComparing(Finding::name));

            return new Scan("ZAP findings loaded", generated, findings);
        } catch (Exception e) {
            return new Scan("ZAP JSON parsing failed: " + e.getMessage(), "Unavailable", List.of());
        }
    }

    private static boolean sameSite(String first, String second) {
        try {
            URI a = URI.create(first);
            URI b = URI.create(second);

            return a.getScheme().equalsIgnoreCase(b.getScheme()) &&
                    a.getHost().equalsIgnoreCase(b.getHost()) &&
                    effectivePort(a) == effectivePort(b);
        } catch (Exception e) {
            return first.replaceAll("/+$", "")
                    .equalsIgnoreCase(second.replaceAll("/+$", ""));
        }
    }

    private static int effectivePort(URI uri) {
        if (uri.getPort() != -1) return uri.getPort();
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    private static Map<?, ?> firstInstance(Map<?, ?> alert) {
        Object raw = alert.get("instances");

        if (raw instanceof List<?> list && !list.isEmpty() &&
                list.get(0) instanceof Map<?, ?> map)
            return map;

        return Map.of();
    }

    private static String buildEvidence(Map<?, ?> alert) {
        Object raw = alert.get("instances");

        if (!(raw instanceof List<?> instances) || instances.isEmpty())
            return "ZAP recorded this alert without individual request observations.";

        StringBuilder details = new StringBuilder();
        boolean literalEvidence = false;
        int limit = Math.min(instances.size(), 5);

        for (int i = 0; i < limit; i++) {
            if (!(instances.get(i) instanceof Map<?, ?> instance)) continue;

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

    private static String buildReport(Target target, List<TestCase> tests, Scan scan) {
        long passed = tests.stream().filter(t -> t.status().equals("PASSED")).count();
        long failed = tests.stream().filter(t -> t.status().equals("FAILED")).count();
        long skipped = tests.stream().filter(t -> t.status().equals("SKIPPED")).count();

        StringBuilder html = new StringBuilder(35000);
        html.append(pageStart(target.title() + " Security Assessment", true));

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

        html.append("</div><p><a href=\"SecurityAssessment.html\">All assessments</a></p></header>");

        html.append("<section><h2>Security Findings</h2><p class=\"note\">")
                .append(escape(scan.status()))
                .append(". Scanner findings are not automatically confirmed exploits.</p>");

        if (scan.status().equals("ZAP findings loaded")) {
            html.append("<div class=\"cards\">");
            card(html, count(scan.findings(), 3), "High");
            card(html, count(scan.findings(), 2), "Medium");
            card(html, count(scan.findings(), 1), "Low");
            card(html, count(scan.findings(), 0), "Informational");
            html.append("</div>");
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
                .append("No matching findings.</p></section>");

        html.append("""
                <section>
                    <h2>Automated Test Results</h2>
                    <p class="note">TestNG success does not independently verify
                    exploitability or challenge completion.</p>
                    <div class="table-scroll">
                    <table>
                        <thead>
                            <tr>
                                <th>Test Method</th>
                                <th>Status</th>
                                <th>Duration</th>
                                <th>Error</th>
                            </tr>
                        </thead>
                        <tbody>
                """);

        for (TestCase test : tests) {
            html.append("<tr><td>").append(escape(test.method()))
                    .append("</td><td class=\"").append(test.status().toLowerCase())
                    .append("\">").append(test.status())
                    .append("</td><td>").append(test.durationMs()).append(" ms</td>")
                    .append("<td class=\"test-error\">").append(escape(test.error()))
                    .append("</td></tr>");
        }

        html.append("</tbody></table></div></section>")
                .append("<p class=\"note\">Generated automatically. External ZAP sites are excluded.</p>")
                .append("</body></html>");

        return html.toString();
    }

    private static String buildIndex(List<Summary> summaries) {
        StringBuilder html = new StringBuilder(4000);
        html.append(pageStart("Security Assessment Index", false));

        html.append("<header><h1>Security Assessments</h1>")
                .append("<p>Automated TestNG and ZAP results by application.</p></header>");

        for (Summary summary : summaries) {
            html.append("<section><h2><a href=\"SecurityAssessment-")
                    .append(escape(summary.target().id())).append(".html\">")
                    .append(escape(summary.target().title())).append("</a></h2>")
                    .append("<p class=\"note\">")
                    .append(escape(summary.target().siteUrl())).append("</p>")
                    .append("<div class=\"cards\">");

            card(html, summary.passed(), "Passed");
            card(html, summary.failed(), "Failed");
            card(html, summary.skipped(), "Skipped");

            html.append("</div><p class=\"note\">")
                    .append(escape(summary.scanStatus()))
                    .append("</p></section>");
        }

        html.append("</body></html>");
        return html.toString();
    }

    private static String pageStart(String title, boolean interactive) {
        return "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\">" +
                "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">" +
                "<title>" + escape(title) + "</title>" +
                "<link rel=\"stylesheet\" href=\"assets/security-assessment.css\">" +
                (interactive ? "<script src=\"assets/security-assessment.js\" defer></script>" : "") +
                "</head><body>";
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

    private static void copyAsset(String fileName) throws IOException {
        Path source = ASSETS.resolve(fileName);
        Path destination = REPORT_DIR.resolve("assets").resolve(fileName);

        if (!Files.isRegularFile(source))
            throw new IOException("Reporting asset not found: " + source.toAbsolutePath());

        Files.createDirectories(destination.getParent());
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
    }
}
