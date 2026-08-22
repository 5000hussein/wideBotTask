package Util;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

import java.time.Duration;

/**
 * WebDriver lifecycle owner.
 *
 * The driver lives in a ThreadLocal so a TestNG suite can run with
 * parallel="classes" without the tests fighting over one browser.
 * Binaries are resolved by Selenium Manager (Selenium 4.6+), so there is no
 * WebDriverManager dependency and nothing to install by hand.
 */
public final class Drivers {

    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

    private Drivers() {
    }

    public static WebDriver setUpDriver(String browserName, boolean headless) {
        WebDriver driver = switch (browserName == null ? "chrome" : browserName.toLowerCase()) {
            case "firefox" -> new FirefoxDriver(firefoxOptions(headless));
            case "edge" -> new EdgeDriver(edgeOptions(headless));
            default -> new ChromeDriver(chromeOptions(headless));
        };

        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(ConfigReader.pageLoadTimeout()));
        // No implicit wait on purpose: mixing implicit and explicit waits makes
        // timeouts unpredictable. Every wait in this framework is explicit.
        if (!headless) {
            driver.manage().window().maximize();
        }

        DRIVER.set(driver);
        return driver;
    }

    public static WebDriver setUpDriver() {
        return setUpDriver(ConfigReader.browser(), ConfigReader.headless());
    }

    public static WebDriver getDriver() {
        WebDriver driver = DRIVER.get();
        if (driver == null) {
            throw new IllegalStateException(
                    "WebDriver was not initialised for thread " + Thread.currentThread().getName()
                            + ". BaseTest#setUp is responsible for creating it.");
        }
        return driver;
    }

    public static boolean hasDriver() {
        return DRIVER.get() != null;
    }

    public static void quitDriver() {
        WebDriver driver = DRIVER.get();
        if (driver != null) {
            try {
                driver.quit();
            } finally {
                DRIVER.remove();
            }
        }
    }

    private static ChromeOptions chromeOptions(boolean headless) {
        ChromeOptions options = new ChromeOptions();
        if (headless) {
            options.addArguments("--headless=new", "--window-size=1920,1080");
        }
        options.addArguments(
                "--no-sandbox",
                "--disable-dev-shm-usage",
                "--disable-gpu",
                "--remote-allow-origins=*",
                "--disable-notifications",
                "--disable-search-engine-choice-screen");
        return options;
    }

    private static FirefoxOptions firefoxOptions(boolean headless) {
        FirefoxOptions options = new FirefoxOptions();
        if (headless) {
            options.addArguments("-headless", "--width=1920", "--height=1080");
        }
        return options;
    }

    private static EdgeOptions edgeOptions(boolean headless) {
        EdgeOptions options = new EdgeOptions();
        if (headless) {
            options.addArguments("--headless=new", "--window-size=1920,1080");
        }
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu");
        return options;
    }
}
