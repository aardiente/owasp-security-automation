
package utilities.reporting;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Classification affects only the presentation layer. Raw ZAP JSON remains intact. */
final class FindingFilter {

    enum Decision { INCLUDE, REVIEW, SUPPRESS, OUT_OF_SCOPE }
    record Classification(Decision decision, String reason) {}
    private record Rule(String id, String glob, Pattern pathPattern) {
        boolean matches(String pluginId, String path) {
            return id.equals(pluginId) && pathPattern.matcher(path).matches();
        }
        String description() { return id + "@" + glob; }
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
            if (!path.startsWith("/")) throw new IOException("Invalid scope path: " + path);
            paths.add(path);
        }
        reviewRules = parseRules(config.optional(prefix + "reviewRules", ""));
        suppressRules = parseRules(config.optional(prefix + "suppressRules", ""));
    }

    boolean matchesOrigin(String url) {
        try { return sameOrigin(URI.create(url)); }
        catch (IllegalArgumentException e) { return false; }
    }

    Classification classify(String url, String pluginId) {
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

        for (Rule rule : suppressRules)
            if (rule.matches(pluginId, path))
                return new Classification(Decision.SUPPRESS, "Explicit suppression rule: " + rule.description());
        for (Rule rule : reviewRules)
            if (rule.matches(pluginId, path))
                return new Classification(Decision.REVIEW, "Needs validation: " + rule.description());
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

    private static List<Rule> parseRules(String raw) throws IOException {
        List<Rule> rules = new ArrayList<>();
        if (raw.isBlank()) return rules;
        for (String item : raw.split(";")) {
            String entry = item.trim();
            if (entry.isEmpty()) continue;
            int at = entry.indexOf('@');
            if (at < 1 || at == entry.length() - 1)
                throw new IOException("Invalid rule, expected pluginId@pathGlob: " + entry);
            String pluginId = entry.substring(0, at);
            String glob = entry.substring(at + 1);
            if (!pluginId.matches("[0-9]+"))
                throw new IOException("Invalid ZAP plugin ID in rule: " + entry);
            rules.add(new Rule(pluginId, glob, globPattern(glob)));
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
