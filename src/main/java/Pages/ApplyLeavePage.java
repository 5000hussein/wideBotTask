package Pages;

import Util.ElementsActions;
import Util.Waits;
import io.qameta.allure.Step;
import org.openqa.selenium.By;

/**
 * Leave → Apply -- the employee-facing self-service form.
 *
 * On the public demo the signed-in Admin holds no leave entitlement, so this
 * screen renders the message "No Leave Types with Leave Balance" instead of a
 * form. That is a genuine environment constraint, not a defect, and the suite
 * asserts it explicitly rather than pretending the screen works: an environment
 * change that grants the account a balance should make a test tell us so.
 */
public class ApplyLeavePage extends BasePage {

    private static final String PATH = "/web/index.php/leave/applyLeave";

    private final By noBalanceMessage = By.xpath(
            "//*[contains(normalize-space(),'No Leave Types with Leave Balance')]");
    private final By leaveTypeDropdown = dropdownByLabel("Leave Type");
    private final By applyButton = By.cssSelector("button[type='submit']");

    @Step("Open Leave > Apply")
    public ApplyLeavePage open() {
        openPath(PATH);
        Waits.waitForLoaderToDisappear(driver);
        return this;
    }

    /** True when the account has no balance and the form is suppressed. */
    public boolean isNoLeaveBalanceMessageDisplayed() {
        return Waits.isElementVisible(driver, noBalanceMessage, 10);
    }

    /** True when the account does have a balance and the form is usable. */
    public boolean isApplyFormAvailable() {
        return Waits.isElementVisible(driver, leaveTypeDropdown, 5)
                && Waits.isElementVisible(driver, applyButton, 5);
    }

    /** Which of the two legitimate states this screen is in. */
    public enum State {
        /** Account holds no entitlement; the explanatory message replaces the form. */
        NO_BALANCE,
        /** Account holds a balance; the apply form is rendered. */
        FORM_AVAILABLE,
        /** Neither -- the screen did not render as either supported state. */
        UNKNOWN
    }

    /**
     * Resolves the screen's state, checking for the no-balance message FIRST.
     *
     * Order matters: the message is the definitive signal and appears quickly,
     * whereas probing for form controls that are never going to exist just burns
     * the timeout and can transiently mis-report while the route is still
     * settling.
     */
    @Step("Determine the state of the Apply Leave screen")
    public State getState() {
        // Bounded re-check: under load this route occasionally renders its body
        // after the shell has settled, and a single look returns UNKNOWN for a
        // screen that is simply a moment behind. Three attempts, then report what
        // was actually seen rather than waiting indefinitely.
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
