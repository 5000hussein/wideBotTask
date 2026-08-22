package Pages;

import Util.ElementsActions;
import Util.Waits;
import io.qameta.allure.Step;
import org.openqa.selenium.By;

/**
 * Leave &gt; Entitlements &gt; Add Entitlements.
 *
 * A newly created employee has a zero balance for every leave type, and
 * OrangeHRM refuses to assign leave against a zero balance without an override.
 * Granting an entitlement first is what makes the leave scenario deterministic
 * rather than dependent on whatever the shared demo data happens to hold.
 */
public class LeaveEntitlementPage extends BasePage {

    private static final String PATH = "/web/index.php/leave/addLeaveEntitlement";

    private final By employeeNameInput = By.xpath(
            "//div[contains(@class,'oxd-input-group')][.//label[normalize-space()='Employee Name']]"
                    + "//input[@placeholder='Type for hints...']");
    private final By leaveTypeDropdown = dropdownByLabel("Leave Type");
    private final By leavePeriodDropdown = dropdownByLabel("Leave Period");
    private final By entitlementField = inputByLabel("Entitlement");
    private final By saveButton = By.cssSelector("button[type='submit']");
    private final By confirmButton = By.xpath("//button[contains(normalize-space(),'Confirm')]");

    @Step("Open Leave > Add Entitlement")
    public LeaveEntitlementPage open() {
        openPath(PATH);
        Waits.waitForElementVisible(driver, entitlementField);
        return this;
    }

    @Step("Select employee {fullName}")
    public boolean selectEmployee(String fullName) {
        return selectEmployeeByName(employeeNameInput, fullName);
    }

    @Step("Select leave type {leaveType}")
    public LeaveEntitlementPage selectLeaveType(String leaveType) {
        ElementsActions.selectFromOxdDropdown(driver, leaveTypeDropdown, leaveType);
        return this;
    }

    /** Every leave type the environment currently offers. */
    public java.util.List<String> getAvailableLeaveTypes() {
        return ElementsActions.getOxdDropdownRealOptions(driver, leaveTypeDropdown);
    }

    public String getSelectedLeavePeriod() {
        return ElementsActions.getOxdDropdownValue(driver, leavePeriodDropdown);
    }

    @Step("Set entitlement to {days} days")
    public LeaveEntitlementPage setEntitlement(String days) {
        ElementsActions.clearAndEnterText(driver, entitlementField, days);
        return this;
    }

    @Step("Save the entitlement")
    public LeaveEntitlementPage save() {
        ElementsActions.clickElement(driver, saveButton);
        // OrangeHRM raises an "Updating Entitlement" confirmation for some changes.
        if (Waits.isElementVisible(driver, confirmButton, 5)) {
            ElementsActions.clickElement(driver, confirmButton);
        }
        captureToast();
        Waits.waitForLoaderToDisappear(driver);
        return this;
    }

    /** Grants {days} of {leaveType} to an employee in one call. */
    @Step("Grant {days} days of {leaveType} to {fullName}")
    public boolean grantEntitlement(String fullName, String leaveType, String days) {
        open();
        if (!selectEmployee(fullName)) {
            return false;
        }
        selectLeaveType(leaveType);
        setEntitlement(days);
        save();
        return wasLastActionSuccessful();
    }
}
