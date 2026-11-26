# Framework refactor — challenge and infrastructure separation

## Scope and compatibility

This second pass builds on the previous reporting refactor (see [reporting refactor notes](REFACTORING_REPORTING.md)). It reorganizes the existing isolated-lab test code, not the application under test. The original challenge operations and assertions are retained, with changes limited to evidence helpers, domain routing, configuration and setup. There are no new challenge implementations. The project remains Java 17/TestNG/Selenium/Testcontainers/ZAP; the POM dependencies remain unchanged.

## Test architecture

| Class | Purpose | Methods |
|---|---|---:|
| `tests.JuiceShop.api.JuiceShopResourceApiTest` | HTTP-driven resource and information challenges | 22 |
| `tests.JuiceShop.api.JuiceShopStateApiTest` | HTTP-driven state, feedback and upload challenges | 16 |
| `tests.JuiceShop.api.JuiceShopCommerceApiTest` | HTTP-driven basket and commerce challenges | 7 |
| `tests.JuiceShop.ui.JuiceShopUiChallengeTest` | Browser-driven and mixed-channel challenges | 28 |
| `tests.JuiceShop.ui.JuiceShopScoreBoardTest` | Final Mass Dispel and Score Board checks | 2 |

The previous 94 declared challenge-class methods included 19 non-challenge API/catalog, access-control or retrieval checks. These 19 have been removed; the 75 retained methods include 74 enabled methods and the one originally disabled method. The two initial UI checks use their existing direct success assertions rather than explicit scoreboard assertions. All other retained methods have an explicit challenge check. This is retained test-method coverage, not a measured count of unique solved challenges.

The auxiliary `JuiceShopZapAlertsTest` (11 methods) and the unscheduled `ChatbotSecurityTest` (2 methods) are removed from the source tree because they do not explicitly verify completion of a challenge. Existing independent active-scan suites and all six DVWA vulnerability tests are retained: DVWA has no Juice Shop challenge catalog. Supporting chatbot and Ollama components remain available; only their non-challenge test class was removed.

`testng/testng-juiceshop.xml` still runs a single sequential TestNG test context. Original priorities and disabled settings are unchanged. Regular tests are assigned to the `juice-challenge` group. Mass Dispel waits on that group and the final Score Board depends on Mass Dispel, both with `alwaysRun = true`. The `preserve-order="false"` setting allows TestNG to consider priorities across the new classes. Do not run the final class alone without its dependency group. Do not enable parallel execution: the application state, suite lifecycle and ZAP context remain sequential by design. Runtime scheduling must be confirmed in a real TestNG run.

API classes are separate from UI classes by their action mechanism; screenshots are still rendered in the browser when required. Mixed tests remain in the UI class. The API test suite is not browser-free.

The machine-readable [challenge inventory](challenge-inventory.json) records all retained and removed methods, their classes, priorities and verification types. It also records hashes for regression comparison.

## Reusable components

- `tests.framework.LabLifecycle<T>` composes application start/setup and ZAP start/export/stop, handles partial startup cleanup, and shares the common teardown policy. `JuiceShopBaseTest` and `BaseDvwaTest` retain their application-specific configuration and existing suite hooks.
- `tests.framework.ProxiedBrowserTest` shares method-level browser setup and teardown. Juice Shop retains its explicit-wait policy; DVWA retains its existing browser settings.
- `JuiceShopChallengeTestBase` initializes one API client and one action facade per invocation and centralizes completion checks and response screenshots.
- `utilities.JuiceShop.JuiceShopChallengeActions` composes `AccountActions`, `BasketActions`, `FeedbackActions`, `ResourceActions`, `CatalogActions`, `UploadActions` and `BrowserActions`. These share `ActionContext`; existing operation method bodies are relocated without rewriting. Shared challenge polling remains in the facade. API client request metadata is centralized without changing request defaults.
- `BaseDvwaTest.prepareSecurityLevel` shares login and security-mode selection across existing DVWA tests and the standalone DVWA scan. `DvwaCases` shares workbook loading, cell conversion, security-level parsing and case expansion while maintaining case-first vs level-first ordering as applicable.
- `pom.common.BrowserPage` shares Selenium driver and explicit wait context. The separate site BasePage classes remain, with target-specific wait defaults and application-specific locators. No new JavaScript executor calls were added.
- `ScreenshotManager` shares browser evidence persistence and metadata finalization. Existing capture types and screenshot policies remain. ZAP JSON output now uses the same configured report path resolver as its consumers.
- The source documentation generator uses JDK-attached comments directly and fails on Java syntax diagnostics. Previously missing method comments are supplied; both HTML documentation files were regenerated.

## Configuration and migration

All new keys are in `src/test/resources/config.properties`, with original defaults. JVM `-D` properties override settings. The configured Juice Shop Docker image, port, page wait and HTTP timeouts preserve the existing defaults. DVWA credentials are defaults for the intentionally vulnerable local lab only. Custom target image/port or network-alias changes may also require updating the corresponding target scope in `report-targets.properties`.

- The seven workbook files moved from `src/test/java/data/` to `src/test/resources/data/` so Maven/IDE test resource loading works normally. Their bytes are unchanged. `test.data.directory` defaults to the classpath `data` folder. `dvwa.sqli.workbook`, `dvwa.fileUpload.workbook`, `dvwa.storedXss.workbook` and `dvwa.reflectedXss.workbook` specify workbook names; JVM overrides accept a full file path. Mark `src/test/resources` as Test Resources in IDEs if necessary.
- `juiceshop.challenges.output` defaults to `JuiceShopChallengesTest-challenges.json`, maintaining the prior output name independently of class renames. Set a different output path via the property if desired.
- `zap.htmlReportDirectory` defaults to `target`. JSON output follows `report.zapDirectory` in reporting configuration. Existing target package prefixes still match the new subpackages.
- Test names are retained, but Java class names and evidence folders change for Juice Shop. Run the updated suite to generate fresh CSV and screenshot evidence. Old evidence is not migrated automatically. The old monolithic class and its action facade's flat API are intentionally removed; update any external run configurations or callers to the new class and domain names.
- The standard suite XML filenames remain unchanged. The separate ZAP suite XMLs remain available.

## Validation

Completed without Maven or external dependencies:

1. Java 17 syntax parsing for all Java source declarations using the existing JDK parser; documentation generator succeeded for 553 Java declarations and 35 JavaScript functions.
2. Source-level comparison against the previous archive: 75 retained challenge method bodies match after normalizing only the intended action-domain routing, evidence helper substitution, evidence-only variable removal, and configured output-path change. All 36 original action method bodies match exactly. All eight DVWA/standalone scan method bodies match apart from the shared setup substitution.
3. All seven Excel workbooks match the original bytes. All original priorities and the originally disabled challenge are retained.
4. `python scripts/check_structure.py` checks the manifest, body hashes, source comments, workbook hashes, TestNG XML class/suite references and final dependency declarations.
5. `python scripts/check_reporting.py` compiles the dependency-free reporting helpers with `javac --release 17` and runs the existing smoke checks.
6. The generated HTML user guide and API reference were regenerated from source comments.

No full Maven compilation or runtime test execution was performed. Maven and the full Selenium/TestNG/ZAP dependency set are unavailable in the execution environment. Java syntax checking is not equivalent to dependency-resolved compilation. Docker, browser behavior, API behavior, TestNG scheduling across classes, screenshot rendering, scan results, and actual challenge completion are not verified. Run the full suite in the existing development environment before treating this as a replacement for the original working build.

Recommended local verification from the repository root (assuming the project's existing Maven/Surefire configuration supports suite XML selection):

```sh
python scripts/check_structure.py
python scripts/check_reporting.py
python scripts/generate_documentation.py
mvn test-compile
mvn test -Dsurefire.suiteXmlFiles=testng/testng-juiceshop.xml
mvn test -Dsurefire.suiteXmlFiles=testng/testng-dvwa.xml
```

The structural manifest is intentionally strict: when deliberately changing test bodies or workbooks in future, review and update the corresponding hashes instead of bypassing the checks.

## Remaining technical debt

This is a broad behavior-preserving refactor, not a claim that every possible abstraction has been extracted. The static HTML and SPA renderers still have separate rendering logic, and specialized page objects, test inputs, application routes and challenge assertions remain application-specific. Further restructuring should be driven by dependency-resolved compilation and runtime regression tests, rather than changing proven selectors or challenge behavior based only on source inspection. The shared sequential state and API evidence browser remain deliberate compatibility trade-offs. Application-specific challenge names and selectors are not moved into arbitrary configuration files because they describe test contracts, not environment settings.

The new archive includes the complete source, resources, suites, generator and regenerated documentation. Git metadata, IDE settings, build outputs, logs and previously generated evidence are excluded. Neither input archive has been modified.
