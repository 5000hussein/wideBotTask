package Pages;

import Util.ConfigReader;
import Util.Drivers;
import Util.ElementsActions;
import Util.Waits;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Shared behaviour for every screen: the driver handle, navigation through the
 * left menu / top tabs, and the toast + breadcrumb readers that nearly every
 * assertion in the suite ends up using.
 *
 * Page objects expose intent ("searchByEmployeeName") and never assertions --
 * the tests own the assertions so that a page object stays reusable by a
 * positive and a negative test alike.
 */
public abstract class BasePage {

    protected final WebDriver driver;

    // ---- chrome that is present on every authenticated screen ----
    protected static final By BREADCRUMB_MODULE = By.cssSelector(".oxd-topbar-header-breadcrumb-module");
    protected static final By USER_DROPDOWN_NAME = By.cssSelector(".oxd-userdropdown-name");
    protected static final By USER_DROPDOWN_TAB = By.cssSelector(".oxd-userdropdown-tab");
    protected static final By SIDE_MENU_ITEM = By.cssSelector(".oxd-main-menu-item");
    protected static final By TOAST = By.cssSelector(".oxd-toast");
    protected static final By TOAST_TITLE = By.cssSelector(".oxd-toast .oxd-text--toast-title");
    protected static final By TOAST_MESSAGE = By.cssSelector(".oxd-toast .oxd-text--toast-message");
    protected static final By FIELD_ERROR = By.cssSelector(".oxd-input-field-error-message");

    protected BasePage() {
        this.driver = Drivers.getDriver();
    }

    // ------------------------------------------------------------ navigation

    /**
     * Navigates to a route and waits for the application shell to exist before
     * anything else looks for an element.
     *
     * driver.get() returns once the document is loaded, but this is a Vue SPA:
     * at that moment the page can still be an empty mount point, and the spinner
     * that waitForLoaderToDisappear looks for has not been rendered yet either --
     * so that wait returns instantly and the caller starts hunting for controls
     * on a blank page. Anchoring on the shell makes "the page is up" the
     * precondition it is supposed to be.
     */
    protected void openPath(String path) {
        driver.get(ConfigReader.baseUrl() + path);

        // Best-effort, and deliberately non-fatal. Waiting for the shell stops the
        // caller from hunting for controls on a not-yet-mounted page, but it must
        // never be the thing that fails a test: this is a generic selector, and
        // when it is the one that times out the report says "'.oxd-layout' not
        // present", which tells the reader nothing. Letting the caller's own
        // specific wait raise the error instead names the control that is
        // actually missing.
        try {
            Waits.waitForElementPresent(driver, By.cssSelector(
                    ".oxd-layout, .orangehrm-login-layout, .orangehrm-login-container,"
                            + " .oxd-main-menu, form"));
        } catch (org.openqa.selenium.TimeoutException e) {
            System.err.println("Application shell did not render within the timeout for " + path
                    + "; continuing so the caller's own wait can report the specific control.");
        }
        Waits.waitForLoaderToDisappear(driver);
    }

    /** Clicks a module in the left sidebar, e.g. "PIM" or "Leave". */
    public void openModule(String moduleName) {
        By module = By.xpath("//aside//a[.//span[normalize-space()='" + moduleName + "']]");
        ElementsActions.clickElement(driver, module);
        Waits.waitForLoaderToDisappear(driver);
    }

    /** Clicks a tab in the module top bar, e.g. "Employee List" or "Assign Leave". */
    public void openTopBarTab(String tabName) {
        By tab = By.xpath("//nav[contains(@class,'oxd-topbar-body-nav')]//a[normalize-space()='" + tabName + "']");
        ElementsActions.clickElement(driver, tab);
        Waits.waitForLoaderToDisappear(driver);
    }

    /**
     * True when OrangeHRM served "403 Module Forbidden" instead of the screen.
     *
     * The shared demo's Admin role is edited by other users, and a module can
     * become inaccessible between runs. Detecting it explicitly turns a
     * twenty-second wait ending in "button[type='submit'] not visible" -- which
     * reads like a broken locator or a product defect -- into an accurate,
     * immediate statement about what the environment actually returned.
     */
    public boolean isModuleForbidden() {
        return Waits.isElementVisible(driver,
                By.xpath("//*[contains(normalize-space(),'Module Forbidden')]"), 3);
    }

    public String getBreadcrumbModule() {
        return ElementsActions.getText(driver, BREADCRUMB_MODULE);
    }

    public String getLoggedInUserName() {
        return ElementsActions.getText(driver, USER_DROPDOWN_NAME);
    }

    public boolean isUserMenuDisplayed() {
        return ElementsActions.isDisplayed(driver, USER_DROPDOWN_TAB);
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    public void refreshPage() {
        driver.navigate().refresh();
        Waits.waitForLoaderToDisappear(driver);
    }

    // ---------------------------------------------------------------- toasts

    private String lastToastText = "";
    private boolean lastToastWasSuccess = false;

    /**
     * Records the toast raised by the action that has just been performed.
     *
     * Must be called immediately after the triggering click, before any wait for
     * a loader or a redirect. OrangeHRM toasts auto-dismiss after a few seconds,
     * so a test that asks for the toast once the page has settled is racing the
     * timeout -- and when it loses, the failure reads "no success message" rather
     * than "we looked too late".
     */
    protected void captureToast() {
        lastToastText = ElementsActions.getToastMessage(driver);
        lastToastWasSuccess = !lastToastText.isBlank()
                && !Waits.isElementVisible(driver, By.cssSelector(".oxd-toast--error"), 1);
    }

    /** Toast text captured at the moment of the last action. */
    public String getLastToastText() {
        return lastToastText;
    }

    /** Whether the last captured toast was a success rather than an error. */
    public boolean wasLastActionSuccessful() {
        return lastToastWasSuccess;
    }

    /** Full toast text (title + body), or empty string if no toast appeared. */
    public String getToastText() {
        return ElementsActions.getToastMessage(driver);
    }

    public boolean isSuccessToastDisplayed() {
        return Waits.isElementVisible(driver, By.cssSelector(".oxd-toast--success"), 15);
    }

    public boolean isErrorToastDisplayed() {
        return Waits.isElementVisible(driver, By.cssSelector(".oxd-toast--error"), 10);
    }

    public void dismissToast() {
        ElementsActions.dismissToast(driver);
    }

    // -------------------------------------------------- field-level errors

    /** Every inline validation message currently rendered on the form. */
    public java.util.List<String> getFieldErrors() {
        if (!Waits.isElementVisible(driver, FIELD_ERROR, 5)) {
            return java.util.List.of();
        }
        return ElementsActions.getAllTexts(driver, FIELD_ERROR);
    }

    public boolean hasFieldError(String expectedText) {
        return getFieldErrors().stream().anyMatch(text -> text.equalsIgnoreCase(expectedText));
    }

    /**
     * The inline error attached to one specific labelled field, rather than
     * "some error exists somewhere on the form".
     */
    public String getFieldErrorFor(String label) {
        By error = By.xpath("//div[contains(@class,'oxd-input-group')]"
                + "[.//label[normalize-space()=" + quote(label) + "]]"
                + "//span[contains(@class,'oxd-input-field-error-message')]");
        return Waits.isElementVisible(driver, error, 5) ? ElementsActions.getText(driver, error) : "";
    }

    // -------------------------------------------------------------- helpers

    /**
     * Builds an XPath string literal that is safe for values containing quotes,
     * e.g. the "Driver's License Number" label.
     */
    protected static String quote(String value) {
        if (!value.contains("'")) {
            return "'" + value + "'";
        }
        if (!value.contains("\"")) {
            return "\"" + value + "\"";
        }
        return "concat('" + value.replace("'", "',\"'\",'") + "')";
    }

    /** The input inside the .oxd-input-group carrying the given label. */
    protected static By inputByLabel(String label) {
        return By.xpath("//div[contains(@class,'oxd-input-group')]"
                + "[.//label[normalize-space()=" + quote(label) + "]]"
                + "//input[not(@type='hidden')]");
    }

    /**
     * Drives an employee-name autocomplete.
     *
     * Types the LAST token of the name rather than the whole string: OrangeHRM
     * matches hints against individual name parts, and a full "first middle last"
     * string returns nothing for employees whose middle name is empty. The
     * suggestion is then matched against the complete name, so the right person
     * is still selected when several employees share a surname.
     */
    protected boolean selectEmployeeByName(By autocompleteInput, String fullName) {
        String mostSelectiveToken = fullName.substring(fullName.lastIndexOf(' ') + 1);
        return ElementsActions.selectFromAutocomplete(driver, autocompleteInput, mostSelectiveToken, fullName);
    }

    /** The oxd custom dropdown inside the .oxd-input-group carrying the given label. */
    protected static By dropdownByLabel(String label) {
        return By.xpath("//div[contains(@class,'oxd-input-group')]"
                + "[.//label[normalize-space()=" + quote(label) + "]]"
                + "//div[contains(@class,'oxd-select-text')]");
    }
}
