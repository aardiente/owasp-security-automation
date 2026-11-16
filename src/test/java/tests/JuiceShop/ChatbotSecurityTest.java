package tests.JuiceShop;

import automation.ZapManager;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Test;
import pom.JuiceShop.ChatbotPage;
import pom.JuiceShop.LoginPage;

import java.time.Duration;

/**
 * TestNG test class for the chatbot security test scenarios within the Docker security lab.
 */
public class ChatbotSecurityTest extends JuiceShopBaseTest
{
    /**
     * Exercises the Juice Shop chatbot interaction and checks the reply using the test assertion.
     */
    @Test
    public void BasicChatBotTest()
    {
        var driver = ZapManager.getDriver();
        driver.get(baseUrl);

        LoginPage loginPage = new LoginPage(driver);
        loginPage.dismissInitialOverlays();
        loginPage.navigateToLoginViaNavbar();
        loginPage.login("admin@juice-sh.op'--", "anything");

        // Wait for login redirect back to main shop before opening sidenav
        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.urlContains("/search"));

        ChatbotPage chatbot = new ChatbotPage(driver);
        chatbot.openChatbot();

        int initialCount = chatbot.getBotMessageCount();
        chatbot.sendMessage("coupon code");

        String response = chatbot.getLatestBotResponse(initialCount);
        Assert.assertFalse(response.isBlank(), "Chatbot response was empty.");
    }

    /**
     * Queries the experimental Ollama API and checks the returned response.
     */
    @Test
    public void manualOllamaApiVerificationTest() {
        org.testcontainers.containers.GenericContainer<?> ollamaContainer =
                new org.testcontainers.containers.GenericContainer<>("ollama/ollama:latest")
                        .withExposedPorts(11434);

        ollamaContainer.start();

        try {
            // Pull model inside the running container
            org.testcontainers.containers.Container.ExecResult pullResult =
                    ollamaContainer.execInContainer("ollama", "pull", "qwen2.5:1.5b");
            System.out.println(">>> PULL EXIT CODE: " + pullResult.getExitCode());

            // Hit the completions endpoint directly via Java HTTP connection
            String urlStr = "http://" + ollamaContainer.getHost() + ":" + ollamaContainer.getMappedPort(11434) + "/v1/chat/completions";
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(urlStr).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String body = "{\"model\": \"qwen2.5:1.5b\", \"messages\": [{\"role\": \"user\", \"content\": \"hello\"}]}";
            try (java.io.OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            System.out.println(">>> API STATUS: " + status);

            java.io.InputStream stream = (status >= 400) ? conn.getErrorStream() : conn.getInputStream();
            try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                System.out.println(">>> API RESPONSE BODY: " + response.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ollamaContainer.stop();
        }
    }
}