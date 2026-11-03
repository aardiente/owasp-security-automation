
# OWASP Security Automation Framework

A Java-based security testing framework for automating vulnerability assessments, security challenges, and browser-based testing against intentionally vulnerable web applications.

Built with **Java, Selenium WebDriver, TestNG, Maven, Docker/Testcontainers, and OWASP ZAP**, with experimental Ollama integration.

**Project Status:** Active Development

---

## Supported Applications

### OWASP Juice Shop

An intentionally vulnerable web application designed to demonstrate modern web security vulnerabilities.

Current testing capabilities include:

- Automated security challenge execution
- SQL injection and cross-site scripting (XSS) testing
- Authentication and authorization testing
- Security challenge completion verification
- Automated screenshot capture
- Score Board progress tracking

### DVWA (Damn Vulnerable Web Application)

An intentionally vulnerable PHP/MySQL application for testing common web application security vulnerabilities.

Current testing capabilities include:

- SQL injection testing
- Reflected XSS testing
- Multiple vulnerability security levels
- Data-driven testing with external payload files
- Automated browser interactions

---

## Technology Stack

| Technology | Purpose |
|---|---|
| Java | Primary programming language |
| Selenium WebDriver | Browser automation |
| TestNG | Test execution, assertions, and suite management |
| Maven | Dependency management |
| Docker | Isolated application and scanning environments |
| Testcontainers | Programmatic Docker container management |
| OWASP ZAP | Web traffic interception and security analysis |
| Apache POI | Excel-based test data |
| Ollama | Experimental local AI integration |

---

## Framework Features

- **Page Object Model:** Separates test logic from web application interactions.
- **Centralized Configuration:** Controls browser behavior through `config.properties`.
- **Headless Browser Support:** Allows automated execution without a visible browser window.
- **Cross-Browser Support:** Provides Selenium driver creation for Chrome, Firefox, Edge, and Safari, subject to browser-specific limitations.
- **Docker Integration:** Starts intentionally vulnerable applications in isolated containers.
- **OWASP ZAP Integration:** Routes Selenium traffic through a security proxy.
- **Separate ZAP Reports:** Generates independent HTML and JSON reports for DVWA and Juice Shop.
- **Automated Security Assessments:** Generates an assessment index, per-application HTML dashboards, and TestNG CSV results.
- **ZAP Finding Details:** Shows severity, confidence, affected URLs, CWE, recommendations, and available request observations.
- **Screenshot Evidence Links:** Associates TestNG executions with screenshots already captured by `ScreenshotManager`; no additional screenshot folder is created.
- **Configurable Reporting:** Keeps application targets and reporting paths in `utilities.reporting/report-targets.properties`.
- **Automated Screenshots:** Captures browser evidence during testing.
- **SlowMo Debugging:** Optionally highlights and delays Selenium interactions.
- **Result Display:** Uses `browser.showResultsMs` to keep JavaScript alerts and selected results visible during SlowMo demonstrations.
- **Security Findings:** DVWA tests intentionally fail assertions when a vulnerability or security finding is detected.
- **TestNG Suites:** Supports individual application suites and combined execution.

---

## Prerequisites

Install the following before running the framework:

- Java Development Kit (JDK)
- IntelliJ IDEA or another Java IDE
- Maven or an IDE with Maven integration
- A running Docker Engine (Docker Desktop on supported systems, or Docker Engine on Linux)
- Google Chrome for ZAP-proxied tests
- Browser drivers managed through Selenium Manager or the local environment

Make sure the Docker daemon is running and accessible to your user before executing container-dependent tests. On Linux, verify the IDE has the same Docker access as a working terminal session.

---

## Project Configuration

The framework uses a centralized properties file to control Selenium browser behavior without requiring changes to Java source code.

### Configuration File Location

`src/test/resources/config.properties`

### Default Configuration

```properties
# Browser configuration
browser.headless=true
browser.viewport.width=1920
browser.viewport.height=1080
browser.implicitWaitSeconds=0

# SlowMo configuration
browser.slowMo.enabled=false
browser.slowMo.delayMs=250
browser.showResultsMs=3000
```

### Configuration Properties

| Property | Default | Description |
|---|---|---|
| `browser.headless` | `true` | Enables or disables headless browser execution |
| `browser.viewport.width` | `1920` | Configures browser window width in pixels |
| `browser.viewport.height` | `1080` | Configures browser window height in pixels |
| `browser.implicitWaitSeconds` | `0` | Sets Selenium's implicit element wait |
| `browser.slowMo.enabled` | `false` | Enables or disables visual interaction highlighting and delays |
| `browser.slowMo.delayMs` | `250` | Sets the delay in milliseconds when SlowMo is enabled |
| `browser.showResultsMs` | `3000` | Keeps supported security results and JavaScript alerts visible during SlowMo mode |

### Headless Mode

Headless mode runs the browser without displaying a visible window.

To enable headless mode:

```properties
browser.headless=true
```

To display the browser during testing:

```properties
browser.headless=false
```

The setting is applied when a new browser session starts.

### Browser Viewport

The configured browser window size is:

```properties
browser.viewport.width=1920
browser.viewport.height=1080
```

The framework applies these dimensions both through browser startup arguments where supported and through Selenium window management.

**Important:** An explicit `1920x1080` browser window resolved previous headless UI interaction failures involving overlapping Juice Shop elements and cookie-consent dialogs.

For that reason, maintaining a consistent browser window size is recommended when switching between headed and headless execution.

The configured window dimensions are not necessarily identical to the webpage's internal viewport dimensions.

### Selenium Implicit Wait

```properties
browser.implicitWaitSeconds=0
```

An implicit wait of zero prevents Selenium's element searches from automatically waiting for optional elements that are not present.

The framework primarily uses explicit waits through `WebDriverWait` and `ExpectedConditions`.

**Recommendation:** Keep the implicit wait at `0` unless a specific testing requirement justifies changing it.

### SlowMo Debugging

SlowMo can be enabled to visually highlight elements and introduce a configurable pause during Selenium interactions.

To enable:

```properties
browser.slowMo.enabled=true
browser.slowMo.delayMs=250
```

To disable:

```properties
browser.slowMo.enabled=false
```

When disabled, the listener does not add its optional highlighting and artificial interaction delays.

SlowMo is intended for debugging and demonstrations. It should generally remain disabled during routine test runs to reduce execution time.

When SlowMo is enabled, `browser.showResultsMs` controls the presentation pause before an observed JavaScript alert is accepted or dismissed, and before selected security tests finish showing results. This pause is disabled when `browser.slowMo.enabled=false`, regardless of the configured duration.

```properties
browser.slowMo.enabled=true
browser.slowMo.delayMs=250
browser.showResultsMs=3000
```

**Note:** SlowMo and result-display pauses are for presentations, not synchronization. Use Selenium explicit waits to determine when pages, elements, or alerts are ready.

### Configuration Loading

Configuration values are loaded by:

`src/test/java/automation/AppConfig.java`

`AppConfig` provides settings to the following components:

| Component | Configuration Usage |
|---|---|
| `BrowserFactory` | Headless mode and browser startup dimensions |
| `DriverManager` | Browser window size and implicit wait |
| `ZapManager` | ZAP-proxied browser window size and implicit wait |
| `SlowMoListener` | Interaction highlighting, delay configuration, and alert-result display |
| `DvwaSqlInjection` | Optional presentation pause before reporting SQL injection results |

Changes to `config.properties` take effect when tests run in a new JVM using the updated configuration.

### Overriding Configuration in IntelliJ

Configuration values can also be overridden using JVM system properties.

In IntelliJ IDEA:

1. Open **Run → Edit Configurations**.
2. Select the relevant TestNG run configuration.
3. Find **VM options**.
4. Enter the properties you want to override.
5. Run the test configuration.

Example:

```text
-Dbrowser.headless=false
-Dbrowser.viewport.width=1920
-Dbrowser.viewport.height=1080
```

System properties take precedence over values defined in `config.properties`.

This is useful for switching between local debugging and automated execution without modifying the configuration file.

---

## Docker Integration

The framework uses Testcontainers to manage vulnerable applications and OWASP ZAP.

Containerized environments include:

- OWASP Juice Shop
- DVWA
- OWASP ZAP

The framework uses a shared Docker network to support communication between application containers and the security proxy.

Container startup and shutdown are managed by the corresponding environment classes and TestNG lifecycle methods.

`ZapManager.initZap()` also waits until the ZAP API responds to `core.version()` before registering the proxy as ready. This prevents the browser from being launched immediately after ZAP announces that it is listening but before its API is usable. If readiness never succeeds, initialization fails explicitly rather than silently proceeding to the UI tests.

---

## Running Tests

Tests can be executed individually through IntelliJ IDEA or using TestNG suite configuration files.

### IntelliJ IDEA

1. Open the project in IntelliJ.
2. Start Docker and verify your user account can access the daemon.
3. Allow Maven dependencies to load.
4. Confirm the desired settings in `config.properties`.
5. Open the test class or TestNG suite XML.
6. Right-click and select **Run**.

### TestNG Suite Configurations

| Suite | Purpose |
|---|---|
| `testng-dvwa.xml` | Execute DVWA security tests |
| `testng-juiceshop.xml` | Execute Juice Shop challenge tests |
| `testng-all.xml` | Execute both application suites sequentially |

### Running with Maven

To run tests with Maven:

```bash
mvn test
```

To select a specific TestNG suite when using Maven Surefire:

```bash
mvn test -Dsurefire.suiteXmlFiles=testng-juiceshop.xml
```

Or:

```bash
mvn test -Dsurefire.suiteXmlFiles=testng-dvwa.xml
```

Suite execution depends on the project's configured Maven Surefire setup.

To run the combined suite explicitly:

```bash
mvn test -Dsurefire.suiteXmlFiles=testng-all.xml
```

### Running the Full Framework

The intended combined suite sequence is:

1. Start the DVWA and ZAP environments.
2. Execute DVWA security tests.
3. Generate the DVWA ZAP report.
4. Shut down DVWA and ZAP.
5. Start the Juice Shop and ZAP environments.
6. Execute Juice Shop security tests.
7. Capture the final Score Board screenshot.
8. Generate the Juice Shop ZAP report.
9. Shut down the remaining containers.
10. The registered TestNG reporters export execution results and generate the per-application security assessments and index.

Application execution is separated to avoid mixing DVWA and Juice Shop traffic histories in the same ZAP report. Reports are generated from suites that actually contain matching test results; a single-application run is not treated as a combined run.

### Interpreting TestNG Failures

**DVWA security tests deliberately use failing assertions to flag findings.** A triggered JavaScript alert in the Reflected XSS test produces a failure, because the payload executed. SQL injection tests can also report failures for demonstrated query manipulation or database error disclosure, depending on the current assertions.

A Selenium `WebDriverException`, failure to set the DVWA security level, or an element timeout during setup is an **automation issue**, not a confirmed vulnerability. Review the failure message and stack trace before classifying a run.

Juice Shop challenge tests generally use the opposite convention: a completed challenge passes.

DVWA Reflected XSS is exercised at LOW, MEDIUM, and HIGH security levels; SQL injection payloads are loaded from `src/test/java/data/sqli_payloads.xlsx` and run at LOW security. Payload execution does not necessarily guarantee every attack succeeds.

---

## OWASP ZAP Integration

OWASP ZAP intercepts Selenium browser traffic and performs passive analysis on requests and responses observed during the tests.

### How It Works

1. Testcontainers starts the application and the ZAP container on a shared Docker network.
2. `ZapManager.initZap()` confirms that the ZAP API responds before continuing.
3. Selenium creates a Chrome browser configured to use the ZAP proxy.
4. The application tests generate requests through that proxy.
5. ZAP records traffic and performs passive analysis.
6. During suite teardown, the framework waits for the passive-scan queue to empty.
7. `ZapManager.generateReport()` exports application-specific ZAP HTML and JSON reports.
8. The TestNG reporters use the available JSON and test results to generate the security assessment dashboards.

### Native ZAP Reports

| Application | HTML report | JSON report |
|---|---|---|
| DVWA | `target/zap-dvwa.html` | `target/security-reports/zap-dvwa.json` |
| OWASP Juice Shop | `target/zap-juiceshop.html` | `target/security-reports/zap-juiceshop.json` |

The native ZAP HTML reports remain available independently of the new assessment dashboard.

### ZAP Reporting Limitations

- The default workflow intercepts test traffic and performs passive analysis; it is not a comprehensive active scan.
- Findings depend on the requests exercised during the run and on which traffic passes through ZAP.
- The presence of a ZAP alert does not independently confirm exploitability.
- A successfully completed Juice Shop challenge does not necessarily produce a corresponding ZAP alert.
- The reporter only displays findings for the application's configured ZAP site, excluding other hosts seen in the capture.
- Report generation requires the corresponding test/scan lifecycle to finish; an unavailable or stale JSON file is reported rather than treated as a successful scan.

---

## Automated Security Assessment Reports

The reporting module combines application-specific ZAP observations with TestNG execution results, then produces browser-viewable HTML reports. It supports both DVWA and OWASP Juice Shop without duplicating the reporter for each application.

### Reporting Components

| File | Responsibility |
|---|---|
| `src/test/java/utilities/reporting/ReportingConfiguration.java` | Loads reporting configuration and resolves report, asset, and evidence locations. |
| `src/test/java/utilities/reporting/SecurityAssessmentReporter.java` | Builds the per-application assessments and shared index. |
| `src/test/java/utilities/reporting/TestNgResultsReporter.java` | Exports TestNG execution results as CSV. |
| `src/test/java/utilities/reporting/report-targets.properties` | Declares application targets, package prefixes, scan inputs, and output filenames. |
| `src/test/java/utilities/reporting/assets/security-assessment.css` | Styles dashboards, finding details, and expandable test evidence. |
| `src/test/java/utilities/reporting/assets/security-assessment.js` | Controls finding search, severity filtering, and expand/collapse interactions. |

The reporters are registered through the existing TestNG service file:

`src/test/resources/META-INF/services/org.testng.ITestNGListener`

The Java code generates the content; CSS and JavaScript remain separate files alongside the reporting classes, under the reporting package's `assets` directory. The reporter copies those assets to the generated report output so the HTML files can load them.

### Generated Assessment Files

With the current reporting configuration and both suites executed, output includes:

```text
target/
├── zap-dvwa.html
├── zap-juiceshop.html
├── screenshots/                         # Owned by ScreenshotManager
│   ├── dvwa/
│   └── juiceshop/
└── security-reports/
    ├── SecurityAssessment.html          # Index for executed applications
    ├── SecurityAssessment-dvwa.html
    ├── SecurityAssessment-juiceshop.html
    ├── testng-results.csv
    ├── zap-dvwa.json
    ├── zap-juiceshop.json
    └── assets/
        ├── security-assessment.css
        └── security-assessment.js
```

The index lists only configured applications whose tests were included in the current TestNG run. The corresponding application pages provide test counts, alert-severity summaries, and expandable details. Opening a generated HTML report does not require a running web server, provided its referenced assets and screenshots remain accessible at their relative locations.

### Configuring Reporting

**Configuration file:** `src/test/java/utilities/reporting/report-targets.properties`

```properties
report.outputDirectory=target/security-reports
report.screenshotDirectory=target/screenshots
report.zapDirectory=target/security-reports
report.assetDirectory=assets
report.indexFile=SecurityAssessment.html
report.assessmentPattern=SecurityAssessment-{id}.html
report.testResultsFile=testng-results.csv

targets=juiceshop,dvwa

target.juiceshop.title=OWASP Juice Shop
target.juiceshop.package=tests.JuiceShop.
target.juiceshop.site=http://juice-shop:3000
target.juiceshop.zapFile=zap-juiceshop.json

target.dvwa.title=DVWA
target.dvwa.package=tests.Dvwa.
target.dvwa.site=http://web-dvwa:80
target.dvwa.zapFile=zap-dvwa.json
```

| Setting | Purpose |
|---|---|
| `report.outputDirectory` | Directory where the assessments, index, and TestNG CSV are written. |
| `report.screenshotDirectory` | Read-only location of screenshots **already saved by `ScreenshotManager`**. This setting does not change screenshot capture or create a second screenshot store. |
| `report.zapDirectory` | Directory from which the assessment reporter reads ZAP JSON reports. |
| `report.assetDirectory` | Directory containing the CSS and JavaScript files relative to the reporting configuration. |
| `report.indexFile` | Filename of the generated assessment index. |
| `report.assessmentPattern` | Per-application filename template; `{id}` is replaced by the configured target ID. |
| `report.testResultsFile` | Filename of the TestNG CSV export. |
| `targets` | Comma-separated IDs of configured applications. |
| `target.<id>.title` | Application display name. |
| `target.<id>.package` | TestNG test class package prefix used to associate tests with the application. |
| `target.<id>.site` | ZAP JSON site name/URL used to filter application findings. |
| `target.<id>.zapFile` | ZAP JSON filename for the application. |

To add another application, add its ID to `targets` and provide the corresponding `target.<id>.*` entries. Its tests and ZAP JSON report must also exist; declaring a target does not automatically create or start a new application test suite.

The reporting configuration loader supports `-Dsecurity.reporting.config=/path/to/config.properties` when the configuration file is elsewhere, and `-Dsecurity.reporting.sourceRoot=...` when discovering the file under a different test-source root. These are **reporter-only** options.

**Path coordination:** `report.zapDirectory` must point to where `ZapManager` actually exports JSON, and `report.screenshotDirectory` must point to where `ScreenshotManager` actually saves PNG files. Changing either reporter property by itself does not reconfigure those producers. The reporter reads the existing artifacts; it does not relocate or re-create them.

### Findings and Evidence

Each assessment shows ZAP findings grouped by their associated application, with a severity summary and interactive search/filter controls. Expanding a finding reveals:

- Risk severity, confidence, and finding name.
- Affected URL, HTTP method, and CWE identifier when present.
- ZAP's description and recommended remediation.
- Up to five individual request observations: URL, parameter/header, matched evidence, attack input, and ZAP's additional context when present.

An alert may not contain a literal response-evidence excerpt. In that case, the report presents available request/context information and a validation note, rather than inventing evidence. Counts represent ZAP alert groups, not the number of independently confirmed vulnerabilities.

### TestNG Results and Screenshot Links

`TestNgResultsReporter` exports suite, test, class, method, status, duration, and error text to the CSV file. `SecurityAssessmentReporter` presents individual tests as expandable entries with their status, duration, error (if any), and screenshots when a matching image is available.

**`ScreenshotManager` remains the sole owner of screenshot capture, storage, and naming.** The assessment only looks up and links existing PNG files; it does not capture screenshots, write a duplicate image directory, or change screenshot behavior. The current reporter determines image associations using test metadata, the screenshot naming conventions, and modification times. It excludes files outside the test's execution time to reduce the risk of linking outdated evidence.

A screenshot may be absent if none was captured during that test, its filename cannot be associated safely, or its modification time is outside the test window. Missing screenshot links do not alter TestNG pass/fail status.

**Interpretation:** A TestNG `PASSED` result, a ZAP detection, and a verified security challenge completion are distinct facts. The dashboard shows test execution and scan observations; it does not independently certify exploitability or challenge completion solely from a passed test or screenshot. In DVWA, some expected security findings are deliberately represented by failing assertions, as explained above.

---

## Automated Screenshots

The framework uses a centralized `ScreenshotManager` utility to capture visual evidence during DVWA and OWASP Juice Shop security tests.

**Source:** `src/test/java/utilities/ScreenshotManager.java`

Screenshots are stored under:

`target/screenshots/`

The screenshot manager dynamically creates a directory for each application and test class. New test classes are supported automatically without manually creating folders or configuring screenshot paths.

### Screenshot Directory Structure

```text
target/screenshots/
├── dvwa/
│   ├── DvwaSqlInjection/
│   │   └── sqlInjection_Low-low-a82f34c5b012.png
│   └── DvwaXSSReflection/
│       ├── XSS_Reflected-low-b91f23a746c0.png
│       └── XSS_Reflected-medium-f834a903b572.png
└── juiceshop/
    └── JuiceShopChallengesTest/
        ├── testSqlInjectionLoginBypass.png
        ├── testDomXssChallenge.png
        └── testScoreBoardChallenge.png
```

*DVWA parameter hashes are illustrative. The class/method names reflect the existing test structure, and only executed tests produce screenshots.*

The security assessment links to files here; it does **not** create an additional screenshot directory.

### DVWA Screenshot Evidence

DVWA tests capture screenshots after executing vulnerability payloads and before reporting security findings through TestNG assertions.

**SQL Injection**

- Captures the actual DVWA webpage after an SQL injection payload is submitted.
- Attempts to capture the complete rendered page, including the injection form, returned database records, and SQL error messages.
- Temporarily adjusts the browser window dimensions to accommodate the webpage's full content.
- Restores the original browser window dimensions after capture.
- Falls back to a normal browser screenshot if full-page capture fails.

**Reflected XSS**

- Detects JavaScript alerts through Selenium WebDriver.
- Records the alert text before dismissing the dialog.
- Captures the DVWA browser page and adds a labeled panel containing the observed alert text.
- Captures the normal browser page when no JavaScript alert executes.

Native JavaScript alerts are not reliably included in Selenium browser screenshots. The labeled XSS evidence panel represents the actual alert text observed by WebDriver, rather than a native browser-dialog screenshot.

### Data-Driven Screenshot Management

DVWA tests use TestNG DataProviders to execute multiple payloads across supported security levels.

To prevent screenshots from overwriting unrelated test results or creating unlimited duplicates, the screenshot manager generates filenames using:

- TestNG test method name
- Security level
- A deterministic hash of the DataProvider parameters

Each unique payload and security-level combination receives its own screenshot.

**Running the same test again with identical parameters overwrites the matching screenshot.**

This provides consistent evidence filenames while preserving separate screenshots for different vulnerability payloads.

Previously generated screenshots using older naming conventions are not automatically deleted.

### OWASP Juice Shop Screenshot Evidence

Juice Shop tests capture screenshots during security challenge execution.

Screenshot coverage includes:

- Browser-based security challenge demonstrations
- SQL injection and XSS challenge results
- Authentication and authorization challenge evidence
- HTTP response previews for API-based challenges
- Completed challenge verification
- Final Score Board progress

Juice Shop screenshot filenames use the TestNG method name (for example, `testScoreBoardChallenge.png`). Capturing the same test method in a later run overwrites its previous PNG. The manager also cleans up legacy timestamped Juice Shop screenshot files from its screenshot directory.

### Juice Shop Score Board

The Score Board challenge test uses TestNG priority `1000` to execute after lower-priority challenge tests in the same execution context.

The final Score Board screenshot provides a visual summary of completed Juice Shop challenges.

### Screenshot Behavior

Screenshots are supported during both headed and headless browser execution.

When SlowMo presentation mode is enabled, supported screenshot operations can pause after capture to keep the relevant result visible.

The screenshot manager is intended to record evidence without changing vulnerability detection logic or TestNG assertions.

**Screenshot output format:** PNG.

No separate text evidence files are generated for DVWA SQL injection or reflected XSS screenshots.

---

## Troubleshooting

### Headless Browser Click Failures

**Symptoms:**

- `ElementClickInterceptedException`
- Cookie-consent buttons blocked by other UI elements
- Tests pass in visible Chrome but fail headless

**Recommended checks:**

1. Verify that `browser.viewport.width=1920`.
2. Verify that `browser.viewport.height=1080`.
3. Confirm that `BrowserFactory.setViewport()` is applied after browser creation.
4. Review application dialogs and notifications that may overlap interactive elements.

Avoid replacing normal Selenium clicks with JavaScript clicks unless there is a specific, verified reason.

### DVWA Security-Level Verification

If DVWA tests fail before payload submission, check `SecurityPage.setSecurityLevel()` and confirm that the DVWA `security` cookie matches the requested LOW, MEDIUM, or HIGH setting. The current implementation verifies the cookie rather than retaining an HTML element across a page reload.

### Expected Security Finding vs. Automation Error

`Visual Verification Confirmed: XSS` followed by a TestNG `AssertionError` is an expected security finding. Errors such as `Node with given id does not belong to the document` indicate a browser synchronization problem instead. SQL error disclosure can be reported as a finding without asserting that an injected query executed successfully.

### Slow Selenium Execution

**Symptoms:**

- UI tests take longer than expected.
- Optional UI elements cause repeated delays.

**Recommended checks:**

1. Set `browser.slowMo.enabled=false`.
2. Set `browser.implicitWaitSeconds=0`.
3. Use explicit waits for elements that genuinely require synchronization.
4. Check whether Chrome startup, Docker operations, or ZAP proxy activity contributes to execution time.

### Configuration File Not Found

If the framework reports that `config.properties` is missing, confirm that the file exists at:

`src/test/resources/config.properties`

Ensure IntelliJ recognizes `src/test/resources` as a test resources directory.

### ZAP Report Not Generated

Verify that:

- The ZAP container started successfully.
- Selenium created the ZAP-proxied browser.
- Test traffic passed through the proxy.
- The suite completed its report-generation lifecycle.
- The `target` directory is writable.

### ZAP Proxy Readiness or Empty Browser Response

If the browser shows `ERR_EMPTY_RESPONSE` or application elements never appear after container startup, ensure the current `ZapManager.initZap()` readiness check is present and succeeding. It must verify `client.core.version()` before Chrome is initialized. A container log line saying that ZAP is listening is not, by itself, sufficient evidence that the proxy API is ready. This readiness check addressed a startup timing issue observed for both DVWA and Juice Shop.

Do not change Selenium selectors or add arbitrary waits to page objects to compensate for an unreachable setup or login page.

### Security Assessment Missing or Incomplete

- Confirm the TestNG service registration contains both reporting listeners and that `report-targets.properties` contains the target's test package prefix.
- Confirm `report.zapDirectory` points to the ZAP JSON export location and the target's `zapFile` value matches the generated filename.
- If the HTML renders without styling or controls, confirm the assets exist under the reporting package and were copied into the report output `assets` folder.
- If findings show no sites, confirm the application generated traffic through ZAP and the scan export contains the configured target URL.
- If screenshots are missing, check the existing `ScreenshotManager` output and confirm the PNG was captured during the same TestNG invocation. Do not create another screenshot folder.

---

## Development Roadmap

- [ ] Automate all feasible OWASP Juice Shop challenges
- [ ] Expand DVWA vulnerability coverage
- [x] Implement centralized browser configuration
- [x] Add configurable headless execution and viewport dimensions
- [x] Add configurable Selenium SlowMo behavior
- [x] Add configurable result/alert presentation duration
- [x] Verify the active DVWA security level using its security cookie
- [x] Add separate DVWA and Juice Shop ZAP HTML and JSON report generation
- [x] Add shared TestNG CSV reporting and per-application HTML assessment dashboards
- [x] Add target-based ZAP finding filtering and expanded request observations
- [x] Add expandable test results with links to existing screenshot evidence
- [x] Separate assessment CSS and JavaScript from Java reporter source
- [x] Add ZAP API readiness verification before initializing proxied browsers
- [ ] Improve reporter integration with ScreenshotManager's storage API to avoid duplicating path/naming assumptions
- [ ] Expand ZAP active scanning and security reporting
- [ ] Add additional API security tests
- [ ] Complete Ollama chatbot integration
- [ ] Develop comprehensive HTML framework documentation
- [ ] Expand automated regression coverage

---

## Security Disclaimer

This project is intended for educational purposes, authorized penetration testing, and security research.

The supported applications are intentionally vulnerable and should only be used in controlled testing environments.

Do not execute security tests against systems without explicit authorization.

---

**OWASP Security Automation Framework — Active Development**
