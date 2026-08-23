package Pages;

import Util.ElementsActions;
import Util.Validations;
import Util.Waits;
import io.qameta.allure.Step;
import org.openqa.selenium.By;

public class LeaveEntitlementPage extends BasePage {
    private static final String PATH = "/web/index.php/leave/addLeaveEntitlement";

    //Locators
    private final By employeeNameInput = By.xpath(
            "//div[contains(@class,'oxd-input-group')][.//label[normalize-space()='Employee Name']]"
                    + "//input[@placeholder='Type for hints...']");
    private final By leaveTypeDropdown = dropdownByLabel("Leave Type");
    private final By entitlementField = inputByLabel("Entitlement");
    private final By saveButton = By.cssSelector("button[type='submit']");
    private final By confirmButton = By.xpath("//button[contains(normalize-space(),'Confirm')]");

    //PageActions
    @Step("Open Leave > Add Entitlement")
    public void open() {
        openPath(PATH);
        Waits.waitForElementVisible(driver, entitlementField);
    }

    @Step("Select employee {fullName}")
    public boolean selectEmployee(String fullName) {
        return selectEmployeeByName(employeeNameInput, fullName);
    }

    @Step("Select leave type {leaveType}")
    public void selectLeaveType(String leaveType) {
        ElementsActions.selectFromOxdDropdown(driver, leaveTypeDropdown, leaveType);
    }

    @Step("Take the first leave type this environment offers")
    public String getFirstAvailableLeaveType() {
        java.util.List<String> types = ElementsActions.getOxdDropdownRealOptions(driver, leaveTypeDropdown);
        Validations.validateFalse(types.isEmpty(),
                "The environment should define at least one leave type");
        return types.get(0);
    }

    @Step("Set entitlement to {days} days")
    public void setEntitlement(String days) {
        ElementsActions.clearAndEnterText(driver, entitlementField, days);
    }

    @Step("Save the entitlement")
    public void save() {
        ElementsActions.clickElement(driver, saveButton);

        if (Waits.isElementVisible(driver, confirmButton, 5)) {
            ElementsActions.clickElement(driver, confirmButton);
        }
        captureToast();
        Waits.waitForLoaderToDisappear(driver);
    }

    //PageAssertions
    @Step("Verify {fullName} can be entitled")
    public void verifyEmployeeIsSelectable(String fullName) {
        Validations.validateTrue(selectEmployee(fullName),
                "The newly created employee should be selectable for an entitlement");
    }

    @Step("Verify the entitlement was granted")
    public void verifyEntitlementGranted() {
        Validations.validateTrue(wasLastActionSuccessful(),
                "Granting the entitlement should be confirmed; toast was: '"
                        + getLastToastText() + "'");
    }
}
