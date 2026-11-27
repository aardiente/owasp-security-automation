package utilities.reporting;

/**
 * Stores the reporting settings for one application, including its name, test package, report paths and screenshot rules.
 */
record ReportingTarget(String id, String title, String packagePrefix, String siteUrl,
                       String zapFile, EvidencePolicy evidencePolicy,
                       String activeZapFile, String activeScanFile) {
    /**
     * Returns the configured screenshot folder name.
     */
    String screenshotFolder() { return evidencePolicy.folder(); }
}
