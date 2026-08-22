package Pages;

import Util.DataFactory;
import Util.ElementsActions;
import Util.Waits;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Leave → Leave List -- used to prove a submitted leave request can be found again. */
public class LeaveListPage extends BasePage {

    private static final String PATH = "/web/index.php/leave/viewLeaveList";

    private final By fromDateField = inputByLabel("From Date");
    private final By toDateField = inputByLabel("To Date");
    private final By statusDropdown = By.xpath(
            "//div[contains(@class,'oxd-input-group')]"
                    + "[.//label[contains(normalize-space(),'Show Leave with Status')]]"
                    + "//div[contains(@class,'oxd-select-text')]");
    private final By leaveTypeDropdown = dropdownByLabel("Leave Type");
    private final By employeeNameInput = By.xpath(
            "//div[contains(@class,'oxd-input-group')][.//label[normalize-space()='Employee Name']]"
                    + "//input[@placeholder='Type for hints...']");
    private final By searchButton = By.cssSelector("button[type='submit']");
    private final By resetButton = By.cssSelector("button[type='reset']");
    private final By tableCard = By.cssSelector(".oxd-table-card");
    private final By tableCell = By.cssSelector(".oxd-table-cell");

    /** Column order verified against the live Leave List header. */
    private static final int COL_DATE = 1;
    private static final int COL_EMPLOYEE_NAME = 2;
    private static final int COL_LEAVE_TYPE = 3;
    private static final int COL_BALANCE = 4;
    private static final int COL_NUMBER_OF_DAYS = 5;
    private static final int COL_STATUS = 6;

    @Step("Open Leave > Leave List")
    public LeaveListPage open() {
        openPath(PATH);
        Waits.waitForElementVisible(driver, searchButton);
        return this;
    }

    @Step("Filter leave from {from} to {to}")
    public LeaveListPage setDateRange(LocalDate from, LocalDate to) {
        ElementsActions.setDate(driver, fromDateField, DataFactory.formatForApp(from));
        ElementsActions.setDate(driver, toDateField, DataFactory.formatForApp(to));
        return this;
    }

    /**
     * The status filter defaults to a subset that excludes admin-assigned leave,
     * so it has to be cleared before searching for a freshly assigned request.
     */
    @Step("Include leave with status {status}")
    public LeaveListPage includeStatus(String status) {
        ElementsActions.selectFromOxdDropdown(driver, statusDropdown, status);
        return this;
    }

    @Step("Filter by employee {fullName}")
    public boolean setEmployeeFilter(String fullName) {
        return selectEmployeeByName(employeeNameInput, fullName);
    }

    @Step("Filter by leave type {leaveType}")
    public LeaveListPage setLeaveTypeFilter(String leaveType) {
        ElementsActions.selectFromOxdDropdown(driver, leaveTypeDropdown, leaveType);
        return this;
    }

    @Step("Run the leave search")
    public LeaveListPage clickSearch() {
        ElementsActions.dismissToast(driver);
        ElementsActions.clickElement(driver, searchButton);
        Waits.waitForLoaderToDisappear(driver);
        waitForResultsToSettle();
        return this;
    }

    /**
     * Waits for the result table to actually render before it is read.
     *
     * As on the employee list, a row only counts once its CELLS exist: the card
     * containers are painted first, so releasing on the container alone scrapes
     * empty shells and reports a leave request that WAS created as missing.
     */
    private void waitForResultsToSettle() {
        try {
            Waits.retryOnStale(driver, d -> {
                boolean noResults = d.findElements(
                                By.xpath("//span[contains(normalize-space(),'No Records Found')]"))
                        .stream().anyMatch(WebElement::isDisplayed);
                if (noResults) {
                    return true;
                }
                return d.findElements(tableCard).stream()
                        .anyMatch(card -> card.findElements(tableCell).size() > COL_STATUS);
            });
        } catch (org.openqa.selenium.TimeoutException e) {
            // Neither state appeared; leave the verdict to the assertions.
        }
    }

    @Step("Reset the leave filters")
    public LeaveListPage clickReset() {
        ElementsActions.clickElement(driver, resetButton);
        Waits.waitForLoaderToDisappear(driver);
        return this;
    }

    public int getRowCount() {
        return ElementsActions.countElements(driver, tableCard);
    }

    /** One consistent snapshot of the table; retried whole if it re-renders mid-read. */
    public List<LeaveRow> getAllRows() {
        return Waits.retryOnStale(driver, d -> {
            List<LeaveRow> rows = new ArrayList<>();
            for (WebElement card : d.findElements(tableCard)) {
                List<WebElement> cells = card.findElements(tableCell);
                if (cells.size() <= COL_STATUS) {
                    continue;
                }
                rows.add(new LeaveRow(
                        cells.get(COL_DATE).getText().trim(),
                        cells.get(COL_EMPLOYEE_NAME).getText().trim(),
                        cells.get(COL_LEAVE_TYPE).getText().trim(),
                        cells.get(COL_BALANCE).getText().trim(),
                        cells.get(COL_NUMBER_OF_DAYS).getText().trim(),
                        cells.get(COL_STATUS).getText().trim()));
            }
            return rows;
        });
    }

    /** Content-based lookup: the row belonging to a named employee. */
    public Optional<LeaveRow> findRowByEmployee(String employeeNameFragment) {
        return getAllRows().stream()
                .filter(row -> row.employeeName().toLowerCase()
                        .contains(employeeNameFragment.toLowerCase()))
                .findFirst();
    }

    /**
     * Content-based lookup that re-runs the search before giving up.
     *
     * A leave request is written and then read back through a different query,
     * and on this shared sandbox the row is occasionally not returned by the
     * first search after the write. Re-running the search distinguishes "the
     * request was never created" -- a real defect -- from "the list had not
     * caught up yet", which is not.
     */
    @Step("Locate leave for {employeeNameFragment}")
    public Optional<LeaveRow> findRowByEmployeeRetrying(String employeeNameFragment) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            Optional<LeaveRow> row = findRowByEmployee(employeeNameFragment);
            if (row.isPresent()) {
                return row;
            }
            System.out.println("Leave row for '" + employeeNameFragment
                    + "' not returned on attempt " + attempt + "; re-running the search.");
            if (attempt < 3) {
                clickSearch();
            }
        }
        return Optional.empty();
    }

    public record LeaveRow(String date, String employeeName, String leaveType,
                           String balance, String numberOfDays, String status) {
    }
}
