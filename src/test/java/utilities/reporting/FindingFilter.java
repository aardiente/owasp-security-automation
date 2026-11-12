
package utilities.reporting;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Classification affects only the presentation layer. Raw ZAP JSON remains intact. */
final class FindingFilter {

    enum Decision { INCLUDE, REVIEW, SUPPRESS, OUT_OF_SCOPE }
    record Classification(Decision decision, String reason) {}

    private record Rule(String selector, String glob, Pattern pathPattern) {
        boolean matches(String pluginId, String findingName, String path) {
            boolean matchesFinding = selector.equals(pluginId) ||
                    normalizeName(selector).equals(normalizeName(findingName));
            return matchesFinding && pathPattern.matcher(path).matches();
        }

        String description() {
            return glob.equals("*") ? selector : selector + "@" + glob;
        }
    }

    private final URI target;
    private final List<String> paths;
    private final List<Rule> reviewRules;
    private final List<Rule> suppressRules;

    FindingFilter(ReportingConfiguration config, String id, String site) throws IOException {
        try {
            target = URI.create(site);
            if (target.getHost() == null || target.getScheme() == null)
                throw new IllegalArgumentException("Missing URL scheme or host");
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid target site for " + id + ": " + site, e);
        }

        String prefix = "target." + id + ".";
        paths = new ArrayList<>();

        for (String raw : config.optional(prefix + "scopePaths", "/").split(",")) {
            String path = raw.trim();
            if (!path.startsWith("/"))
                throw new IOException("Invalid scope path: " + path);
            paths.add(path);
        }

        reviewRules = parseRules(config.optional(prefix + "reviewRules", ""));
        suppressRules = parseRules(config.optional(prefix + "suppressRules", ""));
    }

    boolean matchesOrigin(String url) {
        try {
            return sameOrigin(URI.create(url));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    Classification classify(String url, String pluginId, String findingName) {
        URI uri;

        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            return new Classification(Decision.OUT_OF_SCOPE, "Invalid request URL");
        }

        if (!sameOrigin(uri))
            return new Classification(Decision.OUT_OF_SCOPE, "Outside configured target origin");

        String path = uri.getRawPath();
        if (path == null || path.isBlank()) path = "/";

        boolean inScope = false;

        for (String allowed : paths) {
            if (allowed.equals("/") || path.equals(allowed) ||
                    path.startsWith(allowed.endsWith("/") ? allowed : allowed + "/")) {
                inScope = true;
                break;
            }
        }

        if (!inScope)
            return new Classification(Decision.OUT_OF_SCOPE, "Outside configured path scope");

        for (Rule rule : suppressRules) {
            if (rule.matches(pluginId, findingName, path))
                return new Classification(Decision.SUPPRESS,
                        "Explicit suppression rule: " + rule.description());
        }

        for (Rule rule : reviewRules) {
            if (rule.matches(pluginId, findingName, path))
                return new Classification(Decision.REVIEW,
                        "Needs validation: " + rule.description());
        }

        return new Classification(Decision.INCLUDE, "");
    }

    private boolean sameOrigin(URI uri) {
        return uri.getHost() != null && uri.getScheme() != null &&
                target.getScheme().equalsIgnoreCase(uri.getScheme()) &&
                target.getHost().equalsIgnoreCase(uri.getHost()) &&
                effectivePort(target) == effectivePort(uri);
    }

    private static int effectivePort(URI uri) {
        if (uri.getPort() >= 0) return uri.getPort();
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    private static String normalizeName(String name) {
        if (name == null) return "";
        return name.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static List<Rule> parseRules(String raw) throws IOException {
        List<Rule> rules = new ArrayList<>();
        if (raw.isBlank()) return rules;

        for (String item : raw.split(";")) {
            String entry = item.trim();
            if (entry.isEmpty()) continue;

            int separator = entry.lastIndexOf('@');
            String selector = separator < 0 ? entry : entry.substring(0, separator).trim();
            String glob = separator < 0 ? "*" : entry.substring(separator + 1).trim();

            if (selector.isBlank())
                throw new IOException("Finding name or plugin ID missing in rule: " + entry);

            if (glob.isBlank())
                throw new IOException("Path pattern missing in rule: " + entry);

            rules.add(new Rule(selector, glob, globPattern(glob)));
        }

        return rules;
    }

    private static Pattern globPattern(String glob) {
        StringBuilder regex = new StringBuilder("^");

        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            if (c == '*') regex.append(".*");
            else if (c == '?') regex.append('.');
            else regex.append(Pattern.quote(String.valueOf(c)));
        }

        return Pattern.compile(regex.append('$').toString());
    }
}
