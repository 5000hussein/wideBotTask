package Pages;

import Util.ElementsActions;
import Util.Validations;
import Util.Waits;
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
    private final By jobTitleDropdown = dropdownByLabel("Job Title");
    private final By subUnitDropdown = dropdownByLabel("Sub Unit");

    private final By searchButton = By.cssSelector("button[type='submit']");
    private final By resetButton = By.cssSelector("button[type='reset']");

    private final By tableCard = By.cssSelector(".oxd-table-card");
    private final By tableCell = By.cssSelector(".oxd-table-cell");
    private final By recordsFoundText = By.xpath("//span[contains(@class,'oxd-text--span')][contains(.,'Record')]");
    private final By noRecordsToast = By.xpath("//div[contains(@class,'oxd-toast')][contains(.,'No Records Found')]");

    private static final int COL_ID = 1;
    private static final int COL_FIRST_MIDDLE_NAME = 2;
    private static final int COL_LAST_NAME = 3;
    private static final int COL_JOB_TITLE = 4;
    private static final int COL_EMPLOYMENT_STATUS = 5;
    private static final int COL_SUB_UNIT = 6;
    private static final int COL_SUPERVISOR = 7;

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

    @Step("Filter by job title: {jobTitle}")
    public void setJobTitleFilter(String jobTitle) {
        ElementsActions.selectFromOxdDropdown(driver, jobTitleDropdown, jobTitle);
    }

    @Step("Filter by sub unit: {subUnit}")
    public void setSubUnitFilter(String subUnit) {
        ElementsActions.selectFromOxdDropdown(driver, subUnitDropdown, subUnit);
    }

    public String getSelectedEmploymentStatus() {
        return ElementsActions.getOxdDropdownValue(driver, employmentStatusDropdown);
    }

    public List<String> getAvailableJobTitles() {
        return ElementsActions.getOxdDropdownRealOptions(driver, jobTitleDropdown);
    }

    public List<String> getAvailableEmploymentStatuses() {
        return ElementsActions.getOxdDropdownRealOptions(driver, employmentStatusDropdown);
    }

    public List<String> getAvailableSubUnits() {
        return ElementsActions.getOxdDropdownRealOptions(driver, subUnitDropdown);
    }

    @Step("Run the search")
    public void clickSearch() {
        ElementsActions.dismissToast(driver);
        ElementsActions.clickElement(driver, searchButton);
        Waits.waitForLoaderToDisappear(driver);
        waitForResultsToSettle();
    }

    @Step("Reset the filters")
    public void clickReset() {
        ElementsActions.clickElement(driver, resetButton);
        Waits.waitForLoaderToDisappear(driver);
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
                        .anyMatch(card -> card.findElements(tableCell).size() > COL_SUPERVISOR);
            });
        } catch (org.openqa.selenium.TimeoutException ignored) {
        }
    }

    private boolean isNoResultsIndicated(org.openqa.selenium.WebDriver d) {
        return d.findElements(noRecordsToast).stream().anyMatch(WebElement::isDisplayed)
                || d.findElements(By.xpath("//span[contains(normalize-space(),'No Records Found')]"))
                .stream().anyMatch(WebElement::isDisplayed);
    }

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
                              String jobTitle, String employmentStatus, String subUnit, String supervisor) {
        public String fullName() {
            return (firstAndMiddleName + " " + lastName).trim();
        }
    }

    //PageAssertions
    public void verifyEmployeeListPageLoaded() {
        Validations.validateTrue(
                ElementsActions.isDisplayed(driver, searchButton)
                        && ElementsActions.isDisplayed(driver, employeeIdInput),
                "Employee List page did not load");
    }
}
