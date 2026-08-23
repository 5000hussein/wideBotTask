package Pages;

import Util.ElementsActions;
import Util.Validations;
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
    public void open() {
        openPath(PATH);
        Waits.waitForElementVisible(driver, usernameField);
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

    @Step("Log in as {username}")
    public void loginAs(String username, String password) {
        enterUsername(username);
        enterPassword(password);
        submit();
    }

    public boolean isStillOnLoginPage() {
        return driver.getCurrentUrl().contains("auth/login");
    }

    //PageAssertions
    @Step("Verify the login page shows all its controls")
    public void verifyLoginPageDisplayedWithAllControls() {
        Validations.validateTrue(isLoginPageDisplayed(),
                "Login page (title + branding) should be displayed");
        Validations.validateTrue(isUsernameFieldDisplayed(), "Username field should be available");
        Validations.validateTrue(isPasswordFieldDisplayed(), "Password field should be available");
        Validations.validateTrue(isLoginButtonDisplayed(), "Login button should be available");
        Validations.validateTrue(isLoginButtonEnabled(), "Login button should be enabled");
    }

    @Step("Verify the login was rejected with {expectedMessage}")
    public void verifyLoginRejectedWith(String expectedMessage) {
        Validations.validateEquals(getAlertMessage(), expectedMessage,
                "A clear rejection message should be displayed");
        Validations.validateTrue(isStillOnLoginPage(),
                "A rejected login must not reach the application");
    }
}
