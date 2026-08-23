package Pages;

import Util.Validations;
import Util.ElementsActions;
import Util.Waits;
import io.qameta.allure.Step;
import org.openqa.selenium.By;

public class AddEmployeePage extends BasePage {
    private static final String PATH = "/web/index.php/pim/addEmployee";

    //Locators
    private final By firstNameField = By.name("firstName");
    private final By middleNameField = By.name("middleName");
    private final By lastNameField = By.name("lastName");
    private final By employeeIdField = inputByLabel("Employee Id");
    private final By saveButton = By.cssSelector("button[type='submit']");
    private final By formHeading = By.xpath("//h6[normalize-space()='Add Employee']");

    private String saveToastText = "";
    private boolean saveWasSuccessful = false;

    //PageActions
    @Step("Open PIM > Add Employee")
    public void open() {
        openPath(PATH);
        Waits.waitForElementVisible(driver, firstNameField);
    }


    public String getPrefilledEmployeeId() {
        return ElementsActions.getValue(driver, employeeIdField);
    }

    @Step("Enter first name: {firstName}")
    public void enterFirstName(String firstName) {
        ElementsActions.clearAndEnterText(driver, firstNameField, firstName);
    }

    @Step("Enter middle name: {middleName}")
    public void enterMiddleName(String middleName) {
        ElementsActions.clearAndEnterText(driver, middleNameField, middleName);
    }

    @Step("Enter last name: {lastName}")
    public void enterLastName(String lastName) {
        ElementsActions.clearAndEnterText(driver, lastNameField, lastName);
    }

    @Step("Enter employee id: {employeeId}")
    public void enterEmployeeId(String employeeId) {
        ElementsActions.clearAndEnterText(driver, employeeIdField, employeeId);
    }

    @Step("Fill the form for {firstName} {middleName} {lastName}")
    public void fillForm(String firstName, String middleName, String lastName, String employeeId) {
        enterFirstName(firstName);
        enterMiddleName(middleName);
        enterLastName(lastName);
        enterEmployeeId(employeeId);
    }

    @Step("Save the employee")
    public void save() {
        ElementsActions.clickElement(driver, saveButton);

        saveToastText = ElementsActions.getToastMessage(driver);
        saveWasSuccessful = !saveToastText.isBlank()
                && !Waits.isElementVisible(driver, By.cssSelector(".oxd-toast--error"), 1);

        Waits.waitForUrlContains(driver, "viewPersonalDetails");
        Waits.waitForLoaderToDisappear(driver);
    }

    public String getSaveToastText() {
        return saveToastText;
    }

    public boolean wasSaveSuccessful() {
        return saveWasSuccessful;
    }

    @Step("Save expecting a validation failure")
    public void saveExpectingValidationError() {
        ElementsActions.clickElement(driver, saveButton);
    }

    public boolean isStillOnAddEmployeePage() {
        return driver.getCurrentUrl().contains("addEmployee");
    }

    //PageAssertions
    @Step("Verify OrangeHRM pre-populates an Employee Id")
    public void verifyEmployeeIdIsPrefilled() {
        Validations.validateNotBlank(getPrefilledEmployeeId(),
                "OrangeHRM should pre-populate a suggested Employee Id");
    }

    @Step("Verify the save was confirmed")
    public void verifySaveWasConfirmed() {
        Validations.validateTrue(wasSaveSuccessful(),
                "A success notification should be displayed on save");
        Validations.validateContains(getSaveToastText().toLowerCase(), "success",
                "Notification should confirm success");
    }

    @Step("Verify {fieldLabel} was rejected with {expectedMessage}")
    public void verifyFieldRejectedWith(String fieldLabel, String expectedMessage) {
        Validations.validateEquals(getFieldErrorFor(fieldLabel), expectedMessage,
                "A '" + expectedMessage + "' message should be shown against " + fieldLabel);
        verifySubmissionWasRefused();
    }

    @Step("Verify {fieldLabel} was rejected with a message explaining the limit")
    public void verifyFieldRejectedWithLengthMessage(String fieldLabel) {
        String error = getFieldErrorFor(fieldLabel).toLowerCase();
        Validations.validateNotBlank(error, "An over-length value should raise a validation message");
        Validations.validateTrue(error.contains("should not exceed")
                        || error.contains("required")
                        || error.contains("invalid"),
                "Validation should explain the length limit but said: '" + error + "'");
        verifySubmissionWasRefused();
    }

    @Step("Verify the submission was refused")
    public void verifySubmissionWasRefused() {
        Validations.validateTrue(isStillOnAddEmployeePage(),
                "The form should not navigate away when validation fails");
        Validations.validateFalse(isSuccessToastDisplayed(),
                "No success notification should appear for a rejected submission");
    }

    public void verifyAddEmployeePageLoaded() {
        Validations.validateTrue(
                ElementsActions.isDisplayed(driver, formHeading)
                        && ElementsActions.isDisplayed(driver, firstNameField),
                "Add Employee form did not load");
    }
}
