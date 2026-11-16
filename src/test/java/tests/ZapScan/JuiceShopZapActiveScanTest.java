package tests.ZapScan;

import automation.AppConfig;
import automation.ZapActiveScan;
import automation.ZapManager;
import org.testng.Assert;
import org.testng.annotations.Test;
import tests.JuiceShop.JuiceShopBaseTest;

import java.time.Duration;

/**
 * Standalone Juice Shop assessment that launches a bounded ZAP scan independently of the challenge suite.
 */
public class JuiceShopZapActiveScanTest extends JuiceShopBaseTest {

    /**
     * Selects the Juice Shop active-scan report filename rather than the regular test report.
     *
     * @return the dedicated Juice Shop active-scan HTML report filename
     */
    @Override
    protected String zapReportName() {
        return AppConfig.zapActiveReportName();
    }

    /**
     * Runs the standalone Juice Shop ZAP active scan and verifies completion status.
     *
     * <p>Executes only when the standalone active-scan suite is selected; normal challenge execution does not launch this scan.</p>
     */
    @Test
    public void testJuiceShopActiveScan() {
        ZapManager.getDriver().get(baseUrl);

        boolean completed = ZapActiveScan.run(
                baseUrl,
                Duration.ofMinutes(AppConfig.zapActiveMaxMinutes()),
                AppConfig.zapActiveStatusOutput()
        );

        Assert.assertTrue(completed,
                "ZAP active scan did not complete. Check the active-scan status JSON.");
    }
}
