# OWASP Security Automation Framework

A Java-based security testing framework designed to automate vulnerability assessments, security challenges, and regression testing against intentionally vulnerable web applications.

Built using **Java, Selenium WebDriver, TestNG, Maven, Docker/Testcontainers, and OWASP ZAP**, with an interactive Security Assessment dashboard for analyzing security findings, test execution results, and screenshot evidence.

**Project Status:** Active Development

---

## Project Overview

The OWASP Security Automation Framework provides a modular environment for testing web application security vulnerabilities through automated browser interactions, data-driven test cases, and integrated vulnerability scanning.

The framework currently supports two applications:

### OWASP Juice Shop

An intentionally vulnerable web application used to demonstrate modern web application security issues.

Current capabilities include:

- Automated security challenge execution and verification
- Authentication, authorization, injection, and other security tests
- HTTP/API interactions and browser-based testing
- Automated screenshot evidence
- Score Board challenge completion verification
- OWASP ZAP passive and active vulnerability scanning

### DVWA (Damn Vulnerable Web Application)

A PHP/MySQL-based application designed for testing common web application vulnerabilities.

Current capabilities include:

- SQL injection and cross-site scripting (XSS) testing
- Command injection, file inclusion, and file upload assessments
- Page Object Model (POM) architecture for individual vulnerability pages
- Data-driven testing using Excel workbooks
- Tests across supported security levels: Low, Medium, High, and Impossible
- Automated evidence collection and vulnerability detection

**Security test behavior:** Some tests intentionally report a TestNG failure when a vulnerability is successfully demonstrated. An automation exception or application error is not automatically considered a confirmed vulnerability.

---

## Technology Stack

| Technology | Purpose |
|---|---|
| Java 17+ | Primary framework language |
| Selenium WebDriver | Browser automation |
| TestNG | Test execution, assertions, and suite management |
| Maven | Dependency and build management |
| Docker | Isolated application environments |
| Testcontainers | Automated container lifecycle |
| OWASP ZAP | Security proxy, vulnerability detection, and scanning |
| Apache POI | Excel-driven test data |
| HTML, CSS, JavaScript | Security Assessment dashboard |
| Ollama | Experimental local AI integration |

---

## Key Features

**Automated Test Environments**

Docker and Testcontainers manage the application environments required for testing. Tests can initialize the required containers automatically rather than relying on manually configured web servers.

**Page Object Model**

Application-specific interactions are separated from TestNG assertions, making tests easier to maintain and expand.

**Data-Driven Security Testing**

External Excel workbooks provide reusable test inputs. DVWA tests can combine individual payload cases with the application's supported security levels.

**OWASP ZAP Integration**

Selenium traffic can be routed through ZAP for security analysis. Dedicated TestNG suites support active vulnerability scans independently of ordinary test execution.

**Automated Evidence Collection**

Test executions capture screenshot evidence and generate security assessment artifacts for later review.

**Security Assessment SPA**

A locally hosted dashboard provides application-specific views for reviewing ZAP findings, out-of-scope observations, automated test results, and screenshots. Test results can be grouped by test class.

---

## Getting Started

### 1. Prerequisites

Before running the framework, install:

- **Java Development Kit (JDK 17 or newer)**
- **Apache Maven**
- **Docker Engine** or Docker Desktop
- **Google Chrome**
- **IntelliJ IDEA** (recommended)

On Linux, confirm that the user account running IntelliJ or Maven has permission to access Docker.

### 2. Clone the Repository

Clone the project from GitHub:

```bash
git clone <REPOSITORY_URL>
cd owasp-security-automation
```

Replace `<REPOSITORY_URL>` with the repository's GitHub clone URL.

### 3. Import the Project

1. Launch IntelliJ IDEA.
2. Select **Open** and choose the cloned repository.
3. Allow IntelliJ to import the Maven project using `pom.xml`.
4. Wait for dependency resolution and indexing to complete.
5. Verify that the configured project SDK is JDK 17 or newer.

The project uses Maven to manage its Java dependencies.

### 4. Verify Docker

Confirm Docker is running and accessible:

```bash
docker info
```

You can also inspect existing containers:

```bash
docker ps -a
```

The normal TestNG startup process handles the required application and ZAP container lifecycle. You do not need to manually launch DVWA or Juice Shop before running the standard suites.

The first execution may take longer because Docker needs to download images and initialize the application environments.

### 5. Run Your First Security Assessment

All TestNG XML suites are located in the `testng/` directory.

In IntelliJ IDEA:

1. Open the `testng/` directory.
2. Select `testng-juiceshop.xml` or `testng-dvwa.xml`.
3. Right-click the selected XML file.
4. Choose **Run**.

The framework will initialize the required testing environment, execute the selected tests, and generate the available reports and evidence.

---

### Launching the Security Assessment SPA

The framework includes a local single-page application for viewing previously generated security results.

In IntelliJ IDEA, run:

`tests.LaunchSecurityAssessmentSpa`

Located at:

`src/test/java/tests/LaunchSecurityAssessmentSpa.java`

Then open:

**http://127.0.0.1:8765/**

The dashboard provides access to:

| Dashboard view | Purpose |
|---|---|
| Overview | Summary of test executions, severity, and scan status |
| Security Findings | In-scope ZAP vulnerability observations |
| Out of Scope | Excluded findings retained for separate inspection |
| Automated Tests | Test-class cards with individual execution results |
| Evidence Library | Screenshots captured during automated testing |

The SPA is **read-only**. Opening it does not start Docker containers, run TestNG suites, or execute ZAP scans.

Existing reports can be inspected without repeating the security assessment.

**Note:** Maven's `clean` phase removes the `target/` directory, including generated reports and screenshots. Preserve required evidence before cleaning the project.

---

## Project Structure

```text
owasp-security-automation/
├── docs/
│   ├── README.html
│   └── api.html
├── scripts/
│   └── generate_documentation.py
├── src/
│   └── test/
│       ├── java/
│       │   ├── automation/
│       │   ├── pom/
│       │   ├── tests/
│       │   └── utilities/
│       └── resources/
│           └── data/
├── testng/
│   ├── testng-all.xml
│   ├── testng-dvwa.xml
│   ├── testng-juiceshop.xml
│   ├── testng-zap-active.xml
│   └── testng-zap-dvwa-active.xml
├── pom.xml
└── README.md
```

The framework separates browser automation, Page Object Models, test assertions, reusable utilities, and report generation.

Juice Shop challenge coverage is separated into `tests.JuiceShop.api` (resource, state, and commerce tests) and `tests.JuiceShop.ui` (browser-driven and final Score Board tests). Both use the shared `JuiceShopChallengeTestBase`. Keep the full challenge suite sequential: it shares application state and the final Score Board depends on earlier challenge tests. API tests still use the browser for visual HTTP evidence.

All DVWA workbooks are under `src/test/resources/data/`; configuration keys in `config.properties` select their filenames. JVM properties such as `-Ddvwa.sqli.workbook=/path/to/workbook.xlsx` override an individual workbook. The challenge catalog output is configurable with `-Djuiceshop.challenges.output=/path/to/challenges.json`.

See [Refactoring notes](docs/REFACTORING.md) and [Challenge inventory](docs/challenge-inventory.json) for changes and migration details.

---

## Development Roadmap

The project is actively being expanded beyond basic browser automation toward a more comprehensive security assessment and vulnerability validation framework.

### Phase 1 — Framework Foundation

- [x] Java, Selenium, TestNG, and Maven integration
- [x] Automated Docker environments using Testcontainers
- [x] Page Object Model implementation
- [x] Centralized browser configuration
- [x] Headless execution and configurable browser dimensions
- [x] Selenium SlowMo functionality
- [x] Automated screenshot evidence
- [x] Excel-driven test data

### Phase 2 — Security Testing and Coverage

- [x] Initial DVWA SQL injection and XSS tests
- [x] Automated OWASP Juice Shop challenge verification
- [x] DVWA security-level selection and verification
- [x] Additional DVWA vulnerability page objects and tests
- [x] Complete coverage of feasible OWASP Juice Shop challenges
- [x] Expand DVWA testing across all supported vulnerability pages
- [x] Improve security-level-specific test cases and payload coverage
- [ ] Expand API security testing
- [x] Improve automation reliability and regression coverage

### Phase 3 — Security Scanning and Reporting

- [x] Integrate OWASP ZAP with Selenium
- [x] Generate separate DVWA and Juice Shop security reports
- [x] Implement standalone ZAP active scan suites
- [x] Generate TestNG execution summaries and HTML reports
- [x] Integrate screenshot evidence into assessment results
- [x] Develop a local Security Assessment SPA
- [x] Add target-based findings filtering
- [x] Separate in-scope and out-of-scope observations
- [x] Group automated test results into class-based cards
- [x] Improve correlation of individual tests, payloads, and screenshot evidence
- [x] Add run identifiers to associate results from the same assessment
- [x] Improve vulnerability classification and eliminate false positives
- [x] Distinguish confirmed findings, unverified observations, and automation errors

### Phase 4 — Advanced Vulnerability Validation

- [ ] Validate ZAP findings against actual application behavior
- [x] Implement evidence-based finding prioritization
- [x] Improve the handling and documentation of suppressed findings
- [x] Improve report accuracy and reduce unnecessary findings

### Phase 5 — Framework Maturity

- [x] Expand automated regression coverage
- [x] Improve development-environment startup performance
- [x] Improve automated verification of the reporting pipeline
- [x] Continue improving framework documentation

Roadmap items represent development goals, not guarantees of future functionality.

---

## Documentation

The GitHub README is intended to provide an overview of the framework, first-time startup instructions, and its development roadmap.

For comprehensive technical information, refer to the additional documentation:

**[Complete HTML User Guide](docs/README.html)**

Installation details, scanner workflows, configuration, architecture, troubleshooting, and application usage.

**[Source API Reference](docs/api.html)**

Searchable documentation generated from the project's Java Javadoc and JavaScript JSDoc comments.

The HTML documentation can be regenerated from the project root:

```bash
python3 scripts/generate_documentation.py
```

The documentation generator should not overwrite the manually maintained GitHub README.

---

## Responsible Use

This framework is intended for educational purposes, authorized security testing, and research within controlled environments.

OWASP Juice Shop and DVWA are intentionally vulnerable applications. Security testing should be performed only against systems where explicit authorization has been granted.

---

**OWASP Security Automation Framework — Active Development**
