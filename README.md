# OrangeHRM UI Automation Framework

Senior QA assessment — a Selenium + TestNG + Allure framework covering the OrangeHRM employee
and leave lifecycle, plus the manual testing deliverables.

**Application under test:** https://opensource-demo.orangehrmlive.com (OrangeHRM OS 5.9)

> ### 🧭 New to this repo? Read [**`docs/walkthrough.md`**](docs/walkthrough.md) first.
> One page covering the whole framework: the four layers, the six files that matter, a map of
> every file, one test traced end to end, and the design decisions with the reasoning behind
> each. It's the fastest way to understand what's here and why.

## 📁 Deliverables — start here

| # | Deliverable | Where |
|---|---|---|
| 1 | **Automation framework** | [`src/`](src/) · [`pom.xml`](pom.xml) · [`testng.xml`](testng.xml) · [`suites/`](suites/) |
| 2 | **README** (this file) | [How to execute tests](#how-to-execute-tests) · [Configuration](#configuration) · [Test-data strategy](#test-data-strategy-and-cleanup) |
| — | **Framework walkthrough** — architecture, file map, design decisions | **[`docs/walkthrough.md`](docs/walkthrough.md)** |
| 3 | **Automated tests** — 19 tests, Steps 1–9 | [`src/test/java/Tests/`](src/test/java/Tests/) · [coverage matrix](#scenario-coverage) |
| 4 | **Screenshots** — 18 checkpoints | [`screenshots/`](screenshots/) |
| 5 | **Test report** | **[Live Allure report ↗](https://5000hussein.github.io/wideBotTask/report/)** · source in [`docs/report/`](docs/report/) |

### 📝 Manual Testing Assessment

| Part | Where |
|---|---|
| **1. User story review** — questions, ambiguities, missing requirements, assumptions, risks | **[`docs/manual-testing.md`](docs/manual-testing.md#1-what-i-would-raise-before-testing-starts)** |
| **2. Test coverage design** — partitions, boundary analysis, techniques | [`docs/manual-testing.md` §2](docs/manual-testing.md#2-test-coverage-design) |
| **3. Test cases** — 50+ cases across happy path, calculation, state, negative, cross-cutting | [`docs/manual-testing.md` §3](docs/manual-testing.md#3-test-cases) |
| **4. Exploratory findings** — 6 real issues found in the live app | [`docs/manual-testing.md` §4](docs/manual-testing.md#4-what-exploratory-testing-actually-found) |
| **5. Bug report** — the leave-balance defect, plus one I found myself | **[`docs/bug-report.md`](docs/bug-report.md)** |

> **Latest full run: 19 tests · 0 failures · 0 skipped** — executed live against the demo
> environment. See [Findings](#findings-from-building-this-suite) for the defects this surfaced.

---

## Contents

- [Prerequisites](#prerequisites)
- [Technologies](#technologies)
- [Framework architecture](#framework-architecture)
- [Project structure](#project-structure)
- [How to execute tests](#how-to-execute-tests)
- [Reporting](#reporting)
- [Screenshots](#screenshots)
- [Configuration](#configuration)
- [Test-data strategy and cleanup](#test-data-strategy-and-cleanup)
- [Scenario coverage](#scenario-coverage)
- [Findings from building this suite](#findings-from-building-this-suite)
- [Known environment constraints](#known-environment-constraints)

---

## Prerequisites

| Requirement | Version | Notes |
|---|---|---|
| **Java (JDK)** | **17 or newer** | Compiled with `maven.compiler.release=17`. Developed and executed on JDK 25. |
| **Maven** | **3.8+** | Developed on 3.9.12. |
| **Browser** | Chrome (default), Firefox or Edge | A matching driver is **not** required — Selenium Manager resolves it automatically. |
| Internet access | — | The application under test is a public demo. |

Nothing else needs installing. There is no WebDriverManager dependency and no driver binary to
place on the PATH.

> **Note on Java 25.** AspectJ (which Allure uses to weave `@Step`) must be **1.9.24 or newer**
> to run on Java 25 — earlier versions abort with `Unsupported class file major version 69` and
> the report silently loses all step detail. This is pinned in `pom.xml`.

---

## Technologies

| Technology | Version | Why |
|---|---|---|
| Selenium WebDriver | 4.27.0 | Browser automation; Selenium Manager removes driver management entirely |
| TestNG | 7.10.2 | Groups, suites, dependencies and priorities — the execution model the assessment asks for |
| Allure | 2.29.0 | Execution report with steps, severity, durations and embedded evidence |
| SnakeYAML | 2.3 | Reads `config.yaml` |
| Gson | 2.11.0 | Reads `TestData/data.json` |
| Maven Surefire | 3.5.2 | Test execution and CLI parameterisation |
| SLF4J Simple | 2.0.16 | Quiet, predictable logging |

---

## Framework architecture

Layered, so that a UI change touches exactly one layer.

```
        Tests/                 what SHOULD happen — a sequence of page calls, no assertions
          |
        Pages/                 what the screen CAN do, and what it SHOULD show —
                               actions plus the verify* methods that assert them
          |
   ElementsActions / Waits     how to interact safely — every DOM quirk handled once
          |
        Drivers                browser lifecycle (ThreadLocal)
          |
   Config / Helper             environment values and test data from data.json
```

**Design decisions worth calling out:**

- **Assertions live in the page objects, not the tests.** Each page has a `//PageAssertions`
  section whose `verify*` methods assert against the locators sitting a few lines above them
  (`verifyRowMatches(...)`, `verifySaveWasConfirmed()`, `verifyFieldRejectedWith(...)`). A test
  is therefore a readable sequence of page calls — `EmployeeTest` contains no `Assert` import at
  all — and a screen's expectations change in one file. `Validations` wraps TestNG's `Assert` so
  the pages never import a test framework directly.
- **No implicit waits.** Mixing implicit and explicit waits makes timeouts unpredictable. Every
  wait is explicit and waits for the *meaningful* state — a table row **with cells**, a field
  **with a value** — not a proxy that resolves too early.
- **One browser and session per test class.** The assessment describes a journey (create → find →
  edit → prove persistence), so a class models one journey and its methods are chained with
  `dependsOnMethods`. A broken precondition therefore **skips** its dependants rather than
  producing a second, misleading failure.
- **ThreadLocal driver**, so suites can be parallelised by class without tests sharing a browser.

---

## Project structure

```
.
├── pom.xml
├── testng.xml                       # full suite (Steps 1–9)
├── suites/
│   ├── smoke.xml                    # shortest path proving the app is testable
│   ├── regression.xml               # every functional scenario
│   └── negative.xml                 # validation scenarios only
├── src/main/java/
│   ├── Pages/
│   │   ├── BasePage.java            # driver, navigation, toasts, field errors, label helpers
│   │   ├── LoginPage.java
│   │   ├── DashboardPage.java
│   │   ├── PimEmployeeListPage.java # content-based row lookup, filters, delete
│   │   ├── AddEmployeePage.java
│   │   ├── EmployeeDetailsPage.java
│   │   ├── ApplyLeavePage.java
│   │   ├── AssignLeavePage.java
│   │   ├── LeaveEntitlementPage.java
│   │   └── LeaveListPage.java
│   └── Util/
│       ├── Config.java              # singleton over config.yaml: -D → env var → file
│       ├── Drivers.java             # ThreadLocal WebDriver, chrome/firefox/edge, headless
│       ├── Waits.java               # every wait, stale-tolerant
│       ├── Scrolling.java
│       ├── ElementsActions.java     # all oxd-component interaction rules
│       ├── Helper.java              # data.json reader + app-format dates
│       ├── Validations.java         # assertion wrapper
│       └── ScreenshotUtil.java      # disk + Allure attachment
├── src/main/resources/
│   ├── config.yaml
│   └── TestData/
│       └── data.json                # every literal the tests type
├── src/test/java/
│   ├── Tests/
│   │   ├── BaseTest.java            # lifecycle + test-data ownership/cleanup
│   │   ├── LoginTest.java           # Step 1
│   │   ├── EmployeeTest.java        # Steps 2–7
│   │   ├── LeaveTest.java           # Step 8
│   │   └── NegativeTest.java        # Step 9
│   └── Listeners/
│       ├── TestListener.java        # failure screenshots, durations, page context
│       └── RetryAnalyzer.java       # opt-in, per test
├── docs/
│   ├── manual-testing.md
│   └── bug-report.md
├── screenshots/                     # checkpoint + failure evidence
└── .claude/                         # reusable skills & subagents (see below)
```

---

## How to execute tests

### Run everything

```bash
mvn test
```

### Run a single test class

```bash
mvn test -Dtest=EmployeeTest
```

### Run a single test method

```bash
mvn test -Dtest=EmployeeTest#verifyUserCanCreateNewEmployee
```

### Run a TestNG suite

```bash
mvn test -DsuiteXmlFile=testng.xml
mvn test -DsuiteXmlFile=suites/smoke.xml
mvn test -DsuiteXmlFile=suites/regression.xml
mvn test -DsuiteXmlFile=suites/negative.xml
```

### Run a group

```bash
mvn test -Dgroups=smoke
mvn test -Dgroups=regression
mvn test -Dgroups=negative
```

Groups available: `smoke`, `regression`, `negative`.

> **How all three CLI forms work together.** Surefire ignores `-Dgroups` when `suiteXmlFiles` is
> configured, so hard-coding a suite in the POM would silently break `-Dgroups=smoke`. Instead
> the POM leaves Surefire in class-scanning mode — which keeps both `-Dtest` and `-Dgroups`
> working — and a Maven profile activates the suite configuration **only** when
> `-DsuiteXmlFile=` is actually passed. All three commands above work as written.

### Run against a different browser, or watch it run

```bash
mvn test -Dbrowser=firefox
mvn test -Dheadless=false          # headed, so you can watch
mvn test -Dbrowser=edge -Dheadless=false -Dtest=LoginTest
```

---

## Reporting

**Allure** is used for the execution report. It records test name, status, duration, failure
detail, and the screenshot evidence for both checkpoints and failures. Tests carry
`@Epic` / `@Feature` / `@Story` / `@Severity`, and page-object actions carry `@Step`, so the
report reads as a narrative rather than a list of method names.

### 📊 View the example report

A real report from a full green run (**19 tests, 0 failures, 0 skipped**) is committed to
[`docs/report/`](docs/report/).

> ### ➡️ **[Open the live Allure report](https://5000hussein.github.io/wideBotTask/report/)**

**Please use the link above rather than opening the files directly.** The Allure report is a
single-page app that loads its results over XHR from `data/*.json`, which means:

| How you open it | Result |
|---|---|
| The GitHub Pages link above | ✅ Works — served over HTTP |
| Clicking `docs/report/index.html` on github.com | ❌ Shows the raw HTML source, not the report |
| Double-clicking `index.html` from disk | ❌ Renders **blank** — Chrome blocks the XHRs on `file://` |
| `mvn allure:serve` locally | ✅ Works — starts a local web server |

<details>
<summary>Enabling the GitHub Pages link (one-time repo setting)</summary>

**Settings → Pages → Build and deployment → Source: _Deploy from a branch_ →
Branch: `main`, folder: `/docs` → Save.**

The report is then live at `https://5000hussein.github.io/wideBotTask/report/` after about a
minute. Serving from `/docs` also publishes the Markdown deliverables alongside it.
</details>

### Generating it yourself

```bash
# 1. run the tests (results land in target/allure-results)
mvn test -DsuiteXmlFile=testng.xml

# 2. generate a static HTML report into target/site/allure-maven-plugin
mvn allure:report

# 3. or open it directly in a browser
mvn allure:serve
```

The Allure command-line tool is downloaded automatically by `allure-maven` — nothing to install.

Surefire's own XML/text results are also written to `target/surefire-reports/`, which is useful
for CI and for reading skip reasons verbatim.

---

## Screenshots

Two kinds, both written to `screenshots/` **and** embedded in the Allure report:

1. **Checkpoints** — captured deliberately at meaningful moments via `checkpoint("NN-name")`:
   login/dashboard, employee creation, search results, updated employee, persistence, filtering,
   leave request, and each negative validation.
2. **Failures** — captured **automatically** by `TestListener.onTestFailure`, together with the
   URL and page title at the moment of failure, timestamped so repeated failures do not
   overwrite each other.

---

## Configuration

All environment values live in `src/main/resources/config.yaml`. **No credential or URL
appears in any test class.**

```yaml
base_url: https://opensource-demo.orangehrmlive.com
username: Admin
password: admin123
browser: chrome
headless: true
explicit_wait: 20
page_load_timeout: 45
cleanup_enabled: true
```

Resolution order — first hit wins:

1. **System property** — `-Dusername=qa -Dpassword=***`
2. **Environment variable** — `ORANGEHRM_USERNAME`, `ORANGEHRM_PASSWORD`, `ORANGEHRM_BASE_URL`
3. **`config.yaml`**

Test-data literals are separate, in `src/main/resources/TestData/data.json` — name prefixes,
the entitlement day count, the over-length string, the wrong password. Nothing the suite types
into the application is a literal in a test class.

So pointing the suite at a private environment is a config change, never a code change:

```bash
mvn test -Dbase.url=https://hrm.internal -Dusername=qa -Dpassword=secret
```

**On the credentials in the file:** these are the public demo credentials that OrangeHRM prints
on its own login page — they are not secret. They live in config rather than in the tests so
that the *mechanism* is right: for a real environment, supply them via environment variables or
your CI's secret store and leave the file alone.

> The `ORANGEHRM_` prefix on environment variables is deliberate. An unprefixed `USERNAME`
> lookup collides with the Windows `%USERNAME%` variable — during development this silently
> resolved to the developer's own login and every test failed with `Invalid credentials`.

---

## Test-data strategy and cleanup

**Every literal the suite types lives in `src/main/resources/TestData/data.json`,** read through
`Helper.getData("key")`. No test hard-codes a name, an id, a password or a day count in Java.

**Two employee identities**, because `EmployeeTest` and `LeaveTest` each create a record: the PIM
suite uses `firstName`/`lastName`/`employeeId`, the leave suite the `leave*` keys. They are kept
distinct so a single run cannot fail on OrangeHRM's duplicate-Employee-Id validation.

**Dates** are calculated at runtime, never hard-coded — the leave window is the *next Monday to
the following Wednesday*. Anchoring to a Monday keeps the working-day count deterministic at 3;
a window that straddled a weekend would make the expected day count vary by run day. The app's
`yyyy-dd-MM` quirk is applied in one place, `Helper.APP_DATE_FORMAT`.

**Cleanup.** Every employee a test creates is registered with `registerForCleanup(...)` and
deleted in `@AfterClass`, while the session is still alive.

- Cleanup searches by **last name**, not the full name. The edit scenario deliberately changes
  the first name, so a full-name lookup finds nothing, concludes the record is already gone, and
  silently leaves it behind — precisely the pollution cleanup exists to prevent.
- Cleanup **never fails the suite**. A cleanup problem is housekeeping, not a product defect.
  Anything left behind is logged loudly, with a screenshot, so it can be removed by hand.
- Disable it to inspect records after a run: `-Dcleanup.enabled=false`.

> **Reruns depend on cleanup having succeeded.** Because the identities in `data.json` are fixed
> rather than tagged per run, a record left behind by a crashed run — or by `cleanup.enabled=false`
> — makes the next run fail at creation with *"Employee Id already exists"*. Delete the leftover
> in the UI, or edit the ids in `data.json`. This is the deliberate trade for keeping the test
> data readable and declarative; a timestamp-tagged scheme avoids the collision but puts generated
> values in Java and leaves untraceable rows behind when cleanup cannot run.

**What is deliberately *not* cleaned up, and why:**

| Artefact | Cleaned? | Reason |
|---|---|---|
| Employees created by tests | **Yes** | Deleted via the UI's row action + confirmation |
| Leave entitlements granted to those employees | Indirectly | Deleting the employee removes their entitlements |
| Leave requests submitted for those employees | Indirectly | Removed with the employee |
| Employees from a *crashed* run | No | The process died before `@AfterClass`; find them by the last names in `data.json` |
| Anything created by a **rejected** submission | N/A | Nothing was created — the negative tests assert this |

---

## Scenario coverage

| Step | Requirement | Test | Groups |
|---|---|---|---|
| 1 | Login page displayed, fields and button available | `LoginTest.verifyLoginPageIsDisplayedWithAllControls` | smoke, regression |
| 1 | Login succeeds, dashboard shown, user identified | `LoginTest.verifyUserCanLoginWithValidCredentials` | smoke, regression |
| 2 | Employee search returns matching results | `EmployeeTest.verifyUserCanSearchForAnExistingEmployee` | smoke, regression |
| 3 | Create employee from generated data | `EmployeeTest.verifyUserCanCreateNewEmployee` | smoke, regression |
| 4 | Created employee found in the list | `EmployeeTest.verifyCreatedEmployeeIsFoundInEmployeeList` | regression |
| 4 | Record opens with correct data and accessible tabs | `EmployeeTest.verifyCreatedEmployeeRecordShowsCorrectDetails` | regression |
| 5 | Edit employee fields, update confirmed | `EmployeeTest.verifyUserCanEditEmployeeInformation` | regression |
| 6 | Update survives a refresh | `EmployeeTest.verifyUpdatedInformationSurvivesRefresh` | regression |
| 6 | Update survives navigating away and back | `EmployeeTest.verifyUpdatedInformationSurvivesNavigationAway` | regression |
| 7 | Two filter criteria; returned **data** validated | `EmployeeTest.verifyUserCanFilterEmployeeListByTwoCriteria` | regression |
| 7 | Reset restores the expected result set | `EmployeeTest.verifyResetRestoresFullResultSet` | regression |
| 8 | Apply-leave screen state | `LeaveTest.verifyApplyLeaveScreenMatchesEntitlementState` | regression |
| 8 | Employee + entitlement prerequisites | `LeaveTest.verifyUserCanCreateEmployeeWithLeaveEntitlement` | regression |
| 8 | Leave request submitted, confirmed | `LeaveTest.verifyUserCanSubmitLeaveRequest` | smoke, regression |
| 8 | Submitted leave located afterwards | `LeaveTest.verifySubmittedLeaveCanBeFound` | regression |
| 9A | Required field empty → rejected | `NegativeTest.verifyEmployeeCannotBeCreatedWithoutLastName` | negative, regression |
| 9B | Invalid (over-length) data → rejected | `NegativeTest.verifyEmployeeCannotBeCreatedWithOverlongFirstName` | negative, regression |
| 9C | Invalid leave dates → rejected | `NegativeTest.verifyLeaveCannotBeAssignedWithEndDateBeforeStartDate` | negative, regression |
| 9D | Invalid credentials → rejected | `NegativeTest.verifyInvalidCredentialsAreRejected` | negative, regression |
| 10 | Test-data cleanup | `BaseTest.cleanUpCreatedEmployees` + this README | — |

### How the "no hard-coded row number" requirement is met

Every row is located **by content**. `PimEmployeeListPage.findRowByLastName(...)` scans the
rendered rows for a matching last name, and `openEmployeeByLastName(...)` clicks that row's cell.
No assertion anywhere refers to a row position. Column *indices* are used to map a row's cells to
a typed `EmployeeRow` record — that is the table's schema, verified against the live header, not
a positional assumption about which employee is where.

### How persistence is proven, rather than assumed

Step 4 explicitly does not trust the creation toast:

- creation is confirmed by the **server-assigned `empNumber` in the redirect URL**;
- the employee is then re-found through the **employee list**, a separate server-side query;
- after editing, values are re-read after a refresh **and** after navigating away and back;
- the employee list is cross-checked, because it reads through a different query than the record.

---

## Findings from building this suite

Building the automation surfaced several genuine product and environment issues. The two defects
are written up properly in [`docs/bug-report.md`](docs/bug-report.md).

1. **Dates are `yyyy-dd-MM` — year, day, month.** Proof from the application itself: the 2026
   leave period renders as `2026-01-01 - 2026-31-12`. Using ISO `yyyy-MM-dd` silently submits the
   **wrong day** whenever the day of month is ≤ 12. Raised as **BUG-002**; the suite formats
   dates to match the app's real behaviour via a single constant, `Helper.APP_DATE_FORMAT`.
2. **`Leave → Apply` is unusable for an account with no entitlement** — it renders
   "No Leave Types with Leave Balance" instead of a form. The suite therefore creates leave via
   **Assign Leave** (the administrator's equivalent flow) against an employee it creates and
   entitles itself, keeping the scenario self-contained.
3. **Administrator-assigned leave gets status `Scheduled`, not `Pending Approval`** — it bypasses
   the approval step in the user story's business flow entirely.
4. **Insufficient balance raises a confirmation, not a block** — leave can be assigned beyond the
   available balance by clicking OK.
5. **Personal Details has no Nickname field** in 5.9, so the edit scenario uses First Name and
   Employee Id (the assessment lists these as alternatives).
6. **Job titles were entirely absent** from the environment at times, so Step 7 chooses its
   second filter criterion from what the environment actually offers (Job Title, else Sub Unit).

---

## Known environment constraints

The application under test is a **public, shared, mutable sandbox**. This materially affects
execution, and the framework is built to report these honestly rather than disguise them:

- **The Leave module can return `403 Module Forbidden`.** The demo's Admin role is edited by
  other users of the sandbox. This was confirmed on 3/3 attempts across all three Leave routes
  during development. The suite **detects the 403 explicitly and skips with an accurate
  message**, instead of spending a 20-second timeout and then reporting a missing button — which
  would read like a broken locator or a product defect.
- **The signed-in account's identity changes between runs** — observed as `Hugo Musk`, then
  `Emp_NfJwbv User_WjuoigBT`, then `Donald Trump`. Assertions therefore never depend on a
  specific pre-existing name.
- **Record counts drift during a run** as other users create and delete employees, so
  `verifyResetRestoresFullResultSet` compares against a tolerance and additionally proves an excluded
  record reappears — rather than asserting an exact global count.
- **Reference data disappears.** Job titles, leave types and sub units have each been absent at
  some point. No filter value is ever hard-coded.
- **Existing records contain junk data** (names made of digits and punctuation) left by other
  users, so the search scenario filters for a plausible seed and tries several candidates.

**Recommendation:** for trustworthy results — particularly for the leave-balance arithmetic in
BUG-001 — this feature needs a controlled environment with seeded, known reference data. Balance
correctness cannot be verified against a database that arbitrary third parties are mutating.

---

## `.claude/` — reusable knowledge

The repository also carries the knowledge gained here as Claude Code skills and subagents, so it
is reusable rather than trapped in one person's head:

| Asset | Purpose |
|---|---|
| `skills/orangehrm-locators` | Verified locators and the interaction rules for OrangeHRM's `oxd` components — every rule earned from a real, debugged failure |
| `skills/page-object` | This framework's conventions for adding a page, a test or a suite |
| `skills/bug-report` | Defect-reporting template and standards |
| `agents/locator-scout` | Harvests **verified** locators by writing and running a DOM probe, instead of guessing |
| `agents/test-designer` | Turns a user story into questions, risks and structured test cases |
| `agents/flake-triage` | Classifies a failure as product / test / environment **before** proposing a fix |
