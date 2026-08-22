---
name: page-object
description: Conventions for adding a page object, a test, or a suite to this Selenium + TestNG framework — package layout, naming, what belongs in a page versus a test, waiting rules, and test-data/cleanup expectations. Use when adding a new screen, a new scenario, or reviewing test code in this repo.
---

# Framework conventions

Structure follows the author's established Java layout: capitalised packages, `Pages/` for
screens and `Util/` for shared machinery, with static helpers that take `WebDriver` first.

```
src/main/java/
  Pages/   BasePage + one class per screen
  Util/    Drivers, Waits, Scrolling, ElementsActions,
           ConfigReader, DataFactory, ScreenshotUtil
src/test/java/
  Tests/      BaseTest + one class per journey
  Listeners/  TestListener (reporting), RetryAnalyzer
  Tools/      DOM probes — developer tools, not tests
suites/       smoke.xml, regression.xml, negative.xml
testng.xml    full suite
```

## Adding a page object

1. Extend `BasePage`. It supplies `driver`, navigation, toast capture, field errors,
   `inputByLabel(...)`, `dropdownByLabel(...)`, `selectEmployeeByName(...)` and
   `isModuleForbidden()`.
2. Declare locators as `private final By` fields at the top. Prefer, in order:
   `By.name` → label-scoped XPath → stable `oxd-*` class → structural XPath.
   Never use a locator that encodes a row index or a sibling position.
3. Methods express **intent** (`searchByEmployeeName`), not mechanics (`clickThirdButton`).
4. Return the next page object when an action navigates; return `this` for fluent chaining.
5. Annotate with `@Step("...")` so the Allure report reads as a narrative.
6. **Page objects never assert.** They expose state; tests decide what is correct. This is what
   lets one page serve both a positive and a negative test.
7. Anything that touches the DOM goes through `ElementsActions` / `Waits`. Do not call
   `driver.findElement` in a page object except inside a deliberate retry.

## Adding a test

1. Extend `BaseTest` — it owns the driver (one browser + session per **class**), login, and
   cleanup registration.
2. Every `@Test` needs `groups` (`smoke` / `regression` / `negative`) and a `description`.
   Add `@Story` / `@Severity` / `@Description` for the report.
3. Chain a journey with `dependsOnMethods`, so a broken precondition **skips** its dependants
   instead of failing them a second time with a misleading message.
4. Assertion messages state the expectation **and include the actual value**. A failure should
   be diagnosable from the report alone, without re-running.
5. Call `checkpoint("NN-name")` at meaningful moments — it saves a screenshot to `screenshots/`
   and attaches it to the report.
6. Register anything you create with `registerForCleanup(...)` **immediately** after creating it.

## Waiting rules

- **No implicit waits, ever.** Mixing implicit and explicit waits makes timeouts unpredictable.
- Never `Thread.sleep` in a page object or test. Probes under `Tools/` may.
- Wait for the *meaningful* state, not a proxy for it: rows with cells, not row containers; a
  populated field, not a present field.
- A wait that can legitimately fail should return a **boolean**, not throw. `selectFromAutocomplete`
  returns `false` for "no match" so the test can assert on it, rather than surfacing an opaque
  lambda timeout.
- Bound every retry loop explicitly (3 attempts is the house limit) and log each attempt.

## Test data and cleanup

- Never hard-code data the test creates. `DataFactory` generates a run tag so records are unique
  across runs and identifiable as ours.
- Search for cleanup by the **generated last name** — the part no test modifies. The edit
  scenario changes the first name, so a full-name lookup finds nothing and silently leaves the
  record behind.
- Cleanup runs in `@AfterClass` while the session is still alive, and **never fails the suite**:
  a cleanup problem is not a product defect. It logs loudly instead.
- Never assert an exact global record count on a shared environment.

## Retries

Applied **per test**, never suite-wide, and only where re-running is genuinely idempotent.
Retrying a test that already created data hits duplicate-key validation and turns a transient
blip into a confusing second failure with an unrelated message. Fix flakiness at the wait layer
first; a retry is the last resort, not the first.

## Configuration

Everything environment-specific goes through `ConfigReader`
(system property → `ORANGEHRM_`-prefixed env var → `config.properties`).
Never a literal in a test. Note the prefix is deliberate: an unprefixed `USERNAME` lookup
collides with the Windows `%USERNAME%` variable and silently resolves to the developer's login.

## Probes (`Tools/`)

When a locator is uncertain, **do not guess** — write a probe that dumps the real DOM and run it.
Every rule in the `orangehrm-locators` skill came from one. Probes are committed on purpose: they
are how the next person re-verifies the app after a UI change.
