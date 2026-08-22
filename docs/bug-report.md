# Bug Report

---

## BUG-001 — Leave balance is reduced by 4 days for a 3-day leave request

| Field | Value |
|---|---|
| **ID** | BUG-001 |
| **Title** | Leave balance decremented by 4 days when a 3-day leave request (10–12 Aug) is submitted |
| **Reported by** | Hussein Walid — QA |
| **Date reported** | 2026-08-22 |
| **Module / Component** | Leave → Apply / Leave Entitlements (balance calculation) |
| **Application / Version** | OrangeHRM OS 5.9 |
| **Environment** | `https://opensource-demo.orangehrmlive.com` · Chrome 151 · Windows 11 |
| **Test data** | Employee with a 10-day entitlement of the leave type under test, current leave period |
| **Severity** | **High** |
| **Priority** | **High** |
| **Type** | Functional — calculation / data integrity |
| **Status** | New |

### Summary

When an employee submits a leave request from **10 August to 12 August**, the system correctly
displays the request as **3 leave days**, but the employee's leave balance is reduced by
**4 days**. The number of days charged does not match the number of days requested or approved.

### Steps to reproduce

1. Log in as an employee who has a leave entitlement for the leave type under test.
2. Navigate to **Leave → Apply**.
3. **Record the leave balance shown for the selected leave type** (baseline). *Example: 10.00 days.*
4. Select the leave type.
5. Set **From Date = 10 August** and **To Date = 12 August** (same year, current leave period).
6. Observe the number of days the system calculates for the request.
7. Submit the request.
8. Navigate to **Leave → Entitlements → My Entitlements** (or re-open **Apply** and re-select
   the same leave type) and read the balance again.
9. Compare: `baseline − new balance` against the day count displayed in step 6.

### Expected result

The balance is reduced by exactly the number of leave days attributed to the request.

- Days displayed for the request: **3**
- Expected balance after submission: **10 − 3 = 7.00 days**
- Expected invariant: `balance_before − balance_after == days_displayed`

### Actual result

- Days displayed for the request: **3**
- Balance after submission: **6.00 days**
- Actual decrement: **4 days**
- The invariant is violated by **1 day**: the employee is charged one day more than requested.

### Evidence to attach

- Screenshot A — balance before submission (baseline).
- Screenshot B — the request form showing **3.00** days for 10–12 Aug.
- Screenshot C — the submitted request in **My Leave**, showing 3 days.
- Screenshot D — balance after submission, showing the 4-day reduction.
- Browser devtools **Network** capture of the submit request/response (payload and returned
  `numberOfDays` / balance fields).
- Application/server log excerpt covering the submission timestamp.

### Impact

Employees are charged more leave than they take. This is a **data-integrity and compliance
issue, not a cosmetic one**:

- Employees silently lose entitlement, which is a payroll and employment-terms matter.
- Balances across the organisation drift from the true position, so any downstream reporting,
  accrual or year-end carry-over is also wrong.
- The error compounds: every affected request loses another day.
- It undermines trust in the module — the number shown to the user is not the number applied.

### Frequency

To be confirmed by the triage below — the report is a single observation, and establishing
whether it is systematic or specific to this range is the first job.

### Analysis and likely cause

The displayed count and the charged count are produced by **two different calculations**, and
only one of them is correct. The most probable causes, in order:

1. **Inclusive/exclusive boundary mismatch** — the display uses an exclusive end date
   (`10, 11, 12` = 3) while the deduction adds an inclusive extra day (`12 − 10 + 1 + 1` = 4),
   a classic off-by-one.
2. **Weekend/holiday rules applied inconsistently** — one calculation honours the work week or
   holiday calendar and the other does not. *Note: 10–12 August 2026 is Mon–Wed, so no weekend
   is involved in this particular range, which makes an off-by-one more likely than a
   weekend-handling bug.*
3. **Timezone/DST boundary** — the range is computed in different timezones on client and
   server, pushing one end onto an adjacent day.
4. **Duplicate deduction** — a partial extra deduction from a retry or double event.

### Suggested triage to narrow it down

These are cheap and would confirm the cause quickly:

- Repeat with a **1-day** request (10 → 10). If the balance drops by 2, it is a pure off-by-one
  and independent of range length.
- Repeat with a **5-day** request. If the decrement is always `days + 1`, it is systematic.
- Repeat with a range **spanning a weekend** (Fri → Mon) to separate cause 1 from cause 2.
- Compare the API response's day count with the persisted balance to confirm the two values
  diverge server-side rather than in the UI.

### Related observations

While investigating, a second issue was found that may contribute to confusion in reports of
this kind and is worth raising separately — see **BUG-002**.

### Regression risk / suggested test coverage after the fix

Add an automated assertion of the invariant
`balance_before − balance_after == days_displayed` across: 1 day, 3 days, a weekend-spanning
range, a holiday-spanning range, a half day, and cancel/reject (balance restored). This is test
case **LR-C-12** in the test design.

---

## BUG-002 — Date fields use a non-standard `yyyy-dd-MM` format, so dates are silently misread

| Field | Value |
|---|---|
| **ID** | BUG-002 |
| **Title** | All date fields render and parse as `yyyy-dd-MM` (year–day–month), causing silent wrong-date entry |
| **Severity** | **High** |
| **Priority** | Medium |
| **Type** | Functional / Usability / Internationalisation |
| **Module** | Global — date input (Leave, PIM Personal Details, Leave Entitlements) |
| **Status** | New |

### Summary

Date fields throughout the application use the order **year–day–month**, which is neither
ISO 8601 (`yyyy-MM-dd`) nor a common regional format. Because the placeholder itself reads
`yyyy-dd-mm`, a user following ISO convention enters a valid-looking date that the system
accepts as a **different day**, with no error.

### Steps to reproduce

1. Log in and navigate to any screen with a date field (e.g. **Leave → Assign Leave**, or
   **PIM → Personal Details → Date of Birth**).
2. Observe the field placeholder: it reads `yyyy-dd-mm`.
3. Navigate to **Leave → Entitlements** and observe the **Leave Period** value.

### Expected result

Dates follow a single, unambiguous, documented format — ISO 8601 `yyyy-MM-dd` — or the field is
explicitly localised and labelled.

### Actual result

The order is year–day–month. Confirmed by the application's own rendering of the leave period:

```
2026-01-01 - 2026-31-12
```

The end of the 2026 leave period is 31 December, written **`2026-31-12`** — day before month.

### Impact

- For any day of month **≤ 12** the value is silently ambiguous: entering `2026-05-08`
  intending **8 May** books **5 August** instead, and the form raises no error because the
  input is structurally valid.
- Users cannot detect the mistake from the UI, because it is displayed back in the same
  ambiguous order.
- This is a plausible latent contributor to user-reported "wrong dates" and balance complaints.

### Note for the automation suite

The automated tests format dates as `yyyy-dd-MM` deliberately, to match the application as it
actually behaves. See `DataFactory.APP_DATE_FORMAT`. Should this defect be fixed, that single
constant is the only place that needs to change.
