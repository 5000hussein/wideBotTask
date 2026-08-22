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

public final class ElementsActions {
    private ElementsActions() {
    }

    public static void clickElement(WebDriver driver, By locator) {
        Waits.waitForLoaderToDisappear(driver);
        WebElement element = Waits.waitForElementClickable(driver, locator);
        Scrolling.scrollToElement(driver, element);
        try {
            element.click();
        } catch (ElementClickInterceptedException e) {
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
        return Waits.isElementVisible(driver, locator, Config.getInstance().getExplicitWait());
    }

    public static int countElements(WebDriver driver, By locator) {
        return driver.findElements(locator).size();
    }

    public static void selectFromOxdDropdown(WebDriver driver, By dropdownLocator, String optionText) {
        By anyOption = By.xpath("//div[@role='option']");
        String wanted = normaliseSpaces(optionText);

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
            List<String> offered = getAllTexts(driver, anyOption);
            throw new IllegalStateException("Dropdown option '" + optionText
                    + "' was not offered. Available options: " + offered, e);
        }

        clickElement(driver, match);
        Waits.waitForLoaderToDisappear(driver);
    }

    public static String getOxdDropdownValue(WebDriver driver, By dropdownLocator) {
        return getText(driver, dropdownLocator);
    }

    public static List<String> getOxdDropdownRealOptions(WebDriver driver, By dropdownLocator) {
        By anyOption = By.xpath("//div[@role='option']");

        closeOpenDropdown(driver);
        try {
            return Waits.retryOnStale(driver, d -> {
                if (d.findElements(anyOption).isEmpty()) {
                    clickElement(d, dropdownLocator);
                    return null;
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

    public static void closeOpenDropdown(WebDriver driver) {
        new Actions(driver).sendKeys(Keys.ESCAPE).perform();
        Waits.waitForElementInvisible(driver, By.xpath("//div[@role='option']"), 5);
    }

    public static boolean selectFromAutocomplete(WebDriver driver, By inputLocator, String typedText,
                                                 String optionContains) {
        clearAndEnterText(driver, inputLocator, typedText);

        By anyOption = By.cssSelector("[role='listbox'] [role='option']");
        if (!Waits.isElementVisible(driver, anyOption, 12)) {
            return false;
        }

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
            return false;
        }

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

    public static String normaliseSpaces(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    private static boolean isSearchingPlaceholder(String text) {
        String normalised = normaliseSpaces(text).toLowerCase();
        return normalised.isEmpty() || normalised.startsWith("searching");
    }

    public static void setDate(WebDriver driver, By dateInputLocator, String date) {
        WebElement input = Waits.waitForElementClickable(driver, dateInputLocator);
        Scrolling.scrollToElement(driver, input);
        input.click();
        input.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
        input.sendKeys(date);
        input.sendKeys(Keys.ESCAPE);
    }

    public static String getToastMessage(WebDriver driver) {
        By toastBody = By.cssSelector(".oxd-toast-content");
        if (!Waits.isElementVisible(driver, toastBody, 15)) {
            return "";
        }
        return getText(driver, toastBody);
    }

    public static void dismissToast(WebDriver driver) {
        By closeButton = By.cssSelector(".oxd-toast-close");
        List<WebElement> buttons = driver.findElements(closeButton);
        for (WebElement button : buttons) {
            try {
                javascriptClick(driver, button);
            } catch (RuntimeException ignored) {
            }
        }
        Waits.waitForElementInvisible(driver, By.cssSelector(".oxd-toast"), 10);
    }
}
