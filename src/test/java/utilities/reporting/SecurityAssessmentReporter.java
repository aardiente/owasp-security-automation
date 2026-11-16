
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
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Implements the TestNG HTML assessment reporter: merges test results, ZAP alerts, and screenshot links.
 */
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

    /**
     * TestNG reporter entry point that assembles static HTML assessments from test results and ZAP JSON.
     *
     * <p>Writes the configured assessment pages using existing ZAP JSON and TestNG results; the reporter does not perform a vulnerability scan.</p>
     *
     * @param xmlSuites the TestNG XML suite definitions supplied to the reporter
     * @param suites the TestNG suite results supplied to the reporter
     * @param outputDirectory the default TestNG reporter output directory
     */
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

    /**
     * Loads configured application metadata for static report generation.
     *
     * @param config the reporting configuration used to resolve paths and rules
     * @return the assessment target definitions loaded from reporting configuration
     * @throws IOException if a required file or HTTP stream cannot be read or written
     */
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

    /**
     * Collects applicable test execution results for an application package.
     *
     * @param suites the TestNG suite results supplied to the reporter
     * @param packagePrefix the Java package prefix used to select the application test results
     * @param config the reporting configuration used to resolve paths and rules
     * @return the TestNG test records matching the selected target package
     */
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

    /**
     * Converts TestNG results into normalized report test entries.
     *
     * @param tests the collection of tests examined by the method
     * @param results the TestNG execution results to include in the report
     * @param status the HTTP status code or test execution status being recorded
     * @param packagePrefix the Java package prefix used to select the application test results
     * @param config the reporting configuration used to resolve paths and rules
     */
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

    /**
     * Resolves screenshot evidence associated with a specific TestNG execution.
     *
     * @param result the TestNG test result whose status or evidence is recorded
     * @param config the reporting configuration used to resolve paths and rules
     * @return the screenshot URLs matching the current test execution
     */
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

        if (Files.isRegularFile(exact) && capturedDuring(exact, result))
            matches.add(exact);

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

    /**
     * Checks whether a screenshot was captured within the bounds of a test execution.
     *
     * @param image the screenshot file whose timestamp is being checked
     * @param result the TestNG test result whose status or evidence is recorded
     * @return {@code true} when captured during is satisfied; {@code false} otherwise
     */
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

    /**
     * Computes a reproducible hash of a parameterized test's inputs.
     *
     * @param parameters the TestNG method arguments used to identify an execution
     * @return the short hash representing the test parameters
     */
    private static String parameterHash(Object[] parameters) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(Arrays.deepToString(parameters).getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(Arrays.copyOf(digest, 6));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable.", e);
        }
    }

    /**
     * Removes unsafe characters from a screenshot or report identifier.
     *
     * @param text the input string being normalized, sanitized, or rendered
     * @return a filesystem-safe identifier derived from the supplied text
     */
    private static String sanitize(String text) {
        return text.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    /**
     * Builds an empty scan representation for missing or unreadable input.
     *
     * @param status the HTTP status code or test execution status being recorded
     * @param generated the generation timestamp saved in the report metadata
     * @return the parsed scan metadata and findings for the selected target, or a missing-report status
     */
    private static Scan emptyScan(String status, String generated) {
        return new Scan(status, generated, List.of(), List.of(), List.of(), 0);
    }

    /**
     * Reads ZAP JSON alerts, applies configured scope/triage filters, and produces the report finding collection.
     *
     * @param target the configured application/report target
     * @param tests the collection of tests examined by the method
     * @param config the reporting configuration used to resolve paths and rules
     * @return the parsed scan metadata and findings for the selected target, or a missing-report status
     */
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
                        String findingName = value(alert.get("name"));

                        if (findingName.isBlank())
                            findingName = value(alert.get("alert"));

                        Map<FindingFilter.Decision, List<Map<?, ?>>> grouped =
                                new EnumMap<>(FindingFilter.Decision.class);

                        Map<FindingFilter.Decision, String> reasons =
                                new EnumMap<>(FindingFilter.Decision.class);

                        Object rawInstances = alert.get("instances");
                        if (!(rawInstances instanceof List<?> instances)) continue;

                        for (Object rawInstance : instances) {
                            if (!(rawInstance instanceof Map<?, ?> instance)) continue;

                            FindingFilter.Classification classification =
                                    filter.classify(value(instance.get("uri")), pluginId, findingName);

                            if (classification.decision() == FindingFilter.Decision.OUT_OF_SCOPE) {
                                excludedInstances++;
                                continue;
                            }

                            grouped.computeIfAbsent(classification.decision(),
                                    ignored -> new ArrayList<>()).add(instance);

                            reasons.putIfAbsent(classification.decision(), classification.reason());
                        }

                        for (var group : grouped.entrySet()) {
                            Finding finding = createFinding(alert, group.getValue(),
                                    reasons.get(group.getKey()));

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

            return new Scan("ZAP findings loaded", generated, included, review,
                    suppressed, excludedInstances);
        } catch (Exception e) {
            return emptyScan("ZAP JSON parsing failed: " + e.getMessage(), "Unavailable");
        }
    }

    /**
     * Converts a ZAP alert and its request instances into a display finding.
     *
     * @param alert the ZAP alert entry being converted into a report finding
     * @param instances the request/response observations attached to a ZAP alert
     * @param reason the explanation for the review or filtering decision
     * @return the report finding assembled from a ZAP alert and its request instances
     */
    private static Finding createFinding(Map<?, ?> alert, List<Map<?, ?>> instances, String reason) {
        Map<?, ?> first = instances.get(0);

        return new Finding(value(alert.get("name")),
                Integer.parseInt(value(alert.get("riskcode"))),
                confidenceLabel(value(alert.get("confidence"))),
                value(first.get("uri")),
                value(first.get("method")),
                buildEvidence(instances),
                plain(value(alert.get("desc"))),
                plain(value(alert.get("solution"))),
                value(alert.get("cweid")),
                reason == null ? "" : reason);
    }

    /**
     * Formats request-level evidence recorded for ZAP alert instances.
     *
     * @param instances the request/response observations attached to a ZAP alert
     * @return the formatted evidence summary built from the matching request instances
     */
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

    /**
     * Appends a labeled value to a text evidence block when available.
     *
     * @param out the mutable text buffer receiving generated report markup
     * @param label the human-readable caption rendered with the value
     * @param value the input value to convert or display
     */
    private static void appendLine(StringBuilder out, String label, String value) {
        if (!value.isBlank())
            out.append(label).append(": ").append(value.trim()).append("\n");
    }

    /**
     * Renders a standalone HTML assessment for one configured target.
     *
     * @param target the configured application/report target
     * @param tests the collection of tests examined by the method
     * @param scan the parsed ZAP findings and metadata to render
     * @param config the reporting configuration used to resolve paths and rules
     * @param assets the collection of assets examined by the method
     * @return the complete HTML assessment page for one configured target
     */
    private static String buildReport(Target target, List<TestCase> tests, Scan scan,
                                      ReportingConfiguration config, List<Path> assets) {
        long passed = tests.stream().filter(t -> t.status().equals("PASSED")).count();
        long failed = tests.stream().filter(t -> t.status().equals("FAILED")).count();
        long skipped = tests.stream().filter(t -> t.status().equals("SKIPPED")).count();
        long screenshots = tests.stream().mapToLong(t -> t.screenshots().size()).sum();

        StringBuilder html = new StringBuilder(35000);
        html.append(pageStart(target.title() + " Security Assessment", true,
                assets, config.assetFolderName()));

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

            html.append("</div><p class=\"note\">")
                    .append("Severity counts cover in-scope, unsuppressed alert groups only. ")
                    .append("Filtering is request-level; the original ZAP JSON is retained. ")
                    .append("Needs validation and suppressed findings are not certified false positives.")
                    .append("</p>");
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

        html.append("<details class=\"filter-audit\"><summary><strong>Needs validation (")
                .append(scan.review().size())
                .append(")</strong> — lower-signal findings</summary>")
                .append("<p class=\"note\">These observations match configured review rules. ")
                .append("They remain available for manual validation, not marked false positives.</p>")
                .append("<div class=\"finding-list\">");

        for (Finding finding : scan.review())
            appendFinding(html, finding);

        html.append("</div></details>");

        html.append("<details class=\"filter-audit\"><summary><strong>Suppressed by rule (")
                .append(scan.suppressed().size())
                .append(")</strong></summary>")
                .append("<p class=\"note\">These findings were hidden from the main severity totals ")
                .append("by explicit configuration, but remain available for audit here.</p>")
                .append("<div class=\"finding-list\">");

        for (Finding finding : scan.suppressed())
            appendFinding(html, finding);

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

    /**
     * Appends one test execution and its evidence to a generated report.
     *
     * @param html the HTML buffer receiving escaped report content
     * @param test the TestNG test record to render or inspect
     */
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

    /**
     * Builds the HTML landing page linking to generated assessments.
     *
     * @param summaries the collection of summaries examined by the method
     * @param config the reporting configuration used to resolve paths and rules
     * @param assets the collection of assets examined by the method
     * @return the HTML report index linking generated assessment pages
     * @throws IOException if a required file or HTTP stream cannot be read or written
     */
    private static String buildIndex(List<Summary> summaries, ReportingConfiguration config,
                                     List<Path> assets) throws IOException {
        StringBuilder html = new StringBuilder(4000);
        html.append(pageStart("Security Assessment Index", false,
                assets, config.assetFolderName()));

        html.append("<header><h1>Security Assessments</h1>")
                .append("<p>Automated TestNG and ZAP results by application.</p></header>");

        for (Summary summary : summaries) {
            String assessmentName = config.assessmentFile(summary.target().id())
                    .getFileName().toString();

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

    /**
     * Starts a report HTML document, including styles and scripts when enabled.
     *
     * @param title the heading or browser title of the generated HTML
     * @param interactive whether the HTML page includes interactive filtering controls
     * @param assets the collection of assets examined by the method
     * @param assetFolder the directory containing generated report styles and scripts
     * @return the opening HTML document markup and configured asset links
     */
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

    /**
     * Appends a vulnerability finding section to the generated report.
     *
     * @param html the HTML buffer receiving escaped report content
     * @param finding the filtered ZAP finding to append to the report
     */
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

        if (!finding.reason().isBlank())
            section(html, "Filter decision", finding.reason());

        section(html, "Description", finding.description());

        html.append("<div class=\"evidence-section\"><h3>ZAP Observations</h3><pre>")
                .append(escape(finding.evidence()))
                .append("</pre></div>");

        section(html, "Recommendation", finding.solution());
        html.append("</div></details>");
    }

    /**
     * Appends a labeled finding field to the static HTML report.
     *
     * @param html the HTML buffer receiving escaped report content
     * @param label the human-readable caption rendered with the value
     * @param value the input value to convert or display
     */
    private static void field(StringBuilder html, String label, String value) {
        html.append("<div class=\"evidence-field\"><span class=\"field-label\">")
                .append(escape(label))
                .append("</span><span class=\"field-value\">")
                .append(escape(value.isBlank() ? "Not provided" : value))
                .append("</span></div>");
    }

    /**
     * Appends a titled section with the supplied text to generated HTML.
     *
     * @param html the HTML buffer receiving escaped report content
     * @param title the heading or browser title of the generated HTML
     * @param content the source or input text to parse or transform
     */
    private static void section(StringBuilder html, String title, String content) {
        html.append("<div class=\"evidence-section\"><h3>")
                .append(escape(title)).append("</h3><p>")
                .append(escape(content.isBlank() ? "Not provided" : content))
                .append("</p></div>");
    }

    /**
     * Appends a summary metric card to a generated assessment page.
     *
     * @param html the HTML buffer receiving escaped report content
     * @param number the amount, count, or identifier to format
     * @param label the human-readable caption rendered with the value
     */
    private static void card(StringBuilder html, long number, String label) {
        html.append("<div class=\"card\"><strong>").append(number)
                .append("</strong><span>").append(escape(label))
                .append("</span></div>");
    }

    /**
     * Counts ZAP findings matching the selected severity risk code.
     *
     * @param findings the collection of findings examined by the method
     * @param risk the numeric ZAP alert risk level
     * @return the number of findings matching the selected ZAP risk level
     */
    private static long count(List<Finding> findings, int risk) {
        return findings.stream().filter(f -> f.risk() == risk).count();
    }

    /**
     * Converts a numeric ZAP risk level to a readable severity label.
     *
     * @param risk the numeric ZAP alert risk level
     * @return the human-readable severity corresponding to the ZAP risk level
     */
    private static String severity(int risk) {
        return switch (risk) {
            case 3 -> "High";
            case 2 -> "Medium";
            case 1 -> "Low";
            default -> "Info";
        };
    }

    /**
     * Converts a ZAP confidence identifier to display text.
     *
     * @param confidence the scanner confidence code or label
     * @return the descriptive label for the ZAP confidence code
     */
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

    /**
     * Converts a nullable ZAP report value to text.
     *
     * @param object the value to format for the HTML report
     * @return a safe string representation of the supplied value
     */
    private static String value(Object object) {
        return object == null ? "" : object.toString();
    }

    /**
     * Converts HTML-marked ZAP text into plain display content.
     *
     * @param value the input value to convert or display
     * @return plain text derived from source alert markup
     */
    private static String plain(String value) {
        return value.replaceAll("(?s)<[^>]*>", " ")
                .replace("&nbsp;", " ").replace("&amp;", "&")
                .replace("&lt;", "<").replace("&gt;", ">")
                .replaceAll("\\s+", " ").trim();
    }

    /**
     * Escapes special characters before writing them into generated HTML.
     *
     * @param value the input value to convert or display
     * @return HTML-escaped content suitable for report rendering
     */
    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * Copies CSS/JavaScript report assets into the configured output folder.
     *
     * @param config the reporting configuration used to resolve paths and rules
     * @return the asset paths copied into the generated report directory
     * @throws IOException if a required file or HTTP stream cannot be read or written
     */
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
