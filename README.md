# IntelliJ Test Gap Finder

An AI-assisted IntelliJ plugin that suggests likely overlooked test scenarios for a selected Java method.

## Current status

Phase 2 is implemented:

- Custom action: `Find Likely Test Gaps`
- Action available in:
  - editor context menu
  - Search Everywhere / Find Action
- When caret is inside a Java method, action extracts and displays:
  - class name
  - method name
  - method signature
  - method body text
- When caret is not inside a Java method, action shows:
  - `Place the caret inside a Java method to analyze test gaps.`

## Run locally

```powershell
& ".\gradlew.bat" runIde
```

## Verify build

```powershell
& ".\gradlew.bat" test
```

## Next phases (planned)

- Settings page (API URL, model, API key)
- Java method extraction (selected method)
- Nearby test name discovery
- OpenAI-compatible API call
- Heuristic labels:
  - likely covered
  - possibly covered
  - likely missing
- Editor gutter icon near methods
