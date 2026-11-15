package tests.ZapScan;

import automation.AppConfig;
import automation.ZapActiveScan;
import automation.ZapManager;
import org.testng.Assert;
import org.testng.annotations.Test;
import tests.JuiceShop.JuiceShopBaseTest;

import java.time.Duration;

public class JuiceShopZapActiveScanTest extends JuiceShopBaseTest {

    @Override
    protected String zapReportName() {
        return AppConfig.zapActiveReportName();
    }

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
