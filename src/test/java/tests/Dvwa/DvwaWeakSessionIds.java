package tests.Dvwa;

import automation.ZapManager;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import pom.Dvwa.LoginPage;
import pom.Dvwa.SecurityPage;
import pom.Dvwa.WeakSessionIdsPage;
import utilities.ScreenshotManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

/** Examines DVWA's generated lab session IDs for duplication or known predictable sequences. */
public class DvwaWeakSessionIds extends BaseDvwaTest {
    /**
     * Provides all DVWA security levels as independent TestNG invocations.
     * @return the Low, Medium, High, and Impossible modes
     */
    @DataProvider(name = "securityModes")
    public Object[][] securityModes() {
        return Arrays.stream(SecurityPage.SecurityLevel.values())
                .map(level -> new Object[]{level}).toArray(Object[][]::new);
    }

    /**
     * Generates three session tokens and fails if they are duplicated or fit predictable patterns.
     * @param level the DVWA security level being assessed
     */
    @Test(dataProvider = "securityModes")
    public void sessionTokensAreNotPredictable(SecurityPage.SecurityLevel level) {
        WebDriver driver = ZapManager.getDriver();
        driver.get(baseUrl + LoginPage.loginEndpoint);
        new LoginPage(driver).login("admin", "password");
        driver.get(baseUrl + SecurityPage.securityEndpoint);
        new SecurityPage(driver).setSecurityLevel(level);

        WeakSessionIdsPage page = new WeakSessionIdsPage(driver);
        page.open(baseUrl);
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < 3; i++) tokens.add(page.generateToken());
        boolean predictable = hasPredictablePattern(tokens);
        ScreenshotManager.capture(driver);
        Assert.assertFalse(predictable, "WEAK SESSION IDS CONFIRMED at " + level + ": token samples followed a predictable pattern: " + tokens);
    }

    /**
     * Identifies duplicate, sequential, timestamp-based, or small-counter hashed token generation.
     * @param tokens tokens collected from the DVWA lab
     * @return true if a readily predictable sequence is confirmed
     */
    private boolean hasPredictablePattern(List<String> tokens) {
        if (new HashSet<>(tokens).size() != tokens.size()) return true;
        List<Long> numeric = new ArrayList<>();
        for (String token : tokens) {
            try { numeric.add(Long.parseLong(token)); }
            catch (NumberFormatException ignored) { numeric.clear(); break; }
        }
        if (numeric.size() == tokens.size()) {
            boolean sequential = true;
            for (int i = 1; i < numeric.size(); i++) {
                if (numeric.get(i) != numeric.get(i - 1) + 1) sequential = false;
            }
            if (sequential) return true;
            long now = Instant.now().getEpochSecond();
            for (long token : numeric) {
                if (Math.abs(token - now) <= 300) return true;
            }
        }
        for (int counter = 0; counter <= 1024; counter++) {
            String value = Integer.toString(counter);
            if (tokens.contains(digest("MD5", value)) || tokens.contains(digest("SHA-1", value))) return true;
        }
        return false;
    }

    /**
     * Computes a candidate digest of a simple counter to detect predictable session IDs.
     * @param algorithm digest algorithm used by a possible DVWA implementation
     * @param value candidate sequential counter
     * @return lowercase hexadecimal digest
     */
    private String digest(String algorithm, String value) {
        try {
            byte[] bytes = MessageDigest.getInstance(algorithm).digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Digest unavailable: " + algorithm, e);
        }
    }
}
