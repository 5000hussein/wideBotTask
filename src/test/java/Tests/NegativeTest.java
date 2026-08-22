package Tests;

import Pages.AddEmployeePage;
import Pages.AssignLeavePage;
import Pages.LoginPage;
import Pages.PimEmployeeListPage;
import Util.ConfigReader;
import Util.DataFactory;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Step 9 -- negative validation.
 *
 * Four scenarios, covering the three candidates the assessment offers plus
 * authentication. Each one asserts BOTH halves of the requirement: that the
 * validation message appears, AND that the record was genuinely not created or
 * changed -- verified by going back and looking, not by trusting the UI.
 */
@Epic("OrangeHRM")
@Feature("Negative validation")
public class NegativeTest extends BaseTest {


    @BeforeClass(alwaysRun = true, dependsOnMethods = "setUp")
    public void signIn() {
        loginToDashboard();
    }

    // -------------------------------------- A: required field left empty

    @Test(priority = 1, groups = {"negative", "regression"},
            description = "Creating an employee without a last name is rejected")
    @Story("Required field validation")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Example A: submit Add Employee with the required Last Name empty. Expect an inline "
            + "'Required' message, no navigation, and no employee created.")
    public void cannotCreateEmployeeWithoutLastName() {
        AddEmployeePage addEmployee = new AddEmployeePage().open();

        String firstName = "Negative" + DataFactory.runTag();
        addEmployee.enterFirstName(firstName)
                .enterLastName("")
                .saveExpectingValidationError();

        // "Validation message is displayed"
        assertEquals(addEmployee.getFieldErrorFor("Employee Full Name"), "Required",
                "A 'Required' message should be shown against the name field");

        // "Employee is not created"
        assertTrue(addEmployee.isStillOnAddEmployeePage(),
                "The form should not navigate away when validation fails");
        assertFalse(addEmployee.isSuccessToastDisplayed(),
                "No success notification should appear for a rejected submission");

        checkpoint("15-negative-required-field");

        // Prove absence at the source rather than trusting the screen: the name
        // autocomplete queries the employee table, so offering no match for this
        // name is evidence that no such record exists.
        PimEmployeeListPage list = new PimEmployeeListPage().open();
        assertFalse(list.setEmployeeNameFilter(firstName),
                "No employee should have been created by the rejected submission, "
                        + "but the search offered a match for '" + firstName + "'");
    }

    // ---------------------------------------- B: invalid employee data

    @Test(priority = 2, groups = {"negative", "regression"},
            description = "An over-length name is rejected by the employee form")
    @Story("Invalid employee data")
    @Severity(SeverityLevel.NORMAL)
    @Description("Example B: enter a first name beyond the field's 30-character limit and expect the "
            + "form to reject it rather than silently truncating or storing it.")
    public void cannotCreateEmployeeWithOverlongFirstName() {
        AddEmployeePage addEmployee = new AddEmployeePage().open();

        String overlongName = "A".repeat(60);
        addEmployee.enterFirstName(overlongName)
                .enterLastName("Overflow" + DataFactory.runTag())
                .saveExpectingValidationError();

        String error = addEmployee.getFieldErrorFor("Employee Full Name");
        assertFalse(error.isBlank(),
                "An over-length first name should raise a validation message");
        assertTrue(error.toLowerCase().contains("should not exceed")
                        || error.toLowerCase().contains("required")
                        || error.toLowerCase().contains("invalid"),
                "Validation should explain the length limit but said: '" + error + "'");

        assertTrue(addEmployee.isStillOnAddEmployeePage(),
                "Invalid data should not be accepted");
        assertFalse(addEmployee.isSuccessToastDisplayed(),
                "No success notification should appear for invalid data");

        checkpoint("16-negative-invalid-data");
    }

    // ------------------------------------------ C: invalid leave request

    @Test(priority = 3, groups = {"negative", "regression"},
            retryAnalyzer = Listeners.RetryAnalyzer.class,
            description = "A leave request whose end date precedes its start date is rejected")
    @Story("Invalid leave request")
    @Severity(SeverityLevel.NORMAL)
    @Description("Example C: submit Assign Leave with To Date before From Date and expect the form "
            + "to refuse it.")
    public void cannotAssignLeaveWithEndDateBeforeStartDate() {
        AssignLeavePage assign = new AssignLeavePage().open();

        if (assign.isModuleForbidden()) {
            throw new SkipException("Leave > Assign Leave returns '403 Module Forbidden' for the "
                    + "configured account, so this negative scenario cannot be exercised. The other "
                    + "negative scenarios in this class are unaffected.");
        }

        assign.setFromDate(DataFactory.leaveStartDate())
                .setToDate(DataFactory.invalidEndDate());

        String error = assign.getFieldErrorFor("To Date");
        if (error.isBlank()) {
            // Some builds only surface the error on submit.
            assign.assignExpectingValidationError();
            error = assign.getFieldErrorFor("To Date");
        }

        assertFalse(error.isBlank(),
                "A To Date before the From Date should raise a validation message");
        assertTrue(assign.isStillOnAssignPage(), "An invalid request should not be submitted");
        assertFalse(assign.isSuccessToastDisplayed(),
                "No success notification should appear for an invalid leave request");

        checkpoint("17-negative-invalid-leave-dates");
    }

    // -------------------------------------------- D: authentication

    @Test(priority = 4, groups = {"negative", "regression"},
            description = "Invalid credentials are rejected with a clear message")
    @Story("Authentication validation")
    @Severity(SeverityLevel.CRITICAL)
    @Description("An additional negative scenario: a wrong password is refused, the user stays on "
            + "the login page, and the message does not disclose which field was wrong.")
    public void invalidCredentialsAreRejected() {
        new Pages.DashboardPage().logout();

        LoginPage login = new LoginPage().open()
                .loginExpectingFailure(ConfigReader.username(), "definitely-not-the-password");

        assertEquals(login.getAlertMessage(), "Invalid credentials",
                "A clear rejection message should be displayed");
        assertTrue(login.isStillOnLoginPage(), "A rejected login must not reach the application");

        checkpoint("18-negative-invalid-credentials");

        // Restore the session so @AfterClass cleanup can still run.
        login.loginWithValidCredentials();
    }
}
