package Pages;

import Util.ElementsActions;
import Util.Validations;
import Util.Waits;
import io.qameta.allure.Step;
import org.openqa.selenium.By;

public class DashboardPage extends BasePage {
    //Locators
    private final By dashboardGrid = By.cssSelector(".orangehrm-dashboard-grid");
    private final By dashboardWidget = By.cssSelector(".orangehrm-dashboard-widget");
    private final By logoutMenuItem = By.xpath("//a[normalize-space()='Logout']");

    //PageActions
    @Step("Wait for the dashboard to load")
    public void waitUntilLoaded() {
        Waits.waitForUrlContains(driver, "dashboard");
        Waits.waitForLoaderToDisappear(driver);
    }

    @Step("Verify the dashboard has loaded")
    public boolean isDashboardDisplayed() {
        return Waits.waitForUrlContains(driver, "dashboard/index")
                && ElementsActions.isDisplayed(driver, dashboardGrid)
                && "Dashboard".equals(getBreadcrumbModule());
    }

    public int getWidgetCount() {
        return ElementsActions.countElements(driver, dashboardWidget);
    }

    @Step("Log out")
    public void logout() {
        ElementsActions.clickElement(driver, USER_DROPDOWN_TAB);
        ElementsActions.clickElement(driver, logoutMenuItem);
        Waits.waitForUrlContains(driver, "auth/login");
    }

    //PageAssertions
    @Step("Verify the dashboard loaded and identifies the signed-in user")
    public void verifyDashboardLoadedFor(String loginId) {
        Validations.validateTrue(isDashboardDisplayed(),
                "Dashboard should be displayed after a successful login");
        Validations.validateEquals(getBreadcrumbModule(), "Dashboard",
                "Top bar should identify the Dashboard module");
        Validations.validateTrue(getWidgetCount() > 0, "Dashboard should render at least one widget");
        Validations.validateTrue(isUserMenuDisplayed(),
                "The signed-in user's menu should be displayed");
        Validations.validateNotBlank(getLoggedInUserName(), "Signed-in user name should not be blank");
        Validations.validateNotEquals(getLoggedInUserName(), loginId,
                "OrangeHRM shows the employee's full name, not the login id");
    }
}
