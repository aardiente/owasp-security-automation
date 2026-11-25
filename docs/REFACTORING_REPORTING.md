# Framework refactoring — reporting and evidence infrastructure

## Scope and design

This is an incremental source refactor of the supplied project, not a complete framework rewrite. Existing security test classes, page objects, scan implementations, dependencies, and TestNG suite definitions are unchanged. The refactor focuses on shared infrastructure that can be independently verified without running application security tests.

| Component | Responsibility |
| --- | --- |
| `ReportingConfiguration` | Loads and validates reporting configuration, resolves paths and constructs an immutable target registry. |
| `ReportingTarget` | Represents a configured application's report metadata for both renderers. |
| `EvidencePolicy` | Represents the configured screenshot folder, naming strategy and legacy cleanup flag. |
| `EvidenceNames` | Provides reusable filename rules, parameter identifiers and safe name normalization. |
| `ReportCsv` | Implements the existing CSV interchange format for both export and import. |
| `TestExecutions` | Adapts TestNG suite results into a common execution model. |

The HTML and SPA reporters no longer maintain independent target schemas. The screenshot producer and HTML renderer share filename rules, and both report renderers use the configured evidence folders. The HTML renderer prefers the screenshot filename recorded by TestNG when it matches the expected method and capture window. The CSV reporter and HTML reporter share the same execution adapter. The SPA remains a read-only consumer of report files.

## Configuration and compatibility

The existing `src/test/resources/report-targets.properties` remains the source of reporting configuration. Paths are resolved relative to the working directory, as before. The existing `security.reporting.config` and `security.reporting.resource` selectors remain supported. Set the same configuration before running tests and launching the SPA.

New settings per target:

- `target.<id>.screenshotNaming`: `method`, `parameters`, or `timestamp`. The bundled defaults retain the existing Juice Shop and DVWA filename conventions. Other targets default to timestamped evidence.
- `target.<id>.screenshotCleanupLegacy`: `true` or `false`. The bundled Juice Shop configuration retains the existing once-per-directory legacy timestamp cleanup; DVWA does not opt in. Set it to `false` to preserve old evidence. Cleanup is ignored in timestamp mode.
- `target.<id>.screenshotFolder`: existing setting, now also used by screenshot capture and the HTML report. Defaults to the target ID if absent.

External configuration files created before this change should add the two new naming/cleanup properties for existing targets if they need identical screenshot behavior. Missing new settings default to timestamp naming and no cleanup. Invalid modes, duplicate target identifiers, and directory components in configured filenames are rejected during configuration loading. Existing report filenames, CSV column names, and SPA endpoints are retained. The CSV parser also preserves a single quoted empty field.

## Validation performed

From the project root:

```sh
python scripts/check_reporting.py
```

Passed in the delivery environment using OpenJDK 21 with `javac --release 17`. The script compiles and exercises the dependency-free reporting configuration, filename helpers and CSV codec, including external configuration, invalid naming modes, invalid paths, duplicate targets, and CSV escaping.

Full Maven compilation, TestNG execution, browser tests, Docker integration, and report rendering were not verified. Maven and the required third-party dependencies were not available in the delivery environment. Run your existing Maven/TestNG workflow locally before adopting these changes. The newly added TestNG result adapter is not covered by the dependency-free checks.

The supplied generated `docs/README.html` and `docs/api.html` remain unchanged: documentation regeneration was attempted but the existing strict generator rejected a missing constructor Javadoc in the original `SqlInjectionPage.java`. This file documents the changed architecture separately.

## Remaining technical debt

This pass intentionally leaves the application-specific challenge implementations, payload handling, browser/session lifecycle, and scanning logic unchanged. The largest application-specific classes remain large. Future maintenance should prioritize narrower responsibilities and explicit interfaces, resource/configuration consistency, documented contracts, and regression coverage before further restructuring. The two report renderers still have distinct presentation and report-parsing logic. This archive is not a claim that all redundancy or technical debt has been removed.

## Archive contents

The source tree, original documentation, scripts, resource files and test suite XMLs are included. Git metadata, IDE settings, generated build outputs, logs and prior execution artifacts are excluded. The original archive is unchanged.
