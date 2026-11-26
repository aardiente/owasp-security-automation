package utilities.reporting;

import utilities.EvidenceNames;

/** Evidence layout and naming policy for one configured application. */
public record EvidencePolicy(String folder, EvidenceNames.Mode naming, boolean cleanupLegacy) {}
