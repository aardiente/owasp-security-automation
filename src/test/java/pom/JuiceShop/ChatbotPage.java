package pom.JuiceShop;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Represents the Juice Shop chatbot. Opens the conversation, sends messages and waits for replies.
 */
public class ChatbotPage extends BasePage
{
    private final By sideNavButton = By.cssSelector("button[aria-label='Open Sidenav']");
    private final By supportChatLink = By.cssSelector("a[routerlink='/chatbot']");
    private final By chatInputField = By.cssSelector("textarea.chat-textarea");
    private final By sendButton = By.cssSelector("button.send-button");
    private final By botMessageContents = By.cssSelector(".chat-bubble.assistant .message-content");

    /**
     * Sets up the Juice Shop chatbot page with the provided browser session.
     *
     * @param driver the Selenium WebDriver controlling the application browser
     */
    public ChatbotPage(WebDriver driver)
    {
        super(driver);
    }

    /**
     * Opens the built-in Juice Shop chatbot user interface.
     */
    public void openChatbot()
    {
        wait.until(ExpectedConditions.elementToBeClickable(sideNavButton)).click();
        wait.until(ExpectedConditions.elementToBeClickable(supportChatLink)).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(chatInputField));
    }

    /**
     * Submits a text message to the Juice Shop chatbot.
     *
     * @param prompt the question or instruction sent to the demo chatbot
     */
    public void sendMessage(String prompt)
    {
        WebElement input = wait.until(ExpectedConditions.elementToBeClickable(chatInputField));

        // Use JavaScript to set value and dispatch Angular change events so the submit button enables
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].value = arguments[1]; arguments[0].dispatchEvent(new Event('input'));", input, prompt);

        WebElement submitBtn = wait.until(ExpectedConditions.elementToBeClickable(sendButton));
        submitBtn.click();
    }

    /**
     * Waits for and returns the latest chatbot response after the previous message count.
     *
     * @param previousCount the number of messages already present before the chatbot request
     * @return the latest chatbot reply appearing after the previous message count
     */
    public String getLatestBotResponse(int previousCount)
    {
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        try {
            WebDriverWait llmWait = new WebDriverWait(driver, Duration.ofSeconds(60));
            return llmWait.until(d -> {
                var messages = d.findElements(botMessageContents);
                if (messages.size() <= previousCount) return null;
                String lastText = messages.get(messages.size() - 1).getText().trim();
                return !lastText.isEmpty() ? lastText : null;
            });
        } finally {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        }
    }

    /**
     * Counts the chatbot messages visible in the conversation.
     *
     * @return the number of chatbot messages currently rendered
     */
    public int getBotMessageCount()
    {
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        try {
            return driver.findElements(botMessageContents).size();
        } finally {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        }
    }
}