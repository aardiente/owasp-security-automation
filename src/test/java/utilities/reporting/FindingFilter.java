package utilities.reporting;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Request-level classification; original ZAP JSON is never modified. */
final class FindingFilter {

    enum Decision { INCLUDE, REVIEW, SUPPRESS, OUT_OF_SCOPE }
    record Classification(Decision decision, String reason) {}

    private record Rule(String selector, String glob, Pattern pattern) {
        /**
         * Performs the matches operation used by this component.
         *
         * @param pluginId the ZAP plugin identifier for the finding
         * @param findingName the human-readable ZAP alert name
         * @param path the path or URL passed to the request or file operation
         * @return {@code true} when the finding and URL path match this rule
         */
        boolean matches(String pluginId, String findingName, String path) {
            return (selector.equals(pluginId) || normalized(selector).equals(normalized(findingName)))
                    && pattern.matcher(path).matches();
        }
        /**
         * Performs the description operation used by this component.
         *
         * @return the human-readable selector and path pattern for this rule
         */
        String description() { return glob.equals("*") ? selector : selector + "@" + glob; }
    }

    private final URI origin;
    private final List<String> scopePaths = new ArrayList<>();
    private final List<Rule> review;
    private final List<Rule> suppress;

    /**
     * Creates the finding filter component and initializes the dependencies required by its methods.
     *
     * <p>Creates a per-target scope and triage filter from report settings.</p>
     *
     * @param config the reporting configuration used to resolve paths and rules
     * @param id the configured identifier to look up
     * @param site the application origin used as the report scope
     * @throws IOException if a required file or HTTP stream cannot be read or written
     */
    FindingFilter(ReportingConfiguration config, String id, String site) throws IOException {
        try {
            origin = URI.create(site);
            if (origin.getHost() == null || origin.getScheme() == null)
                throw new IllegalArgumentException("Missing scheme/host");
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid configured site for " + id + ": " + site, e);
        }
        String prefix = "target." + id + ".";
        for (String raw : config.optional(prefix + "scopePaths", "/").split(",")) {
            String path = raw.trim();
            if (!path.startsWith("/")) throw new IOException("Invalid scope path: " + path);
            scopePaths.add(path);
        }
        review = rules(config.optional(prefix + "reviewRules", ""));
        suppress = rules(config.optional(prefix + "suppressRules", ""));
    }

    /**
     * Checks whether the supplied request URL belongs to the configured application origin.
     *
     * @param url the URL to navigate to, inspect, or request
     * @return {@code true} when the URL shares the configured scheme, hostname, and effective port
     */
    boolean matchesOrigin(String url) {
        try { return sameOrigin(URI.create(url)); }
        catch (IllegalArgumentException e) { return false; }
    }

    /**
     * Assigns a report finding to INCLUDE, REVIEW, SUPPRESS, or OUT_OF_SCOPE based on configured policy.
     *
     * <p>A finding can be included, marked for review, suppressed by rule, or excluded when its URL is outside the configured target origin or path.</p>
     *
     * @param url the URL to navigate to, inspect, or request
     * @param pluginId the ZAP plugin identifier for the finding
     * @param findingName the human-readable ZAP alert name
     * @return the in-scope, review, suppression, or out-of-scope decision with its reason
     */
    Classification classify(String url, String pluginId, String findingName) {
        URI uri;
        try { uri = URI.create(url); }
        catch (IllegalArgumentException e) {
            return new Classification(Decision.OUT_OF_SCOPE, "Invalid request URL");
        }
        if (!sameOrigin(uri))
            return new Classification(Decision.OUT_OF_SCOPE, "Outside configured target origin");
        String path = uri.getRawPath();
        if (path == null || path.isBlank()) path = "/";
        boolean inScope = false;
        for (String allowed : scopePaths) {
            if (allowed.equals("/") || path.equals(allowed) ||
                    path.startsWith(allowed.endsWith("/") ? allowed : allowed + "/")) {
                inScope = true; break;
            }
        }
        if (!inScope)
            return new Classification(Decision.OUT_OF_SCOPE, "Outside configured path scope");
        for (Rule rule : suppress)
            if (rule.matches(pluginId, findingName, path))
                return new Classification(Decision.SUPPRESS, "Explicit suppression: " + rule.description());
        for (Rule rule : review)
            if (rule.matches(pluginId, findingName, path))
                return new Classification(Decision.REVIEW, "Needs validation: " + rule.description());
        return new Classification(Decision.INCLUDE, "");
    }

    /**
     * Compares a candidate URI against the configured scheme, host, and effective port.
     *
     * @param uri the URI whose scheme, host and port are being examined
     * @return {@code true} when the candidate URI has the configured origin
     */
    private boolean sameOrigin(URI uri) {
        return uri.getHost() != null && uri.getScheme() != null &&
                origin.getScheme().equalsIgnoreCase(uri.getScheme()) &&
                origin.getHost().equalsIgnoreCase(uri.getHost()) && port(origin) == port(uri);
    }

    /**
     * Returns an explicit URL port or the appropriate default HTTP(S) port.
     *
     * @param uri the URI whose scheme, host and port are being examined
     * @return the explicit or default port used for origin comparison
     */
    private static int port(URI uri) {
        if (uri.getPort() >= 0) return uri.getPort();
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    /**
     * Normalizes a finding name for case-insensitive rule matching.
     *
     * @param name the finding, resource, or application name to resolve
     * @return normalized alert text used for case-insensitive rule matching
     */
    private static String normalized(String name) {
        return name == null ? "" : name.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    /**
     * Parses the configured review and suppression rules into matchable expressions.
     *
     * @param raw the unparsed source value or rule expression
     * @return the parsed finding-name/path rules declared in reporting configuration
     * @throws IOException if a required file or HTTP stream cannot be read or written
     */
    private static List<Rule> rules(String raw) throws IOException {
        List<Rule> result = new ArrayList<>();
        if (raw.isBlank()) return result;
        for (String part : raw.split(";")) {
            String entry = part.trim();
            if (entry.isEmpty()) continue;
            int at = entry.lastIndexOf('@');
            String name = at < 0 ? entry : entry.substring(0, at).trim();
            String glob = at < 0 ? "*" : entry.substring(at + 1).trim();
            if (name.isEmpty() || glob.isEmpty()) throw new IOException("Invalid finding rule: " + entry);
            StringBuilder regex = new StringBuilder("^");
            for (char c : glob.toCharArray()) {
                if (c == '*') regex.append(".*");
                else if (c == '?') regex.append('.');
                else regex.append(Pattern.quote(String.valueOf(c)));
            }
            result.add(new Rule(name, glob, Pattern.compile(regex.append('$').toString())));
        }
        return result;
    }
}
