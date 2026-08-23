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
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Optional;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
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
            description = "Searching by name returns only employees matching that name")
    @Story("Employee search")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Step 2: an employee that already exists is located by name and every returned row "
            + "is verified against the search criteria.")
    public void verifyUserCanSearchForAnExistingEmployee() {
        list.open();
        list.verifyEmployeeListPageLoaded();

        int totalBefore = list.verifyListHasRecords();
        PimEmployeeListPage.EmployeeRow seed = pickSeedOfferedByAutocomplete();

        list.clickSearch();
        list.verifyEveryRowMatches(seed.lastName(), totalBefore);
        list.verifyRowMatches(seed.lastName(), seed.id(), seed.firstAndMiddleName());

        checkpoint("03-employee-search-results");
    }

    @Test(priority = 2, groups = {"smoke", "regression"},
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

    private PimEmployeeListPage.EmployeeRow pickSeedOfferedByAutocomplete() {
        for (PimEmployeeListPage.EmployeeRow candidate : pickSearchableEmployees(list.getAllRows())) {
            if (list.setEmployeeNameFilter(candidate.fullName())) {
                return candidate;
            }
            list.open();
        }
        throw new AssertionError("No existing employee could be located through the name autocomplete");
    }

    private List<PimEmployeeListPage.EmployeeRow> pickSearchableEmployees(
            List<PimEmployeeListPage.EmployeeRow> rows) {
        List<PimEmployeeListPage.EmployeeRow> candidates = rows.stream()
                .filter(row -> row.lastName().matches("[A-Za-z]{3,}"))
                .filter(row -> row.firstAndMiddleName().matches("[A-Za-z][A-Za-z ]{2,}"))
                .limit(5)
                .toList();
        if (candidates.isEmpty()) {
            throw new SkipException(
                    "No employee with an alphabetic name exists in this environment to search for.");
        }
        return candidates;
    }

    @Test(priority = 8, groups = {"regression"},
            description = "Two filter criteria applied together return only matching data")
    @Story("Employee list filtering")
    @Severity(SeverityLevel.NORMAL)
    @Description("Step 7a: filter by Employment Status AND Job Title, then verify the DATA in every "
            + "returned row honours both criteria.")
    public void verifyUserCanFilterEmployeeListByTwoCriteria() {
        list.open();

        List<String> statuses = list.getAvailableEmploymentStatuses();
        assertFalse(statuses.isEmpty(), "The environment should define employment statuses to filter by");

        List<SecondCriterion> secondCriteria = availableSecondCriteria(list);

        String chosenStatus = null;
        String chosenSecondValue = null;
        SecondCriterion second = null;
        List<PimEmployeeListPage.EmployeeRow> statusRows = List.of();

        outer:
        for (SecondCriterion criterion : secondCriteria) {
            for (String status : statuses) {
                list.open();
                list.setEmploymentStatusFilter(status);
                list.clickSearch();

                List<PimEmployeeListPage.EmployeeRow> rows = list.getAllRows();
                if (rows.isEmpty()) {
                    continue;
                }

                Optional<String> candidate = rows.stream()
                        .map(criterion.valueOf())
                        .filter(value -> !value.isBlank())
                        .filter(value -> criterion.options().stream()
                                .anyMatch(option -> option.equalsIgnoreCase(value)))
                        .findFirst();

                if (candidate.isPresent()) {
                    second = criterion;
                    chosenStatus = status;
                    chosenSecondValue = candidate.get();
                    statusRows = rows;
                    break outer;
                }
            }
        }
        assertNotNull(second, "No second filter criterion had data to filter on in this environment");
        assertNotNull(chosenStatus, "No employment status returned employees to filter on");
        assertNotNull(chosenSecondValue, "No filterable second-criterion value was found");
        System.out.println("Second filter criterion in use: " + second.label());

        for (PimEmployeeListPage.EmployeeRow row : statusRows) {
            assertEquals(row.employmentStatus(), chosenStatus,
                    "Every row should match the Employment Status filter");
        }

        second.apply().accept(chosenSecondValue);
        list.clickSearch();

        List<PimEmployeeListPage.EmployeeRow> filtered = list.getAllRows();
        assertFalse(filtered.isEmpty(),
                "Filtering by a combination that exists should return at least one row");

        for (PimEmployeeListPage.EmployeeRow row : filtered) {
            assertEquals(row.employmentStatus(), chosenStatus,
                    "Every row should match the Employment Status filter");
            assertEquals(second.valueOf().apply(row), chosenSecondValue,
                    "Every row should match the " + second.label() + " filter");
        }
        assertTrue(filtered.size() <= statusRows.size(),
                "Adding a second criterion must not widen the result set");

        System.out.println("Filtered by status='" + chosenStatus + "' and " + second.label()
                + "='" + chosenSecondValue + "' -> " + filtered.size() + " rows");
        checkpoint("10-employee-list-filtered");
    }

    private record SecondCriterion(String label, List<String> options,
                                   java.util.function.Consumer<String> apply,
                                   java.util.function.Function<PimEmployeeListPage.EmployeeRow, String> valueOf) {
    }

    private List<SecondCriterion> availableSecondCriteria(PimEmployeeListPage list) {
        List<SecondCriterion> candidates = List.of(
                new SecondCriterion("Job Title", list.getAvailableJobTitles(),
                        list::setJobTitleFilter, PimEmployeeListPage.EmployeeRow::jobTitle),
                new SecondCriterion("Sub Unit", list.getAvailableSubUnits(),
                        list::setSubUnitFilter, PimEmployeeListPage.EmployeeRow::subUnit));

        List<SecondCriterion> usable = candidates.stream()
                .filter(criterion -> !criterion.options().isEmpty())
                .toList();
        if (usable.isEmpty()) {
            throw new SkipException(
                    "This environment defines neither job titles nor sub units, so there is no "
                            + "second criterion available to filter by.");
        }
        return usable;
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
