package Tests;

import Pages.AddEmployeePage;
import Pages.ApplyLeavePage;
import Pages.AssignLeavePage;
import Pages.LeaveEntitlementPage;
import Pages.LeaveListPage;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

/**
 * Step 8 -- Leave request.
 *
 * Route note. Leave &gt; Apply cannot be used on this environment: the signed-in
 * Admin holds no leave entitlement, so that screen renders "No Leave Types with
 * Leave Balance" instead of a form -- {@link #applyLeaveScreenMatchesEntitlementState()}
 * asserts the screen is correct for whichever state it is in, rather than
 * leaving it as an unexplained gap. The suite therefore creates the request
 * through Leave &gt; Assign Leave, the administrator's equivalent flow, against a
 * dedicated employee it creates and entitles itself. That keeps the scenario
 * self-contained instead of depending on whatever entitlements the shared demo
 * data happens to hold.
 *
 * Dates are calculated at runtime (next Monday to next Wednesday), never
 * hard-coded, and are anchored to a Monday so the working-day count is stable.
 */
@Epic("OrangeHRM")
@Feature("Leave")
public class LeaveTest extends BaseTest {

    private DataFactory.Employee leaveEmployee;
    private String leaveType;
    private LocalDate fromDate;
    private LocalDate toDate;

    @BeforeClass(alwaysRun = true, dependsOnMethods = "setUp")
    public void signIn() {
        loginToDashboard();
        leaveEmployee = DataFactory.newEmployee();
        fromDate = DataFactory.leaveStartDate();
        toDate = DataFactory.leaveEndDate();

        // Fail fast, and accurately, when the account cannot reach the module at
        // all. The permissions attached to the shared demo's Admin role are
        // changed by other users of the sandbox; when the Leave module is revoked
        // every screen below returns 403, and without this check each test spends
        // its full timeout and then reports a missing control -- which looks like
        // a broken locator or a product defect instead of an access problem.
        new ApplyLeavePage().open();
        if (new ApplyLeavePage().isModuleForbidden()) {
            throw new SkipException(
                    "The Leave module returns '403 Module Forbidden' for user '"
                            + dashboardPage.getLoggedInUserName() + "' on " + ConfigReader.baseUrl()
                            + ". The Leave scenarios cannot run until that account's role is granted "
                            + "access to the Leave module. This is an environment permission state, "
                            + "not a product defect and not a test failure.");
        }
    }

    @Test(priority = 1, groups = {"regression"},
            retryAnalyzer = Listeners.RetryAnalyzer.class,
            description = "Apply Leave renders correctly for the account entitlement state")
    @Story("Apply for leave")
    @Severity(SeverityLevel.MINOR)
    @Description("Documents the environment constraint that makes the self-service Apply screen "
            + "unusable for the Admin account, and pins it so a change in entitlement is noticed.")
    public void applyLeaveScreenMatchesEntitlementState() {
        ApplyLeavePage apply = new ApplyLeavePage().open();
        ApplyLeavePage.State state = apply.getState();

        // Asserted rather than skipped. The entitlement held by the shared demo
        // account is outside this suite's control, so the test verifies that the
        // screen is CORRECT for whichever state it is in. That keeps the result
        // meaningful either way, instead of reporting a skip the reader then has
        // to go and investigate.
        assertNotEquals(state, ApplyLeavePage.State.UNKNOWN,
                "Apply Leave should render either the request form or the no-balance message");

        if (state == ApplyLeavePage.State.NO_BALANCE) {
            assertTrue(apply.isNoLeaveBalanceMessageDisplayed(),
                    "Apply Leave should explain that the account has no leave types with a balance");
            assertFalse(apply.isApplyFormAvailable(),
                    "The request form must not be offered when there is no balance to spend");
            System.out.println("Apply Leave: no entitlement for this account, as expected on the demo.");
        } else {
            assertTrue(apply.isApplyFormAvailable(),
                    "An account holding a balance should be offered a usable request form");
            System.out.println("Apply Leave: this account now holds a balance and the form is available.");
        }

        checkpoint("12-apply-leave-state");
    }

    // In "smoke" as well as "regression" on purpose: submitLeaveRequest is a smoke
    // test and depends on this one, and TestNG refuses to run a group whose
    // members depend on methods the filter excluded. A group has to be closed
    // over its dependencies.
    @Test(priority = 2, groups = {"smoke", "regression"},
            description = "An employee is created and entitled so leave can be assigned to them")
    @Story("Leave prerequisites")
    @Severity(SeverityLevel.NORMAL)
    @Description("Creates a dedicated employee and grants a leave entitlement, so the leave request "
            + "does not depend on pre-existing shared data.")
    public void createEmployeeWithLeaveEntitlement() {
        new AddEmployeePage().open().saveAndOpenRecord(leaveEmployee);
        registerForCleanup(leaveEmployee);

        LeaveEntitlementPage entitlement = new LeaveEntitlementPage().open();

        // The employee has to be chosen FIRST: entitlements are per-employee, and
        // the Leave Type list is only populated once the form knows who it is for.
        assertTrue(entitlement.selectEmployee(leaveEmployee.fullName()),
                "The newly created employee should be selectable for an entitlement");

        List<String> availableTypes = entitlement.getAvailableLeaveTypes();
        assertFalse(availableTypes.isEmpty(), "The environment should define at least one leave type");
        leaveType = availableTypes.get(0);
        System.out.println("Using leave type: " + leaveType);

        entitlement.selectLeaveType(leaveType)
                .setEntitlement("10")
                .save();

        assertTrue(entitlement.wasLastActionSuccessful(),
                "Granting the entitlement should be confirmed; toast was: '"
                        + entitlement.getLastToastText() + "'");
        entitlement.dismissToast();
    }

    @Test(priority = 3, groups = {"smoke", "regression"},
            dependsOnMethods = "createEmployeeWithLeaveEntitlement",
            description = "A leave request is submitted for calculated dates")
    @Story("Submit a leave request")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Step 8a: submit a leave request using a runtime-calculated date range and verify "
            + "the success notification.")
    public void submitLeaveRequest() {
        AssignLeavePage assign = new AssignLeavePage().open();
        assertTrue(assign.isDisplayed(), "Assign Leave form should be displayed");

        assertTrue(assign.selectEmployee(leaveEmployee.fullName()),
                "The entitled employee should be selectable on Assign Leave");
        assign.selectLeaveType(leaveType);

        String balance = assign.getLeaveBalance();
        System.out.println("Reported leave balance before submitting: " + balance);

        assign.setFromDate(fromDate)
                .setToDate(toDate)
                .setComment("Submitted by automated regression run " + DataFactory.runTag());

        // The dates must have landed in the app's yyyy-dd-MM format.
        assertEquals(assign.getFromDateValue(), DataFactory.formatForApp(fromDate),
                "From Date should hold the calculated start date");
        assertEquals(assign.getToDateValue(), DataFactory.formatForApp(toDate),
                "To Date should hold the calculated end date");

        assign.clickAssign();

        assertTrue(assign.wasLastActionSuccessful(),
                "Submitting the leave request should be confirmed; toast was: '"
                        + assign.getLastToastText() + "'");
        checkpoint("13-leave-request-submitted");
        assign.dismissToast();
    }

    @Test(priority = 4, groups = {"regression"}, dependsOnMethods = "submitLeaveRequest",
            description = "The submitted leave request can be located afterwards")
    @Story("Submit a leave request")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Step 8b: find the submitted request in the Leave List and verify its employee, "
            + "type, date range and day count -- proving it was persisted, not just announced.")
    public void submittedLeaveCanBeFound() {
        LeaveListPage leaveList = new LeaveListPage().open();

        // The status filter defaults to a subset that hides admin-assigned leave.
        leaveList.includeStatus("Scheduled");
        leaveList.setDateRange(fromDate, toDate);
        assertTrue(leaveList.setEmployeeFilter(leaveEmployee.fullName()),
                "The employee should be selectable on the Leave List filter");
        leaveList.clickSearch();

        Optional<LeaveListPage.LeaveRow> row = leaveList.findRowByEmployeeRetrying(leaveEmployee.lastName());
        assertTrue(row.isPresent(),
                "The submitted leave request should be findable for " + leaveEmployee.fullName());

        LeaveListPage.LeaveRow leave = row.get();
        assertTrue(leave.leaveType().contains(leaveType),
                "The stored leave type should be '" + leaveType + "' but was '" + leave.leaveType() + "'");
        assertEquals(leave.numberOfDays(), String.valueOf(DataFactory.expectedLeaveDays()) + ".00",
                "A Monday-to-Wednesday request should be " + DataFactory.expectedLeaveDays() + " days");
        assertFalse(leave.status().isBlank(), "The request should carry a status");

        System.out.println("Found leave: " + leave);
        checkpoint("14-leave-request-found");
    }
}
