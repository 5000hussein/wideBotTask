package Pages;

import Util.ElementsActions;
import Util.Waits;
import io.qameta.allure.Step;
import org.openqa.selenium.By;

public class DashboardPage extends BasePage {
    private final By dashboardGrid = By.cssSelector(".orangehrm-dashboard-grid");
    private final By dashboardWidget = By.cssSelector(".orangehrm-dashboard-widget");
    private final By logoutMenuItem = By.xpath("//a[normalize-space()='Logout']");

    @Step("Verify the dashboard has loaded")
    public boolean isDashboardDisplayed() {
        return Waits.waitForUrlContains(driver, "dashboard/index")
                && ElementsActions.isDisplayed(driver, dashboardGrid)
                && "Dashboard".equals(getBreadcrumbModule());
    }

    public int getWidgetCount() {
        return ElementsActions.countElements(driver, dashboardWidget);
    }

    public boolean isLoggedInUserDisplayed() {
        return isUserMenuDisplayed() && !getLoggedInUserName().isBlank();
    }

    @Step("Log out")
    public LoginPage logout() {
        ElementsActions.clickElement(driver, USER_DROPDOWN_TAB);
        ElementsActions.clickElement(driver, logoutMenuItem);
        Waits.waitForUrlContains(driver, "auth/login");
        return new LoginPage();
    }
}
