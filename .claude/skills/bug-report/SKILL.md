---
name: bug-report
description: Template and standards for writing a defect report — required fields, severity vs priority, how to write reproduction steps, and the expected-vs-actual discipline. Use when reporting a bug found during testing, or reviewing a bug report for quality.
---

# Writing a defect report

A bug report has one job: let someone who was not there **reproduce, judge and fix** the problem
without asking you a single question.

## Required fields

| Field | Notes |
|---|---|
| ID / Title | Title states the symptom **and** the context in one line. "Leave balance reduced by 4 days for a 3-day request (10–12 Aug)" — not "Balance wrong". |
| Environment | URL, build/version, browser + version, OS. A bug without an environment is not reproducible. |
| Test data / preconditions | The exact account state needed — e.g. "employee with a 10-day entitlement". |
| Severity | Impact on the **product**. Set by QA. |
| Priority | Urgency of the **fix**. Set with the PO. Keep them separate — they are different questions. |
| Steps to reproduce | Numbered, from a known starting state. Include the step that **records the baseline**. |
| Expected result | Cite the requirement or the invariant it violates. |
| Actual result | Concrete observed values, not "it's wrong". |
| Evidence | Screenshots, network capture, logs, timestamps. |
| Impact | Who is harmed and how. This is what drives prioritisation. |
| Frequency | Always / intermittent / once. Say honestly if unconfirmed. |

## Standards

**Expected vs actual must both be numbers where numbers are involved.**
> Expected: 10 − 3 = 7.00 · Actual: 6.00 · Decrement 4, off by 1.

State the **invariant** that broke. `balance_before − balance_after == days_displayed` is far
more useful to a developer than prose, and it converts directly into a regression test.

**Include the baseline step.** A balance bug is unreproducible unless the reader knows the
starting balance. The most common defect in defect reports is a missing precondition.

**Separate observation from analysis.** Put likely causes in their own section, ranked, and
label them as hypotheses. Never let a guess look like an observation.

**Add cheap triage that narrows the cause.** Suggest the one or two extra runs that would
distinguish your hypotheses — e.g. "repeat with a 1-day request; if it drops by 2, it is a pure
off-by-one independent of range length". This is what separates a senior report.

**Do not bundle.** One defect per report. If you find a second issue while investigating, file it
separately and cross-reference.

**Close with regression coverage.** Name the test that should exist after the fix so the bug
cannot come back silently.

## Severity guide

| Severity | Meaning |
|---|---|
| Critical | Data loss/corruption, security breach, or a core flow entirely blocked with no workaround |
| High | Core function wrong or blocked; workaround is costly. **Wrong financial/entitlement arithmetic is always at least High** — it is a compliance matter, not cosmetic |
| Medium | Function wrong in a secondary path, or a reasonable workaround exists |
| Low | Cosmetic, wording, or rare edge case with negligible impact |

Silent wrongness outranks a visible crash. A crash gets noticed; a number that is quietly one
too small does not, and it compounds every time it happens.
