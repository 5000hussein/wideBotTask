package Pages;

import Util.ElementsActions;
import Util.Waits;
import io.qameta.allure.Step;
import org.openqa.selenium.By;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PIM &gt; Employee record, Personal Details tab.
 *
 * The screen renders TWO oxd forms -- Personal Details and Custom Fields -- each
 * with its own type=submit button. Every action here is therefore scoped to the
 * form that actually owns the field, otherwise "click Save" is a coin flip.
 */
public class EmployeeDetailsPage extends BasePage {

    private final By firstNameField = By.name("firstName");
    private final By middleNameField = By.name("middleName");
    private final By lastNameField = By.name("lastName");
    private final By employeeIdField = inputByLabel("Employee Id");
    private final By otherIdField = inputByLabel("Other Id");

    /** Save button belonging to the Personal Details form specifically. */
    private final By personalDetailsSave =
            By.xpath("//form[.//input[@name='firstName']]//button[@type='submit']");

    private final By nameBanner = By.cssSelector(".orangehrm-edit-employee-name");
    private final By tabs = By.cssSelector(".orangehrm-tabs-item");
    private final By personalDetailsHeading = By.xpath("//h6[normalize-space()='Personal Details']");

    /** Toast captured by AddEmployeePage at save time, before it auto-dismissed. */
    private String creationToastText = "";
    private boolean creationWasSuccessful = false;

    void setCreationToast(String toastText, boolean wasSuccessful) {
        this.creationToastText = toastText;
        this.creationWasSuccessful = wasSuccessful;
    }

    /** The notification shown when this record was created. */
    public String getCreationToastText() {
        return creationToastText;
    }

    public boolean wasCreationConfirmed() {
        return creationWasSuccessful;
    }

    @Step("Verify the employee record is displayed")
    public boolean isDisplayed() {
        return driver.getCurrentUrl().contains("viewPersonalDetails")
                && ElementsActions.isDisplayed(driver, personalDetailsHeading)
                && ElementsActions.isDisplayed(driver, firstNameField);
    }

    /**
     * Blocks until the record's data has actually been bound to the form.
     *
     * The Personal Details inputs are rendered EMPTY and populated a moment
     * later once the record request returns, and the form loader can clear
     * before that happens. Reading a field without this gate returns "" and the
     * failure looks like data loss instead of a race.
     */
    @Step("Wait for the employee record to finish loading")
    public EmployeeDetailsPage waitForRecordToLoad() {
        Waits.retryOnStale(driver, d -> {
            String firstName = d.findElement(firstNameField).getDomProperty("value");
            return firstName != null && !firstName.isBlank();
        });
        return this;
    }

    /**
     * The server-assigned internal id from the URL
     * (/pim/viewPersonalDetails/empNumber/{n}). Its presence is proof the record
     * exists server-side, independent of anything the UI claims in a toast.
     */
    public String getEmpNumberFromUrl() {
        Matcher matcher = Pattern.compile("empNumber/(\\d+)").matcher(driver.getCurrentUrl());
        return matcher.find() ? matcher.group(1) : "";
    }

    // -------------------------------------------------------------- reads

    public String getFirstName() {
        return ElementsActions.getValue(driver, firstNameField);
    }

    public String getMiddleName() {
        return ElementsActions.getValue(driver, middleNameField);
    }

    public String getLastName() {
        return ElementsActions.getValue(driver, lastNameField);
    }

    public String getEmployeeId() {
        return ElementsActions.getValue(driver, employeeIdField);
    }

    /**
     * Name as rendered in the record header, e.g. "Automation Test QA1234".
     *
     * The banner is populated by its own request, separate from the form fields,
     * so it can still be an empty element after the inputs have been filled in.
     * Waiting for non-blank text here stops that showing up as "the header shows
     * the wrong name" when it simply had not arrived yet.
     */
    public String getDisplayedName() {
        try {
            return Waits.retryOnStale(driver, d -> {
                String text = d.findElement(nameBanner).getText().replace("\n", " ").trim();
                return text.isBlank() ? null : text;
            });
        } catch (org.openqa.selenium.TimeoutException e) {
            return "";
        }
    }

    public List<String> getAvailableTabs() {
        return ElementsActions.getAllTexts(driver, tabs);
    }

    public boolean isTabAvailable(String tabName) {
        return getAvailableTabs().stream().anyMatch(tab -> tab.equalsIgnoreCase(tabName));
    }

    @Step("Open the {tabName} tab")
    public EmployeeDetailsPage openTab(String tabName) {
        ElementsActions.clickElement(driver,
                By.xpath("//div[contains(@class,'orangehrm-tabs-item')][normalize-space()=" + quote(tabName) + "]"));
        Waits.waitForLoaderToDisappear(driver);
        return this;
    }

    // ------------------------------------------------------------- writes

    @Step("Change first name to {firstName}")
    public EmployeeDetailsPage setFirstName(String firstName) {
        ElementsActions.clearAndEnterText(driver, firstNameField, firstName);
        return this;
    }

    @Step("Change last name to {lastName}")
    public EmployeeDetailsPage setLastName(String lastName) {
        ElementsActions.clearAndEnterText(driver, lastNameField, lastName);
        return this;
    }

    @Step("Change employee id to {employeeId}")
    public EmployeeDetailsPage setEmployeeId(String employeeId) {
        ElementsActions.clearAndEnterText(driver, employeeIdField, employeeId);
        return this;
    }

    @Step("Change other id to {otherId}")
    public EmployeeDetailsPage setOtherId(String otherId) {
        ElementsActions.clearAndEnterText(driver, otherIdField, otherId);
        return this;
    }

    /** Empties a required field, so the form can be saved into a rejection. */
    @Step("Clear the last name field")
    public EmployeeDetailsPage clearLastName() {
        ElementsActions.clearAndEnterText(driver, lastNameField, "");
        return this;
    }

    @Step("Save the Personal Details form")
    public EmployeeDetailsPage savePersonalDetails() {
        // A leftover toast from a previous action sits over the button.
        ElementsActions.dismissToast(driver);
        ElementsActions.clickElement(driver, personalDetailsSave);
        captureToast();
        Waits.waitForLoaderToDisappear(driver);
        return this;
    }

    @Step("Reload the employee record")
    public EmployeeDetailsPage reload() {
        refreshPage();
        Waits.waitForElementVisible(driver, firstNameField);
        return waitForRecordToLoad();
    }

    /** Leaves the record and comes back by URL -- a stronger persistence check than F5. */
    @Step("Navigate away and return to this employee record")
    public EmployeeDetailsPage navigateAwayAndReturn() {
        String recordUrl = driver.getCurrentUrl();
        new DashboardPage().openModule("Dashboard");
        Waits.waitForUrlContains(driver, "dashboard");
        driver.get(recordUrl);
        Waits.waitForElementVisible(driver, firstNameField);
        Waits.waitForLoaderToDisappear(driver);
        return waitForRecordToLoad();
    }
}
