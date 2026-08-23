package Tests;

import Pages.AddEmployeePage;
import Pages.ApplyLeavePage;
import Pages.AssignLeavePage;
import Pages.LeaveEntitlementPage;
import Pages.LeaveListPage;
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

import java.time.LocalDate;

@Epic("OrangeHRM")
@Feature("Leave")
public class LeaveTest extends BaseTest {
    private final String leaveFirstName = Helper.getData("leaveFirstName");
    private final String leaveMiddleName = Helper.getData("leaveMiddleName");
    private final String leaveLastName = Helper.getData("leaveLastName");
    private final String leaveEmployeeId = Helper.getData("leaveEmployeeId");
    private final String leaveFullName = leaveFirstName + " " + leaveMiddleName + " " + leaveLastName;

    private final LocalDate fromDate = Helper.leaveStartDate();
    private final LocalDate toDate = Helper.leaveEndDate();
    private String leaveType;

    private AddEmployeePage addEmployee;
    private ApplyLeavePage apply;
    private LeaveEntitlementPage entitlement;
    private AssignLeavePage assign;
    private LeaveListPage leaveList;

    @BeforeClass(alwaysRun = true, dependsOnMethods = "setUp")
    public void signIn() {
        loginToDashboard();

        addEmployee = new AddEmployeePage();
        apply = new ApplyLeavePage();
        entitlement = new LeaveEntitlementPage();
        assign = new AssignLeavePage();
        leaveList = new LeaveListPage();

        apply.open();
        if (apply.isModuleForbidden()) {
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
        apply.open();
        apply.verifyScreenMatchesEntitlementState();

        checkpoint("12-apply-leave-state");
    }

    @Test(priority = 2, groups = {"smoke", "regression"},
            description = "An employee is created and entitled so leave can be assigned to them")
    @Story("Leave prerequisites")
    @Severity(SeverityLevel.NORMAL)
    @Description("Creates a dedicated employee and grants a leave entitlement, so the leave request "
            + "does not depend on pre-existing shared data.")
    public void verifyUserCanCreateEmployeeWithLeaveEntitlement() {
        addEmployee.open();
        addEmployee.fillForm(leaveFirstName, leaveMiddleName, leaveLastName, leaveEmployeeId);
        addEmployee.save();
        registerForCleanup(leaveLastName);

        entitlement.open();
        entitlement.verifyEmployeeIsSelectable(leaveFullName);

        leaveType = entitlement.getFirstAvailableLeaveType();
        entitlement.selectLeaveType(leaveType);
        entitlement.setEntitlement(Helper.getData("entitlementDays"));
        entitlement.save();
        entitlement.verifyEntitlementGranted();

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
        assign.open();
        assign.verifyAssignLeavePageLoaded();
        assign.verifyEmployeeIsSelectable(leaveFullName);

        assign.selectLeaveType(leaveType);
        assign.setFromDate(fromDate);
        assign.setToDate(toDate);
        assign.setComment(Helper.getData("leaveComment"));
        assign.verifyDateRangeAccepted(fromDate, toDate);

        assign.clickAssign();
        assign.verifyLeaveWasSubmitted();

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
        leaveList.open();
        leaveList.includeStatus("Scheduled");
        leaveList.setDateRange(fromDate, toDate);
        leaveList.verifyEmployeeIsSelectable(leaveFullName);
        leaveList.clickSearch();

        leaveList.verifyStoredLeave(leaveLastName, leaveType, Helper.getData("expectedLeaveDays"));

        checkpoint("14-leave-request-found");
    }
}
