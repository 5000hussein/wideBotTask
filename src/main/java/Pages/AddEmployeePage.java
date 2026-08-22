package Pages;

import Util.DataFactory;
import Util.ElementsActions;
import Util.Waits;
import io.qameta.allure.Step;
import org.openqa.selenium.By;

public class AddEmployeePage extends BasePage {

    private static final String PATH = "/web/index.php/pim/addEmployee";

    private final By firstNameField = By.name("firstName");
    private final By middleNameField = By.name("middleName");
    private final By lastNameField = By.name("lastName");
    private final By employeeIdField = inputByLabel("Employee Id");
    private final By saveButton = By.cssSelector("button[type='submit']");
    private final By formHeading = By.xpath("//h6[normalize-space()='Add Employee']");

    @Step("Open PIM > Add Employee")
    public AddEmployeePage open() {
        openPath(PATH);
        Waits.waitForElementVisible(driver, firstNameField);
        return this;
    }

    public boolean isDisplayed() {
        return ElementsActions.isDisplayed(driver, formHeading)
                && ElementsActions.isDisplayed(driver, firstNameField);
    }

    public String getPrefilledEmployeeId() {
        return ElementsActions.getValue(driver, employeeIdField);
    }

    @Step("Enter first name: {firstName}")
    public AddEmployeePage enterFirstName(String firstName) {
        ElementsActions.clearAndEnterText(driver, firstNameField, firstName);
        return this;
    }

    @Step("Enter middle name: {middleName}")
    public AddEmployeePage enterMiddleName(String middleName) {
        ElementsActions.clearAndEnterText(driver, middleNameField, middleName);
        return this;
    }

    @Step("Enter last name: {lastName}")
    public AddEmployeePage enterLastName(String lastName) {
        ElementsActions.clearAndEnterText(driver, lastNameField, lastName);
        return this;
    }

    @Step("Enter employee id: {employeeId}")
    public AddEmployeePage enterEmployeeId(String employeeId) {
        ElementsActions.clearAndEnterText(driver, employeeIdField, employeeId);
        return this;
    }

    @Step("Fill the form for {employee}")
    public AddEmployeePage fillForm(DataFactory.Employee employee) {
        enterFirstName(employee.firstName());
        enterMiddleName(employee.middleName());
        enterLastName(employee.lastName());
        enterEmployeeId(employee.employeeId());
        return this;
    }

    @Step("Save {employee} and open the created record")
    public EmployeeDetailsPage saveAndOpenRecord(DataFactory.Employee employee) {
        fillForm(employee);
        ElementsActions.clickElement(driver, saveButton);

        // Read immediately after the click, before any wait. OrangeHRM toasts
        // auto-dismiss well before the record page finishes loading, so asking for
        // the toast once the page has settled loses it -- and the failure then
        // reads "no success message" rather than "we looked too late".
        String saveToastText = ElementsActions.getToastMessage(driver);
        boolean saveToastWasSuccess = !saveToastText.isBlank()
                && !Waits.isElementVisible(driver, By.cssSelector(".oxd-toast--error"), 1);

        Waits.waitForUrlContains(driver, "viewPersonalDetails");
        Waits.waitForLoaderToDisappear(driver);

        EmployeeDetailsPage details = new EmployeeDetailsPage();
        details.waitForRecordToLoad();
        details.setCreationToast(saveToastText, saveToastWasSuccess);
        return details;
    }

    @Step("Save expecting a validation failure")
    public AddEmployeePage saveExpectingValidationError() {
        ElementsActions.clickElement(driver, saveButton);
        return this;
    }

    public boolean isStillOnAddEmployeePage() {
        return driver.getCurrentUrl().contains("addEmployee");
    }
}
