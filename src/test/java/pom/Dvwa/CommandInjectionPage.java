package pom.Dvwa;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * Represents DVWA's command injection practice page. Provides browser controls and results used by the related security test.
 */
public class CommandInjectionPage extends BasePage {
    private static final String ENDPOINT = "/vulnerabilities/exec/";
    private final By addressInput = By.name("ip");
    private final By submitButton = By.name("Submit");
    private final By commandOutput = By.cssSelector(".vulnerable_code_area pre");

    /**
     * Sets up the DVWA command injection page with the provided browser session.
     *
     * @param driver active DVWA browser
     */
    public CommandInjectionPage(WebDriver driver) { super(driver); }

    /**
     * Opens the command-injection exercise and checks that its input is available.
     *
     * @param baseUrl DVWA application's base URL
     */
    public void open(String baseUrl) {
        driver.get(baseUrl + ENDPOINT);
        wait.until(ExpectedConditions.visibilityOfElementLocated(addressInput));
    }

    /**
     * Submits a harmless marker probe appropriate to the selected lab level. Low accepts a statement separator; Medium and higher receive the pipe-based probe. A returned marker is the only accepted evidence of additional command execution.
     *
     * @param marker marker consisting exclusively of letters, digits, or underscores
     * @param level DVWA mode currently selected for this test
     */
    public void submitMarkerProbe(String marker, SecurityPage.SecurityLevel level) {
        if (marker == null || !marker.matches("[A-Za-z0-9_]+"))
            throw new IllegalArgumentException("Marker must contain letters, digits, or underscores only");
        if (level == null) throw new IllegalArgumentException("Security level cannot be null");

        String inputValue = switch (level) {
            case LOW -> "127.0.0.1; echo " + marker;
            case MEDIUM, HIGH, IMPOSSIBLE -> "127.0.0.1|echo " + marker;
        };
        WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(addressInput));
        input.clear();
        input.sendKeys(inputValue);
        wait.until(ExpectedConditions.elementToBeClickable(submitButton)).click();
        waitForNewPage(input, addressInput);
    }

    /**
     * Checks for a marker emitted as a complete output line by the server. This does not confuse merely echoing the submitted input with execution.
     *
     * @param marker the previously submitted output marker
     * @return true if a command-result line matches the marker exactly
     */
    public boolean hasExecutedMarker(String marker) {
        for (WebElement block : driver.findElements(commandOutput)) {
            for (String line : block.getText().split("\\R")) {
                if (marker.equals(line.trim())) return true;
            }
        }
        return false;
    }
}
