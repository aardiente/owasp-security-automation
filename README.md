
# OWASP Security Automation Framework

A Java-based security testing framework for automating vulnerability assessments, security challenges, and browser-based testing against intentionally vulnerable web applications.

Built with **Java, Selenium WebDriver, TestNG, Maven, Docker, OWASP ZAP, and Ollama**.

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
- **Separate ZAP Reports:** Generates independent HTML reports for DVWA and Juice Shop.
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
- Docker Desktop with the Docker engine running
- Google Chrome for ZAP-proxied tests
- Browser drivers managed through Selenium Manager or the local environment

Make sure Docker Desktop is running before executing container-dependent tests.

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

---

## Running Tests

Tests can be executed individually through IntelliJ IDEA or using TestNG suite configuration files.

### IntelliJ IDEA

1. Open the project in IntelliJ.
2. Start Docker Desktop.
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

Application execution is separated to avoid mixing DVWA and Juice Shop traffic histories in the same ZAP report.

### Interpreting TestNG Failures

**DVWA security tests deliberately use failing assertions to flag findings.** A triggered JavaScript alert in the Reflected XSS test produces a failure, because the payload executed. SQL injection tests can also report failures for demonstrated query manipulation or database error disclosure, depending on the current assertions.

A Selenium `WebDriverException`, failure to set the DVWA security level, or an element timeout during setup is an **automation issue**, not a confirmed vulnerability. Review the failure message and stack trace before classifying a run.

Juice Shop challenge tests generally use the opposite convention: a completed challenge passes.

DVWA Reflected XSS is exercised at LOW, MEDIUM, and HIGH security levels; SQL injection payloads are loaded from `src/test/java/data/sqli_payloads.xlsx` and run at LOW security. Payload execution does not necessarily guarantee every attack succeeds.

---

## OWASP ZAP Integration

OWASP ZAP is used to intercept and analyze traffic generated during Selenium test execution.

### How It Works

1. The framework starts the ZAP Docker environment.
2. Selenium creates a Chrome browser configured to use the ZAP proxy.
3. Browser traffic passes through ZAP.
4. ZAP records HTTP requests and responses.
5. ZAP performs passive analysis on observed traffic.
6. At suite completion, the framework waits for pending passive scanning to finish.
7. An HTML report is generated.

### Separate Application Reports

| Application | Output File |
|---|---|
| DVWA | `target/zap-dvwa.html` |
| OWASP Juice Shop | `target/zap-juiceshop.html` |

Reports can be opened directly in a web browser.

### Reporting Limitations

The current ZAP integration focuses on browser traffic interception, passive scanning, and HTML report generation.

- Automated active scanning is not currently part of the default reporting workflow.
- Traffic that bypasses the configured ZAP proxy may not appear in reports.
- Findings depend on the requests generated during the test run.
- Successfully completing a Juice Shop challenge does not guarantee that ZAP will report a related vulnerability.
- Reports are generated only when the relevant scanning and reporting lifecycle completes.

Additional scanning and reporting functionality is planned.

---

## Automated Screenshots

The framework includes a reusable `ScreenshotManager` utility.

Screenshots are saved in:

`target/screenshots/`

Each screenshot uses the associated TestNG test method name.

Example:

```text
target/
├── screenshots/
│   ├── testSqlInjectionLoginBypass.png
│   ├── testDomXssChallenge.png
│   └── testScoreBoardChallenge.png
├── zap-dvwa.html
└── zap-juiceshop.html
```

Screenshots with matching names are overwritten during subsequent runs.

### Juice Shop Score Board

The Score Board challenge test is configured with TestNG priority `1000` so that it executes after lower-priority challenge tests in the same execution context.

Its screenshot provides a visual summary of completed Juice Shop challenges.

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

---

## Development Roadmap

- [ ] Automate all feasible OWASP Juice Shop challenges
- [ ] Expand DVWA vulnerability coverage
- [x] Implement centralized browser configuration
- [x] Add configurable headless execution and viewport dimensions
- [x] Add configurable Selenium SlowMo behavior
- [x] Add configurable result/alert presentation duration
- [x] Verify the active DVWA security level using its security cookie
- [x] Add separate DVWA and Juice Shop ZAP report generation
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
