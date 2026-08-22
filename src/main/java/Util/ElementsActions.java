package Util;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Interaction layer. Page objects never touch driver.findElement directly --
 * they go through here so that waiting, scrolling and the OrangeHRM-specific
 * quirks are handled in exactly one place.
 */
public final class ElementsActions {

    private ElementsActions() {
    }

    // ---------------------------------------------------------------- basics

    public static void clickElement(WebDriver driver, By locator) {
        // OrangeHRM overlays a full-form loader on navigation and on every save.
        // It is transparent to waitForElementClickable but absolutely does eat
        // the click, so nothing may be interacted with until it has gone.
        Waits.waitForLoaderToDisappear(driver);
        WebElement element = Waits.waitForElementClickable(driver, locator);
        Scrolling.scrollToElement(driver, element);
        try {
            element.click();
        } catch (ElementClickInterceptedException e) {
            // A toast or the sticky header ate the click -- go through the DOM.
            javascriptClick(driver, element);
        }
    }

    public static void clickElement(WebDriver driver, WebElement element) {
        Scrolling.scrollToElement(driver, element);
        try {
            element.click();
        } catch (ElementClickInterceptedException e) {
            javascriptClick(driver, element);
        }
    }

    public static void javascriptClick(WebDriver driver, WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }

    public static void enterText(WebDriver driver, By locator, String text) {
        WebElement element = Waits.waitForElementClickable(driver, locator);
        Scrolling.scrollToElement(driver, element);
        element.sendKeys(text);
    }

    /**
     * Clears then types. element.clear() alone is not enough on OrangeHRM: the
     * Vue model does not always observe a programmatic clear, so the old value
     * comes back on save. Ctrl+A followed by the new value is what actually sticks.
     */
    public static void clearAndEnterText(WebDriver driver, By locator, String text) {
        Waits.waitForLoaderToDisappear(driver);
        WebElement element = Waits.waitForElementClickable(driver, locator);
        Scrolling.scrollToElement(driver, element);
        element.click();
        element.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
        element.sendKeys(text);
    }

    public static String getText(WebDriver driver, By locator) {
        return Waits.waitForElementVisible(driver, locator).getText().trim();
    }

    public static String getValue(WebDriver driver, By locator) {
        String value = Waits.waitForElementVisible(driver, locator).getDomProperty("value");
        return value == null ? "" : value.trim();
    }

    public static List<String> getAllTexts(WebDriver driver, By locator) {
        return Waits.waitForAllElementsVisible(driver, locator).stream()
                .map(WebElement::getText)
                .map(String::trim)
                .collect(Collectors.toList());
    }

    public static boolean isDisplayed(WebDriver driver, By locator) {
        return Waits.isElementVisible(driver, locator, ConfigReader.explicitWait());
    }

    /** Short-timeout variant for "this should NOT be here" assertions. */
    public static boolean isDisplayedQuick(WebDriver driver, By locator) {
        return Waits.isElementVisible(driver, locator, 3);
    }

    public static int countElements(WebDriver driver, By locator) {
        return driver.findElements(locator).size();
    }

    // ------------------------------------------------- OrangeHRM oxd widgets

    /**
     * Selects a value from an oxd-select (OrangeHRM's custom dropdown -- it is
     * NOT a native select element, so the Selenium Select class does not apply).
     *
     * @param dropdownLocator the .oxd-select-text container to open
     * @param optionText      visible text of the option to pick
     */
    public static void selectFromOxdDropdown(WebDriver driver, By dropdownLocator, String optionText) {
        By anyOption = By.xpath("//div[@role='option']");
        String wanted = normaliseSpaces(optionText);

        // Options are matched by iterating the rendered elements rather than by
        // building an XPath predicate on their text. Option labels come straight
        // from user-entered configuration and routinely carry doubled spaces,
        // non-breaking spaces and quotes, none of which survive being pasted
        // into an XPath string comparison.
        WebElement match;
        try {
            match = Waits.retryOnStale(driver, d -> {
                if (d.findElements(anyOption).isEmpty()) {
                    clickElement(d, dropdownLocator);
                }
                for (WebElement option : d.findElements(anyOption)) {
                    if (normaliseSpaces(option.getText()).equalsIgnoreCase(wanted)) {
                        return option;
                    }
                }
                return null;
            });
        } catch (org.openqa.selenium.TimeoutException e) {
            // Name the option and list what was actually on offer: an opaque
            // "condition failed on a lambda" tells a reader nothing about which
            // value was missing.
            List<String> offered = getAllTexts(driver, anyOption);
            throw new IllegalStateException("Dropdown option '" + optionText
                    + "' was not offered. Available options: " + offered, e);
        }

        clickElement(driver, match);
        Waits.waitForLoaderToDisappear(driver);
    }

    /** Reads the currently selected value out of an oxd-select. */
    public static String getOxdDropdownValue(WebDriver driver, By dropdownLocator) {
        return getText(driver, dropdownLocator);
    }

    /** Returns every option in an oxd-select, then closes it again. */
    public static List<String> getOxdDropdownOptions(WebDriver driver, By dropdownLocator) {
        clickElement(driver, dropdownLocator);
        By options = By.xpath("//div[@role='option']");
        List<String> values = getAllTexts(driver, options);
        closeOpenDropdown(driver);
        return values;
    }

    /**
     * Returns the real, selectable options of an oxd-select.
     *
     * These dropdowns are populated by a request that resolves AFTER the page is
     * otherwise interactive, and until it does they render a placeholder such as
     * "-- Select --" or "No leave types defined" as a normal option. Reading them
     * once therefore reports an empty environment for an environment that is
     * merely still loading, so the read is retried until real values appear.
     *
     * @return the selectable options, or an empty list if there genuinely are none
     */
    public static List<String> getOxdDropdownRealOptions(WebDriver driver, By dropdownLocator) {
        By anyOption = By.xpath("//div[@role='option']");

        // Start from a known state. Reading two dropdowns in a row otherwise
        // fails on the second: the click meant to open it instead lands on the
        // first dropdown's overlay and merely dismisses that, so no options ever
        // render and the environment looks like it has none configured.
        closeOpenDropdown(driver);
        try {
            return Waits.retryOnStale(driver, d -> {
                if (d.findElements(anyOption).isEmpty()) {
                    clickElement(d, dropdownLocator);
                    return null; // give the options a poll interval to render
                }
                List<String> real = d.findElements(anyOption).stream()
                        .map(WebElement::getText)
                        .map(ElementsActions::normaliseSpaces)
                        .filter(value -> !value.isEmpty())
                        .filter(value -> !value.startsWith("--"))
                        .filter(value -> !isSearchingPlaceholder(value))
                        .filter(value -> !value.toLowerCase().startsWith("no "))
                        .collect(Collectors.toList());
                return real.isEmpty() ? null : real;
            });
        } catch (org.openqa.selenium.TimeoutException e) {
            return List.of();
        } finally {
            closeOpenDropdown(driver);
        }
    }

    /**
     * Closes an open oxd-select.
     *
     * The dropdown is a div, so sendKeys on it throws ElementNotInteractable --
     * the key has to go to the document instead. An open dropdown left behind
     * covers the next field on the form.
     */
    public static void closeOpenDropdown(WebDriver driver) {
        new Actions(driver).sendKeys(Keys.ESCAPE).perform();
        Waits.waitForElementInvisible(driver, By.xpath("//div[@role='option']"), 5);
    }

    /**
     * Drives an oxd autocomplete (Employee Name, Supervisor Name, ...). These
     * fields validate against the suggestion list, so typing alone leaves the
     * form in an "Invalid" state -- a suggestion must actually be clicked.
     *
     * @return true if a suggestion was selected, false if the field reported no match
     */
    public static boolean selectFromAutocomplete(WebDriver driver, By inputLocator, String typedText,
                                                 String optionContains) {
        clearAndEnterText(driver, inputLocator, typedText);

        By anyOption = By.cssSelector("[role='listbox'] [role='option']");
        if (!Waits.isElementVisible(driver, anyOption, 12)) {
            return false;
        }

        // The dropdown publishes a transient in-progress entry as a real
        // [role=option] while the request is in flight. Reading the list as soon
        // as an option exists therefore sees only that placeholder and concludes
        // "no match" for an employee that does exist. Wait for it to clear first.
        //
        // Matched as a PREFIX deliberately: the application renders it as
        // "Searching...." with four dots, so an exact comparison against
        // "Searching..." silently never matches and the placeholder gets treated
        // as a genuine suggestion.
        List<WebElement> options;
        try {
            options = Waits.retryOnStale(driver, d -> {
                List<WebElement> found = d.findElements(anyOption);
                if (found.isEmpty()) {
                    return null;
                }
                boolean stillSearching = found.stream()
                        .anyMatch(option -> isSearchingPlaceholder(option.getText()));
                return stillSearching ? null : found;
            });
        } catch (org.openqa.selenium.TimeoutException e) {
            // The suggestion request never resolved. That is "no match available"
            // from the caller's point of view -- reported as a return value so the
            // caller can assert on it, rather than as an exception that surfaces
            // as an opaque lambda timeout in the report.
            return false;
        }

        // "No Records Found" is rendered AS an option rather than as a separate
        // element, so a genuine no-match is detected by reading the text.
        String wanted = normaliseSpaces(optionContains).toLowerCase();
        WebElement match = null;
        for (WebElement option : options) {
            String text = normaliseSpaces(option.getText());
            if (text.equalsIgnoreCase("No Records Found")) {
                return false;
            }
            if (isSearchingPlaceholder(text)) {
                continue;
            }
            // Whitespace is collapsed on both sides: OrangeHRM joins first,
            // middle and last name unconditionally, so an employee with no
            // middle name renders as "Jane  Doe" with a double space.
            if (text.toLowerCase().contains(wanted)) {
                match = option;
                break;
            }
        }
        if (match == null) {
            return false;
        }

        clickElement(driver, match);
        return true;
    }

    /** Collapses runs of whitespace to a single space. */
    public static String normaliseSpaces(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    /** True for the dropdown's in-flight placeholder, however many dots it uses. */
    private static boolean isSearchingPlaceholder(String text) {
        String normalised = normaliseSpaces(text).toLowerCase();
        return normalised.isEmpty() || normalised.startsWith("searching");
    }

    /**
     * Types into an oxd date field and closes the picker.
     * The calendar overlay stays open after typing and intercepts the next
     * click, so ESCAPE here is load-bearing, not cosmetic.
     */
    public static void setDate(WebDriver driver, By dateInputLocator, String date) {
        WebElement input = Waits.waitForElementClickable(driver, dateInputLocator);
        Scrolling.scrollToElement(driver, input);
        input.click();
        input.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
        input.sendKeys(date);
        input.sendKeys(Keys.ESCAPE);
    }

    /** Text of the currently displayed toast, or empty if none appeared. */
    public static String getToastMessage(WebDriver driver) {
        By toastBody = By.cssSelector(".oxd-toast-content");
        if (!Waits.isElementVisible(driver, toastBody, 15)) {
            return "";
        }
        return getText(driver, toastBody);
    }

    /**
     * Dismisses any toast that is on screen. Toasts sit above the form and will
     * intercept clicks on whatever is underneath them for their full lifetime.
     */
    public static void dismissToast(WebDriver driver) {
        By closeButton = By.cssSelector(".oxd-toast-close");
        List<WebElement> buttons = driver.findElements(closeButton);
        for (WebElement button : buttons) {
            try {
                javascriptClick(driver, button);
            } catch (RuntimeException ignored) {
                // The toast expired on its own between find and click -- fine.
            }
        }
        Waits.waitForElementInvisible(driver, By.cssSelector(".oxd-toast"), 10);
    }
}
