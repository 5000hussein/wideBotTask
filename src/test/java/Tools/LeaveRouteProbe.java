package Tools;

import Util.Config;
import Util.Drivers;
import Util.Waits;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class LeaveRouteProbe {
    public static void main(String[] args) throws Exception {
        WebDriver driver = Drivers.setUpDriver();
        try {
            long t0 = System.currentTimeMillis();
            driver.get(Config.getInstance().getBaseUrl() + "/web/index.php/auth/login");
            Waits.waitForElementVisible(driver, By.name("username"));
            driver.findElement(By.name("username")).sendKeys(Config.getInstance().getUsername());
            driver.findElement(By.name("password")).sendKeys(Config.getInstance().getPassword());
            driver.findElement(By.cssSelector("button[type='submit']")).click();
            Waits.waitForUrlContains(driver, "dashboard");
            System.out.println("login took " + (System.currentTimeMillis() - t0) + "ms");

            String[] routes = {
                    "/web/index.php/leave/applyLeave",
                    "/web/index.php/leave/addLeaveEntitlement",
                    "/web/index.php/leave/assignLeave"};

            for (int attempt = 1; attempt <= 3; attempt++) {
                System.out.println("\n########## ATTEMPT " + attempt + " ##########");
                for (String route : routes) {
                    long start = System.currentTimeMillis();
                    driver.get(Config.getInstance().getBaseUrl() + route);
                    long loaded = System.currentTimeMillis() - start;
                    Thread.sleep(4000);
                    String url = driver.getCurrentUrl();
                    String body = driver.findElement(By.tagName("body")).getText().trim();
                    System.out.println(route);
                    System.out.println("   get=" + loaded + "ms redirectedToLogin=" + url.contains("auth/login"));
                    System.out.println("   inputGroups=" + driver.findElements(By.cssSelector(".oxd-input-group")).size()
                            + " submitBtns=" + driver.findElements(By.cssSelector("button[type='submit']")).size()
                            + " selects=" + driver.findElements(By.cssSelector(".oxd-select-text")).size()
                            + " shell=" + !driver.findElements(By.cssSelector(".oxd-layout")).isEmpty()
                            + " bodyLen=" + body.length());
                    String tail = body.length() > 220 ? body.substring(body.length() - 220) : body;
                    System.out.println("   bodyTail=[" + tail.replace("\n", " | ") + "]");
                }
            }
        } finally {
            Drivers.quitDriver();
        }
    }
}
