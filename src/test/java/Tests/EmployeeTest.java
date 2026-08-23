package Tests;

import Pages.AddEmployeePage;
import Pages.EmployeeDetailsPage;
import Pages.PimEmployeeListPage;
import Util.Helper;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Epic("OrangeHRM")
@Feature("PIM - Employee Management")
public class EmployeeTest extends BaseTest {
    private final String firstName = Helper.getData("firstName");
    private final String middleName = Helper.getData("middleName");
    private final String lastName = Helper.getData("lastName");
    private final String employeeId = Helper.getData("employeeId");
    private final String fullName = firstName + " " + middleName + " " + lastName;
    private final String firstAndMiddleName = firstName + " " + middleName;

    private final String updatedFirstName = Helper.getData("updatedFirstName");
    private final String updatedEmployeeId = Helper.getData("updatedEmployeeId");
    private final String updatedFullName = updatedFirstName + " " + middleName + " " + lastName;

    private AddEmployeePage addEmployee;
    private PimEmployeeListPage list;
    private EmployeeDetailsPage details;
    private String empNumber;

    @BeforeClass(alwaysRun = true, dependsOnMethods = "setUp")
    public void signIn() {
        loginToDashboard();

        addEmployee = new AddEmployeePage();
        list = new PimEmployeeListPage();
        details = new EmployeeDetailsPage();
    }

    @Test(priority = 1, groups = {"smoke", "regression"},
            description = "A new employee is created from data held in data.json")
    @Story("Employee creation")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Step 3: create an employee and verify the success notification and the employee id.")
    public void verifyUserCanCreateNewEmployee() {
        addEmployee.open();
        addEmployee.verifyAddEmployeePageLoaded();
        addEmployee.verifyEmployeeIdIsPrefilled();

        addEmployee.fillForm(firstName, middleName, lastName, employeeId);
        addEmployee.save();
        registerForCleanup(lastName);
        addEmployee.verifySaveWasConfirmed();

        details.waitForRecordToLoad();
        empNumber = details.verifyEmpNumberAssigned();
        details.verifyRecordShows(firstName, employeeId);

        checkpoint("04-employee-created");
        details.dismissToast();
    }

    @Test(priority = 2, groups = {"smoke", "regression"},
            dependsOnMethods = "verifyUserCanCreateNewEmployee",
            description = "Searching by name returns only employees matching that name")
    @Story("Employee search")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Step 2: an employee is located by name and every returned row is verified "
            + "against the search criteria.")
    public void verifyUserCanSearchForAnExistingEmployee() {
        list.open();
        list.verifyEmployeeListPageLoaded();

        int totalBefore = list.verifyListHasRecords();
        list.verifyEmployeeIsOffered(fullName);
        list.clickSearch();

        list.verifyEveryRowMatches(lastName, totalBefore);
        list.verifyRowMatches(lastName, employeeId, firstAndMiddleName);

        checkpoint("03-employee-search-results");
    }

    @Test(priority = 3, groups = {"regression"}, dependsOnMethods = "verifyUserCanCreateNewEmployee",
            description = "The created employee can be found again in the employee list")
    @Story("Employee creation is persisted")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Step 4a: prove persistence by finding the new employee through the employee list, "
            + "not by trusting the creation toast.")
    public void verifyCreatedEmployeeIsFoundInEmployeeList() {
        list.open();
        list.verifyEmployeeIsOffered(fullName);
        list.clickSearch();

        list.verifyExactlyOneRecordFound();
        list.verifyRowMatches(lastName, employeeId, firstAndMiddleName);

        checkpoint("05-created-employee-in-list");
    }

    @Test(priority = 4, groups = {"regression"}, dependsOnMethods = "verifyCreatedEmployeeIsFoundInEmployeeList",
            description = "The created employee record opens and shows the data it was created with")
    @Story("Employee creation is persisted")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Step 4b: open the record from the list and verify name, employee id and that the "
            + "detail tabs are reachable.")
    public void verifyCreatedEmployeeRecordShowsCorrectDetails() {
        list.open();
        list.setEmployeeNameFilter(fullName);
        list.clickSearch();
        list.openEmployeeByLastName(lastName);

        details.waitForRecordToLoad();
        details.verifyEmployeeDetailsPageLoaded();
        details.verifyOpenedRecordIs(empNumber);
        details.verifyRecordHolds(firstName, middleName, lastName, employeeId);
        details.verifyTabsAreAvailable("Job", "Contact Details");

        checkpoint("06-created-employee-record");
    }

    @Test(priority = 5, groups = {"regression"}, dependsOnMethods = "verifyCreatedEmployeeRecordShowsCorrectDetails",
            description = "Employee fields can be edited and the update is confirmed")
    @Story("Employee edit")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Step 5: change first name and employee id to new values, save, and verify both the "
            + "success notification and the values on screen.")
    public void verifyUserCanEditEmployeeInformation() {
        details.open(empNumber);
        details.verifyEditChangesValues(updatedFirstName, updatedEmployeeId);

        details.setFirstName(updatedFirstName);
        details.setEmployeeId(updatedEmployeeId);
        details.savePersonalDetails();
        details.verifyUpdateConfirmed();

        details.dismissToast();
        details.verifyRecordShows(updatedFirstName, updatedEmployeeId);

        checkpoint("07-employee-updated");
    }

    @Test(priority = 6, groups = {"regression"}, dependsOnMethods = "verifyUserCanEditEmployeeInformation",
            description = "The updated values survive a page refresh")
    @Story("Employee edit is persisted")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Step 6a: reload the record and confirm the edit did not revert.")
    public void verifyUpdatedInformationSurvivesRefresh() {
        details.open(empNumber);
        details.reload();

        details.verifyRecordShows(updatedFirstName, updatedEmployeeId);
        details.verifyRecordDidNotRevertTo(firstName);

        checkpoint("08-update-persisted-after-refresh");
    }

    @Test(priority = 7, groups = {"regression"}, dependsOnMethods = "verifyUpdatedInformationSurvivesRefresh",
            description = "The updated values survive navigating away and back")
    @Story("Employee edit is persisted")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Step 6b: leave the employee record entirely, return to it, and confirm the update "
            + "is still there -- including in the employee list, which reads from a different query.")
    public void verifyUpdatedInformationSurvivesNavigationAway() {
        details.open(empNumber);
        details.navigateAwayAndReturn();
        details.verifyRecordShows(updatedFirstName, updatedEmployeeId);

        list.open();
        list.setEmployeeNameFilter(updatedFullName);
        list.clickSearch();
        list.verifyRowWasUpdated(lastName, updatedFirstName, updatedEmployeeId);

        checkpoint("09-update-persisted-after-navigation");
    }

    @Test(priority = 8, groups = {"regression"},
            description = "Two filter criteria applied together return only matching data")
    @Story("Employee list filtering")
    @Severity(SeverityLevel.NORMAL)
    @Description("Step 7a: filter by Employment Status AND Sub Unit, then verify the DATA in every "
            + "returned row honours both criteria.")
    public void verifyUserCanFilterEmployeeListByTwoCriteria() {
        list.open();
        list.verifyFilteringByStatusAndSubUnit();

        checkpoint("10-employee-list-filtered");
    }

    @Test(priority = 9, groups = {"regression"}, dependsOnMethods = "verifyUserCanFilterEmployeeListByTwoCriteria",
            description = "Clearing the filters restores the full result set")
    @Story("Employee list filtering")
    @Severity(SeverityLevel.NORMAL)
    @Description("Step 7b: Reset clears every criterion and the unfiltered total returns.")
    public void verifyResetRestoresFullResultSet() {
        list.open();
        list.verifyResetRestoresFullResultSet();

        checkpoint("11-employee-list-filters-reset");
    }
}
