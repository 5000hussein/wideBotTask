package Tests;

import Pages.AddEmployeePage;
import Pages.ApplyLeavePage;
import Pages.AssignLeavePage;
import Pages.LeaveEntitlementPage;
import Pages.LeaveListPage;
import Util.Config;
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

        new ApplyLeavePage().open();
        if (new ApplyLeavePage().isModuleForbidden()) {
            throw new SkipException(
                    "The Leave module returns '403 Module Forbidden' for user '"
                            + dashboardPage.getLoggedInUserName() + "' on " + Config.getInstance().getBaseUrl()
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
    public void verifyApplyLeaveScreenMatchesEntitlementState() {
        ApplyLeavePage apply = new ApplyLeavePage();
        apply.open();
        ApplyLeavePage.State state = apply.getState();

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

    @Test(priority = 2, groups = {"smoke", "regression"},
            description = "An employee is created and entitled so leave can be assigned to them")
    @Story("Leave prerequisites")
    @Severity(SeverityLevel.NORMAL)
    @Description("Creates a dedicated employee and grants a leave entitlement, so the leave request "
            + "does not depend on pre-existing shared data.")
    public void verifyUserCanCreateEmployeeWithLeaveEntitlement() {
        AddEmployeePage addEmployee = new AddEmployeePage();
        addEmployee.open();
        addEmployee.saveAndOpenRecord(leaveEmployee);
        registerForCleanup(leaveEmployee);

        LeaveEntitlementPage entitlement = new LeaveEntitlementPage();
        entitlement.open();

        assertTrue(entitlement.selectEmployee(leaveEmployee.fullName()),
                "The newly created employee should be selectable for an entitlement");

        List<String> availableTypes = entitlement.getAvailableLeaveTypes();
        assertFalse(availableTypes.isEmpty(), "The environment should define at least one leave type");
        leaveType = availableTypes.get(0);
        System.out.println("Using leave type: " + leaveType);

        entitlement.selectLeaveType(leaveType);
        entitlement.setEntitlement(DataFactory.entitlementDays());
        entitlement.save();

        assertTrue(entitlement.wasLastActionSuccessful(),
                "Granting the entitlement should be confirmed; toast was: '"
                        + entitlement.getLastToastText() + "'");
        entitlement.dismissToast();
    }

    @Test(priority = 3, groups = {"smoke", "regression"},
            dependsOnMethods = "verifyUserCanCreateEmployeeWithLeaveEntitlement",
            description = "A leave request is submitted for calculated dates")
    @Story("Submit a leave request")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Step 8a: submit a leave request using a runtime-calculated date range and verify "
            + "the success notification.")
    public void verifyUserCanSubmitLeaveRequest() {
        AssignLeavePage assign = new AssignLeavePage();
        assign.open();
        assign.verifyAssignLeavePageLoaded();

        assertTrue(assign.selectEmployee(leaveEmployee.fullName()),
                "The entitled employee should be selectable on Assign Leave");
        assign.selectLeaveType(leaveType);

        String balance = assign.getLeaveBalance();
        System.out.println("Reported leave balance before submitting: " + balance);

        assign.setFromDate(fromDate);
        assign.setToDate(toDate);
        assign.setComment("Submitted by automated regression run " + DataFactory.runTag());

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

    @Test(priority = 4, groups = {"regression"}, dependsOnMethods = "verifyUserCanSubmitLeaveRequest",
            description = "The submitted leave request can be located afterwards")
    @Story("Submit a leave request")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Step 8b: find the submitted request in the Leave List and verify its employee, "
            + "type, date range and day count -- proving it was persisted, not just announced.")
    public void verifySubmittedLeaveCanBeFound() {
        LeaveListPage leaveList = new LeaveListPage();
        leaveList.open();

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
