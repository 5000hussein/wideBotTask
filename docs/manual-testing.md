# Manual Testing Assessment — Employee Leave Request

> Feature under review: **Employee Leave Request** (OrangeHRM 5.9)
> Environment used for exploration: `https://opensource-demo.orangehrmlive.com`

---

## 1. What I would raise before testing starts

The user story is a reasonable skeleton, but it is not yet testable as written. Below are the
things I would take to the BA/PO **before** committing to a test plan, because each one changes
what "correct" means — and therefore changes the tests.

I have separated genuine **ambiguities** (nobody knows the answer yet) from **missing
requirements** (a decision exists but is not written down) and **risks** (the answer is known
but the design is dangerous).

### 1.1 Ambiguities — the acceptance criteria do not define the answer

| # | Question | Why it matters | Blocking? |
|---|---|---|---|
| Q1 | "System calculates the number of leave days" — calculated **how**? Are weekends excluded? Public holidays? Does the employee's configured work week apply? | This single rule drives most of the arithmetic tests. A Fri→Mon request is 2 days or 4 days depending on the answer. | **Yes** |
| Q2 | Are **half-day** / partial-day requests in scope? OrangeHRM supports a duration of "Half Day — Morning/Afternoon". | Changes the day count from an integer to a decimal, and changes balance arithmetic. | **Yes** |
| Q3 | What happens when the request **exceeds the available balance**? Hard block, warning-and-allow, or allow-and-go-negative? | The live application currently *asks for confirmation and allows it*, which contradicts a naive reading of AC10 ("System prevents invalid leave requests"). | **Yes** |
| Q4 | AC7 says a submitted request has "Pending Approval" status. Is that true for **every** creation route? An administrator using *Assign Leave* produces status **"Scheduled"**, not "Pending Approval". | If Assign Leave is in scope, AC7 is factually wrong as written. | **Yes** |
| Q5 | Can leave be requested for **past dates**? For dates in a closed leave period? | Determines whether a whole class of negative tests is expected to pass or fail. | No |
| Q6 | AC9 — "cancel a pending request". Can an **approved** request be cancelled? Who may cancel: only the employee, or also the manager/admin? | Defines the state machine's legal transitions. | No |
| Q7 | Is the optional comment length-limited? Is it visible to the approver? | Field-level validation and a privacy question. | No |
| Q8 | Overlapping requests — may an employee hold two requests covering the same date? | Common real-world defect source; not mentioned at all. | **Yes** |

### 1.2 Missing requirements — not stated anywhere in the story

1. **No balance rules.** The story never mentions leave balance, yet the defect being reported
   is a balance defect. Entitlement, accrual, carry-over and the leave period boundary are all
   undefined. *A feature whose core arithmetic is undefined cannot be signed off.*
2. **No negative/error specification.** AC10 says "prevents invalid leave requests" without
   defining what "invalid" is or what the user should see. Untestable as written.
3. **No permissions model.** Who can see whose requests? The business flow shows a manager
   decision but never says how the manager is determined (supervisor? role?).
4. **No notification requirement.** Does the manager get told a request is waiting? The flow
   implies an approval step but no hand-off.
5. **No audit/history requirement.** Can anyone see who cancelled or rejected, and when?
6. **No timezone rule.** "Today" for a distributed workforce is ambiguous; boundary tests need it.
7. **No concurrency rule.** Two approvers acting on one request; or the employee cancelling
   while the manager approves.
8. **No accessibility or localisation criteria**, despite this being a date-entry feature —
   see the date-format defect in §4.

### 1.3 Assumptions I would proceed under (and would state in the test plan)

- A1: Leave days exclude weekends and configured public holidays.
- A2: Balance is decremented at **submission** (not at approval) — this is what the reported
  defect implies, and it is worth confirming because it determines when to assert the balance.
- A3: A rejected or cancelled request returns the days to the balance.
- A4: The leave period is the calendar year unless configured otherwise.
- A5: Only the requesting employee and their approval chain can view a request.

### 1.4 Risks

| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| **Balance arithmetic is wrong** (the reported bug) | High — directly affects employee entitlement, and is a payroll/HR compliance issue, not just a UI nit | Confirmed present | Widen coverage across day-count boundaries; verify at DB/API level, not only in the UI |
| Day-count rule undefined (Q1) | High — every calculation test is unverifiable | High | Block sign-off until specified |
| Balance can go negative (Q3) | High — employees take leave they have not earned | Confirmed possible | Clarify intended rule; add explicit tests |
| Date format `yyyy-dd-MM` (§4) | High — users silently book the wrong dates | Confirmed present | Raise as a defect in its own right |
| State machine undefined (Q6) | Medium — illegal transitions may be reachable | Medium | Model the states and test every transition, legal and illegal |
| Shared/unstable test environment | Medium — blocks execution, false failures | Confirmed (see §5) | Dedicated test environment with seeded, known data |
| No notification to approver | Medium — requests silently stall | Medium | Confirm whether in scope |

---

## 2. Test coverage design

I would structure coverage in five layers, deepest risk first. Priorities: **P1** must pass to
ship, **P2** should pass, **P3** is desirable.

### 2.1 Coverage model

| Layer | What it covers | Technique |
|---|---|---|
| Happy path | The flow in the story, end to end | Scenario-based |
| Calculation | Day count and balance arithmetic | Boundary value analysis + equivalence partitioning |
| State machine | Pending → Approved / Rejected / Cancelled | State transition testing |
| Validation | Every rejection route | Negative testing, error guessing |
| Cross-cutting | Permissions, concurrency, persistence, a11y | Risk-based exploratory |

### 2.2 Equivalence partitions and boundaries for the date range

The day-count field is where the reported defect lives, so it gets the most rigour.

| Partition | Example (Mon 10 Aug 2026 baseline) | Expected days |
|---|---|---|
| Single day, mid-week | Tue 11 → Tue 11 | 1 |
| Multi-day within one week | Mon 10 → Wed 12 | 3 |
| Range spanning a weekend | Fri 14 → Mon 17 | 2 |
| Weekend only | Sat 15 → Sun 16 | 0 (or rejected) |
| Range containing a public holiday | holiday-dependent | excludes the holiday |
| Range spanning month end | Mon 31 Aug → Wed 2 Sep | 3 |
| Range spanning the leave-period/year end | 30 Dec → 2 Jan | split or rejected — **see Q5** |
| Range spanning a leap day | 27 Feb → 2 Mar 2028 | 3 |
| Reversed range | To < From | rejected |
| Same day, half day | 11 Aug, Morning | 0.5 |

---

## 3. Test cases

Format: **ID · Title · Priority · Steps · Expected result**.
Preconditions common to all: a user with a known leave entitlement of **10 days** of a known
leave type exists, and the leave period is the current calendar year.

### 3.1 Happy path (maps to AC1–AC8)

| ID | Title | Pri | Steps | Expected |
|---|---|---|---|---|
| LR-H-01 | Leave Request page opens | P1 | Log in → Leave → Apply | Form displays Leave Type, From Date, To Date, Comment, Apply |
| LR-H-02 | Leave types are listed | P1 | Open Leave Type dropdown | Only types the employee has a balance for are offered |
| LR-H-03 | Balance is shown for the selected type | P1 | Select a leave type | The current balance is displayed and matches the entitlement |
| LR-H-04 | Select a valid date range | P1 | From = next Mon, To = next Wed | Both accepted; no validation error |
| LR-H-05 | **Day count is calculated correctly** | P1 | Set Mon→Wed | Shows exactly **3.00** days |
| LR-H-06 | Optional comment accepted | P2 | Enter a comment, submit | Request is created and the comment is stored and visible |
| LR-H-07 | Submit with no comment | P2 | Leave comment empty, submit | Request created — the comment is optional (AC5) |
| LR-H-08 | Status after submission | P1 | Submit, open My Leave | Status is **Pending Approval** (AC7) |
| LR-H-09 | Request is retrievable | P1 | Leave → My Leave, filter by the date range | The request appears with correct type, dates, day count, status |
| LR-H-10 | **Balance decremented correctly** | P1 | Note balance before (10) → submit 3 days → re-read balance | Balance is **exactly 7** — *this is the reported defect* |
| LR-H-11 | Employee can view submitted requests | P1 | Leave → My Leave | All own requests are listed (AC8) |
| LR-H-12 | Cancel a pending request | P1 | Open a pending request → Cancel | Status becomes Cancelled (AC9) |
| LR-H-13 | Balance restored after cancel | P1 | Cancel the 3-day request → re-read balance | Balance returns to 10 |

### 3.2 Calculation and boundaries

| ID | Title | Pri | Steps | Expected |
|---|---|---|---|---|
| LR-C-01 | Single day | P1 | From = To = a working Tuesday | 1.00 day; balance −1 |
| LR-C-02 | Weekend excluded | P1 | Fri → Mon | 2.00 days, not 4 (**pending Q1**) |
| LR-C-03 | Weekend-only request | P2 | Sat → Sun | 0 days, or rejected with a clear message |
| LR-C-04 | Public holiday excluded | P2 | Range containing a configured holiday | Holiday not counted |
| LR-C-05 | Spans month end | P2 | 31 Aug → 2 Sep | 3.00 days |
| LR-C-06 | Spans leave-period end | P2 | 30 Dec → 2 Jan | Per Q5 — split across periods or rejected |
| LR-C-07 | Leap day | P3 | 27 Feb → 2 Mar 2028 | 3.00 days, 29 Feb counted |
| LR-C-08 | Half day | P2 | Single day, duration Half Day | 0.50 days; balance −0.5 |
| LR-C-09 | Request equal to full balance | P1 | Request exactly 10 of 10 days | Accepted; balance becomes 0 |
| LR-C-10 | Request one day over balance | P1 | Request 11 of 10 days | Per Q3 — blocked, or explicit confirmation |
| LR-C-11 | Very long range | P3 | 12-month range | Handled without timeout or overflow |
| LR-C-12 | **Balance decrement equals displayed days** | P1 | For each of LR-C-01/02/05/08: record displayed days and balance delta | The two are **always equal** — the generalisation of the reported bug |

### 3.3 State transitions (AC7, AC9)

| ID | Title | Pri | Expected |
|---|---|---|---|
| LR-S-01 | Pending → Approved (manager) | P1 | Status Approved; balance stays reduced |
| LR-S-02 | Pending → Rejected (manager) | P1 | Status Rejected; **balance restored** |
| LR-S-03 | Pending → Cancelled (employee) | P1 | Status Cancelled; balance restored |
| LR-S-04 | Approved → Cancelled | P2 | Per Q6 — allowed or blocked, but never silently ignored |
| LR-S-05 | Cancel an already-rejected request | P2 | Not offered; no state change |
| LR-S-06 | Double-approve (two approvers, one request) | P2 | Second action rejected; balance deducted **once** |
| LR-S-07 | Cancel while the manager approves (race) | P2 | One outcome wins; balance remains consistent |

### 3.4 Negative and validation (AC10)

| ID | Title | Pri | Expected |
|---|---|---|---|
| LR-N-01 | To Date before From Date | P1 | Validation message; not submitted |
| LR-N-02 | Submit with no leave type | P1 | "Required" on Leave Type; not submitted |
| LR-N-03 | Submit with no dates | P1 | "Required" on both date fields; not submitted |
| LR-N-04 | Non-existent date (31 Feb) | P2 | Rejected as invalid |
| LR-N-05 | Malformed date (`abcd`, `13/45/2026`) | P2 | Rejected; no crash |
| LR-N-06 | Overlapping request | P1 | Per Q8 — rejected with a clear message |
| LR-N-07 | Leave type with zero balance | P1 | Not offered, or blocked with an explanation |
| LR-N-08 | Comment at and beyond max length | P3 | Boundary accepted; beyond rejected or truncated per spec |
| LR-N-09 | XSS payload in comment | P2 | Stored and rendered as inert text, never executed |
| LR-N-10 | Date far in the past | P2 | Per Q5 |
| LR-N-11 | Date far in the future (year 2999) | P3 | Rejected or handled gracefully |
| LR-N-12 | Session expires before submit | P2 | Redirect to login; **no partial request created** |
| LR-N-13 | Double-submit (click Apply twice) | P1 | Exactly **one** request created, balance deducted once |
| LR-N-14 | Browser back after submit, resubmit | P2 | No duplicate request |

### 3.5 Permissions, persistence and cross-cutting

| ID | Title | Pri | Expected |
|---|---|---|---|
| LR-X-01 | Employee cannot view another employee's request | P1 | Access denied / not listed |
| LR-X-02 | Direct URL to another's request ID | P1 | Authorisation enforced server-side, not just hidden in the UI |
| LR-X-03 | Request persists after refresh | P1 | Still present and unchanged |
| LR-X-04 | Request persists after logout/login | P1 | Still present |
| LR-X-05 | Values persist after navigating away and back | P1 | No reversion |
| LR-X-06 | Keyboard-only completion | P2 | Whole form usable without a mouse |
| LR-X-07 | Screen-reader labels on date fields | P2 | Fields and errors are announced |
| LR-X-08 | Date entry in another locale | P2 | Interpreted consistently — see §4 |
| LR-X-09 | Concurrent submissions by many users | P3 | No cross-contamination of balances |

---

## 4. What exploratory testing actually found

These are observations from the live application, not hypotheticals. Each was reproduced while
building the automation.

**F1 — The date format is `yyyy-dd-MM` (year–day–month).**
Every date field renders and parses as year–day–month. Proof from the application itself: the
2026 leave period is displayed as `2026-01-01 - 2026-31-12`, i.e. 31 December is written
`2026-31-12`. This is neither ISO 8601 nor any common regional format.
**Impact:** for any day of month ≤ 12 the date is silently ambiguous — a user entering
`2026-05-08` intending 8 May books 5 August instead, with no error. High severity, and I would
raise it as its own defect. It is also a strong latent cause of balance/day-count complaints.

**F2 — `Leave → Apply` is unusable for an account with no entitlement.**
It renders "No Leave Types with Leave Balance" instead of a form. Correct behaviour arguably,
but it means AC1 ("Employee can open the Leave Request page") is not satisfiable for a new
employee, and the story does not describe this state at all.

**F3 — Insufficient balance is a confirmation, not a block.**
Assigning more days than the balance raises a confirm dialog and proceeds on OK. This directly
contradicts a naive reading of AC10 and is the substance of Q3.

**F4 — Administrator-assigned leave is created as `Scheduled`, not `Pending Approval`.**
It bypasses the approval step in the business flow entirely. AC7 does not hold for this route.

**F5 — Leave types can be absent entirely.**
At one point the environment had no leave types configured, making the whole feature
unreachable. There is no helpful empty-state guidance for an administrator.

**F6 — Permissions can silently remove the module.**
The Leave module returned `403 Module Forbidden` for the admin account (see §5). The user is
shown a bare 403 with no route to recovery.

---

## 5. Environment observations (relevant to any test effort here)

The public demo is shared and mutable, which materially affects testing:

- The signed-in account's identity changed between runs — observed as `Hugo Musk`, then
  `Emp_NfJwbv User_WjuoigBT`, then `Donald Trump`.
- The **Leave module began returning `403 Module Forbidden`** for the admin account partway
  through this exercise, reproduced on 3/3 attempts across all three Leave routes.
- Job titles were configured at some points and completely absent at others.
- Employee records contain junk data left by other users (names of digits and punctuation).

**Recommendation:** a controlled environment with seeded, known reference data is a
prerequisite for trustworthy results on this feature. Balance arithmetic in particular cannot
be verified against a database that arbitrary third parties are mutating.

---

## 6. What I would automate versus keep manual

| Automate | Keep manual / exploratory |
|---|---|
| LR-H-01…13, LR-C-01…12 (deterministic, high value, repetitive) | Accessibility (LR-X-06/07) — needs human judgement |
| LR-N-01…07, LR-N-13 | Usability of the date picker, and the §4 F1 format problem |
| LR-X-03/04/05 persistence | Exploratory around the state machine and races (LR-S-06/07) |
| The balance-vs-days invariant (LR-C-12) — ideally at **API level** for speed and precision | First-time verification of any new leave-type configuration |

The balance invariant is the single most valuable thing to automate, because it is the defect
class actually found and it is cheap to assert on every build.
