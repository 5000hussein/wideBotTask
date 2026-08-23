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

public final class ScreenshotUtil {
    private static final Path SCREENSHOT_DIR = Paths.get("screenshots");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private ScreenshotUtil() {
    }

    public static Path capture(String name) {
        if (!Drivers.hasDriver()) {
            return null;
        }
        try {
            byte[] png = ((TakesScreenshot) Drivers.getDriver()).getScreenshotAs(OutputType.BYTES);

            Allure.addAttachment(name, "image/png", new ByteArrayInputStream(png), ".png");

            Files.createDirectories(SCREENSHOT_DIR);
            String fileName = sanitise(name) + ".png";
            Path target = SCREENSHOT_DIR.resolve(fileName);
            Files.write(target, png);
            return target;
        } catch (IOException | RuntimeException e) {
            System.err.println("Could not capture screenshot '" + name + "': " + e.getMessage());
            return null;
        }
    }

    public static Path captureFailure(String testName) {
        return capture("FAILED-" + testName + "-" + LocalDateTime.now().format(STAMP));
    }

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
