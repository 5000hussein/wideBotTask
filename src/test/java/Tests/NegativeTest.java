package Tests;

import Pages.AddEmployeePage;
import Pages.AssignLeavePage;
import Pages.DashboardPage;
import Pages.LoginPage;
import Pages.PimEmployeeListPage;
import Util.Config;
import Util.Helper;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Epic("OrangeHRM")
@Feature("Negative validation")
public class NegativeTest extends BaseTest {
    private final String negativeFirstName = Helper.getData("negativeFirstName");
    private final String negativeLastName = Helper.getData("negativeLastName");
    private final String overlongFirstName = Helper.getData("overlongFirstName");
    private final String invalidPassword = Helper.getData("invalidPassword");

    private AddEmployeePage addEmployee;
    private PimEmployeeListPage list;
    private AssignLeavePage assign;
    private LoginPage login;
    private DashboardPage dashboard;

    @BeforeClass(alwaysRun = true, dependsOnMethods = "setUp")
    public void signIn() {
        loginToDashboard();

        addEmployee = new AddEmployeePage();
        list = new PimEmployeeListPage();
        assign = new AssignLeavePage();
        login = new LoginPage();
        dashboard = new DashboardPage();
    }

    @Test(priority = 1, groups = {"negative", "regression"},
            description = "Creating an employee without a last name is rejected")
    @Story("Required field validation")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Example A: submit Add Employee with the required Last Name empty. Expect an inline "
            + "'Required' message, no navigation, and no employee created.")
    public void verifyEmployeeCannotBeCreatedWithoutLastName() {
        addEmployee.open();
        addEmployee.enterFirstName(negativeFirstName);
        addEmployee.enterLastName("");
        addEmployee.saveExpectingValidationError();
        addEmployee.verifyFieldRejectedWith("Employee Full Name", "Required");

        checkpoint("15-negative-required-field");

        list.open();
        list.verifyNoEmployeeNamed(negativeFirstName);
    }

    @Test(priority = 2, groups = {"negative", "regression"},
            description = "An over-length name is rejected by the employee form")
    @Story("Invalid employee data")
    @Severity(SeverityLevel.NORMAL)
    @Description("Example B: enter a first name beyond the field's 30-character limit and expect the "
            + "form to reject it rather than silently truncating or storing it.")
    public void verifyEmployeeCannotBeCreatedWithOverlongFirstName() {
        addEmployee.open();
        addEmployee.enterFirstName(overlongFirstName);
        addEmployee.enterLastName(negativeLastName);
        addEmployee.saveExpectingValidationError();
        addEmployee.verifyFieldRejectedWithLengthMessage("Employee Full Name");

        checkpoint("16-negative-invalid-data");
    }

    @Test(priority = 3, groups = {"negative", "regression"},
            retryAnalyzer = Listeners.RetryAnalyzer.class,
            description = "A leave request whose end date precedes its start date is rejected")
    @Story("Invalid leave request")
    @Severity(SeverityLevel.NORMAL)
    @Description("Example C: submit Assign Leave with To Date before From Date and expect the form "
            + "to refuse it.")
    public void verifyLeaveCannotBeAssignedWithEndDateBeforeStartDate() {
        assign.open();

        if (assign.isModuleForbidden()) {
            throw new SkipException("Leave > Assign Leave returns '403 Module Forbidden' for the "
                    + "configured account, so this negative scenario cannot be exercised. The other "
                    + "negative scenarios in this class are unaffected.");
        }

        assign.setFromDate(Helper.leaveStartDate());
        assign.setToDate(Helper.invalidEndDate());
        assign.verifyDateRejected("To Date");

        checkpoint("17-negative-invalid-leave-dates");
    }

    @Test(priority = 4, groups = {"negative", "regression"},
            description = "Invalid credentials are rejected with a clear message")
    @Story("Authentication validation")
    @Severity(SeverityLevel.CRITICAL)
    @Description("An additional negative scenario: a wrong password is refused, the user stays on "
            + "the login page, and the message does not disclose which field was wrong.")
    public void verifyInvalidCredentialsAreRejected() {
        dashboard.logout();

        login.open();
        login.loginAs(Config.getInstance().getUsername(), invalidPassword);
        login.verifyLoginRejectedWith("Invalid credentials");

        checkpoint("18-negative-invalid-credentials");

        login.loginAs(Config.getInstance().getUsername(), Config.getInstance().getPassword());
        dashboard.waitUntilLoaded();
    }
}
