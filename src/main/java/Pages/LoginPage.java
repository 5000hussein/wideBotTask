package Pages;

import Util.Config;
import Util.ElementsActions;
import Util.Waits;
import io.qameta.allure.Step;
import org.openqa.selenium.By;

public class LoginPage extends BasePage {
    private static final String PATH = "/web/index.php/auth/login";

    //Locators
    private final By usernameField = By.name("username");
    private final By passwordField = By.name("password");
    private final By loginButton = By.cssSelector("button[type='submit']");
    private final By brandingLogo = By.cssSelector(".orangehrm-login-branding img");
    private final By loginTitle = By.xpath("//h5[normalize-space()='Login']");
    private final By alertText = By.cssSelector(".oxd-alert-content-text");

    //PageActions
    @Step("Open the OrangeHRM login page")
    public LoginPage open() {
        openPath(PATH);
        Waits.waitForElementVisible(driver, usernameField);
        return this;
    }

    public boolean isLoginPageDisplayed() {
        return ElementsActions.isDisplayed(driver, loginTitle)
                && ElementsActions.isDisplayed(driver, brandingLogo);
    }

    public boolean isUsernameFieldDisplayed() {
        return ElementsActions.isDisplayed(driver, usernameField);
    }

    public boolean isPasswordFieldDisplayed() {
        return ElementsActions.isDisplayed(driver, passwordField);
    }

    public boolean isLoginButtonDisplayed() {
        return ElementsActions.isDisplayed(driver, loginButton);
    }

    public boolean isLoginButtonEnabled() {
        return Waits.waitForElementVisible(driver, loginButton).isEnabled();
    }

    public String getAlertMessage() {
        return Waits.isElementVisible(driver, alertText, 10)
                ? ElementsActions.getText(driver, alertText)
                : "";
    }

    @Step("Enter username: {username}")
    public void enterUsername(String username) {
        ElementsActions.clearAndEnterText(driver, usernameField, username);
    }

    @Step("Enter password")
    public void enterPassword(String password) {
        ElementsActions.clearAndEnterText(driver, passwordField, password);
    }

    @Step("Submit the login form")
    public void submit() {
        ElementsActions.clickElement(driver, loginButton);
    }

    @Step("Log in with the configured credentials")
    public DashboardPage loginWithValidCredentials() {
        return loginAs(Config.getInstance().getUsername(), Config.getInstance().getPassword());
    }

    @Step("Log in as {username}")
    public DashboardPage loginAs(String username, String password) {
        enterUsername(username);
        enterPassword(password);
        submit();
        Waits.waitForUrlContains(driver, "dashboard");
        Waits.waitForLoaderToDisappear(driver);
        return new DashboardPage();
    }

    @Step("Attempt login as {username} expecting rejection")
    public void loginExpectingFailure(String username, String password) {
        if (!username.isEmpty()) {
            enterUsername(username);
        }
        if (!password.isEmpty()) {
            enterPassword(password);
        }
        submit();
    }

    public boolean isStillOnLoginPage() {
        return driver.getCurrentUrl().contains("auth/login");
    }
}
