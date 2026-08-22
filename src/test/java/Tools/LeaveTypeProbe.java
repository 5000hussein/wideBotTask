package Tools;

import Util.ConfigReader;
import Util.Drivers;
import Util.Waits;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

/** Does this environment define any leave types, and where are they configured? */
public class LeaveTypeProbe {

    private static WebDriver driver;

    public static void main(String[] args) throws Exception {
        driver = Drivers.setUpDriver();
        try {
            login();
            screen("LEAVE TYPE LIST", "/web/index.php/leave/leaveTypeList");
            screen("DEFINE LEAVE TYPE", "/web/index.php/leave/defineLeaveType");
        } finally {
            Drivers.quitDriver();
        }
    }

    private static void screen(String name, String path) throws Exception {
        driver.get(ConfigReader.baseUrl() + path);
        Waits.waitForLoaderToDisappear(driver);
        Thread.sleep(3000);
        System.out.println("\n======== " + name + " ========");
        System.out.println("URL: " + driver.getCurrentUrl());
        System.out.println(String.valueOf(((JavascriptExecutor) driver).executeScript("""
                const out = [];
                document.querySelectorAll('.oxd-input-group').forEach(g => {
                  const lbl = (g.querySelector('label')||{}).innerText || '-';
                  const i = g.querySelector('input,textarea,.oxd-select-text');
                  out.push('  LABEL[' + lbl.trim() + '] -> ' + (i ? i.tagName.toLowerCase()
                    + '(name=' + (i.getAttribute('name')||'-') + ',cls='
                    + (i.getAttribute('class')||'-').substring(0,40) + ')' : 'none'));
                });
                out.push('  BUTTONS: ' + [...document.querySelectorAll('.oxd-button')]
                    .map(b => b.innerText.trim() + '{' + (b.getAttribute('type')||'-') + '}')
                    .filter(t => t.length > 3).join(' | '));
                out.push('  HEADERS: ' + [...document.querySelectorAll('.oxd-table-header-cell')]
                    .map(c => c.innerText.trim()).join(' | '));
                out.push('  ROWS: ' + document.querySelectorAll('.oxd-table-card').length);
                [...document.querySelectorAll('.oxd-table-card')].slice(0,5).forEach((r,i) =>
                    out.push('   ROW[' + i + ']: ' + [...r.querySelectorAll('.oxd-table-cell')]
                        .map(c => c.innerText.trim()).join(' ~ ')));
                [...document.querySelectorAll('span')].forEach(s => {
                  const t=(s.innerText||'').trim();
                  if (/Record/i.test(t) && t.length < 40) out.push('  COUNT: [' + t + ']');
                });
                return out.join('\\n');
                """)));
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
