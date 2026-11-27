package tests.JuiceShop.api;

import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;
import tests.JuiceShop.JuiceShopChallengeTestBase;

/**
 * Contains API-based Juice Shop tests related to application resources and information protection. Checks the challenge results recorded by the lab.
 */
public class JuiceShopResourceApiTest extends JuiceShopChallengeTestBase {

    /**
     * Checks access restrictions on confidential documents in the Juice Shop lab. Uses the shared API test tools to verify and report the recorded outcome.
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
     * Checks protection of historical sales data in the Juice Shop lab. Uses the shared API test tools to verify and report the recorded outcome.
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
     * Checks protection of application signature files in the Juice Shop lab. Uses the shared API test tools to verify and report the recorded outcome.
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
     * Checks maintenance of redirect policies in the Juice Shop lab. Uses the shared API test tools to verify and report the recorded outcome.
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
     * Checks protection of account email information in the Juice Shop lab. Uses the shared API test tools to verify and report the recorded outcome.
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
     * Checks the application's nested Easter Egg completion state in the Juice Shop lab. Uses the shared API test tools to verify and report the recorded outcome.
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
     * Checks protection of design documents in the Juice Shop lab. Uses the shared API test tools to verify and report the recorded outcome.
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
     * Checks premium-content access restrictions in the Juice Shop lab. Uses the shared API test tools to verify and report the recorded outcome.
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
     * Checks the application's Imaginary challenge completion state in the Juice Shop lab. Uses the shared API test tools to verify and report the recorded outcome.
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
     * Checks redirect policy enforcement in the Juice Shop lab. Uses the shared API test tools to verify and report the recorded outcome.
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
     * Checks recognition of legacy dependency-name risks in the Juice Shop lab. Uses the shared API test tools to verify and report the recorded outcome.
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
     * Checks recognition of dashboard dependency-name risks in the Juice Shop lab. Uses the shared API test tools to verify and report the recorded outcome.
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
     * Checks the application's embedded-information challenge in the Juice Shop lab. Uses the shared API test tools to verify and report the recorded outcome.
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
     * Checks recognition of software supply-chain risks in the Juice Shop lab. Uses the shared API test tools to verify and report the recorded outcome.
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
     * Checks protection of unreleased product information in the Juice Shop lab. Uses the shared API test tools to verify and report the recorded outcome.
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
     * Checks protection of database details in the Juice Shop lab. Uses the shared API test tools to verify and report the recorded outcome.
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
     * Checks protection of account credential information in the Juice Shop lab. Uses the shared API test tools to verify and report the recorded outcome.
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
     * Checks protection of order data in the Juice Shop lab. Uses the shared API test tools to verify and report the recorded outcome.
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
     * Checks protection of API credential information in the Juice Shop lab. Uses the shared API test tools to verify and report the recorded outcome.
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
     * Checks recognition of infrastructure security risks in the Juice Shop lab. Uses the shared API test tools to verify and report the recorded outcome.
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
     * Checks recognition of published security advisory information in the Juice Shop lab. Uses the shared API test tools to verify and report the recorded outcome.
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
     * Checks protection of application credential information in the Juice Shop lab. Uses the shared API test tools to verify and report the recorded outcome.
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
