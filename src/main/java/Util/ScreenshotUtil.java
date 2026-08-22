package Util;

import io.qameta.allure.Allure;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Screenshot capture for both purposes the assessment asks for:
 * named checkpoints on the happy path, and automatic evidence on failure.
 *
 * Every capture is written twice -- to screenshots/ on disk (so the files can
 * be committed and reviewed without opening a report) and into the Allure
 * result as an embedded attachment.
 */
public final class ScreenshotUtil {

    private static final Path SCREENSHOT_DIR = Paths.get("screenshots");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private ScreenshotUtil() {
    }

    /** Named checkpoint, e.g. "01-login-dashboard". */
    public static Path capture(String name) {
        if (!Drivers.hasDriver()) {
            return null;
        }
        return capture(Drivers.getDriver(), name);
    }

    public static Path capture(WebDriver driver, String name) {
        try {
            byte[] png = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);

            Allure.addAttachment(name, "image/png", new ByteArrayInputStream(png), ".png");

            Files.createDirectories(SCREENSHOT_DIR);
            String fileName = sanitise(name) + ".png";
            Path target = SCREENSHOT_DIR.resolve(fileName);
            Files.write(target, png);
            return target;
        } catch (IOException | RuntimeException e) {
            // A screenshot failure must never be the reason a test result changes.
            System.err.println("Could not capture screenshot '" + name + "': " + e.getMessage());
            return null;
        }
    }

    /** Failure evidence -- timestamped so repeated failures do not overwrite. */
    public static Path captureFailure(String testName) {
        return capture("FAILED-" + testName + "-" + LocalDateTime.now().format(STAMP));
    }

    /** Attaches the current URL and page title, which is usually the fastest triage clue. */
    public static void attachPageContext() {
        if (!Drivers.hasDriver()) {
            return;
        }
        WebDriver driver = Drivers.getDriver();
        try {
            Allure.addAttachment("Page context", "text/plain",
                    "URL:   " + driver.getCurrentUrl() + System.lineSeparator()
                            + "Title: " + driver.getTitle());
        } catch (RuntimeException e) {
            System.err.println("Could not read page context: " + e.getMessage());
        }
    }

    private static String sanitise(String name) {
        return name.replaceAll("[^A-Za-z0-9._-]", "-");
    }
}
