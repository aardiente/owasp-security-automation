
package pom.Dvwa;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;
import java.util.Locale;

/**
 * DVWA page object exposing Selenium interactions for the sql injection module.
 */
public class SqlInjectionPage extends BasePage {

    public static final String endpoint = "/vulnerabilities/sqli/";

    private final By idInputField = By.name("id");
    private final By submitButton = By.name("Submit");
    private final By resultRecords = By.cssSelector(".vulnerable_code_area pre");

    /**
     * Creates the sql injection page component and initializes the dependencies required by its methods.
     *
     * <p>Creates a SQL injection page instance and initializes its dependencies.</p>
     *
     * @param driver the Selenium WebDriver controlling the application browser
     */
    public SqlInjectionPage(WebDriver driver) {
        super(driver);
    }

    /**
     * Submits a SQL injection test input through the DVWA SQL injection form.
     *
     * @param payload the test input submitted to the intentionally vulnerable lab application
     */
    public void injectPayload(String payload) {
        WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(idInputField));
        input.clear();
        input.sendKeys(payload);

        wait.until(ExpectedConditions.elementToBeClickable(submitButton)).click();
        wait.until(ExpectedConditions.urlContains("id="));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
    }

    /**
     * Counts the SQL injection result records visible on the page.
     *
     * @return the number of result records shown after an SQL injection probe
     */
    public int getResultCount() {
        List<WebElement> results = driver.findElements(resultRecords);
        int count = 0;

        for (WebElement result : results) {
            String text = result.getText();

            if (text.contains("First name:") && text.contains("Surname:")) {
                count++;
            }
        }

        return count;
    }

    /**
     * Checks whether SQL error is present or meets the expected condition.
     *
     * @return {@code true} if the response contains a recognizable SQL error
     */
    public boolean hasSqlError() {
        String response = getResponseText().toLowerCase(Locale.ROOT);

        return response.contains("sql syntax")
                || response.contains("unknown column")
                || response.contains("mysqli error")
                || response.contains("sqlstate");
    }

    /**
     * Reads the SQL injection module response text displayed by DVWA.
     *
     * @return the response text displayed by the SQL injection page
     */
    public String getResponseText() {
        return wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body"))).getText();
    }

    /**
     * Reads the rendered output from the most recent DVWA SQL injection submission.
     *
     * @return the rendered result body after submitting the injection input
     */
    public String getInjectionResultText() {
        return driver.findElements(resultRecords).stream()
                .map(WebElement::getText)
                .reduce("", (a, b) -> a + "\n" + b);
    }
}
