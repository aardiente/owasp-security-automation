package utilities;

import java.util.Locale;

/**
 * Provides centralized operating system and display server detection
 * for platform-specific framework configuration.
 */
public final class SystemEnvironment {

    public enum OperatingSystem {
        WINDOWS,
        MACOS,
        LINUX,
        BSD,
        SOLARIS,
        AIX,
        UNKNOWN
    }

    public enum DisplayServer {
        WAYLAND,
        X11,
        UNKNOWN,
        NOT_APPLICABLE
    }

    private static final OperatingSystem OS = detectOperatingSystem();

    /**
     * Prevents instantiation of this utility class.
     */
    private SystemEnvironment() {}

    /**
     * Detects the operating system used by the current JVM.
     *
     * @return detected operating system
     */
    private static OperatingSystem detectOperatingSystem() {
        String name = System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT);

        if (name.contains("mac") || name.contains("darwin"))
            return OperatingSystem.MACOS;

        if (name.contains("windows"))
            return OperatingSystem.WINDOWS;

        if (name.contains("linux"))
            return OperatingSystem.LINUX;

        if (name.contains("bsd"))
            return OperatingSystem.BSD;

        if (name.contains("sunos") || name.contains("solaris"))
            return OperatingSystem.SOLARIS;

        if (name.contains("aix"))
            return OperatingSystem.AIX;

        return OperatingSystem.UNKNOWN;
    }

    /**
     * Returns the detected operating system.
     *
     * @return current operating system
     */
    public static OperatingSystem getOperatingSystem() {
        return OS;
    }

    /**
     * Determines whether the current operating system matches
     * the specified operating system.
     *
     * @param operatingSystem operating system to compare
     * @return true if the operating systems match
     */
    public static boolean is(OperatingSystem operatingSystem) {
        return OS == operatingSystem;
    }

    /**
     * Detects the display server used by the current Linux session.
     *
     * @return detected display server
     */
    public static DisplayServer getDisplayServer() {
        if (!is(OperatingSystem.LINUX))
            return DisplayServer.NOT_APPLICABLE;

        String session = System.getenv("XDG_SESSION_TYPE");

        if ("wayland".equalsIgnoreCase(session))
            return DisplayServer.WAYLAND;

        if ("x11".equalsIgnoreCase(session))
            return DisplayServer.X11;

        if (hasEnvironmentVariable("WAYLAND_DISPLAY"))
            return DisplayServer.WAYLAND;

        if (hasEnvironmentVariable("DISPLAY"))
            return DisplayServer.X11;

        return DisplayServer.UNKNOWN;
    }

    /**
     * Determines whether the current environment uses Linux
     * with a Wayland display server.
     *
     * @return true if Linux and Wayland are detected
     */
    public static boolean isLinuxWayland() {
        return is(OperatingSystem.LINUX)
                && getDisplayServer() == DisplayServer.WAYLAND;
    }

    /**
     * Determines whether an environment variable contains a value.
     *
     * @param name environment variable name
     * @return true if the variable exists and is not blank
     */
    private static boolean hasEnvironmentVariable(String name) {
        String value = System.getenv(name);
        return value != null && !value.isBlank();
    }
}