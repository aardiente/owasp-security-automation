package utilities.reporting;

import utilities.EvidenceNames;

/**
 * Stores the screenshot folder, naming rules and cleanup preferences for one application.
 */
public record EvidencePolicy(String folder, EvidenceNames.Mode naming, boolean cleanupLegacy) {}
