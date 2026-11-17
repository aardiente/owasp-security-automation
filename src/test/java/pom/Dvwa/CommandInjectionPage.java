package pom.Dvwa;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

/** Encapsulates form interaction and output inspection for DVWA's command injection lab. */
public class CommandInjectionPage extends BasePage {
    private static final String ENDPOINT = "/vulnerabilities/exec/";
    private final By addressInput = By.name("ip");
    private final By submitButton = By.name("Submit");
    private final By commandOutput = By.cssSelector(".vulnerable_code_area pre");

    /**
     * Creates the command injection page object.
     * @param driver the active DVWA WebDriver
     */
    public CommandInjectionPage(WebDriver driver) { super(driver); }

    /**
     * Opens the command injection page and waits for its form.
     * @param baseUrl the running DVWA application's base URL
     */
    public void open(String baseUrl) {
        driver.get(baseUrl + ENDPOINT);
        wait.until(ExpectedConditions.visibilityOfElementLocated(addressInput));
    }

    /**
     * Submits a loopback ping piped into a harmless marker command to assess injection.
     * @param marker unique marker expected only in executed command output
     */
    public void submitMarkerProbe(String marker) {
        if (!marker.matches("[A-Za-z0-9_]+")) throw new IllegalArgumentException("Marker must be alphanumeric/underscore");
        WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(addressInput));
        input.clear();
        input.sendKeys("127.0.0.1|echo " + marker);
        wait.until(ExpectedConditions.elementToBeClickable(submitButton)).click();
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
    }

    /**
     * Detects the marker as a complete output line, excluding messages that merely echo rejected input.
     * @param marker previously submitted marker
     * @return true when a command output line equals the marker
     */
    public boolean hasExecutedMarker(String marker) {
        for (WebElement pre : driver.findElements(commandOutput)) {
            for (String line : pre.getText().split("\\R")) {
                if (line.trim().equals(marker)) return true;
            }
        }
        return false;
    }
}
