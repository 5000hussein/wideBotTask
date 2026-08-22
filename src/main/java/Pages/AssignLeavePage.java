package Pages;

import Util.DataFactory;
import Util.ElementsActions;
import Util.Waits;
import io.qameta.allure.Step;
import org.openqa.selenium.By;

import java.time.LocalDate;

public class AssignLeavePage extends BasePage {
    private static final String PATH = "/web/index.php/leave/assignLeave";

    private final By employeeNameInput = By.xpath(
            "//div[contains(@class,'oxd-input-group')][.//label[normalize-space()='Employee Name']]"
                    + "//input[@placeholder='Type for hints...']");
    private final By leaveTypeDropdown = dropdownByLabel("Leave Type");
    private final By fromDateField = inputByLabel("From Date");
    private final By toDateField = inputByLabel("To Date");
    private final By commentsField = By.cssSelector("textarea.oxd-textarea");
    private final By assignButton = By.cssSelector("button[type='submit']");
    private final By leaveBalanceValue = By.xpath(
            "//div[contains(@class,'oxd-input-group')][.//label[contains(normalize-space(),'Leave Balance')]]"
                    + "//p[contains(@class,'orangehrm-leave-balance')]");

    private final By confirmationOkButton = By.xpath(
            "//div[contains(@class,'oxd-dialog-container')]//button[normalize-space()='Ok']");

    @Step("Open Leave > Assign Leave")
    public AssignLeavePage open() {
        openPath(PATH);
        Waits.waitForElementVisible(driver, assignButton);
        return this;
    }

    public boolean isDisplayed() {
        return ElementsActions.isDisplayed(driver, assignButton)
                && ElementsActions.isDisplayed(driver, fromDateField);
    }

    @Step("Select employee {fullName}")
    public boolean selectEmployee(String fullName) {
        return selectEmployeeByName(employeeNameInput, fullName);
    }

    @Step("Select leave type {leaveType}")
    public AssignLeavePage selectLeaveType(String leaveType) {
        ElementsActions.selectFromOxdDropdown(driver, leaveTypeDropdown, leaveType);
        return this;
    }

    public java.util.List<String> getAvailableLeaveTypes() {
        return ElementsActions.getOxdDropdownRealOptions(driver, leaveTypeDropdown);
    }

    @Step("Set From Date to {date}")
    public AssignLeavePage setFromDate(LocalDate date) {
        ElementsActions.setDate(driver, fromDateField, DataFactory.formatForApp(date));
        return this;
    }

    @Step("Set To Date to {date}")
    public AssignLeavePage setToDate(LocalDate date) {
        ElementsActions.setDate(driver, toDateField, DataFactory.formatForApp(date));
        return this;
    }

    public String getFromDateValue() {
        return ElementsActions.getValue(driver, fromDateField);
    }

    public String getToDateValue() {
        return ElementsActions.getValue(driver, toDateField);
    }

    @Step("Add comment")
    public AssignLeavePage setComment(String comment) {
        ElementsActions.clearAndEnterText(driver, commentsField, comment);
        return this;
    }

    public String getLeaveBalance() {
        return Waits.isElementVisible(driver, leaveBalanceValue, 8)
                ? ElementsActions.getText(driver, leaveBalanceValue)
                : "";
    }

    @Step("Submit the leave assignment")
    public AssignLeavePage clickAssign() {
        ElementsActions.clickElement(driver, assignButton);

        if (Waits.isElementVisible(driver, confirmationOkButton, 2)) {
            ElementsActions.clickElement(driver, confirmationOkButton);
        }
        captureToast();
        Waits.waitForLoaderToDisappear(driver);
        return this;
    }

    @Step("Submit expecting a validation failure")
    public AssignLeavePage assignExpectingValidationError() {
        ElementsActions.clickElement(driver, assignButton);
        return this;
    }

    public boolean isStillOnAssignPage() {
        return driver.getCurrentUrl().contains("assignLeave");
    }
}
