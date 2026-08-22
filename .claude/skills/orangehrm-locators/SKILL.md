---
name: orangehrm-locators
description: Verified locators and interaction rules for OrangeHRM 5.x "oxd" (Vue) components — dropdowns, autocompletes, date fields, tables, toasts. Use when writing or fixing Selenium locators for OrangeHRM, or when a test fails with ElementNotInteractable, ElementClickIntercepted, StaleElementReference, an empty table read, or a dropdown option that is never found.
---

# OrangeHRM 5.x locator and interaction rules

Everything here was verified against `opensource-demo.orangehrmlive.com` (OrangeHRM OS 5.9) by
DOM probe, not guessed. Each rule exists because ignoring it produced a real, debugged failure.

## The one-line summary

OrangeHRM is a **Vue SPA using custom `oxd-*` components**. Almost nothing is a native HTML
control. `Select`, `element.clear()`, and "click the first button in the row" all fail here.

## Verified locators

| Element | Locator |
|---|---|
| Username / password | `By.name("username")` · `By.name("password")` |
| Login submit | `button[type='submit']` |
| Login error banner | `.oxd-alert-content-text` → e.g. `Invalid credentials` |
| Inline field error | `.oxd-input-field-error-message` |
| Top-bar module name | `.oxd-topbar-header-breadcrumb-module` |
| Signed-in user name | `.oxd-userdropdown-name` |
| Side menu item | `//aside//a[.//span[normalize-space()='PIM']]` |
| Table row | `.oxd-table-card` |
| Table cell | `.oxd-table-cell` |
| Record count banner | `//span[contains(@class,'oxd-text--span')][contains(.,'Record')]` → `(150) Records Found` |
| Toast (any) | `.oxd-toast` · body `.oxd-toast-content` · success `.oxd-toast--success` · error `.oxd-toast--error` |
| Loader / spinner | `.oxd-loading-spinner, .oxd-form-loader` |
| Custom dropdown | `.oxd-select-text` |
| Dropdown option | `//div[@role='option']` |
| Autocomplete input | `input[placeholder='Type for hints...']` |
| Autocomplete option | `[role='listbox'] [role='option']` |
| 403 page | `//*[contains(normalize-space(),'Module Forbidden')]` |

**Field-by-label** is the most robust pattern, since most inputs have no `name`:

```java
By.xpath("//div[contains(@class,'oxd-input-group')]"
       + "[.//label[normalize-space()='Employee Id']]"
       + "//input[not(@type='hidden')]")
```

Quote labels safely — `Driver's License Number` contains an apostrophe.

## Rules that are load-bearing

### 1. Dropdowns (`oxd-select`) are divs, not `<select>`
- `org.openqa.selenium.support.ui.Select` does **not** work.
- Open by clicking `.oxd-select-text`, then click the `//div[@role='option']` you want.
- **Match options by iterating elements and comparing normalised text**, not with an XPath text
  predicate — option labels come from user configuration and carry doubled/non-breaking spaces.
- Close with `new Actions(driver).sendKeys(Keys.ESCAPE)`. Sending ESCAPE **to the dropdown div
  throws ElementNotInteractable** — the key must go to the document.
- An open dropdown covers the next field. Always close before touching anything else.
- Reading two dropdowns in a row fails unless you close the first: the second click lands on the
  first one's overlay. Start every read from a known-closed state.

### 2. Dropdown options load *after* the page is interactive
A freshly opened dropdown can legitimately show only `-- Select --` or a placeholder such as
`No leave types defined` while its request is still in flight. Reading once reports an empty
environment for one that is merely slow. **Poll until real options appear.**

On **Add Entitlement**, the Leave Type list only populates **after an employee is selected** —
entitlements are per-employee. Fill the form top-to-bottom.

### 3. Autocompletes must have a suggestion *clicked*
Typing alone leaves the field `Invalid` and the search never runs.
- The in-flight placeholder is **`Searching....` — four dots.** Match it as a **prefix**; an
  exact comparison against `Searching...` silently never matches and the placeholder gets
  treated as a real suggestion, so a valid employee is reported as "no match".
- `No Records Found` is rendered **as an option**, not as a separate element.
- Type the **most selective token** (the last name), not the full name: hints are matched
  against name parts, and a full "first middle last" string returns nothing for employees with
  no middle name.
- Employees with no middle name render as `Jane  Doe` — **double space**. Normalise whitespace
  on both sides before comparing.

### 4. Dates use `yyyy-dd-MM` — year, DAY, month
Proof from the app: the 2026 leave period reads `2026-01-01 - 2026-31-12`.
ISO `yyyy-MM-dd` silently submits the wrong day when the day ≤ 12, and is rejected when > 12.
After typing a date, send `ESCAPE` — the calendar overlay stays open and eats the next click.

### 5. Tables render row containers before their cells
`.oxd-table-card` elements appear first and fill in a beat later. Waiting on the container alone
releases the wait while every row is an empty shell, so a search that matched returns "no rows".
**Wait for a row whose `.oxd-table-cell` count is complete.** Retry the whole row scrape on
staleness, not each cell — the table swaps the entire row set when it re-renders.

Column order (Employee List): `0` checkbox · `1` Id · `2` First (& Middle) Name · `3` Last Name ·
`4` Job Title · `5` Employment Status · `6` Sub Unit · `7` Supervisor · `8` Actions.

### 6. Toasts auto-dismiss in a few seconds
**Capture the toast immediately after the click that triggers it** — before waiting for a loader
or a redirect. Reading it after the page settles is a race you usually lose, and it fails as
"no success message" rather than "we looked too late". Never spend a long timeout probing for an
optional confirm dialog first; that alone can outlive the toast.

A stale toast also **intercepts clicks** on whatever is under it, and satisfies naive
"No Records Found" checks. Dismiss toasts before the next action.

### 7. The form loader eats clicks
`.oxd-form-loader` is invisible to `elementToBeClickable` but absolutely does intercept.
Wait for it to clear **before every click and every text entry**.

### 8. `element.clear()` is not enough
The Vue model does not always observe a programmatic clear, so the old value returns on save.
Use `Keys.chord(Keys.CONTROL, "a")` then `Keys.DELETE`, then type.

### 9. Personal Details has TWO forms and TWO submit buttons
Personal Details and Custom Fields each have their own `button[type='submit']`.
Scope the click: `//form[.//input[@name='firstName']]//button[@type='submit']`.

Fields are rendered **empty and populated a moment later**. Gate every read on the record
actually being bound (e.g. wait for `firstName`'s value to be non-blank), or you will read `""`
and report data loss that is really a race. The name banner
(`.orangehrm-edit-employee-name`) loads from a *separate* request again.

### 10. Row action buttons are told apart by their icon
Each row has two identical-looking `button.oxd-icon-button`. Pick by the icon it contains
(`i.bi-trash` for delete), never by position — "the first button" deletes or edits depending on
the build. Confirm dialog: `//button[contains(normalize-space(),'Yes, Delete')]`.

## Environment traps on the public demo

- The Leave module can return **`403 Module Forbidden`** — the shared Admin role is edited by
  other users. Detect it explicitly and skip with an accurate message rather than timing out.
- `Leave → Apply` shows **"No Leave Types with Leave Balance"** when the account has no
  entitlement; use **Assign Leave** (admin route) instead. Admin-assigned leave is created with
  status **`Scheduled`**, not `Pending Approval`.
- Job titles, leave types and sub units are sometimes entirely absent. Never hard-code a filter
  value — read the dropdown and pick one that has data.
- Assigning more days than the balance raises a **confirm dialog**, it does not block.
- Other users create/delete employees continuously: never assert an exact global record count.
- Employee records contain junk names (digits, punctuation). Filter candidates before using one
  as a search seed.
