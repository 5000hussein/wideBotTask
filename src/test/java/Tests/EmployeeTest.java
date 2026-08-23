package Tests;

import Pages.AddEmployeePage;
import Pages.EmployeeDetailsPage;
import Pages.PimEmployeeListPage;
import Util.Config;
import Util.Helper;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

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

    private static final int RESET_COUNT_TOLERANCE = 25;

    private AddEmployeePage addEmployee;
    private PimEmployeeListPage list;
    private String empNumber;

    @BeforeClass(alwaysRun = true, dependsOnMethods = "setUp")
    public void signIn() {
        loginToDashboard();

        addEmployee = new AddEmployeePage();
        list = new PimEmployeeListPage();
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

        EmployeeDetailsPage details = new EmployeeDetailsPage();
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

        EmployeeDetailsPage details = new EmployeeDetailsPage();
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
        EmployeeDetailsPage details = openCreatedEmployeeRecord();
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
        EmployeeDetailsPage details = openCreatedEmployeeRecord();
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
        EmployeeDetailsPage details = openCreatedEmployeeRecord();
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
        int unfilteredTotal = list.getRecordCount();
        assertTrue(unfilteredTotal > 0, "The unfiltered list should report a record count");

        String excludedLastName = list.getAllRows().stream()
                .map(PimEmployeeListPage.EmployeeRow::lastName)
                .filter(name -> !name.isBlank())
                .findFirst()
                .orElse("");

        String chosenStatus = null;
        int filteredTotal = -1;
        for (String status : list.getAvailableEmploymentStatuses()) {
            list.setEmploymentStatusFilter(status);
            list.clickSearch();
            int count = list.getRecordCount();
            if (count > 0 && count < unfilteredTotal) {
                chosenStatus = status;
                filteredTotal = count;
                break;
            }
            list.clickReset();
        }
        assertNotNull(chosenStatus, "No employment status produced a narrowed result set");
        assertTrue(filteredTotal < unfilteredTotal,
                "The filter should return fewer records than the unfiltered list");

        list.clickReset();

        assertEquals(list.getSelectedEmploymentStatus(), "-- Select --",
                "Reset should clear the Employment Status criterion");

        int afterReset = list.getRecordCount();
        assertTrue(afterReset > filteredTotal,
                "Reset should return more records (" + afterReset + ") than the filtered set ("
                        + filteredTotal + ")");

        int drift = Math.abs(afterReset - unfilteredTotal);
        assertTrue(drift <= RESET_COUNT_TOLERANCE,
                "Reset should restore the unfiltered result set: expected about " + unfilteredTotal
                        + " but found " + afterReset + " (drift of " + drift
                        + " exceeds the " + RESET_COUNT_TOLERANCE + " allowed for concurrent activity)");

        if (!excludedLastName.isBlank()) {
            assertTrue(list.findRowByLastName(excludedLastName).isPresent(),
                    "The employee '" + excludedLastName + "' excluded by the filter should be listed again");
        }

        System.out.println("Reset: filtered=" + filteredTotal + " -> restored=" + afterReset
                + " (baseline " + unfilteredTotal + ")");
        checkpoint("11-employee-list-filters-reset");
    }


    private EmployeeDetailsPage openCreatedEmployeeRecord() {
        driver.get(Config.getInstance().getBaseUrl()
                + "/web/index.php/pim/viewPersonalDetails/empNumber/" + empNumber);
        Util.Waits.waitForLoaderToDisappear(driver);
        EmployeeDetailsPage details = new EmployeeDetailsPage();
        details.waitForRecordToLoad();
        details.verifyEmployeeDetailsPageLoaded();
        return details;
    }
}
