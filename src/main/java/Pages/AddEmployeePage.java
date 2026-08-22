package Pages;

import Util.DataFactory;
import Util.ElementsActions;
import Util.Waits;
import io.qameta.allure.Step;
import org.openqa.selenium.By;

/** PIM → Add Employee. */
public class AddEmployeePage extends BasePage {

    private static final String PATH = "/web/index.php/pim/addEmployee";

    private final By firstNameField = By.name("firstName");
    private final By middleNameField = By.name("middleName");
    private final By lastNameField = By.name("lastName");
    private final By employeeIdField = inputByLabel("Employee Id");
    private final By saveButton = By.cssSelector("button[type='submit']");
    private final By cancelButton = By.xpath("//button[normalize-space()='Cancel']");
    private final By formHeading = By.xpath("//h6[normalize-space()='Add Employee']");

    private String saveToastText = "";
    private boolean saveToastWasSuccess = false;

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

    /** OrangeHRM pre-fills a suggested employee id; the tests overwrite it. */
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

    @Step("Save the new employee")
    public AddEmployeePage clickSave() {
        ElementsActions.clickElement(driver, saveButton);
        Waits.waitForLoaderToDisappear(driver);
        return this;
    }

    /**
     * Saves and waits for the redirect to the created employee's record.
     * That redirect -- not the toast -- is the first real evidence the record
     * was persisted, because the URL carries the server-assigned empNumber.
     *
     * The toast is read HERE, immediately after the click, and cached. It
     * auto-dismisses after a few seconds, so asserting on it once the redirect
     * and the record load have completed is a race the test loses more often
     * than it wins -- and it fails as "no success message" rather than as the
     * timing problem it actually is.
     */
    @Step("Save {employee} and open the created record")
    public EmployeeDetailsPage saveAndOpenRecord(DataFactory.Employee employee) {
        fillForm(employee);
        ElementsActions.clickElement(driver, saveButton);

        saveToastText = ElementsActions.getToastMessage(driver);
        saveToastWasSuccess = !saveToastText.isBlank()
                && !Waits.isElementVisible(driver, By.cssSelector(".oxd-toast--error"), 1);

        Waits.waitForUrlContains(driver, "viewPersonalDetails");
        Waits.waitForLoaderToDisappear(driver);

        EmployeeDetailsPage details = new EmployeeDetailsPage();
        details.waitForRecordToLoad();
        details.setCreationToast(saveToastText, saveToastWasSuccess);
        return details;
    }

    /** Toast text captured at the moment of saving, before it auto-dismissed. */
    public String getSaveToastText() {
        return saveToastText;
    }

    public boolean wasSaveSuccessful() {
        return saveToastWasSuccess;
    }

    /** Saves expecting the form to reject the input; stays on Add Employee. */
    @Step("Save expecting a validation failure")
    public AddEmployeePage saveExpectingValidationError() {
        ElementsActions.clickElement(driver, saveButton);
        return this;
    }

    public boolean isStillOnAddEmployeePage() {
        return driver.getCurrentUrl().contains("addEmployee");
    }

    @Step("Cancel out of the form")
    public PimEmployeeListPage clickCancel() {
        ElementsActions.clickElement(driver, cancelButton);
        Waits.waitForUrlContains(driver, "viewEmployeeList");
        return new PimEmployeeListPage();
    }
}
