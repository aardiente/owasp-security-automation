package utilities.reporting;

import utilities.EvidenceNames;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Checks that the reporting tools work as expected without launching the browsers, lab applications or scanner.
 */
public final class ReportingSmokeCheck {
    /**
     * Runs the dependency-free regression checks.
     */
    public static void main(String[] args) throws IOException {
        ReportingConfiguration config = ReportingConfiguration.load();
        check(config.targets().size() == 2, "default target count");
        check(config.targets().get(0).evidencePolicy().naming() == EvidenceNames.Mode.METHOD, "method naming");
        check(config.targets().get(1).evidencePolicy().naming() == EvidenceNames.Mode.PARAMETERS, "parameter naming");
        check(config.screenshotDirectory().endsWith(Path.of("target", "screenshots")), "default root");
        check(EvidenceNames.deterministicName(EvidenceNames.Mode.METHOD, "sample", new Object[]{"one"}).equals("sample.png"), "stable method filename");
        check(EvidenceNames.deterministicName(EvidenceNames.Mode.PARAMETERS, "sample", new Object[]{"one"}).equals("sample-one-" + EvidenceNames.parameterHash(new Object[]{"one"}) + ".png"), "stable parameter filename");
        check(EvidenceNames.deterministicName(EvidenceNames.Mode.PARAMETERS, "sample", null).equals("sample.png"), "no parameters");
        check(EvidenceNames.deterministicName(EvidenceNames.Mode.TIMESTAMP, "sample", null) == null, "timestamp fallback");
        check(EvidenceNames.parameterHash(new Object[]{"one"}).length() == 12, "short hash length");
        check(EvidenceNames.sanitize("a/b c").equals("a_b_c"), "sanitization");
        check(ReportCsv.parse("header,other\r\n" + ReportCsv.quote("a,b\"c") + "," + ReportCsv.quote("line\nnext")).equals(List.of(List.of("header", "other"), List.of("a,b\"c", "line\\nnext"))), "CSV round trip");
        check(ReportCsv.parse("\"\"").equals(List.of(List.of(""))), "empty quoted field");
        check(ReportCsv.parse("\"a\nb\",c\n").equals(List.of(List.of("a\nb", "c"))), "embedded newline");
        Path custom = Files.createTempFile("report-config-check", ".properties");
        String old = System.getProperty("security.reporting.config");
        try {
            String defaults = Files.readString(Path.of("src/test/resources/report-targets.properties"));
            Files.writeString(custom, defaults.replace("target.juiceshop.package=tests.JuiceShop.", "target.juiceshop.package=utilities.reporting.").replace("target.juiceshop.screenshotFolder=juiceshop", "target.juiceshop.screenshotFolder=custom-evidence").replace("report.screenshotDirectory=target/screenshots", "report.screenshotDirectory=custom-output"));
            System.setProperty("security.reporting.config", custom.toString());
            ReportingConfiguration external = ReportingConfiguration.load();
            check(external.evidencePolicyFor(ReportingSmokeCheck.class).folder().equals("custom-evidence"), "external target folder");
            check(external.screenshotDirectory().endsWith("custom-output"), "external output root");
            Files.writeString(custom, defaults.replace("targets=juiceshop,dvwa", "targets=juiceshop,juiceshop"));
            expectInvalid();
            Files.writeString(custom, defaults.replace("target.dvwa.screenshotNaming=parameters", "target.dvwa.screenshotNaming=invalid"));
            expectInvalid();
            Files.writeString(custom, defaults.replace("target.dvwa.screenshotFolder=dvwa", "target.dvwa.screenshotFolder=../outside"));
            expectInvalid();
        } finally {
            if (old == null) System.clearProperty("security.reporting.config");
            else System.setProperty("security.reporting.config", old);
            Files.deleteIfExists(custom);
        }
        System.out.println("Reporting smoke checks passed.");
    }

    /**
     * Verifies that loading the current invalid configuration fails.
     */
    private static void expectInvalid() throws IOException {
        try {
            ReportingConfiguration.load();
        } catch (IOException expected) {
            return;
        }
        throw new AssertionError("Expected invalid configuration rejection");
    }

    /**
     * Throws an result check error if a regression check fails.
     */
    private static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
    }
}
