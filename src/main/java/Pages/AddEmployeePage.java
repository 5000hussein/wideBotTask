package Pages;

import Util.DataFactory;
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

    //PageActions
    @Step("Open PIM > Add Employee")
    public AddEmployeePage open() {
        openPath(PATH);
        Waits.waitForElementVisible(driver, firstNameField);
        return this;
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

    @Step("Fill the form for {employee}")
    public void fillForm(DataFactory.Employee employee) {
        enterFirstName(employee.firstName());
        enterMiddleName(employee.middleName());
        enterLastName(employee.lastName());
        enterEmployeeId(employee.employeeId());
    }

    @Step("Save {employee} and open the created record")
    public EmployeeDetailsPage saveAndOpenRecord(DataFactory.Employee employee) {
        fillForm(employee);
        ElementsActions.clickElement(driver, saveButton);

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
    public void saveExpectingValidationError() {
        ElementsActions.clickElement(driver, saveButton);
    }

    public boolean isStillOnAddEmployeePage() {
        return driver.getCurrentUrl().contains("addEmployee");
    }

    //PageAssertions
    public void verifyAddEmployeePageLoaded() {
        Validations.validateTrue(
                ElementsActions.isDisplayed(driver, formHeading)
                        && ElementsActions.isDisplayed(driver, firstNameField),
                "Add Employee form did not load");
    }
}
