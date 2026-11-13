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
        boolean matches(String pluginId, String findingName, String path) {
            return (selector.equals(pluginId) || normalized(selector).equals(normalized(findingName)))
                    && pattern.matcher(path).matches();
        }
        String description() { return glob.equals("*") ? selector : selector + "@" + glob; }
    }

    private final URI origin;
    private final List<String> scopePaths = new ArrayList<>();
    private final List<Rule> review;
    private final List<Rule> suppress;

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

    boolean matchesOrigin(String url) {
        try { return sameOrigin(URI.create(url)); }
        catch (IllegalArgumentException e) { return false; }
    }

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

    private boolean sameOrigin(URI uri) {
        return uri.getHost() != null && uri.getScheme() != null &&
                origin.getScheme().equalsIgnoreCase(uri.getScheme()) &&
                origin.getHost().equalsIgnoreCase(uri.getHost()) && port(origin) == port(uri);
    }

    private static int port(URI uri) {
        if (uri.getPort() >= 0) return uri.getPort();
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    private static String normalized(String name) {
        return name == null ? "" : name.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

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
