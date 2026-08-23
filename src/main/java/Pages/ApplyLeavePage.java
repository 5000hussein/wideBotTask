package Pages;

import Util.Validations;
import Util.Waits;
import io.qameta.allure.Step;
import org.openqa.selenium.By;

public class ApplyLeavePage extends BasePage {
    private static final String PATH = "/web/index.php/leave/applyLeave";

    //Locators
    private final By noBalanceMessage = By.xpath(
            "//*[contains(normalize-space(),'No Leave Types with Leave Balance')]");
    private final By leaveTypeDropdown = dropdownByLabel("Leave Type");
    private final By applyButton = By.cssSelector("button[type='submit']");
    private final By screenResolved = By.xpath(
            "//*[contains(normalize-space(),'No Leave Types with Leave Balance')]"
                    + " | //div[contains(@class,'oxd-input-group')]"
                    + "[.//label[normalize-space()='Leave Type']]"
                    + "//div[contains(@class,'oxd-select-text')]");

    //PageActions
    @Step("Open Leave > Apply")
    public void open() {
        openPath(PATH);
        Waits.waitForLoaderToDisappear(driver);
    }

    public boolean isNoLeaveBalanceMessageDisplayed() {
        return Waits.isElementVisible(driver, noBalanceMessage, 10);
    }

    public boolean isApplyFormAvailable() {
        return Waits.isElementVisible(driver, leaveTypeDropdown, 5)
                && Waits.isElementVisible(driver, applyButton, 5);
    }

    //PageAssertions
    @Step("Verify the Apply screen matches the account's entitlement state")
    public void verifyScreenMatchesEntitlementState() {
        Validations.validateTrue(Waits.isElementVisible(driver, screenResolved, 15),
                "Apply Leave should render either the request form or the no-balance message");

        if (isNoLeaveBalanceMessageDisplayed()) {
            Validations.validateFalse(isApplyFormAvailable(),
                    "The request form must not be offered when there is no balance to spend");
        } else {
            Validations.validateTrue(isApplyFormAvailable(),
                    "An account holding a balance should be offered a usable request form");
        }
    }
}
