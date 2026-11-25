package utilities.reporting;

/** Immutable metadata shared by the HTML assessment and read-only SPA. */
record ReportingTarget(String id, String title, String packagePrefix, String siteUrl,
                       String zapFile, EvidencePolicy evidencePolicy,
                       String activeZapFile, String activeScanFile) {
    /** Returns the configured evidence directory name. */
    String screenshotFolder() { return evidencePolicy.folder(); }
}
