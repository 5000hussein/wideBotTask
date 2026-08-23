package Tests;

import Listeners.TestListener;
import Pages.DashboardPage;
import Pages.LoginPage;
import Pages.PimEmployeeListPage;
import Util.Config;
import Util.Drivers;
import Util.ScreenshotUtil;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import java.util.ArrayList;
import java.util.List;

@Listeners({TestListener.class})
public abstract class BaseTest {
    protected WebDriver driver;
    protected LoginPage loginPage;
    protected DashboardPage dashboardPage;

    private final List<String> createdLastNames = new ArrayList<>();

    @BeforeClass(alwaysRun = true)
    @Parameters({"browser"})
    public void setUp(@Optional String browserFromSuite) {
        String browser = browserFromSuite == null || browserFromSuite.isBlank()
                ? Config.getInstance().getBrowser()
                : browserFromSuite;

        driver = Drivers.setUpDriver(browser, Config.getInstance().isHeadless());
        loginPage = new LoginPage();
        loginPage.open();
    }

    @Step("Sign in and land on the dashboard")
    protected DashboardPage loginToDashboard() {
        LoginPage login = new LoginPage();
        login.open();
        login.loginWithValidCredentials();

        dashboardPage = new DashboardPage();
        return dashboardPage;
    }

    protected void registerForCleanup(String lastName) {
        createdLastNames.add(lastName);
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        try {
            cleanUpCreatedEmployees();
        } finally {
            Drivers.quitDriver();
        }
    }

    private void cleanUpCreatedEmployees() {
        if (createdLastNames.isEmpty()) {
            return;
        }
        if (!Config.getInstance().isCleanupEnabled()) {
            System.out.println("Cleanup disabled (cleanup.enabled=false). Records left in place: "
                    + createdLastNames);
            return;
        }
        if (!Drivers.hasDriver()) {
            System.err.println("No live session for cleanup; leftover records: "
                    + createdLastNames);
            return;
        }

        for (String lastName : createdLastNames) {
            try {
                PimEmployeeListPage list = new PimEmployeeListPage();
                list.open();

                if (!list.setEmployeeNameFilter(lastName)) {
                    System.out.println("Cleanup: '" + lastName
                            + "' no longer offered by search -- assuming already removed.");
                    continue;
                }
                list.clickSearch();
                if (list.findRowByLastName(lastName).isEmpty()) {
                    continue;
                }
                list.deleteEmployeeByLastName(lastName);
                System.out.println("Cleanup: deleted employee " + lastName);
            } catch (RuntimeException e) {
                System.err.println("Cleanup FAILED for " + lastName
                        + " (" + e.getClass().getSimpleName() + ": " + e.getMessage()
                        + "). This record must be removed manually.");
                ScreenshotUtil.capture("cleanup-failed-" + lastName);
            }
        }
        createdLastNames.clear();
    }

    protected void checkpoint(String name) {
        ScreenshotUtil.capture(name);
    }
}
