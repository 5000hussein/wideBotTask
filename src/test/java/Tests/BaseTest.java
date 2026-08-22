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

/**
 * Lifecycle and test-data ownership for every test class.
 *
 * Scope: one browser + one authenticated session per test CLASS. The scenarios
 * in this assessment are a journey -- create, then find, then edit, then prove
 * it persisted -- so a class models one journey and its methods are chained
 * with dependsOnMethods. A downstream step therefore SKIPS when its
 * precondition fails, instead of reporting a second, misleading failure.
 *
 * Test data: anything a test creates is registered here and removed in
 * {@link #tearDown()} while the session is still alive. See README, "Test-data
 * strategy".
 */
@Listeners({TestListener.class})
public abstract class BaseTest {

    protected WebDriver driver;
    protected LoginPage loginPage;
    protected DashboardPage dashboardPage;

    /** Employees created by this class, deleted after it finishes. */
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

    /**
     * Signs in with the configured credentials. Called explicitly by the tests
     * that need an authenticated session, so that LoginTest can still exercise
     * the unauthenticated screen.
     */
    @Step("Sign in and land on the dashboard")
    protected DashboardPage loginToDashboard() {
        dashboardPage = new LoginPage().open().loginWithValidCredentials();
        return dashboardPage;
    }

    /** Marks an employee for deletion once this class is done. */
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

    /**
     * Deletes every employee this class created.
     *
     * Runs while the session is still open, and never fails the suite: cleanup
     * is housekeeping, and a cleanup problem must not be reported as a product
     * defect. Anything left behind is logged loudly instead, because a silent
     * cleanup failure on a shared environment is how test data accumulates.
     */
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

                // Search by LAST NAME, not by the full name the employee was
                // created with. The edit scenario deliberately changes the first
                // name, so a full-name lookup finds nothing, concludes the record
                // is already gone, and quietly leaves it in the shared database --
                // the exact pollution this cleanup exists to prevent. The last
                // name is the generated, unique, never-modified part.
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

    /** Named checkpoint screenshot, saved to disk and attached to the report. */
    protected void checkpoint(String name) {
        ScreenshotUtil.capture(name);
    }
}
