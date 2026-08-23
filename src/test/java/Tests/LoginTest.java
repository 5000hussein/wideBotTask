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
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Epic("OrangeHRM")
@Feature("Authentication")
public class LoginTest extends BaseTest {
    private LoginPage login;
    private DashboardPage dashboard;

    @BeforeClass(alwaysRun = true, dependsOnMethods = "setUp")
    public void initPages() {
        login = new LoginPage();
        dashboard = new DashboardPage();
    }

    @Test(priority = 1, groups = {"smoke", "regression"},
            retryAnalyzer = Listeners.RetryAnalyzer.class,
            description = "Login page renders with username, password and Login button")
    @Story("Login page is displayed")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Step 1a: the login page and each of its three required controls are present.")
    public void verifyLoginPageIsDisplayedWithAllControls() {
        login.open();
        login.verifyLoginPageDisplayedWithAllControls();

        checkpoint("01-login-page");
    }

    @Test(priority = 2, groups = {"smoke", "regression"},
            dependsOnMethods = "verifyLoginPageIsDisplayedWithAllControls",
            description = "Valid credentials sign in and land on the dashboard")
    @Story("Successful login")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Step 1b: login succeeds, the dashboard renders, and the signed-in user is identified.")
    public void verifyUserCanLoginWithValidCredentials() {
        login.open();
        login.loginWithValidCredentials();
        dashboard.verifyDashboardLoadedFor(Config.getInstance().getUsername());

        checkpoint("02-dashboard-after-login");
    }
}
