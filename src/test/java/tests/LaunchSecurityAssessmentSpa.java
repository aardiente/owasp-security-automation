package tests;

/**
 * Starts the local Security Assessment dashboard so existing reports can be viewed without running tests again.
 */
public final class LaunchSecurityAssessmentSpa {

    /**
     * Prevents creation of this launcher class.
     */
    private LaunchSecurityAssessmentSpa() {}

    /**
     * Starts the Security Assessment SPA using the reporting configuration available on the test project resources.
     *
     * @param args startup arguments forwarded to the SPA
     * @throws java.io.IOException if the server cannot initialize or bind to its configured address
     */
    public static void main(String[] args) throws java.io.IOException {
        utilities.reporting.SecurityAssessmentSpa.main(args);
    }
}
