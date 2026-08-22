package Tools;

import Util.ConfigReader;
import Util.Drivers;
import Util.Waits;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/** What does the Employee Name autocomplete actually render while typing? */
public class AutocompleteProbe {

    private static WebDriver driver;

    public static void main(String[] args) throws Exception {
        driver = Drivers.setUpDriver();
        try {
            login();

            driver.get(ConfigReader.baseUrl() + "/web/index.php/pim/viewEmployeeList");
            Waits.waitForLoaderToDisappear(driver);
            Waits.waitForElementVisible(driver, By.cssSelector(".oxd-table-card"), 30);
            Thread.sleep(2000);

            // Grab a real last name from the table to type.
            WebElement firstCard = driver.findElements(By.cssSelector(".oxd-table-card")).get(0);
            var cells = firstCard.findElements(By.cssSelector(".oxd-table-cell"));
            String first = cells.get(2).getText().trim();
            String last = cells.get(3).getText().trim();
            System.out.println("Seed row -> first(&middle)=[" + first + "] last=[" + last + "]");

            for (String typed : new String[]{last, first, first + " " + last}) {
                probeTyping(typed);
            }
        } finally {
            Drivers.quitDriver();
        }
    }

    private static void probeTyping(String typed) throws Exception {
        System.out.println("\n############ TYPING: [" + typed + "] ############");
        WebElement input = driver.findElement(
                By.cssSelector("input[placeholder='Type for hints...']"));
        input.clear();
        input.sendKeys(org.openqa.selenium.Keys.chord(org.openqa.selenium.Keys.CONTROL, "a"),
                org.openqa.selenium.Keys.DELETE);
        input.sendKeys(typed);

        for (int i = 1; i <= 4; i++) {
            Thread.sleep(1200);
            System.out.println("-- after " + (i * 1200) + "ms:");
            System.out.println(dump());
        }
    }

    private static String dump() {
        return String.valueOf(((JavascriptExecutor) driver).executeScript("""
                const out = [];
                const boxes = document.querySelectorAll('[role="listbox"]');
                out.push('listbox count: ' + boxes.length);
                boxes.forEach((b,i) => {
                  out.push('  listbox[' + i + '] class=' + b.getAttribute('class'));
                  b.querySelectorAll('[role="option"]').forEach((o,j) => {
                    out.push('    option[' + j + '] text=[' + o.innerText.trim().replace(/\\n/g,'|')
                      + '] class=' + o.getAttribute('class'));
                  });
                });
                const drop = document.querySelectorAll('.oxd-autocomplete-dropdown');
                out.push('autocomplete-dropdown count: ' + drop.length);
                drop.forEach(d => out.push('  dropdown innerText=[' + d.innerText.trim().replace(/\\n/g,'|') + ']'));
                const err = document.querySelectorAll('.oxd-input-field-error-message');
                err.forEach(e => out.push('  FIELD ERROR: [' + e.innerText.trim() + ']'));
                return out.join('\\n');
                """));
    }

    private static void login() {
        driver.get(ConfigReader.baseUrl() + "/web/index.php/auth/login");
        Waits.waitForElementVisible(driver, By.name("username"));
        driver.findElement(By.name("username")).sendKeys(ConfigReader.username());
        driver.findElement(By.name("password")).sendKeys(ConfigReader.password());
        driver.findElement(By.cssSelector("button[type='submit']")).click();
        Waits.waitForUrlContains(driver, "dashboard");
    }
}
