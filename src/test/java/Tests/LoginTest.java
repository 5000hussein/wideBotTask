package Tests;

import Pages.DashboardPage;
import Pages.LoginPage;
import Util.Config;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

@Epic("OrangeHRM")
@Feature("Authentication")
public class LoginTest extends BaseTest {
    @Test(priority = 1, groups = {"smoke", "regression"},
            retryAnalyzer = Listeners.RetryAnalyzer.class,
            description = "Login page renders with username, password and Login button")
    @Story("Login page is displayed")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Step 1a: the login page and each of its three required controls are present.")
    public void verifyLoginPageIsDisplayedWithAllControls() {
        LoginPage login = new LoginPage();
        login.open();

        assertTrue(login.isLoginPageDisplayed(), "Login page (title + branding) should be displayed");
        assertTrue(login.isUsernameFieldDisplayed(), "Username field should be available");
        assertTrue(login.isPasswordFieldDisplayed(), "Password field should be available");
        assertTrue(login.isLoginButtonDisplayed(), "Login button should be available");
        assertTrue(login.isLoginButtonEnabled(), "Login button should be enabled");

        checkpoint("01-login-page");
    }

    @Test(priority = 2, groups = {"smoke", "regression"},
            dependsOnMethods = "verifyLoginPageIsDisplayedWithAllControls",
            description = "Valid credentials sign in and land on the dashboard")
    @Story("Successful login")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Step 1b: login succeeds, the dashboard renders, and the signed-in user is identified.")
    public void verifyUserCanLoginWithValidCredentials() {
        LoginPage login = new LoginPage();
        login.open();
        DashboardPage dashboard = login.loginWithValidCredentials();

        assertTrue(dashboard.isDashboardDisplayed(),
                "Dashboard should be displayed after a successful login");
        assertEquals(dashboard.getBreadcrumbModule(), "Dashboard",
                "Top bar should identify the Dashboard module");
        assertTrue(dashboard.getWidgetCount() > 0,
                "Dashboard should render at least one widget");

        assertTrue(dashboard.isLoggedInUserDisplayed(),
                "The signed-in user's menu should be displayed");
        String displayedUser = dashboard.getLoggedInUserName();
        assertFalse(displayedUser.isBlank(), "Signed-in user name should not be blank");
        assertNotEquals(displayedUser, Config.getInstance().getUsername(),
                "OrangeHRM shows the employee's full name, not the login id");

        System.out.println("Signed in as: " + displayedUser);
        checkpoint("02-dashboard-after-login");
    }
}
