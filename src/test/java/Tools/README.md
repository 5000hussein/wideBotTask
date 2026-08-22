# DOM probes

Developer tools, **not tests**. Each has a `main` and prints the real DOM shape of a screen, so
locators can be written against verified selectors instead of guesses. Every locator rule in
`.claude/skills/orangehrm-locators/SKILL.md` came from running one of these.

They are committed deliberately: they are how the next person re-verifies the application after
a UI change, and how an unexplained failure gets diagnosed without guessing.

| Probe | Answers |
|---|---|
| `LocatorProbe`      | What is on each screen? Walks login → dashboard → PIM → leave, dumping interactive elements and labels. |
| `FormProbe`         | What are the form fields, tables and buttons of each screen, without the per-row noise? |
| `AutocompleteProbe` | What does the "Type for hints..." dropdown render over time? (Found the `Searching....` four-dot placeholder.) |
| `DropdownProbe`     | What options does each Employee List filter actually offer? (Found job titles absent.) |
| `LeaveTypeProbe`    | Are leave types configured, and where? |
| `LeaveRouteProbe`   | Are the Leave routes reachable, and how fast? (Found `403 Module Forbidden`.) |

## Running one

```bash
mvn -q test-compile
mvn -q dependency:build-classpath -Dmdep.outputFile=target/cp.txt -Dmdep.includeScope=test

# Windows (semicolons)
java -cp "target/classes;target/test-classes;$(cat target/cp.txt)" Tools.FormProbe

# Linux / macOS (colons)
java -cp "target/classes:target/test-classes:$(cat target/cp.txt)" Tools.FormProbe
```

Run headed to watch: add `-Dheadless=false`.
