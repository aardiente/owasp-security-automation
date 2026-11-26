package tests.JuiceShop.api;

import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;
import tests.JuiceShop.JuiceShopChallengeTestBase;

/** HTTP-driven challenge checks with shared response evidence. */
public class JuiceShopResourceApiTest extends JuiceShopChallengeTestBase {

    /**
     * Exercises the Juice Shop Confidential Document challenge in the isolated lab and records the test outcome.
     *
     * <p>Requests {@code /ftp/acquisitions.md} through the API client.</p>
     * <p>Checks the Score Board challenge state for {@code Confidential Document}.</p>
     * <p>Captures a rendered HTTP response as screenshot evidence.</p>
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 3, groups = "juice-challenge")
    public void testConfidentialDocumentChallenge() throws Exception {
        var response = api.get("/ftp/acquisitions.md");
        captureResponseEvidence(response);

        Assert.assertEquals(response.statusCode(), 200, "Confidential document was not accessible.");
        Assert.assertFalse(response.body().isBlank(), "Confidential document was empty.");
        assertChallengeSolved("Confidential Document");
    }

    /**
     * Exercises the Juice Shop Forgotten Sales Backup challenge in the isolated lab and records the test outcome.
     *
     * <p>Requests {@code /ftp/coupons_2013.md.bak%2500.pdf} through the API client.</p>
     * <p>Checks the Score Board challenge state for {@code Forgotten Sales Backup}.</p>
     * <p>Captures a rendered HTTP response as screenshot evidence.</p>
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 20, groups = "juice-challenge")
    public void testForgottenSalesBackupChallenge() throws Exception {
        var response = api.get("/ftp/coupons_2013.md.bak%2500.pdf");
        Assert.assertEquals(response.statusCode(), 200, "Sales backup was not accessible.");

        captureResponseEvidence(response);
        assertChallengeSolved("Forgotten Sales Backup");
    }

    /**
     * Exercises the Juice Shop Misplaced Signature File challenge in the isolated lab and records the test outcome.
     *
     * <p>Requests {@code /ftp/suspicious_errors.yml%2500.md} through the API client.</p>
     * <p>Checks the Score Board challenge state for {@code Misplaced Signature File}.</p>
     * <p>Captures a rendered HTTP response as screenshot evidence.</p>
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 21, groups = "juice-challenge")
    public void testMisplacedSignatureFileChallenge() throws Exception {
        var response = api.get("/ftp/suspicious_errors.yml%2500.md");
        Assert.assertEquals(response.statusCode(), 200, "SIEM signature file was not accessible.");

        captureResponseEvidence(response);
        assertChallengeSolved("Misplaced Signature File");
    }

    /**
     * Exercises the Juice Shop Outdated Allowlist challenge in the isolated lab and records the test outcome.
     *
     * <p>Requests {@code /redirect?to=} through the API client.</p>
     * <p>Checks the Score Board challenge state for {@code Outdated Allowlist}.</p>
     * <p>Captures a rendered HTTP response as screenshot evidence.</p>
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 22, groups = "juice-challenge")
    public void testOutdatedAllowlistChallenge() throws Exception {
        String destination = "https://blockchain.info/address/1AbKfgvw9psQ41NbLi8kufDQTezwG8DRZm";
        var response = api.get("/redirect?to=" + destination);
        String location = response.headers().firstValue("location").orElse("");
        Assert.assertEquals(response.statusCode(), 302, "Expected a redirect response.");
        Assert.assertEquals(location, destination, "Unexpected redirect destination.");

        captureResponseEvidence(response);
        assertChallengeSolved("Outdated Allowlist");
    }

    /**
     * Exercises the Juice Shop Email Leak challenge in the isolated lab and records the test outcome.
     *
     * <p>Requests {@code /rest/user/whoami?callback=leak} through the API client.</p>
     * <p>Checks the Score Board challenge state for {@code Email Leak}.</p>
     * <p>Captures a rendered HTTP response as screenshot evidence.</p>
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 36, groups = "juice-challenge")
    public void testEmailLeakChallenge() throws Exception {
        var response = api.get("/rest/user/whoami?callback=leak");
        captureResponseEvidence(response);
        assertChallengeSolved("Email Leak");
    }

    /**
     * Exercises the Juice Shop Nested Easter Egg challenge in the isolated lab and records the test outcome.
     *
     * <p>Calls {@code retrieveNestedEasterEgg} through the shared challenge-action helper.</p>
     * <p>Checks the Score Board challenge state for {@code Nested Easter Egg}.</p>
     * <p>Captures a rendered HTTP response as screenshot evidence.</p>
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 39, groups = "juice-challenge")
    public void testNestedEasterEggChallenge() throws Exception {
        var response = actions.resources().retrieveNestedEasterEgg();
        captureResponseEvidence(response);
        Assert.assertEquals(response.statusCode(), 200);
        assertChallengeSolved("Nested Easter Egg");
    }

    /**
     * Exercises the Juice Shop Retrieve Blueprint challenge in the isolated lab and records the test outcome.
     *
     * <p>Calls {@code retrieveBlueprint} through the shared challenge-action helper.</p>
     * <p>Checks the Score Board challenge state for {@code Retrieve Blueprint}.</p>
     * <p>Captures a rendered HTTP response as screenshot evidence.</p>
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 40, groups = "juice-challenge")
    public void testRetrieveBlueprintChallenge() throws Exception {
        var response = actions.resources().retrieveBlueprint();
        captureResponseEvidence(response);
        Assert.assertEquals(response.statusCode(), 200);
        assertChallengeSolved("Retrieve Blueprint");
    }

    /**
     * Exercises the Juice Shop Premium Paywall challenge in the isolated lab and records the test outcome.
     *
     * <p>Calls {@code openPremiumPage} through the shared challenge-action helper.</p>
     * <p>Checks the Score Board challenge state for {@code Premium Paywall}.</p>
     * <p>Captures a rendered HTTP response as screenshot evidence.</p>
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 42, groups = "juice-challenge")
    public void testPremiumPaywallChallenge() throws Exception {
        var response = actions.resources().openPremiumPage();
        captureResponseEvidence(response);
        Assert.assertEquals(response.statusCode(), 200);
        assertChallengeSolved("Premium Paywall");
    }

    /**
     * Exercises the Juice Shop Imaginary Challenge challenge in the isolated lab and records the test outcome.
     *
     * <p>Calls {@code applyImaginaryContinueCode} through the shared challenge-action helper.</p>
     * <p>Checks the Score Board challenge state for {@code Imaginary Challenge}.</p>
     * <p>Captures a rendered HTTP response as screenshot evidence.</p>
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 43, groups = "juice-challenge")
    public void testImaginaryChallenge() throws Exception {
        var response = actions.resources().applyImaginaryContinueCode();
        captureResponseEvidence(response);
        assertChallengeSolved("Imaginary Challenge");
    }

    /**
     * Exercises the Juice Shop Allowlist Bypass challenge in the isolated lab and records the test outcome.
     *
     * <p>Calls {@code bypassRedirectAllowlist} through the shared challenge-action helper.</p>
     * <p>Checks the Score Board challenge state for {@code Allowlist Bypass}.</p>
     * <p>Captures a rendered HTTP response as screenshot evidence.</p>
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 44, groups = "juice-challenge")
    public void testAllowlistBypassChallenge() throws Exception {
        var response = actions.resources().bypassRedirectAllowlist();
        captureResponseEvidence(response);
        Assert.assertEquals(response.statusCode(), 302);
        assertChallengeSolved("Allowlist Bypass");
    }

    /**
     * Exercises the Juice Shop Legacy Typosquatting challenge in the isolated lab and records the test outcome.
     *
     * <p>Calls {@code submitFeedback} through the shared challenge-action helper.</p>
     * <p>Checks the Score Board challenge state for {@code Legacy Typosquatting}.</p>
     * <p>Captures a rendered HTTP response as screenshot evidence.</p>
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 45, groups = "juice-challenge")
    public void testLegacyTyposquattingChallenge() throws Exception {
        var response = actions.feedback().submitFeedback("Suspect dependency: epilogue-js");
        captureResponseEvidence(response);
        assertChallengeSolved("Legacy Typosquatting");
    }

    /**
     * Exercises the Juice Shop Frontend Typosquatting challenge in the isolated lab and records the test outcome.
     *
     * <p>Calls {@code submitFeedback} through the shared challenge-action helper.</p>
     * <p>Checks the Score Board challenge state for {@code Frontend Typosquatting}.</p>
     * <p>Captures a rendered HTTP response as screenshot evidence.</p>
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 46, groups = "juice-challenge")
    public void testFrontendTyposquattingChallenge() throws Exception {
        var response = actions.feedback().submitFeedback("Suspect frontend module: ngy-cookie");
        captureResponseEvidence(response);
        assertChallengeSolved("Frontend Typosquatting");
    }

    /**
     * Exercises the Juice Shop Steganography challenge in the isolated lab and records the test outcome.
     *
     * <p>Calls {@code submitFeedback} through the shared challenge-action helper.</p>
     * <p>Checks the Score Board challenge state for {@code Steganography}.</p>
     * <p>Captures a rendered HTTP response as screenshot evidence.</p>
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 47, groups = "juice-challenge")
    public void testSteganographyChallenge() throws Exception {
        var response = actions.feedback().submitFeedback("The hidden character is Pickle Rick");
        captureResponseEvidence(response);
        assertChallengeSolved("Steganography");
    }

    /**
     * Exercises the Juice Shop Supply Chain Attack challenge in the isolated lab and records the test outcome.
     *
     * <p>Calls {@code submitFeedback} through the shared challenge-action helper.</p>
     * <p>Checks the Score Board challenge state for {@code Supply Chain Attack}.</p>
     * <p>Captures a rendered HTTP response as screenshot evidence.</p>
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 48, groups = "juice-challenge")
    public void testSupplyChainAttackChallenge() throws Exception {
        var response = actions.feedback().submitFeedback("https://github.com/eslint/eslint-scope/issues/39");
        captureResponseEvidence(response);
        assertChallengeSolved("Supply Chain Attack");
    }

    /**
     * Exercises the Juice Shop Leaked Unsafe Product challenge in the isolated lab and records the test outcome.
     *
     * <p>Calls {@code submitFeedback} through the shared challenge-action helper.</p>
     * <p>Checks the Score Board challenge state for {@code Leaked Unsafe Product}.</p>
     * <p>Captures a rendered HTTP response as screenshot evidence.</p>
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 49, groups = "juice-challenge")
    public void testLeakedUnsafeProductChallenge() throws Exception {
        var response = actions.feedback().submitFeedback("Rippertuer Special Juice contains unsafe ingredients: Eurogium Edule and Hueteroneel");
        captureResponseEvidence(response);
        assertChallengeSolved("Leaked Unsafe Product");
    }

    /**
     * Exercises the Juice Shop Database Schema challenge in the isolated lab and records the test outcome.
     *
     * <p>Calls {@code retrieveDatabaseSchema}, {@code waitForChallenge} through the shared challenge-action helper.</p>
     * <p>Checks the Score Board challenge state for {@code Database Schema}.</p>
     * <p>Captures a rendered HTTP response as screenshot evidence.</p>
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 52, groups = "juice-challenge")
    public void testDatabaseSchemaChallenge() throws Exception {
        var response = actions.resources().retrieveDatabaseSchema();

        Assert.assertEquals(response.statusCode(), 200, "Database schema request failed.");
        Assert.assertTrue(response.body().contains("CREATE TABLE"),
                "Database schema was not returned.");

        captureResponseEvidence(response);
        actions.waitForChallenge("Database Schema");
        assertChallengeSolved("Database Schema");
    }

    /**
     * Exercises the Juice Shop User Credentials challenge in the isolated lab and records the test outcome.
     *
     * <p>Calls {@code retrieveUserCredentials} through the shared challenge-action helper.</p>
     * <p>Checks the Score Board challenge state for {@code User Credentials}.</p>
     * <p>Captures a rendered HTTP response as screenshot evidence.</p>
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 53, groups = "juice-challenge")
    public void testUserCredentialsChallenge() throws Exception {
        var response = actions.resources().retrieveUserCredentials();
        captureResponseEvidence(response);
        Assert.assertEquals(response.statusCode(), 200);
        assertChallengeSolved("User Credentials");
    }

    /**
     * Exercises the Juice Shop NoSQL Exfiltration challenge in the isolated lab and records the test outcome.
     *
     * <p>Calls {@code exfiltrateOrdersViaNoSql} through the shared challenge-action helper.</p>
     * <p>Checks the Score Board challenge state for {@code NoSQL Exfiltration}.</p>
     * <p>Captures a rendered HTTP response as screenshot evidence.</p>
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 54, groups = "juice-challenge", enabled = false)
    public void testNoSqlExfiltrationChallenge() throws Exception {
        var response = actions.resources().exfiltrateOrdersViaNoSql();
        captureResponseEvidence(response);
        if (!api.isChallengeSolved("NoSQL Exfiltration"))
            throw new SkipException("NoSQL Exfiltration is disabled in the Juice Shop Docker environment.");
        assertChallengeSolved("NoSQL Exfiltration");
    }

    /**
     * Exercises the Juice Shop Leaked API Key challenge in the isolated lab and records the test outcome.
     *
     * <p>Calls {@code submitFeedback}, {@code waitForChallenge} through the shared challenge-action helper.</p>
     * <p>Checks the Score Board challenge state for {@code Leaked API Key}.</p>
     * <p>Captures a rendered HTTP response as screenshot evidence.</p>
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 66, groups = "juice-challenge")
    public void testLeakedApiKeyChallenge() throws Exception {
        var response = actions.feedback().submitFeedback(
                "A leaked API key was identified: 6PPi37DBxP4lDwlriuaxP15HaDJpsUXY5TspVmie"
        );

        captureResponseEvidence(response);
        actions.waitForChallenge("Leaked API Key");
        assertChallengeSolved("Leaked API Key");
    }

    /**
     * Exercises the Juice Shop Vulnerable Infrastructure challenge in the isolated lab and records the test outcome.
     *
     * <p>Calls {@code submitFeedback}, {@code waitForChallenge} through the shared challenge-action helper.</p>
     * <p>Checks the Score Board challenge state for {@code Vulnerable Infrastructure}.</p>
     * <p>Captures a rendered HTTP response as screenshot evidence.</p>
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 67, groups = "juice-challenge")
    public void testVulnerableInfrastructureChallenge() throws Exception {
        var response = actions.feedback().submitFeedback(
                "Security finding: The infrastructure references the vulnerable Docker image mongo:4.4.29."
        );

        captureResponseEvidence(response);
        actions.waitForChallenge("Vulnerable Infrastructure");
        assertChallengeSolved("Vulnerable Infrastructure");
    }

    /**
     * Exercises the Juice Shop Security Advisory challenge in the isolated lab and records the test outcome.
     *
     * <p>Calls {@code submitFeedback}, {@code waitForChallenge} through the shared challenge-action helper.</p>
     * <p>Checks the Score Board challenge state for {@code Security Advisory}.</p>
     * <p>Captures a rendered HTTP response as screenshot evidence.</p>
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 87, groups = "juice-challenge")
    public void testSecurityAdvisoryChallenge() throws Exception {
        var response = actions.feedback().submitFeedback("Security advisory checksum: " +
                "7e7ce7c65db3bf0625fcea4573d25cff41f2f7e3474f2c74334b14fc65bb4fd26af802ad17a3a03bf0eee6827a00fb8f7905f338c31b5e6ea9cb31620242e843");
        captureResponseEvidence(response);
        actions.waitForChallenge("Security Advisory");
        assertChallengeSolved("Security Advisory");
    }

    /**
     * Exercises the Juice Shop Exposed Credentials challenge in the isolated lab and records the test outcome.
     *
     * <p>Requests {@code /rest/user/whoami} through the API client.</p>
     * <p>Checks the Score Board challenge state for {@code Exposed Credentials}.</p>
     * <p>Captures a rendered HTTP response as screenshot evidence.</p>
     *
     * @throws Exception if a browser, HTTP, container, or scanner operation fails
     */
    @Test(priority = 92, groups = "juice-challenge")
    public void testExposedCredentialsChallenge() throws Exception {
        var session = api.login("testing@juice-sh.op", "IamUsedForTesting");
        var response = api.get("/rest/user/whoami", session.token());

        captureResponseEvidence(response);
        Assert.assertEquals(response.statusCode(), 200, "Seeded testing account authentication failed.");
        assertChallengeSolved("Exposed Credentials");
    }
}
