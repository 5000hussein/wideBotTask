package Pages;

import Util.ElementsActions;
import Util.Waits;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PIM &gt; Employee List.
 *
 * Rows are addressed by content, never by index. Every lookup goes through
 * {@link #findRowByLastName(String)}, which scans the rendered rows for a
 * matching last name -- so the tests keep working when the result order or the
 * page size changes, and a search that returns the wrong employee fails loudly
 * instead of quietly asserting against whatever happened to be in row 2.
 */
public class PimEmployeeListPage extends BasePage {

    private static final String PATH = "/web/index.php/pim/viewEmployeeList";

    private final By employeeNameInput = By.xpath(
            "//div[contains(@class,'oxd-input-group')][.//label[normalize-space()='Employee Name']]"
                    + "//input[@placeholder='Type for hints...']");
    private final By employeeIdInput = inputByLabel("Employee Id");
    private final By employmentStatusDropdown = dropdownByLabel("Employment Status");
    private final By includeDropdown = dropdownByLabel("Include");
    private final By jobTitleDropdown = dropdownByLabel("Job Title");
    private final By subUnitDropdown = dropdownByLabel("Sub Unit");

    private final By searchButton = By.cssSelector("button[type='submit']");
    private final By resetButton = By.cssSelector("button[type='reset']");
    private final By addButton = By.xpath("//button[normalize-space()='Add']");

    private final By tableCard = By.cssSelector(".oxd-table-card");
    private final By tableCell = By.cssSelector(".oxd-table-cell");
    private final By recordsFoundText = By.xpath("//span[contains(@class,'oxd-text--span')][contains(.,'Record')]");
    private final By noRecordsToast = By.xpath("//div[contains(@class,'oxd-toast')][contains(.,'No Records Found')]");

    /** Column order verified against the live table header. */
    private static final int COL_ID = 1;
    private static final int COL_FIRST_MIDDLE_NAME = 2;
    private static final int COL_LAST_NAME = 3;
    private static final int COL_JOB_TITLE = 4;
    private static final int COL_EMPLOYMENT_STATUS = 5;
    private static final int COL_SUB_UNIT = 6;
    private static final int COL_SUPERVISOR = 7;

    @Step("Open PIM > Employee List")
    public PimEmployeeListPage open() {
        openPath(PATH);
        Waits.waitForElementVisible(driver, searchButton);
        Waits.waitForLoaderToDisappear(driver);
        return this;
    }

    public boolean isDisplayed() {
        return ElementsActions.isDisplayed(driver, searchButton)
                && ElementsActions.isDisplayed(driver, employeeIdInput);
    }

    // ------------------------------------------------------------- filters

    /**
     * Employee Name is an autocomplete that validates against its own suggestion
     * list -- typing without picking a hint leaves the field "Invalid" and the
     * search never runs.
     *
     * @return false when the application offered no matching employee
     */
    @Step("Filter by employee name: {fullName}")
    public boolean setEmployeeNameFilter(String fullName) {
        return selectEmployeeByName(employeeNameInput, fullName);
    }

    /** Types into Employee Name without selecting a hint -- for negative checks. */
    @Step("Type '{text}' into the employee name filter without picking a hint")
    public PimEmployeeListPage typeEmployeeNameOnly(String text) {
        ElementsActions.clearAndEnterText(driver, employeeNameInput, text);
        return this;
    }

    @Step("Filter by employee id: {employeeId}")
    public PimEmployeeListPage setEmployeeIdFilter(String employeeId) {
        ElementsActions.clearAndEnterText(driver, employeeIdInput, employeeId);
        return this;
    }

    @Step("Filter by employment status: {status}")
    public PimEmployeeListPage setEmploymentStatusFilter(String status) {
        ElementsActions.selectFromOxdDropdown(driver, employmentStatusDropdown, status);
        return this;
    }

    @Step("Filter by job title: {jobTitle}")
    public PimEmployeeListPage setJobTitleFilter(String jobTitle) {
        ElementsActions.selectFromOxdDropdown(driver, jobTitleDropdown, jobTitle);
        return this;
    }

    @Step("Filter by sub unit: {subUnit}")
    public PimEmployeeListPage setSubUnitFilter(String subUnit) {
        ElementsActions.selectFromOxdDropdown(driver, subUnitDropdown, subUnit);
        return this;
    }

    @Step("Filter by include: {include}")
    public PimEmployeeListPage setIncludeFilter(String include) {
        ElementsActions.selectFromOxdDropdown(driver, includeDropdown, include);
        return this;
    }

    public String getSelectedEmploymentStatus() {
        return ElementsActions.getOxdDropdownValue(driver, employmentStatusDropdown);
    }

    public String getSelectedJobTitle() {
        return ElementsActions.getOxdDropdownValue(driver, jobTitleDropdown);
    }

    public String getEmployeeIdFilterValue() {
        return ElementsActions.getValue(driver, employeeIdInput);
    }

    /** Selectable job titles, excluding the "-- Select --" placeholder. */
    public List<String> getAvailableJobTitles() {
        return ElementsActions.getOxdDropdownRealOptions(driver, jobTitleDropdown);
    }

    /** Selectable employment statuses, excluding the "-- Select --" placeholder. */
    public List<String> getAvailableEmploymentStatuses() {
        return ElementsActions.getOxdDropdownRealOptions(driver, employmentStatusDropdown);
    }

    /** Selectable sub units, excluding the "-- Select --" placeholder. */
    public List<String> getAvailableSubUnits() {
        return ElementsActions.getOxdDropdownRealOptions(driver, subUnitDropdown);
    }

    @Step("Run the search")
    public PimEmployeeListPage clickSearch() {
        // Clear any toast still on screen from a previous action FIRST. A
        // lingering "No Records Found" toast from an earlier search otherwise
        // satisfies the no-results check below, so settling stops immediately and
        // the new result set is read while the table is still empty.
        ElementsActions.dismissToast(driver);
        ElementsActions.clickElement(driver, searchButton);
        Waits.waitForLoaderToDisappear(driver);
        waitForResultsToSettle();
        return this;
    }

    @Step("Reset the filters")
    public PimEmployeeListPage clickReset() {
        ElementsActions.clickElement(driver, resetButton);
        Waits.waitForLoaderToDisappear(driver);
        waitForResultsToSettle();
        return this;
    }

    @Step("Open the Add Employee form")
    public AddEmployeePage clickAdd() {
        ElementsActions.clickElement(driver, addButton);
        Waits.waitForUrlContains(driver, "addEmployee");
        return new AddEmployeePage();
    }

    // ------------------------------------------------------------- results

    /**
     * The table re-renders asynchronously after a search.
     *
     * Settling deliberately does NOT accept the "(n) Records Found" banner as
     * proof: that banner is painted before the rows are, so a read taken on it
     * returns an empty table and the test reports "filter matched nothing" for a
     * filter that matched fine. Only actual rows, or an explicit no-results
     * indication, count as settled.
     */
    private void waitForResultsToSettle() {
        Waits.waitForLoaderToDisappear(driver);
        try {
            // A row is only "settled" once its CELLS exist. OrangeHRM paints the
            // .oxd-table-card containers first and fills them a beat later, so
            // waiting on the container alone releases the wait while every row is
            // still an empty shell -- which getAllRows() then correctly skips,
            // producing an empty result set for a search that matched.
            Waits.retryOnStale(driver, d -> {
                if (isNoResultsIndicated(d)) {
                    return true;
                }
                return d.findElements(tableCard).stream()
                        .anyMatch(card -> card.findElements(tableCell).size() > COL_SUPERVISOR);
            });
        } catch (org.openqa.selenium.TimeoutException e) {
            // Neither rows nor a no-results marker: leave it to the assertions.
        }
    }

    /**
     * Only VISIBLE no-results markers count. OrangeHRM keeps the element in the
     * DOM and hides it, so a presence-only check reports "no results" for a
     * search that returned plenty.
     */
    private boolean isNoResultsIndicated(org.openqa.selenium.WebDriver d) {
        return d.findElements(noRecordsToast).stream().anyMatch(WebElement::isDisplayed)
                || d.findElements(By.xpath("//span[contains(normalize-space(),'No Records Found')]"))
                .stream().anyMatch(WebElement::isDisplayed);
    }

    /** Parses the "(150) Records Found" banner; -1 when it is absent. */
    public int getRecordCount() {
        if (!Waits.isElementVisible(driver, recordsFoundText, 8)) {
            return isNoRecordsFound() ? 0 : -1;
        }
        String text = ElementsActions.getText(driver, recordsFoundText);
        Matcher matcher = Pattern.compile("\\((\\d+)\\)").matcher(text);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return text.contains("No Records") ? 0 : -1;
    }

    public boolean isNoRecordsFound() {
        return driver.findElements(tableCard).isEmpty();
    }

    public int getDisplayedRowCount() {
        return ElementsActions.countElements(driver, tableCard);
    }

    /**
     * Every row currently rendered, mapped to a typed record.
     *
     * The whole extraction is retried on staleness rather than each cell read:
     * the Vue table swaps the entire row set when it re-renders, so a partial
     * scrape would mix rows from before and after the refresh. Retrying the
     * whole pass guarantees one consistent snapshot.
     */
    public List<EmployeeRow> getAllRows() {
        return Waits.retryOnStale(driver, d -> {
            List<EmployeeRow> rows = new ArrayList<>();
            for (WebElement card : d.findElements(tableCard)) {
                List<WebElement> cells = card.findElements(tableCell);
                if (cells.size() <= COL_SUPERVISOR) {
                    continue;
                }
                rows.add(new EmployeeRow(
                        text(cells, COL_ID),
                        text(cells, COL_FIRST_MIDDLE_NAME),
                        text(cells, COL_LAST_NAME),
                        text(cells, COL_JOB_TITLE),
                        text(cells, COL_EMPLOYMENT_STATUS),
                        text(cells, COL_SUB_UNIT),
                        text(cells, COL_SUPERVISOR)));
            }
            return rows;
        });
    }

    /** Content-based row lookup -- the alternative to "click row 2". */
    public Optional<EmployeeRow> findRowByLastName(String lastName) {
        return getAllRows().stream()
                .filter(row -> row.lastName().equalsIgnoreCase(lastName))
                .findFirst();
    }

    public Optional<EmployeeRow> findRowByEmployeeId(String employeeId) {
        return getAllRows().stream()
                .filter(row -> row.id().equals(employeeId))
                .findFirst();
    }

    /** The live row element whose Last Name cell matches -- used to click actions. */
    private WebElement rowElementByLastName(String lastName) {
        By row = By.xpath("//div[contains(@class,'oxd-table-card')]"
                + "[.//div[contains(@class,'oxd-table-cell')][normalize-space()=" + quote(lastName) + "]]");
        return Waits.waitForElementVisible(driver, row);
    }

    @Step("Open the employee record for last name {lastName}")
    public EmployeeDetailsPage openEmployeeByLastName(String lastName) {
        WebElement row = rowElementByLastName(lastName);
        // The cell itself carries the row click handler that routes to the record.
        WebElement nameCell = row.findElements(tableCell).get(COL_LAST_NAME);
        ElementsActions.clickElement(driver, nameCell);
        Waits.waitForUrlContains(driver, "viewPersonalDetails");
        Waits.waitForLoaderToDisappear(driver);
        return new EmployeeDetailsPage().waitForRecordToLoad();
    }

    /**
     * Deletes the employee whose last name matches, via the row's trash action
     * and the confirmation modal. Used by the cleanup step.
     */
    @Step("Delete the employee with last name {lastName}")
    public PimEmployeeListPage deleteEmployeeByLastName(String lastName) {
        WebElement row = rowElementByLastName(lastName);

        // Each row carries two identical-looking icon buttons (delete and edit).
        // They are told apart by the icon they contain, not by their position:
        // picking "the first button" deletes or edits depending on the build, and
        // clicking edit here means cleanup silently leaves the record behind.
        WebElement deleteButton = row.findElements(By.cssSelector("button.oxd-icon-button")).stream()
                .filter(button -> !button.findElements(By.cssSelector("i.bi-trash")).isEmpty())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No delete action found on the row for last name " + lastName));
        ElementsActions.clickElement(driver, deleteButton);

        By confirmDelete = By.xpath("//button[contains(normalize-space(),'Yes, Delete')]");
        ElementsActions.clickElement(driver, confirmDelete);
        Waits.waitForLoaderToDisappear(driver);
        return this;
    }

    private static String text(List<WebElement> cells, int index) {
        return cells.get(index).getText().trim();
    }

    /** One row of the employee table. */
    public record EmployeeRow(String id, String firstAndMiddleName, String lastName,
                              String jobTitle, String employmentStatus, String subUnit, String supervisor) {

        public String fullName() {
            return (firstAndMiddleName + " " + lastName).trim();
        }
    }
}
