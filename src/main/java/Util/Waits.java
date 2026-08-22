package Util;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

public final class Waits {
    public static final By LOADER = By.cssSelector(".oxd-loading-spinner, .oxd-form-loader");

    private Waits() {
    }

    private static WebDriverWait wait(WebDriver driver) {
        return wait(driver, ConfigReader.explicitWait());
    }

    private static WebDriverWait wait(WebDriver driver, int seconds) {
        WebDriverWait webDriverWait = new WebDriverWait(driver, Duration.ofSeconds(seconds));
        webDriverWait.ignoring(StaleElementReferenceException.class);
        return webDriverWait;
    }

    public static WebElement waitForElementPresent(WebDriver driver, By locator) {
        return wait(driver).until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    public static WebElement waitForElementVisible(WebDriver driver, By locator) {
        return wait(driver).until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public static WebElement waitForElementVisible(WebDriver driver, By locator, int seconds) {
        return wait(driver, seconds).until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public static WebElement waitForElementClickable(WebDriver driver, By locator) {
        return wait(driver).until(ExpectedConditions.elementToBeClickable(locator));
    }

    public static List<WebElement> waitForAllElementsVisible(WebDriver driver, By locator) {
        return wait(driver).until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
    }

    public static boolean waitForElementInvisible(WebDriver driver, By locator, int seconds) {
        try {
            return wait(driver, seconds).until(ExpectedConditions.invisibilityOfElementLocated(locator));
        } catch (TimeoutException e) {
            return false;
        }
    }

    public static boolean waitForUrlContains(WebDriver driver, String fragment) {
        return wait(driver).until(ExpectedConditions.urlContains(fragment));
    }

    public static void waitForLoaderToDisappear(WebDriver driver) {
        waitForElementInvisible(driver, LOADER, ConfigReader.explicitWait());
    }

    public static boolean isElementVisible(WebDriver driver, By locator, int seconds) {
        try {
            wait(driver, seconds).until(ExpectedConditions.visibilityOfElementLocated(locator));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    public static <T> T retryOnStale(WebDriver driver, Function<WebDriver, T> action) {
        FluentWait<WebDriver> fluentWait = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(ConfigReader.explicitWait()))
                .pollingEvery(Duration.ofMillis(300))
                .ignoring(StaleElementReferenceException.class)
                .ignoring(NoSuchElementException.class);
        return fluentWait.until(action);
    }
}
