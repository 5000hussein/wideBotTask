package Util;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public final class Scrolling {

    private Scrolling() {
    }

    public static void scrollToElement(WebDriver driver, By locator) {
        scrollToElement(driver, driver.findElement(locator));
    }

    /**
     * Scrolls to block:center rather than the top of the viewport: OrangeHRM has
     * a sticky top bar that covers anything flush to the top, which surfaces as
     * ElementClickInterceptedException.
     */
    public static void scrollToElement(WebDriver driver, WebElement element) {
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center', inline:'nearest'});", element);
    }

    public static void scrollToTop(WebDriver driver) {
        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0);");
    }

    public static void scrollToBottom(WebDriver driver) {
        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight);");
    }
}
