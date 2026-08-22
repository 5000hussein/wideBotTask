package Tools;

import Util.Config;
import Util.Drivers;
import Util.ElementsActions;
import Util.Waits;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class DropdownProbe {
    public static void main(String[] args) {
        WebDriver driver = Drivers.setUpDriver();
        try {
            driver.get(Config.getInstance().getBaseUrl() + "/web/index.php/auth/login");
            Waits.waitForElementVisible(driver, By.name("username"));
            driver.findElement(By.name("username")).sendKeys(Config.getInstance().getUsername());
            driver.findElement(By.name("password")).sendKeys(Config.getInstance().getPassword());
            driver.findElement(By.cssSelector("button[type='submit']")).click();
            Waits.waitForUrlContains(driver, "dashboard");

            driver.get(Config.getInstance().getBaseUrl() + "/web/index.php/pim/viewEmployeeList");
            Waits.waitForLoaderToDisappear(driver);
            Waits.waitForElementVisible(driver, By.cssSelector(".oxd-table-card"), 30);

            for (String label : new String[]{"Employment Status", "Include", "Job Title", "Sub Unit"}) {
                By dd = By.xpath("//div[contains(@class,'oxd-input-group')]"
                        + "[.//label[normalize-space()='" + label + "']]"
                        + "//div[contains(@class,'oxd-select-text')]");
                System.out.println("\n=== " + label + " ===");
                System.out.println("  located: " + !driver.findElements(dd).isEmpty());
                System.out.println("  options: " + ElementsActions.getOxdDropdownRealOptions(driver, dd));
            }
        } finally {
            Drivers.quitDriver();
        }
    }
}
