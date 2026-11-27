package utilities;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Creates consistent screenshot filenames so saved evidence can be matched to the correct test in reports.
 */
public final class EvidenceNames {
    /**
     * Prevents creation of the naming utility.
     */
    private EvidenceNames() {}

    /**
     * Filename modes used by the screenshot naming policy: one per method, one per test inputs, or individual screenshots named by time.
     */
    public enum Mode { METHOD, PARAMETERS, TIMESTAMP }

    /**
     * Creates a consistent screenshot filename, or leaves timestamp-based naming to the caller.
     */
    public static String deterministicName(Mode mode, String method, Object[] parameters) {
        String name = sanitize(method);
        return switch (mode) {
            case METHOD -> name + ".png";
            case PARAMETERS -> parameters == null || parameters.length == 0 ? name + ".png"
                    : name + "-" + sanitize(String.valueOf(parameters[0]))
                    + "-" + parameterHash(parameters) + ".png";
            case TIMESTAMP -> null;
        };
    }

    /**
     * Creates a folder for unregistered test packages.
     */
    public static String defaultFolder(String packageName) {
        String[] parts = packageName.split("\\.");
        String application = parts.length > 1 && parts[0].equalsIgnoreCase("tests")
                ? parts[1] : parts[parts.length - 1];
        return application.isBlank() ? "default" : sanitize(application.toLowerCase(Locale.ROOT));
    }

    /**
     * Returns a short, consistent identifier based on the test inputs.
     */
    public static String parameterHash(Object[] parameters) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(Arrays.deepToString(parameters).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(Arrays.copyOf(digest, 6));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable.", e);
        }
    }

    /**
     * Preserves the existing filesystem-safe naming convention.
     */
    public static String sanitize(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
