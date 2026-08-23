package Pages;

import Util.Config;
import Util.Drivers;
import Util.ElementsActions;
import Util.Waits;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public abstract class BasePage {
    protected final WebDriver driver;

    //Locators
    protected static final By BREADCRUMB_MODULE = By.cssSelector(".oxd-topbar-header-breadcrumb-module");
    protected static final By USER_DROPDOWN_NAME = By.cssSelector(".oxd-userdropdown-name");
    protected static final By USER_DROPDOWN_TAB = By.cssSelector(".oxd-userdropdown-tab");

    protected BasePage() {
        this.driver = Drivers.getDriver();
    }

    //PageActions
    protected void openPath(String path) {
        driver.get(Config.getInstance().getBaseUrl() + path);
        Waits.waitForLoaderToDisappear(driver);
    }

    public void openModule(String moduleName) {
        By module = By.xpath("//aside//a[.//span[normalize-space()='" + moduleName + "']]");
        ElementsActions.clickElement(driver, module);
        Waits.waitForLoaderToDisappear(driver);
    }

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

    private String lastToastText = "";
    private boolean lastToastWasSuccess = false;

    protected void captureToast() {
        lastToastText = ElementsActions.getToastMessage(driver);
        lastToastWasSuccess = !lastToastText.isBlank()
                && !Waits.isElementVisible(driver, By.cssSelector(".oxd-toast--error"), 1);
    }

    public String getLastToastText() {
        return lastToastText;
    }

    public boolean wasLastActionSuccessful() {
        return lastToastWasSuccess;
    }

    public boolean isSuccessToastDisplayed() {
        return Waits.isElementVisible(driver, By.cssSelector(".oxd-toast--success"), 15);
    }

    public void dismissToast() {
        ElementsActions.dismissToast(driver);
    }

    public String getFieldErrorFor(String label) {
        By error = By.xpath("//div[contains(@class,'oxd-input-group')]"
                + "[.//label[normalize-space()=" + quote(label) + "]]"
                + "//span[contains(@class,'oxd-input-field-error-message')]");
        return Waits.isElementVisible(driver, error, 5) ? ElementsActions.getText(driver, error) : "";
    }

    protected static String quote(String value) {
        if (!value.contains("'")) {
            return "'" + value + "'";
        }
        if (!value.contains("\"")) {
            return "\"" + value + "\"";
        }
        return "concat('" + value.replace("'", "',\"'\",'") + "')";
    }

    protected static By inputByLabel(String label) {
        return By.xpath("//div[contains(@class,'oxd-input-group')]"
                + "[.//label[normalize-space()=" + quote(label) + "]]"
                + "//input[not(@type='hidden')]");
    }

    protected boolean selectEmployeeByName(By autocompleteInput, String fullName) {
        String mostSelectiveToken = fullName.substring(fullName.lastIndexOf(' ') + 1);
        return ElementsActions.selectFromAutocomplete(driver, autocompleteInput, mostSelectiveToken, fullName);
    }

    protected static By dropdownByLabel(String label) {
        return By.xpath("//div[contains(@class,'oxd-input-group')]"
                + "[.//label[normalize-space()=" + quote(label) + "]]"
                + "//div[contains(@class,'oxd-select-text')]");
    }
}
