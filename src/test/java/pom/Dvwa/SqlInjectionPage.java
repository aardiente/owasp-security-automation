package pom.Dvwa;

import automation.ZapManager;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

/**
 * Represents DVWA's SQL injection practice page. Provides a shared way for tests to interact with the page and read results at different security levels.
 */
public class SqlInjectionPage extends BasePage {
    public static final String endpoint = "/vulnerabilities/sqli/";
    private static final By SQLI_MODULE = By.cssSelector(".vulnerable_code_area");
    private static final Pattern SQL_ERROR = Pattern.compile(
            "(?i)sql syntax|unknown column|mysqli[_ ](?:error|sql_exception)|sqlstate|error in your sql"
                    + "|the used select statements|different number of columns|column count doesn't match"
                    + "|operand should contain|syntax error");

    private final By idInput = By.name("id");
    private final By submitButton = By.name("Submit");
    private final By results = By.cssSelector(".vulnerable_code_area pre");
    private final By highInputLink = By.cssSelector(".vulnerable_code_area a[onclick*='session-input.php']");

    /**
     * Sets up the DVWA sql injection page with the provided browser session.
     *
     * @param driver current test browser
     */
    public SqlInjectionPage(WebDriver driver) { super(driver); }

    /**
     * Opens the SQL injection module and waits for its form to appear.
     *
     * @param baseUrl running application URL
     */
    public void open(String baseUrl) {
        driver.get(baseUrl + endpoint);
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(SQLI_MODULE));
        } catch (TimeoutException e) {
            throw new IllegalStateException("DVWA SQL Injection module could not be opened. "
                    + pageContext(), e);
        }
    }

    /**
     * Submits through DVWA's normal GET form (Low and Impossible).
     */
    public void injectPayload(String payload) {
        WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(idInput));
        input.clear();
        input.sendKeys(payload);
        wait.until(ExpectedConditions.elementToBeClickable(submitButton)).click();
        wait.until(ExpectedConditions.urlContains("id="));
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
    }

    /**
     * Posts directly to the server because Medium provides a fixed-value SELECT, not a text box.
     */
    public HttpResponse<String> injectMediumPayload(String baseUrl, String payload) {
        List<WebElement> dropdowns = driver.findElements(By.cssSelector(".vulnerable_code_area select[name='id']"));
        if (dropdowns.isEmpty()) throw new IllegalStateException("Medium SQLi dropdown was not found");

        String cookies = driver.manage().getCookies().stream()
                .map(cookie -> cookie.getName() + "=" + cookie.getValue())
                .collect(Collectors.joining("; "));
        if (cookies.isBlank()) throw new IllegalStateException("Authenticated DVWA cookies are missing");

        URI proxy = URI.create("http://" + ZapManager.getProxyAddress());
        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .proxy(ProxySelector.of(new InetSocketAddress(proxy.getHost(), proxy.getPort())))
                .connectTimeout(Duration.ofSeconds(10)).build();
        String form = "id=" + URLEncoder.encode(payload, StandardCharsets.UTF_8) + "&Submit=Submit";
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + endpoint))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Cookie", cookies)
                .POST(HttpRequest.BodyPublishers.ofString(form)).build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 && !SQL_ERROR.matcher(response.body()).find())
                throw new IllegalStateException("Medium SQLi request returned HTTP " + response.statusCode()
                        + " without recognizable SQL error disclosure");
            if (!response.body().contains("vulnerable_code_area") && !SQL_ERROR.matcher(response.body()).find())
                throw new IllegalStateException("Medium SQLi response did not contain the SQL Injection module");
            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted submitting Medium SQLi probe", e);
        } catch (IOException e) {
            throw new IllegalStateException("Could not submit Medium SQLi probe through ZAP", e);
        }
    }

    /**
     * Uses DVWA's existing popup; submitting it sets the session ID tested on the main page.
     */
    public void injectHighPayload(String baseUrl, String payload) {
        String originalHandle = driver.getWindowHandle();
        Set<String> previous = driver.getWindowHandles();
        wait.until(ExpectedConditions.elementToBeClickable(highInputLink)).click();
        wait.until(d -> d.getWindowHandles().size() > previous.size());
        String popupHandle = driver.getWindowHandles().stream().filter(h -> !previous.contains(h))
                .findFirst().orElseThrow(() -> new IllegalStateException("DVWA session input popup did not open"));

        driver.switchTo().window(popupHandle);
        try {
            WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(idInput));
            input.clear();
            input.sendKeys(payload);
            wait.until(ExpectedConditions.elementToBeClickable(submitButton)).click();
            wait.until(ExpectedConditions.textToBePresentInElementLocated(By.tagName("body"), "Session ID:"));
        } finally {
            if (driver.getWindowHandles().contains(popupHandle)) driver.close();
            driver.switchTo().window(originalHandle);
        }
        // The session-backed query can fail before DVWA renders its normal SQLi page.
        driver.get(baseUrl + endpoint);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
    }

    /**
     * Reads the rendered response without retaining WebElements from a replaced document.
     */
    public String getInjectionResultText() {
        String html;
        try {
            html = wait.until(d -> {
                try {
                    String source = d.getPageSource();
                    return source.contains("vulnerable_code_area") || source.contains("Something went wrong.")
                            || SQL_ERROR.matcher(source).find() ? source : null;
                } catch (WebDriverException e) {
                    // Chrome may replace the page while its source is being read.
                    if (e.getMessage() != null && e.getMessage().contains("Node with given id does not belong to the document"))
                        return null;
                    throw e;
                }
            });
        } catch (TimeoutException e) {
            throw new IllegalStateException("DVWA SQLi response was not ready. " + pageContext(), e);
        }

        if (html.contains("vulnerable_code_area")) return getHttpResultText(html);
        // High deliberately hides SQL failures with this generic message, without the normal module markup.
        if (html.contains("Something went wrong.")) return "Something went wrong.";
        if (SQL_ERROR.matcher(html).find()) return getHttpResultText(html);
        throw new IllegalStateException("Unexpected DVWA SQLi response. " + pageContext());
    }

    /**
     * Summarizes the current browser context for module-loading diagnostics.
     *
     * @return diagnostic context string
     */
    private String pageContext() {
        String body;
        try {
            body = driver.getPageSource().replaceAll("(?is)<script\\b[^>]*>.*?</script>", "")
                    .replaceAll("(?is)<style\\b[^>]*>.*?</style>", "")
                    .replaceAll("(?is)<[^>]+>", " ").replaceAll("\\s+", " ").strip();
        } catch (WebDriverException e) {
            body = "<document changed during inspection>";
        }
        return "URL=" + driver.getCurrentUrl() + ", title=" + driver.getTitle()
                + ", body=" + body.substring(0, Math.min(350, body.length()));
    }

    /**
     * Extracts the PRE blocks from Medium's actual HTTP response without executing their HTML.
     */
    public String getHttpResultText(String html) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(?is)<pre\\b[^>]*>(.*?)</pre>").matcher(html);
        StringBuilder output = new StringBuilder();
        while (matcher.find()) {
            String block = matcher.group(1).replaceAll("(?i)<br\\s*/?>", "\n")
                    .replaceAll("(?is)<[^>]+>", "").replace("&nbsp;", " ")
                    .replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
            if (!output.isEmpty()) output.append('\n');
            output.append(block.strip());
        }
        if (output.isEmpty() && SQL_ERROR.matcher(html).find())
            return html.replaceAll("(?is)<script\\b[^>]*>.*?</script>", "")
                    .replaceAll("(?is)<[^>]+>", " ").replace("&nbsp;", " ")
                    .replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
        return output.toString();
    }
}
