package Pages;

import Util.Helper;
import Util.Validations;
import Util.ElementsActions;
import Util.Waits;
import io.qameta.allure.Step;
import org.openqa.selenium.By;

import java.time.LocalDate;

public class AssignLeavePage extends BasePage {
    private static final String PATH = "/web/index.php/leave/assignLeave";

    //Locators
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

    //PageActions
    @Step("Open Leave > Assign Leave")
    public void open() {
        openPath(PATH);
        Waits.waitForElementVisible(driver, assignButton);
    }


    @Step("Select employee {fullName}")
    public boolean selectEmployee(String fullName) {
        return selectEmployeeByName(employeeNameInput, fullName);
    }

    @Step("Select leave type {leaveType}")
    public void selectLeaveType(String leaveType) {
        ElementsActions.selectFromOxdDropdown(driver, leaveTypeDropdown, leaveType);
    }

    @Step("Set From Date to {date}")
    public void setFromDate(LocalDate date) {
        ElementsActions.setDate(driver, fromDateField, Helper.formatForApp(date));
    }

    @Step("Set To Date to {date}")
    public void setToDate(LocalDate date) {
        ElementsActions.setDate(driver, toDateField, Helper.formatForApp(date));
    }

    public String getFromDateValue() {
        return ElementsActions.getValue(driver, fromDateField);
    }

    public String getToDateValue() {
        return ElementsActions.getValue(driver, toDateField);
    }

    @Step("Add comment")
    public void setComment(String comment) {
        ElementsActions.clearAndEnterText(driver, commentsField, comment);
    }

    public String getLeaveBalance() {
        return Waits.isElementVisible(driver, leaveBalanceValue, 8)
                ? ElementsActions.getText(driver, leaveBalanceValue)
                : "";
    }

    @Step("Submit the leave assignment")
    public void clickAssign() {
        ElementsActions.clickElement(driver, assignButton);

        if (Waits.isElementVisible(driver, confirmationOkButton, 2)) {
            ElementsActions.clickElement(driver, confirmationOkButton);
        }
        captureToast();
        Waits.waitForLoaderToDisappear(driver);
    }

    @Step("Submit expecting a validation failure")
    public void assignExpectingValidationError() {
        ElementsActions.clickElement(driver, assignButton);
    }

    public boolean isStillOnAssignPage() {
        return driver.getCurrentUrl().contains("assignLeave");
    }

    //PageAssertions
    @Step("Verify {fullName} can be assigned leave")
    public void verifyEmployeeIsSelectable(String fullName) {
        Validations.validateTrue(selectEmployee(fullName),
                "The entitled employee should be selectable on Assign Leave");
    }

    @Step("Verify the date range was accepted")
    public void verifyDateRangeAccepted(LocalDate from, LocalDate to) {
        Validations.validateEquals(getFromDateValue(), Helper.formatForApp(from),
                "From Date should hold the calculated start date");
        Validations.validateEquals(getToDateValue(), Helper.formatForApp(to),
                "To Date should hold the calculated end date");
    }

    @Step("Verify the leave request was submitted")
    public void verifyLeaveWasSubmitted() {
        Validations.validateTrue(wasLastActionSuccessful(),
                "Submitting the leave request should be confirmed; toast was: '"
                        + getLastToastText() + "'");
    }

    @Step("Verify {fieldLabel} was rejected and nothing was assigned")
    public void verifyDateRejected(String fieldLabel) {
        String error = getFieldErrorFor(fieldLabel);
        if (error.isBlank()) {
            assignExpectingValidationError();
            error = getFieldErrorFor(fieldLabel);
        }
        Validations.validateNotBlank(error,
                "A " + fieldLabel + " before the From Date should raise a validation message");
        Validations.validateTrue(isStillOnAssignPage(), "An invalid request should not be submitted");
        Validations.validateFalse(isSuccessToastDisplayed(),
                "No success notification should appear for an invalid leave request");
    }

    public void verifyAssignLeavePageLoaded() {
        Validations.validateTrue(
                ElementsActions.isDisplayed(driver, assignButton)
                        && ElementsActions.isDisplayed(driver, fromDateField),
                "Assign Leave form did not load");
    }
}
