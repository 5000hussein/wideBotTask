package Pages;

import Util.ElementsActions;
import Util.Validations;
import Util.Waits;
import io.qameta.allure.Step;
import org.openqa.selenium.By;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmployeeDetailsPage extends BasePage {
    //Locators
    private final By firstNameField = By.name("firstName");
    private final By middleNameField = By.name("middleName");
    private final By lastNameField = By.name("lastName");
    private final By employeeIdField = inputByLabel("Employee Id");

    private final By personalDetailsSave =
            By.xpath("//form[.//input[@name='firstName']]//button[@type='submit']");

    private final By nameBanner = By.cssSelector(".orangehrm-edit-employee-name");
    private final By tabs = By.cssSelector(".orangehrm-tabs-item");
    private final By personalDetailsHeading = By.xpath("//h6[normalize-space()='Personal Details']");

    //PageActions
    @Step("Wait for the employee record to finish loading")
    public void waitForRecordToLoad() {
        Waits.retryOnStale(driver, d -> {
            String firstName = d.findElement(firstNameField).getDomProperty("value");
            return firstName != null && !firstName.isBlank();
        });
    }

    public String getEmpNumberFromUrl() {
        Matcher matcher = Pattern.compile("empNumber/(\\d+)").matcher(driver.getCurrentUrl());
        return matcher.find() ? matcher.group(1) : "";
    }

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

    @Step("Change first name to {firstName}")
    public void setFirstName(String firstName) {
        ElementsActions.clearAndEnterText(driver, firstNameField, firstName);
    }

    @Step("Change employee id to {employeeId}")
    public void setEmployeeId(String employeeId) {
        ElementsActions.clearAndEnterText(driver, employeeIdField, employeeId);
    }

    @Step("Save the Personal Details form")
    public void savePersonalDetails() {
        ElementsActions.dismissToast(driver);
        ElementsActions.clickElement(driver, personalDetailsSave);
        captureToast();
        Waits.waitForLoaderToDisappear(driver);
    }

    @Step("Reload the employee record")
    public void reload() {
        refreshPage();
        Waits.waitForElementVisible(driver, firstNameField);
        waitForRecordToLoad();
    }

    @Step("Navigate away and return to this employee record")
    public void navigateAwayAndReturn() {
        String recordUrl = driver.getCurrentUrl();
        new DashboardPage().openModule("Dashboard");
        Waits.waitForUrlContains(driver, "dashboard");
        driver.get(recordUrl);
        Waits.waitForElementVisible(driver, firstNameField);
        Waits.waitForLoaderToDisappear(driver);
        waitForRecordToLoad();
    }

    //PageAssertions
    public void verifyEmployeeDetailsPageLoaded() {
        Validations.validateTrue(
                driver.getCurrentUrl().contains("viewPersonalDetails")
                        && ElementsActions.isDisplayed(driver, personalDetailsHeading)
                        && ElementsActions.isDisplayed(driver, firstNameField),
                "Employee record did not load");
    }
}
