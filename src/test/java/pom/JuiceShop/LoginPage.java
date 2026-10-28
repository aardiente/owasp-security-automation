
package pom.JuiceShop;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.ElementClickInterceptedException;

import java.util.List;
import java.time.Duration;

public class LoginPage extends BasePage {

    private final By welcomeClose = By.cssSelector("button[aria-label='Close Welcome Banner']");
    private final By welcomeBackdrop = By.cssSelector(".cdk-overlay-backdrop");
    private final By cookieDialog = By.cssSelector(".cc-window[aria-label='cookieconsent']");
    private final By cookieDismiss = By.cssSelector("a[aria-label='dismiss cookie message']");

    private final By accountMenu = By.id("navbarAccount");
    private final By loginMenuButton = By.id("navbarLoginButton");
    private final By logoutButton = By.id("navbarLogoutButton");

    private final By sideNavButton = By.cssSelector("button[aria-label='Open Sidenav']");
    private final By sideNavLogin = By.cssSelector("mat-sidenav a[aria-label='Go to login page']");
    private final By sideNavLogout = By.cssSelector("mat-sidenav a[aria-label='Logout']");

    private final By emailField = By.id("email");
    private final By passwordField = By.id("password");
    private final By loginButton = By.id("loginButton");

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    @Override
    public void dismissInitialOverlays() {
        try {
            WebDriverWait startupWait = new WebDriverWait(driver, Duration.ofSeconds(3));
            WebElement closeButton = startupWait.until(
                    ExpectedConditions.visibilityOfElementLocated(welcomeClose)
            );

            closeButton.click();
            wait.until(ExpectedConditions.invisibilityOfElementLocated(welcomeClose));
        } catch (org.openqa.selenium.TimeoutException ignored) {
            // Welcome dialog was not displayed.
        }

        dismissCookieConsent();
    }

    private void dismissCookieConsent() {
        List<WebElement> cookieBanners = driver.findElements(cookieDialog);
        if (cookieBanners.stream().noneMatch(WebElement::isDisplayed)) return;

        try {
            wait.until(ExpectedConditions.elementToBeClickable(cookieDismiss)).click();
            wait.until(ExpectedConditions.invisibilityOfElementLocated(cookieDialog));
        } catch (ElementClickInterceptedException e) {
            System.out.println("Cookie consent is blocked by another overlay.");

            for (WebElement dialog : driver.findElements(By.cssSelector("[role='dialog'], mat-dialog-container"))) {
                if (dialog.isDisplayed()) {
                    System.out.println("Visible dialog: " + dialog.getText());
                }
            }

            throw e;
        }
    }

    @Override
    public void navigateToLoginViaNavbar() {
        wait.until(d -> !d.findElements(accountMenu).isEmpty()
                || !d.findElements(sideNavButton).isEmpty()
                || !d.findElements(emailField).isEmpty());

        if (driver.findElements(emailField).stream().anyMatch(WebElement::isDisplayed)) return;

        if (driver.findElements(accountMenu).stream().anyMatch(WebElement::isDisplayed)) {
            wait.until(ExpectedConditions.elementToBeClickable(accountMenu)).click();
            wait.until(ExpectedConditions.elementToBeClickable(loginMenuButton)).click();
        } else {
            wait.until(ExpectedConditions.elementToBeClickable(sideNavButton)).click();
            wait.until(ExpectedConditions.elementToBeClickable(sideNavLogin)).click();
        }

        wait.until(ExpectedConditions.visibilityOfElementLocated(emailField));
    }

    public void login(String email, String password) {
        WebElement emailInput = wait.until(ExpectedConditions.visibilityOfElementLocated(emailField));
        emailInput.clear();
        emailInput.sendKeys(email);

        WebElement passwordInput = wait.until(ExpectedConditions.visibilityOfElementLocated(passwordField));
        passwordInput.clear();
        passwordInput.sendKeys(password);

        dismissCookieConsent();
        wait.until(ExpectedConditions.elementToBeClickable(loginButton)).click();
    }

    public void loginFromHome(String baseUrl, String email, String password) {
        driver.get(baseUrl);
        dismissInitialOverlays();
        navigateToLoginViaNavbar();
        login(email, password);
        new WebDriverWait(driver, Duration.ofSeconds(15)).until(ExpectedConditions.urlContains("/search"));
    }

    public boolean isUserLoggedIn() {
        try {
            if (driver.findElements(accountMenu).stream().anyMatch(WebElement::isDisplayed)) {
                wait.until(ExpectedConditions.elementToBeClickable(accountMenu)).click();
                return wait.until(ExpectedConditions.visibilityOfElementLocated(logoutButton)).isDisplayed();
            }

            wait.until(ExpectedConditions.elementToBeClickable(sideNavButton)).click();
            return wait.until(ExpectedConditions.visibilityOfElementLocated(sideNavLogout)).isDisplayed();

        } catch (Exception e) {
            return false;
        }
    }
}
