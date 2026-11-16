# OWASP Security Automation Framework

A Java security-testing framework for **OWASP Juice Shop** and **DVWA**, built to automate exercises against intentionally vulnerable lab applications and review the results.

The project uses Selenium WebDriver, TestNG, Maven, Docker/Testcontainers, and OWASP ZAP. Its local Security Assessment SPA displays saved findings, test results, and screenshot evidence. Passive and active scan results are available within each application's dashboard.

## Requirements

- JDK 17 or newer
- Maven (or IntelliJ IDEA with Maven support)
- Docker Engine running and accessible to your user account
- Google Chrome for browser-based tests

## Getting started

1. Clone or download this repository and open the project folder containing `pom.xml`.
2. Import the Maven project in IntelliJ IDEA, or work from a terminal with Maven installed.
3. Start Docker and ensure your user account can access the Docker Engine.
4. Run a TestNG suite from IntelliJ, or use Maven:

   ```bash
   # OWASP Juice Shop tests
   mvn test -Dsurefire.suiteXmlFiles=testng-juiceshop.xml

   # DVWA tests
   mvn test -Dsurefire.suiteXmlFiles=testng-dvwa.xml
   ```

The standalone ZAP active scans have their own TestNG suites and can be run separately from the regular tests.

## Viewing the results

Run `utilities.reporting.SecurityAssessmentSpa` from the IDE, then open `http://127.0.0.1:8765/` in your browser. The dashboard reads previously generated reports; simply opening it does not start a scan.

## Full documentation

See **[README.html](README.html)** for the full user guide, scanner workflows, troubleshooting, and advanced usage.

For a searchable reference generated from the Javadoc and JSDoc in the source files, see **[API documentation](docs/api.html)**.

> **Scope:** This project is intended for isolated training applications and systems you are explicitly authorized to assess.
