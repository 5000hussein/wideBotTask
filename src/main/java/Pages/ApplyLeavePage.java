package Pages;

import Util.ElementsActions;
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

    //PageActions
    @Step("Open Leave > Apply")
    public ApplyLeavePage open() {
        openPath(PATH);
        Waits.waitForLoaderToDisappear(driver);
        return this;
    }

    public boolean isNoLeaveBalanceMessageDisplayed() {
        return Waits.isElementVisible(driver, noBalanceMessage, 10);
    }

    public boolean isApplyFormAvailable() {
        return Waits.isElementVisible(driver, leaveTypeDropdown, 5)
                && Waits.isElementVisible(driver, applyButton, 5);
    }

    public enum State {
        NO_BALANCE,

        FORM_AVAILABLE,

        UNKNOWN
    }

    @Step("Determine the state of the Apply Leave screen")
    public State getState() {
        for (int attempt = 1; attempt <= 3; attempt++) {
            if (isNoLeaveBalanceMessageDisplayed()) {
                return State.NO_BALANCE;
            }
            if (isApplyFormAvailable()) {
                return State.FORM_AVAILABLE;
            }
            System.out.println("Apply Leave screen not resolved on attempt " + attempt + "; reloading.");
            if (attempt < 3) {
                open();
            }
        }
        return State.UNKNOWN;
    }
}
