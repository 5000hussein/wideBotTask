package Pages;

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

    @Step("Wait until the Apply Leave screen shows the form or the no-balance message")
    public boolean waitForScreenToResolve() {
        return Waits.isElementVisible(driver, screenResolved, 15);
    }
}
