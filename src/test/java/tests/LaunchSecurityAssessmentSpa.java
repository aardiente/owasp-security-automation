package tests;

/**
 * Provides a convenient entry point for launching the Security Assessment SPA
 * directly from the test source root.
 *
 * <p>The SPA is a read-only dashboard that displays previously generated
 * OWASP ZAP findings, automated test results, and screenshot evidence.</p>
 *
 * <p>Launching the SPA does not start Docker containers, execute TestNG
 * suites, or initiate active/passive security scans.</p>
 */
public final class LaunchSecurityAssessmentSpa {

    /**
     * Prevents instantiation of this launcher class.
     */
    private LaunchSecurityAssessmentSpa() {}

    /**
     * Starts the Security Assessment SPA using the reporting configuration
     * available on the test classpath.
     *
     * @param args startup arguments forwarded to the SPA
     * @throws java.io.IOException if the server cannot initialize or bind
     *                             to its configured address
     */
    public static void main(String[] args) throws java.io.IOException {
        utilities.reporting.SecurityAssessmentSpa.main(args);
    }
}
