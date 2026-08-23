package Pages;

import Util.Helper;
import Util.ElementsActions;
import Util.Validations;
import Util.Waits;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LeaveListPage extends BasePage {
    private static final String PATH = "/web/index.php/leave/viewLeaveList";

    //Locators
    private final By fromDateField = inputByLabel("From Date");
    private final By toDateField = inputByLabel("To Date");
    private final By statusDropdown = By.xpath(
            "//div[contains(@class,'oxd-input-group')]"
                    + "[.//label[contains(normalize-space(),'Show Leave with Status')]]"
                    + "//div[contains(@class,'oxd-select-text')]");
    private final By employeeNameInput = By.xpath(
            "//div[contains(@class,'oxd-input-group')][.//label[normalize-space()='Employee Name']]"
                    + "//input[@placeholder='Type for hints...']");
    private final By searchButton = By.cssSelector("button[type='submit']");
    private final By resetButton = By.cssSelector("button[type='reset']");
    private final By tableCard = By.cssSelector(".oxd-table-card");
    private final By tableCell = By.cssSelector(".oxd-table-cell");

    private static final int COL_EMPLOYEE_NAME = 2;
    private static final int COL_LEAVE_TYPE = 3;
    private static final int COL_NUMBER_OF_DAYS = 5;
    private static final int COL_STATUS = 6;

    //PageActions
    @Step("Open Leave > Leave List")
    public void open() {
        openPath(PATH);
        Waits.waitForElementVisible(driver, searchButton);
    }

    @Step("Filter leave from {from} to {to}")
    public void setDateRange(LocalDate from, LocalDate to) {
        ElementsActions.setDate(driver, fromDateField, Helper.formatForApp(from));
        ElementsActions.setDate(driver, toDateField, Helper.formatForApp(to));
    }

    @Step("Include leave with status {status}")
    public void includeStatus(String status) {
        ElementsActions.selectFromOxdDropdown(driver, statusDropdown, status);
    }

    @Step("Filter by employee {fullName}")
    public boolean setEmployeeFilter(String fullName) {
        return selectEmployeeByName(employeeNameInput, fullName);
    }

    @Step("Run the leave search")
    public void clickSearch() {
        ElementsActions.dismissToast(driver);
        ElementsActions.clickElement(driver, searchButton);
        Waits.waitForLoaderToDisappear(driver);
        waitForResultsToSettle();
    }

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
        } catch (org.openqa.selenium.TimeoutException ignored) {
        }
    }

    @Step("Reset the leave filters")
    public void clickReset() {
        ElementsActions.clickElement(driver, resetButton);
        Waits.waitForLoaderToDisappear(driver);
    }

    public List<LeaveRow> getAllRows() {
        return Waits.retryOnStale(driver, d -> {
            List<LeaveRow> rows = new ArrayList<>();
            for (WebElement card : d.findElements(tableCard)) {
                List<WebElement> cells = card.findElements(tableCell);
                if (cells.size() <= COL_STATUS) {
                    continue;
                }
                rows.add(new LeaveRow(
                        cells.get(COL_EMPLOYEE_NAME).getText().trim(),
                        cells.get(COL_LEAVE_TYPE).getText().trim(),
                        cells.get(COL_NUMBER_OF_DAYS).getText().trim(),
                        cells.get(COL_STATUS).getText().trim()));
            }
            return rows;
        });
    }

    public Optional<LeaveRow> findRowByEmployee(String employeeNameFragment) {
        return getAllRows().stream()
                .filter(row -> row.employeeName().toLowerCase()
                        .contains(employeeNameFragment.toLowerCase()))
                .findFirst();
    }

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

    public record LeaveRow(String employeeName, String leaveType,
                           String numberOfDays, String status) {
    }

    //PageAssertions
    @Step("Verify {fullName} can be filtered on")
    public void verifyEmployeeIsSelectable(String fullName) {
        Validations.validateTrue(setEmployeeFilter(fullName),
                "The employee should be selectable on the Leave List filter");
    }

    @Step("Verify the stored leave for {lastName} is {expectedType} over {expectedDays} days")
    public void verifyStoredLeave(String lastName, String expectedType, String expectedDays) {
        LeaveRow leave = findRowByEmployeeRetrying(lastName)
                .orElseThrow(() -> new AssertionError(
                        "The submitted leave request should be findable for " + lastName));

        Validations.validateContains(leave.leaveType(), expectedType,
                "The stored leave type should be '" + expectedType + "'");
        Validations.validateEquals(leave.numberOfDays(), expectedDays + ".00",
                "A Monday-to-Wednesday request should be " + expectedDays + " days");
        Validations.validateNotBlank(leave.status(), "The request should carry a status");
    }
}
