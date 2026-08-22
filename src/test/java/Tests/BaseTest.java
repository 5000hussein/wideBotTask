package Tests;

import Listeners.TestListener;
import Pages.DashboardPage;
import Pages.LoginPage;
import Pages.PimEmployeeListPage;
import Util.ConfigReader;
import Util.DataFactory;
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

    private final List<DataFactory.Employee> createdEmployees = new ArrayList<>();

    @BeforeClass(alwaysRun = true)
    @Parameters({"browser"})
    public void setUp(@Optional String browserFromSuite) {
        String browser = browserFromSuite == null || browserFromSuite.isBlank()
                ? ConfigReader.browser()
                : browserFromSuite;

        driver = Drivers.setUpDriver(browser, ConfigReader.headless());
        loginPage = new LoginPage().open();
    }

    @Step("Sign in and land on the dashboard")
    protected DashboardPage loginToDashboard() {
        dashboardPage = new LoginPage().open().loginWithValidCredentials();
        return dashboardPage;
    }

    protected void registerForCleanup(DataFactory.Employee employee) {
        createdEmployees.add(employee);
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
        if (createdEmployees.isEmpty()) {
            return;
        }
        if (!ConfigReader.cleanupEnabled()) {
            System.out.println("Cleanup disabled (cleanup.enabled=false). Records left in place: "
                    + createdEmployees.stream().map(DataFactory.Employee::lastName).toList());
            return;
        }
        if (!Drivers.hasDriver()) {
            System.err.println("No live session for cleanup; leftover records: "
                    + createdEmployees.stream().map(DataFactory.Employee::lastName).toList());
            return;
        }

        for (DataFactory.Employee employee : createdEmployees) {
            try {
                PimEmployeeListPage list = new PimEmployeeListPage().open();

                if (!list.setEmployeeNameFilter(employee.lastName())) {
                    System.out.println("Cleanup: '" + employee.lastName()
                            + "' no longer offered by search -- assuming already removed.");
                    continue;
                }
                list.clickSearch();
                if (list.findRowByLastName(employee.lastName()).isEmpty()) {
                    continue;
                }
                list.deleteEmployeeByLastName(employee.lastName());
                System.out.println("Cleanup: deleted employee " + employee.fullName());
            } catch (RuntimeException e) {
                System.err.println("Cleanup FAILED for " + employee.fullName()
                        + " (" + e.getClass().getSimpleName() + ": " + e.getMessage()
                        + "). This record must be removed manually.");
                ScreenshotUtil.capture("cleanup-failed-" + employee.lastName());
            }
        }
        createdEmployees.clear();
    }

    protected void checkpoint(String name) {
        ScreenshotUtil.capture(name);
    }
}
