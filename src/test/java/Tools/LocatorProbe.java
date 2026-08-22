package Tools;

import Util.Config;
import Util.Drivers;
import Util.ElementsActions;
import Util.Waits;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

public class LocatorProbe {
    private static WebDriver driver;

    public static void main(String[] args) {
        driver = Drivers.setUpDriver();
        try {
            probeLogin();
            login();
            probeDashboard();
            probeEmployeeList();
            probeAddEmployee();
            probePersonalDetails();
            probeLeaveApply();
            probeLeaveAssign();
            probeLeaveList();
        } catch (Exception e) {
            System.out.println("### PROBE FAILED: " + e.getClass().getSimpleName() + " -> " + e.getMessage());
            System.out.println("### URL AT FAILURE: " + driver.getCurrentUrl());
        } finally {
            Drivers.quitDriver();
        }
    }

    private static void section(String name) {
        System.out.println("\n================ " + name + " ================");
        System.out.println("URL: " + driver.getCurrentUrl());
    }

    private static void dumpInteractive() {
        String script = """
                const out = [];
                document.querySelectorAll('input,button,select,textarea,[role="option"],[role="listbox"],.oxd-select-text').forEach(el => {
                  const r = el.getBoundingClientRect();
                  if (r.width === 0 && r.height === 0) return;
                  out.push([
                    el.tagName.toLowerCase(),
                    'type=' + (el.getAttribute('type') || '-'),
                    'name=' + (el.getAttribute('name') || '-'),
                    'ph=' + (el.getAttribute('placeholder') || '-'),
                    'class=' + (el.getAttribute('class') || '-').substring(0, 90),
                    'text=' + (el.innerText || '').trim().substring(0, 40).replace(/\\n/g, '|')
                  ].join('  '));
                });
                return out.join('\\n');
                """;
        System.out.println(((JavascriptExecutor) driver).executeScript(script));
    }

    private static void dumpLabels() {
        String script = """
                const out = [];
                document.querySelectorAll('.oxd-input-group').forEach(g => {
                  const label = (g.querySelector('label') || {}).innerText || '-';
                  const input = g.querySelector('input, textarea, .oxd-select-text');
                  const kind = input ? (input.tagName.toLowerCase() + '/' + (input.getAttribute('class')||'').substring(0,60)) : 'none';
                  const req = g.querySelector('.oxd-text--span.oxd-input-field-error-message, .oxd-text--span') ? 'required?' : '';
                  out.push('LABEL[' + label.trim().replace(/\\n/g,' ') + ']  -> ' + kind + '  ' + req);
                });
                return out.join('\\n');
                """;
        System.out.println(((JavascriptExecutor) driver).executeScript(script));
    }

    private static void dumpTable() {
        String script = """
                const out = [];
                out.push('header cells: ' + document.querySelectorAll('.oxd-table-header-cell').length);
                document.querySelectorAll('.oxd-table-header-cell').forEach(c => out.push('  HDR: ' + c.innerText.trim()));
                out.push('rows(.oxd-table-card): ' + document.querySelectorAll('.oxd-table-card').length);
                const first = document.querySelector('.oxd-table-card');
                if (first) {
                  first.querySelectorAll('.oxd-table-cell').forEach((c,i) => out.push('  CELL[' + i + ']: ' + c.innerText.trim().replace(/\\n/g,'|')));
                }
                document.querySelectorAll('.orangehrm-horizontal-padding span, .oxd-text--span').forEach(s => {
                  const t = s.innerText.trim();
                  if (t.match(/Record/i)) out.push('  RECORDS TEXT: [' + t + ']  class=' + s.getAttribute('class'));
                });
                return out.join('\\n');
                """;
        System.out.println(((JavascriptExecutor) driver).executeScript(script));
    }

    private static void go(String path) {
        driver.get(Config.getInstance().getBaseUrl() + path);
        Waits.waitForLoaderToDisappear(driver);
        sleep(1500);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void probeLogin() {
        go("/web/index.php/auth/login");
        section("LOGIN");
        dumpInteractive();
        System.out.println("-- branding img present: "
                + (driver.findElements(By.cssSelector(".orangehrm-login-branding img")).size() > 0));
        System.out.println("-- demo cred block: "
                + driver.findElements(By.cssSelector(".orangehrm-demo-credentials")).size());
    }

    private static void login() {
        driver.findElement(By.name("username")).sendKeys(Config.getInstance().getUsername());
        driver.findElement(By.name("password")).sendKeys(Config.getInstance().getPassword());
        driver.findElement(By.cssSelector("button[type='submit']")).click();
        Waits.waitForUrlContains(driver, "dashboard");
        Waits.waitForLoaderToDisappear(driver);
        sleep(2000);
    }

    private static void probeDashboard() {
        section("DASHBOARD");
        System.out.println("-- topbar module: "
                + text(".oxd-topbar-header-breadcrumb-module"));
        System.out.println("-- userdropdown name: " + text(".oxd-userdropdown-name"));
        System.out.println("-- userdropdown tab present: "
                + driver.findElements(By.cssSelector(".oxd-userdropdown-tab")).size());
        System.out.println("-- sidepanel items:");
        driver.findElements(By.cssSelector(".oxd-main-menu-item span"))
                .forEach(e -> System.out.println("     [" + e.getText().trim() + "]"));
        System.out.println("-- dashboard widgets: "
                + driver.findElements(By.cssSelector(".orangehrm-dashboard-widget")).size());
    }

    private static void probeEmployeeList() {
        go("/web/index.php/pim/viewEmployeeList");
        section("PIM > EMPLOYEE LIST");
        dumpLabels();
        dumpInteractive();
        dumpTable();
        System.out.println("-- buttons: ");
        driver.findElements(By.cssSelector("button")).forEach(b -> {
            String t = b.getText().trim();
            if (!t.isEmpty()) {
                System.out.println("     [" + t + "] class=" + b.getAttribute("class"));
            }
        });
    }

    private static void probeAddEmployee() {
        go("/web/index.php/pim/addEmployee");
        section("PIM > ADD EMPLOYEE");
        dumpLabels();
        dumpInteractive();
    }

    private static void probePersonalDetails() {
        go("/web/index.php/pim/viewPersonalDetails/empNumber/7");
        section("PIM > PERSONAL DETAILS (empNumber 7)");
        dumpLabels();
        System.out.println("-- save buttons: "
                + driver.findElements(By.cssSelector("button[type='submit']")).size());
        System.out.println("-- employee name banner: " + text(".orangehrm-edit-employee-name h6"));
        System.out.println("-- tabs:");
        driver.findElements(By.cssSelector(".orangehrm-tabs-item"))
                .forEach(e -> System.out.println("     [" + e.getText().trim() + "]"));
    }

    private static void probeLeaveApply() {
        go("/web/index.php/leave/applyLeave");
        section("LEAVE > APPLY");
        dumpLabels();
        dumpInteractive();
        System.out.println("-- page text snippet: "
                + text(".orangehrm-card-container").replace("\n", " | "));
    }

    private static void probeLeaveAssign() {
        go("/web/index.php/leave/assignLeave");
        section("LEAVE > ASSIGN");
        dumpLabels();
        dumpInteractive();
    }

    private static void probeLeaveList() {
        go("/web/index.php/leave/viewLeaveList");
        section("LEAVE > LEAVE LIST");
        dumpLabels();
        dumpTable();
    }

    private static String text(String css) {
        var elements = driver.findElements(By.cssSelector(css));
        return elements.isEmpty() ? "<absent>" : elements.get(0).getText().trim();
    }
}
