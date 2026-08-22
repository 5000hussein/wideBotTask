package Tools;

import Util.ConfigReader;
import Util.Drivers;
import Util.Waits;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

/** Focused probe: forms, tables and toasts, without the per-row button noise. */
public class FormProbe {

    private static WebDriver driver;

    public static void main(String[] args) {
        driver = Drivers.setUpDriver();
        try {
            login();
            screen("PIM > EMPLOYEE LIST", "/web/index.php/pim/viewEmployeeList");
            table();
            screen("PIM > ADD EMPLOYEE", "/web/index.php/pim/addEmployee");
            screen("PIM > PERSONAL DETAILS emp 7", "/web/index.php/pim/viewPersonalDetails/empNumber/7");
            details();
            screen("LEAVE > APPLY", "/web/index.php/leave/applyLeave");
            bodyText();
            screen("LEAVE > ASSIGN", "/web/index.php/leave/assignLeave");
            screen("LEAVE > LEAVE LIST", "/web/index.php/leave/viewLeaveList");
            table();
            screen("LEAVE > ENTITLEMENTS ADD", "/web/index.php/leave/addLeaveEntitlement");
        } catch (Exception e) {
            System.out.println("### FAILED: " + e.getClass().getSimpleName() + " " + e.getMessage());
            System.out.println("### URL: " + driver.getCurrentUrl());
        } finally {
            Drivers.quitDriver();
        }
    }

    private static void login() {
        driver.get(ConfigReader.baseUrl() + "/web/index.php/auth/login");
        Waits.waitForElementVisible(driver, By.name("username"));
        driver.findElement(By.name("username")).sendKeys(ConfigReader.username());
        driver.findElement(By.name("password")).sendKeys(ConfigReader.password());
        driver.findElement(By.cssSelector("button[type='submit']")).click();
        Waits.waitForUrlContains(driver, "dashboard");
        sleep(2000);
    }

    private static void screen(String name, String path) {
        driver.get(ConfigReader.baseUrl() + path);
        Waits.waitForLoaderToDisappear(driver);
        sleep(2500);
        System.out.println("\n=========== " + name + " ===========");
        System.out.println("URL: " + driver.getCurrentUrl());
        System.out.println(js("""
                const out = [];
                document.querySelectorAll('.oxd-input-group').forEach(g => {
                  const lbl = (g.querySelector('label')||{}).innerText || '-';
                  const star = g.querySelector('.oxd-text--span') ? '*' : '';
                  const inputs = [...g.querySelectorAll('input,textarea,.oxd-select-text')].map(i => {
                    if (i.classList.contains('oxd-select-text')) return 'OXD-SELECT[' + i.innerText.trim() + ']';
                    return i.tagName.toLowerCase()
                      + '(name=' + (i.getAttribute('name')||'-')
                      + ',ph=' + (i.getAttribute('placeholder')||'-')
                      + ',type=' + (i.getAttribute('type')||'-')
                      + ',cls=' + (i.getAttribute('class')||'-').substring(0,45) + ')';
                  });
                  out.push('  ' + (lbl.trim().replace(/\\n/g,' ')) + star + ' => ' + inputs.join(' , '));
                });
                out.push('  --- radios: ' + [...document.querySelectorAll('.oxd-radio-input')].length);
                out.push('  --- buttons: ' + [...document.querySelectorAll('.oxd-button')]
                    .map(b => b.innerText.trim() + '{' + (b.getAttribute('type')||'-') + '}')
                    .filter(t => t.length > 3).join(' | '));
                return out.join('\\n');
                """));
    }

    private static void table() {
        System.out.println(js("""
                const out = [];
                out.push('  HEADERS: ' + [...document.querySelectorAll('.oxd-table-header-cell')]
                    .map(c => c.innerText.trim().replace(/\\n/g,'')).join(' | '));
                out.push('  ROW COUNT (.oxd-table-card): ' + document.querySelectorAll('.oxd-table-card').length);
                const first = document.querySelector('.oxd-table-card');
                if (first) out.push('  ROW0 CELLS: ' + [...first.querySelectorAll('.oxd-table-cell')]
                    .map((c,i) => i + ':[' + c.innerText.trim().replace(/\\n/g,'') + ']').join(' '));
                [...document.querySelectorAll('span')].forEach(s => {
                  const t = (s.innerText||'').trim();
                  if (/Record/i.test(t) && t.length < 40) out.push('  COUNT SPAN: [' + t + '] cls=' + s.getAttribute('class'));
                });
                return out.join('\\n');
                """));
    }

    private static void details() {
        System.out.println(js("""
                const out = [];
                out.push('  NAME BANNER: [' + ((document.querySelector('.orangehrm-edit-employee-name')||{}).innerText||'-').trim().replace(/\\n/g,' ') + ']');
                out.push('  TABS: ' + [...document.querySelectorAll('.orangehrm-tabs-item')].map(t => t.innerText.trim()).join(' | '));
                out.push('  SUBMIT BUTTONS: ' + document.querySelectorAll('button[type=submit]').length);
                out.push('  FORMS: ' + document.querySelectorAll('form.oxd-form').length);
                document.querySelectorAll('form.oxd-form').forEach((f,i) => {
                  const h = f.querySelector('.oxd-text--h6');
                  out.push('   FORM[' + i + '] heading=' + (h ? h.innerText.trim() : '-')
                    + ' inputs=' + f.querySelectorAll('input').length);
                });
                return out.join('\\n');
                """));
    }

    private static void bodyText() {
        String body = driver.findElement(By.tagName("body")).getText().trim();
        System.out.println("  BODY: " + body.replace("\n", " | "));
    }

    private static String js(String script) {
        return String.valueOf(((JavascriptExecutor) driver).executeScript(script));
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
