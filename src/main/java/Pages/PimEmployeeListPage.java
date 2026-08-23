package Pages;

import Util.ElementsActions;
import Util.Validations;
import Util.Waits;
import org.testng.SkipException;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PimEmployeeListPage extends BasePage {
    private static final String PATH = "/web/index.php/pim/viewEmployeeList";

    //Locators
    private final By employeeNameInput = By.xpath(
            "//div[contains(@class,'oxd-input-group')][.//label[normalize-space()='Employee Name']]"
                    + "//input[@placeholder='Type for hints...']");
    private final By employeeIdInput = inputByLabel("Employee Id");
    private final By employmentStatusDropdown = dropdownByLabel("Employment Status");
    private final By subUnitDropdown = dropdownByLabel("Sub Unit");

    private final By searchButton = By.cssSelector("button[type='submit']");
    private final By resetButton = By.cssSelector("button[type='reset']");

    private final By tableCard = By.cssSelector(".oxd-table-card");
    private final By tableCell = By.cssSelector(".oxd-table-cell");
    private final By recordsFoundText = By.xpath("//span[contains(@class,'oxd-text--span')][contains(.,'Record')]");
    private final By noRecordsFound = By.xpath(
            "//div[contains(@class,'oxd-toast')][contains(.,'No Records Found')]"
                    + " | //span[contains(normalize-space(),'No Records Found')]");

    private static final int RESET_COUNT_TOLERANCE = 25;

    private static final int COL_ID = 1;
    private static final int COL_FIRST_MIDDLE_NAME = 2;
    private static final int COL_LAST_NAME = 3;
    private static final int COL_EMPLOYMENT_STATUS = 5;
    private static final int COL_SUB_UNIT = 6;

    //A complete row carries every column through Supervisor
    private static final int MIN_ROW_CELLS = 8;

    //PageActions
    @Step("Open PIM > Employee List")
    public void open() {
        openPath(PATH);
        Waits.waitForElementVisible(driver, searchButton);
        Waits.waitForLoaderToDisappear(driver);
    }


    @Step("Filter by employee name: {fullName}")
    public boolean setEmployeeNameFilter(String fullName) {
        return selectEmployeeByName(employeeNameInput, fullName);
    }

    @Step("Filter by employment status: {status}")
    public void setEmploymentStatusFilter(String status) {
        ElementsActions.selectFromOxdDropdown(driver, employmentStatusDropdown, status);
    }

    @Step("Filter by sub unit: {subUnit}")
    public void setSubUnitFilter(String subUnit) {
        ElementsActions.selectFromOxdDropdown(driver, subUnitDropdown, subUnit);
    }

    public String getSelectedEmploymentStatus() {
        return ElementsActions.getOxdDropdownValue(driver, employmentStatusDropdown);
    }

    public List<String> getAvailableEmploymentStatuses() {
        return ElementsActions.getOxdDropdownRealOptions(driver, employmentStatusDropdown);
    }

    @Step("Run the search")
    public void clickSearch() {
        ElementsActions.dismissToast(driver);
        ElementsActions.clickElement(driver, searchButton);
        waitForResultsToSettle();
    }

    @Step("Reset the filters")
    public void clickReset() {
        ElementsActions.clickElement(driver, resetButton);
        waitForResultsToSettle();
    }

    private void waitForResultsToSettle() {
        Waits.waitForLoaderToDisappear(driver);
        try {
            Waits.retryOnStale(driver, d -> {
                if (isNoResultsIndicated(d)) {
                    return true;
                }
                return d.findElements(tableCard).stream()
                        .anyMatch(card -> card.findElements(tableCell).size() >= MIN_ROW_CELLS);
            });
        } catch (org.openqa.selenium.TimeoutException ignored) {
        }
    }

    private boolean isNoResultsIndicated(org.openqa.selenium.WebDriver d) {
        return d.findElements(noRecordsFound).stream().anyMatch(WebElement::isDisplayed);
    }

    public int getRecordCount() {
        if (!Waits.isElementVisible(driver, recordsFoundText, 8)) {
            return driver.findElements(tableCard).isEmpty() ? 0 : -1;
        }
        String text = ElementsActions.getText(driver, recordsFoundText);
        Matcher matcher = Pattern.compile("\\((\\d+)\\)").matcher(text);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return text.contains("No Records") ? 0 : -1;
    }

    public List<EmployeeRow> getAllRows() {
        return Waits.retryOnStale(driver, d -> {
            List<EmployeeRow> rows = new ArrayList<>();
            for (WebElement card : d.findElements(tableCard)) {
                List<WebElement> cells = card.findElements(tableCell);
                if (cells.size() < MIN_ROW_CELLS) {
                    continue;
                }
                rows.add(new EmployeeRow(
                        text(cells, COL_ID),
                        text(cells, COL_FIRST_MIDDLE_NAME),
                        text(cells, COL_LAST_NAME),
                        text(cells, COL_EMPLOYMENT_STATUS),
                        text(cells, COL_SUB_UNIT)));
            }
            return rows;
        });
    }

    public Optional<EmployeeRow> findRowByLastName(String lastName) {
        return getAllRows().stream()
                .filter(row -> row.lastName().equalsIgnoreCase(lastName))
                .findFirst();
    }

    private WebElement rowElementByLastName(String lastName) {
        By row = By.xpath("//div[contains(@class,'oxd-table-card')]"
                + "[.//div[contains(@class,'oxd-table-cell')][normalize-space()=" + quote(lastName) + "]]");
        return Waits.waitForElementVisible(driver, row);
    }

    @Step("Open the employee record for last name {lastName}")
    public void openEmployeeByLastName(String lastName) {
        WebElement row = rowElementByLastName(lastName);

        WebElement nameCell = row.findElements(tableCell).get(COL_LAST_NAME);
        ElementsActions.clickElement(driver, nameCell);
        Waits.waitForUrlContains(driver, "viewPersonalDetails");
        Waits.waitForLoaderToDisappear(driver);
    }

    @Step("Delete the employee with last name {lastName}")
    public void deleteEmployeeByLastName(String lastName) {
        WebElement row = rowElementByLastName(lastName);

        WebElement deleteButton = row.findElements(By.cssSelector("button.oxd-icon-button")).stream()
                .filter(button -> !button.findElements(By.cssSelector("i.bi-trash")).isEmpty())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No delete action found on the row for last name " + lastName));
        ElementsActions.clickElement(driver, deleteButton);

        By confirmDelete = By.xpath("//button[contains(normalize-space(),'Yes, Delete')]");
        ElementsActions.clickElement(driver, confirmDelete);
        Waits.waitForLoaderToDisappear(driver);
    }

    private static String text(List<WebElement> cells, int index) {
        return cells.get(index).getText().trim();
    }

    public record EmployeeRow(String id, String firstAndMiddleName, String lastName,
                              String employmentStatus, String subUnit) {
    }

    //PageAssertions
    @Step("Narrow the list with a filter, reset it, and verify the full set returns")
    public void verifyResetRestoresFullResultSet() {
        int unfilteredTotal = getRecordCount();
        Validations.validateTrue(unfilteredTotal > 0, "The unfiltered list should report a record count");

        String excludedLastName = getAllRows().stream()
                .map(EmployeeRow::lastName)
                .filter(name -> !name.isBlank())
                .findFirst()
                .orElse("");

        int filteredTotal = narrowWithAnyStatus(unfilteredTotal);
        clickReset();

        Validations.validateEquals(getSelectedEmploymentStatus(), "-- Select --",
                "Reset should clear the Employment Status criterion");

        int afterReset = getRecordCount();
        Validations.validateTrue(afterReset > filteredTotal,
                "Reset should return more records (" + afterReset + ") than the filtered set ("
                        + filteredTotal + ")");

        int drift = Math.abs(afterReset - unfilteredTotal);
        Validations.validateTrue(drift <= RESET_COUNT_TOLERANCE,
                "Reset should restore the unfiltered result set: expected about " + unfilteredTotal
                        + " but found " + afterReset + " (drift of " + drift
                        + " exceeds the " + RESET_COUNT_TOLERANCE + " allowed for concurrent activity)");

        if (!excludedLastName.isBlank()) {
            Validations.validateTrue(findRowByLastName(excludedLastName).isPresent(),
                    "The employee '" + excludedLastName + "' excluded by the filter should be listed again");
        }
    }

    private int narrowWithAnyStatus(int unfilteredTotal) {
        for (String status : getAvailableEmploymentStatuses()) {
            setEmploymentStatusFilter(status);
            clickSearch();

            int count = getRecordCount();
            if (count > 0 && count < unfilteredTotal) {
                return count;
            }
            clickReset();
        }
        throw new AssertionError("No employment status produced a narrowed result set");
    }
    @Step("Verify {fullName} is offered by the name autocomplete")
    public void verifyEmployeeIsOffered(String fullName) {
        Validations.validateTrue(setEmployeeNameFilter(fullName),
                "The employee should be offered by the name autocomplete: " + fullName);
    }

    @Step("Verify the list holds employees to search for")
    public int verifyListHasRecords() {
        int total = getRecordCount();
        Validations.validateTrue(total > 0, "The environment should contain employees to search for");
        return total;
    }

    @Step("Verify exactly one record matched")
    public void verifyExactlyOneRecordFound() {
        Validations.validateEquals(getRecordCount(), 1,
                "The test employee name is unique, so exactly one record should match");
    }

    @Step("Verify the row for {lastName} holds id {employeeId}")
    public void verifyRowMatches(String lastName, String employeeId, String firstAndMiddleName) {
        EmployeeRow row = findRowByLastName(lastName)
                .orElseThrow(() -> new AssertionError(
                        "The employee should appear in the employee list: " + lastName));

        Validations.validateEquals(row.id(), employeeId, "Employee Id should match what was entered");
        Validations.validateEquals(row.firstAndMiddleName(), firstAndMiddleName,
                "First (& middle) name should match what was entered");
    }

    @Step("Verify the row for {lastName} reports the updated {firstName} and {employeeId}")
    public void verifyRowWasUpdated(String lastName, String firstName, String employeeId) {
        EmployeeRow row = findRowByLastName(lastName)
                .orElseThrow(() -> new AssertionError(
                        "The edited employee should still be findable in the employee list: " + lastName));

        Validations.validateEquals(row.id(), employeeId,
                "The employee list should report the updated employee id");
        Validations.validateContains(row.firstAndMiddleName(), firstName,
                "The employee list should report the updated first name");
    }

    @Step("Filter by Employment Status and Sub Unit and verify every row honours both")
    public void verifyFilteringByStatusAndSubUnit() {
        List<String> statuses = getAvailableEmploymentStatuses();
        Validations.validateFalse(statuses.isEmpty(),
                "The environment should define employment statuses to filter by");

        for (String status : statuses) {
            open();
            setEmploymentStatusFilter(status);
            clickSearch();

            List<EmployeeRow> statusRows = getAllRows();
            Optional<String> subUnit = statusRows.stream()
                    .map(EmployeeRow::subUnit)
                    .filter(value -> !value.isBlank())
                    .findFirst();
            if (subUnit.isEmpty()) {
                continue;
            }

            for (EmployeeRow row : statusRows) {
                Validations.validateEquals(row.employmentStatus(), status,
                        "Every row should match the Employment Status filter");
            }

            setSubUnitFilter(subUnit.get());
            clickSearch();

            List<EmployeeRow> filtered = getAllRows();
            Validations.validateFalse(filtered.isEmpty(),
                    "Filtering by a combination that exists should return at least one row");
            for (EmployeeRow row : filtered) {
                Validations.validateEquals(row.employmentStatus(), status,
                        "Every row should match the Employment Status filter");
                Validations.validateEquals(row.subUnit(), subUnit.get(),
                        "Every row should match the Sub Unit filter");
            }
            Validations.validateTrue(filtered.size() <= statusRows.size(),
                    "Adding a second criterion must not widen the result set");
            return;
        }

        throw new SkipException(
                "No employment status in this environment returned employees carrying a sub unit, "
                        + "so there is no two-criteria combination to filter on.");
    }

    @Step("Verify every result carries the searched last name {lastName}")
    public void verifyEveryRowMatches(String lastName, int unfilteredTotal) {
        List<EmployeeRow> results = getAllRows();
        Validations.validateFalse(results.isEmpty(), "Search results should be displayed for " + lastName);
        Validations.validateTrue(getRecordCount() < unfilteredTotal,
                "A name filter should narrow the result set below the unfiltered total");

        for (EmployeeRow row : results) {
            Validations.validateEquals(row.lastName(), lastName,
                    "Every returned row should carry the searched last name");
        }
    }

    @Step("Verify no employee is offered for {name}")
    public void verifyNoEmployeeNamed(String name) {
        Validations.validateFalse(setEmployeeNameFilter(name),
                "No employee should exist for '" + name + "', but the search offered a match");
    }

    public void verifyEmployeeListPageLoaded() {
        Validations.validateTrue(
                ElementsActions.isDisplayed(driver, searchButton)
                        && ElementsActions.isDisplayed(driver, employeeIdInput),
                "Employee List page did not load");
    }
}
