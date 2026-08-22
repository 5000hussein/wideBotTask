package Tests;

import Pages.AddEmployeePage;
import Pages.EmployeeDetailsPage;
import Pages.PimEmployeeListPage;
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

import java.util.List;
import java.util.Optional;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

@Epic("OrangeHRM")
@Feature("PIM - Employee Management")
public class EmployeeTest extends BaseTest {
    private DataFactory.Employee employee;
    private DataFactory.Employee editedEmployee;
    private String empNumber;

    private static final int RESET_COUNT_TOLERANCE = 25;

    @BeforeClass(alwaysRun = true, dependsOnMethods = "setUp")
    public void signIn() {
        loginToDashboard();
        employee = DataFactory.newEmployee();
    }

    @Test(priority = 1, groups = {"smoke", "regression"},
            description = "Searching by name returns only employees matching that name")
    @Story("Employee search")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Step 2: an employee that already exists is located by name and every returned row "
            + "is verified against the search criteria.")
    public void searchExistingEmployeeByName() {
        PimEmployeeListPage list = new PimEmployeeListPage().open();
        assertTrue(list.isDisplayed(), "Employee List page should be displayed");

        int totalBefore = list.getRecordCount();
        assertTrue(totalBefore > 0, "The environment should contain employees to search for");

        PimEmployeeListPage.EmployeeRow seed = null;
        for (PimEmployeeListPage.EmployeeRow candidate : pickSearchableEmployees(list.getAllRows())) {
            if (list.setEmployeeNameFilter(candidate.fullName())) {
                seed = candidate;
                break;
            }
            System.out.println("Seed candidate not offered by the autocomplete, trying the next: "
                    + candidate.fullName());
            list.open();
        }
        assertNotNull(seed, "No existing employee could be located through the name autocomplete");
        String seedFullName = seed.fullName();

        list.clickSearch();

        List<PimEmployeeListPage.EmployeeRow> results = list.getAllRows();
        assertFalse(results.isEmpty(), "Search results should be displayed for '" + seedFullName + "'");
        assertTrue(list.getRecordCount() < totalBefore,
                "A name filter should narrow the result set below the unfiltered total");

        for (PimEmployeeListPage.EmployeeRow row : results) {
            assertEquals(row.lastName(), seed.lastName(),
                    "Every returned row should carry the searched last name");
        }
        Optional<PimEmployeeListPage.EmployeeRow> found = list.findRowByLastName(seed.lastName());
        assertTrue(found.isPresent(), "The searched employee should be present in the results");

        assertEquals(found.get().firstAndMiddleName(), seed.firstAndMiddleName(),
                "First (& middle) name should match the record that was searched for");
        assertEquals(found.get().id(), seed.id(), "Employee Id should match");

        checkpoint("03-employee-search-results");
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

    @Test(priority = 2, groups = {"smoke", "regression"},
            description = "A new employee is created from generated data")
    @Story("Employee creation")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Step 3: create an employee with runtime-generated data and verify the success "
            + "notification and the generated employee id.")
    public void createNewEmployee() {
        AddEmployeePage addEmployee = new AddEmployeePage().open();
        assertTrue(addEmployee.isDisplayed(), "Add Employee form should be displayed");

        String prefilledId = addEmployee.getPrefilledEmployeeId();
        assertFalse(prefilledId.isBlank(), "OrangeHRM should pre-populate a suggested Employee Id");

        EmployeeDetailsPage details = addEmployee.saveAndOpenRecord(employee);
        registerForCleanup(employee);

        assertTrue(details.wasCreationConfirmed(),
                "A success notification should be displayed on save");
        assertTrue(details.getCreationToastText().toLowerCase().contains("success"),
                "Notification should confirm success but was: '"
                        + details.getCreationToastText() + "'");

        empNumber = details.getEmpNumberFromUrl();
        assertFalse(empNumber.isBlank(),
                "The created record should have a server-assigned employee number in its URL");
        assertEquals(details.getEmployeeId(), employee.employeeId(),
                "The Employee Id we supplied should be stored on the record");

        checkpoint("04-employee-created");
        details.dismissToast();
    }

    @Test(priority = 3, groups = {"regression"}, dependsOnMethods = "createNewEmployee",
            description = "The created employee can be found again in the employee list")
    @Story("Employee creation is persisted")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Step 4a: prove persistence by finding the new employee through the employee list, "
            + "not by trusting the creation toast.")
    public void createdEmployeeIsFoundInEmployeeList() {
        PimEmployeeListPage list = new PimEmployeeListPage().open();

        assertTrue(list.setEmployeeNameFilter(employee.fullName()),
                "The new employee should be offered by the name autocomplete");
        list.clickSearch();

        assertEquals(list.getRecordCount(), 1,
                "The generated name is unique, so exactly one record should match");

        Optional<PimEmployeeListPage.EmployeeRow> row = list.findRowByLastName(employee.lastName());
        assertTrue(row.isPresent(), "The created employee should appear in the employee list");
        assertEquals(row.get().id(), employee.employeeId(), "Employee Id should match what was entered");
        assertEquals(row.get().firstAndMiddleName(), employee.firstAndMiddleName(),
                "First (& middle) name should match what was entered");

        checkpoint("05-created-employee-in-list");
    }

    @Test(priority = 4, groups = {"regression"}, dependsOnMethods = "createdEmployeeIsFoundInEmployeeList",
            description = "The created employee record opens and shows the data it was created with")
    @Story("Employee creation is persisted")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Step 4b: open the record from the list and verify name, employee id and that the "
            + "detail tabs are reachable.")
    public void openCreatedEmployeeRecordAndValidateDetails() {
        PimEmployeeListPage list = new PimEmployeeListPage().open();
        list.setEmployeeNameFilter(employee.fullName());
        list.clickSearch();

        EmployeeDetailsPage details = list.openEmployeeByLastName(employee.lastName());

        assertTrue(details.isDisplayed(), "The employee record should be displayed");
        assertEquals(details.getEmpNumberFromUrl(), empNumber,
                "Opening from the list should reach the same record that creation returned");

        assertEquals(details.getFirstName(), employee.firstName(), "First name should match");
        assertEquals(details.getMiddleName(), employee.middleName(), "Middle name should match");
        assertEquals(details.getLastName(), employee.lastName(), "Last name should match");

        assertEquals(details.getEmployeeId(), employee.employeeId(), "Employee Id should be populated");

        String banner = details.getDisplayedName();
        assertTrue(banner.contains(employee.firstName()) && banner.contains(employee.lastName()),
                "The record header should show the employee's name but was: '" + banner + "'");

        assertTrue(details.isTabAvailable("Job"), "The Job tab should be accessible");
        assertTrue(details.isTabAvailable("Contact Details"), "The Contact Details tab should be accessible");

        checkpoint("06-created-employee-record");
    }

    @Test(priority = 5, groups = {"regression"}, dependsOnMethods = "openCreatedEmployeeRecordAndValidateDetails",
            description = "Employee fields can be edited and the update is confirmed")
    @Story("Employee edit")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Step 5: change first name and employee id to new values, save, and verify both the "
            + "success notification and the values on screen.")
    public void editEmployeeInformation() {
        EmployeeDetailsPage details = openCreatedEmployeeRecord();

        String originalFirstName = details.getFirstName();
        String originalEmployeeId = details.getEmployeeId();

        editedEmployee = employee
                .withFirstName(DataFactory.updatedFirstName())
                .withEmployeeId(DataFactory.updatedEmployeeId());

        assertNotEquals(editedEmployee.firstName(), originalFirstName,
                "The edit must use a value different from the original");
        assertNotEquals(editedEmployee.employeeId(), originalEmployeeId,
                "The edit must use an employee id different from the original");

        details.setFirstName(editedEmployee.firstName())
                .setEmployeeId(editedEmployee.employeeId())
                .savePersonalDetails();

        assertTrue(details.wasLastActionSuccessful(), "A success notification should confirm the update");
        assertTrue(details.getLastToastText().toLowerCase().contains("success"),
                "Update notification should confirm success but was: '" + details.getLastToastText() + "'");

        details.dismissToast();

        assertEquals(details.getFirstName(), editedEmployee.firstName(),
                "The updated first name should be visible after saving");
        assertEquals(details.getEmployeeId(), editedEmployee.employeeId(),
                "The updated employee id should be visible after saving");

        checkpoint("07-employee-updated");
    }

    @Test(priority = 6, groups = {"regression"}, dependsOnMethods = "editEmployeeInformation",
            description = "The updated values survive a page refresh")
    @Story("Employee edit is persisted")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Step 6a: reload the record and confirm the edit did not revert.")
    public void updatedInformationSurvivesRefresh() {
        EmployeeDetailsPage details = openCreatedEmployeeRecord().reload();

        assertEquals(details.getFirstName(), editedEmployee.firstName(),
                "The updated first name should survive a refresh");
        assertEquals(details.getEmployeeId(), editedEmployee.employeeId(),
                "The updated employee id should survive a refresh");
        assertNotEquals(details.getFirstName(), employee.firstName(),
                "The record must not revert to its pre-edit first name");

        checkpoint("08-update-persisted-after-refresh");
    }

    @Test(priority = 7, groups = {"regression"}, dependsOnMethods = "updatedInformationSurvivesRefresh",
            description = "The updated values survive navigating away and back")
    @Story("Employee edit is persisted")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Step 6b: leave the employee record entirely, return to it, and confirm the update "
            + "is still there -- including in the employee list, which reads from a different query.")
    public void updatedInformationSurvivesNavigationAway() {
        EmployeeDetailsPage details = openCreatedEmployeeRecord().navigateAwayAndReturn();

        assertEquals(details.getFirstName(), editedEmployee.firstName(),
                "The updated first name should still be present after navigating away and back");
        assertEquals(details.getEmployeeId(), editedEmployee.employeeId(),
                "The updated employee id should still be present after navigating away and back");

        PimEmployeeListPage list = new PimEmployeeListPage().open();
        list.setEmployeeNameFilter(editedEmployee.fullName());
        list.clickSearch();

        Optional<PimEmployeeListPage.EmployeeRow> row = list.findRowByLastName(editedEmployee.lastName());
        assertTrue(row.isPresent(), "The edited employee should still be findable in the employee list");
        assertEquals(row.get().id(), editedEmployee.employeeId(),
                "The employee list should report the updated employee id");
        assertTrue(row.get().firstAndMiddleName().contains(editedEmployee.firstName()),
                "The employee list should report the updated first name");

        checkpoint("09-update-persisted-after-navigation");
    }

    @Test(priority = 8, groups = {"regression"},
            description = "Two filter criteria applied together return only matching data")
    @Story("Employee list filtering")
    @Severity(SeverityLevel.NORMAL)
    @Description("Step 7a: filter by Employment Status AND Job Title, then verify the DATA in every "
            + "returned row honours both criteria.")
    public void filterEmployeeListByTwoCriteria() {
        PimEmployeeListPage list = new PimEmployeeListPage().open();

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

    @Test(priority = 9, groups = {"regression"}, dependsOnMethods = "filterEmployeeListByTwoCriteria",
            description = "Clearing the filters restores the full result set")
    @Story("Employee list filtering")
    @Severity(SeverityLevel.NORMAL)
    @Description("Step 7b: Reset clears every criterion and the unfiltered total returns.")
    public void resetRestoresFullResultSet() {
        PimEmployeeListPage list = new PimEmployeeListPage().open();
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
        driver.get(Util.ConfigReader.baseUrl()
                + "/web/index.php/pim/viewPersonalDetails/empNumber/" + empNumber);
        Util.Waits.waitForLoaderToDisappear(driver);
        EmployeeDetailsPage details = new EmployeeDetailsPage().waitForRecordToLoad();
        assertTrue(details.isDisplayed(), "The employee record should be reachable by its record number");
        return details;
    }
}
