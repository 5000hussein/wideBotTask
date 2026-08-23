package Pages;

import Util.Config;
import Util.Drivers;
import Util.ElementsActions;
import Util.Waits;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public abstract class BasePage {
    protected final WebDriver driver;

    protected BasePage() {
        this.driver = Drivers.getDriver();
    }

    //PageActions
    protected void openPath(String path) {
        driver.get(Config.getInstance().getBaseUrl() + path);
        Waits.waitForLoaderToDisappear(driver);
    }

    public boolean isModuleForbidden() {
        return Waits.isElementVisible(driver,
                By.xpath("//*[contains(normalize-space(),'Module Forbidden')]"), 3);
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
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
