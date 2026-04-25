# IntelliJ Test Gap Finder

AI-assisted IntelliJ IDEA plugin that suggests likely overlooked test scenarios for a selected Java method.

It combines PSI-based code context, discovered test method names, and LLM reasoning to provide a heuristic (not formal) assessment of test coverage gaps.

## What this plugin does

For a selected Java method, the plugin:

1. Extracts method context from PSI (class, signature, body).
2. Finds a related test class (heuristic: `ClassNameTest` / `ClassNameTests` in test roots).
3. Extracts existing test method names (`@Test`, optional `@DisplayName`).
4. Builds structured LLM input (`MethodLlmInput`) with extracted signals.
5. Calls an OpenAI-compatible chat completions endpoint.
6. Parses strict JSON response.
7. Normalizes inconsistent coverage labels.
8. Renders a structured Tool Window report with checklist copy support.

## Current MVP scope

- Language support: **Java method analysis**
- Test context: **test method names only**
- LLM transport: **OpenAI-compatible APIs**
- Coverage labels:
  - `likely_covered`
  - `possibly_covered`
  - `likely_missing`

Not included (by design for MVP):

- Formal coverage analysis (JaCoCo parsing, test execution)
- Full call-chain/context propagation across all layers
- Automated test code generation

## Main features implemented

- Action: `Find Likely Test Gaps`
  - Editor context menu
  - Search Everywhere / Find Action
- Gutter icon near Java method names
- Tool Window (`Test Gap Finder`) with:
  - Method summary
  - Existing test methods
  - LLM summary
  - Suggested scenarios
  - Coverage assessment
  - `Copy as checklist` button
- Settings page (`Tools > Test Gap Finder`):
  - API Base URL
  - API key
  - Model name
- Loading/error states and parse-failure fallback messages

## Architecture overview

High-level flow:

`Action/Gutter -> MethodAnalysisCoordinator -> TestDiscoveryService -> LlmSuggestionService -> OpenAiCompatibleLlmClient -> LlmResponseParser -> LlmResultNormalizer -> TestGapResultsPresenter`

Key packages:

- `actions` - entry action
- `gutter` - method line marker
- `analysis` - method extraction/orchestration/signals
- `discovery` - test class + test method lookup
- `llm` - client, contract, parser, normalization
- `settings` - persistent plugin settings
- `ui.toolwindow` - rendering and interactions
- `i18n` - message bundle access

## Prerequisites

- JDK 21 (project toolchain)
- Windows/macOS/Linux
- IntelliJ Platform Gradle plugin environment (handled by Gradle)

## Run in sandbox IntelliJ

```powershell
Set-Location "yourPath\TestGapFinder"
.\gradlew.bat runIde
```

## Build / compile checks

```powershell
Set-Location "yourPath\TestGapFinder"
.\gradlew.bat compileKotlin --no-daemon
.\gradlew.bat buildPlugin --no-daemon
```

If `buildPlugin` fails with a file-lock error on sandbox JAR, close the sandbox IDE and rerun.

## Configure LLM provider

Inside sandbox IDE:

`Settings -> Tools -> Test Gap Finder`

Set:

- `API Base URL` (example: `https://api.openai.com/v1`)
- `API key`
- `Model name` (example: `gpt-4o-mini`)

### Provider-agnostic note

The plugin expects an OpenAI-compatible endpoint implementing `/chat/completions`.

## Usage

1. Open a Java source file.
2. Place caret inside a method.
3. Trigger `Find Likely Test Gaps` (context menu or action search), or click the gutter icon.
4. Review results in `Test Gap Finder` Tool Window.
5. Optionally click `Copy as checklist`.

## Output contract (LLM JSON)

Expected shape:

- `summary`
- `scenarios[]`
  - `title`
  - `category` (`happy_path|boundary|invalid_input|exception_path|edge_case`)
  - `priority` (`high|medium|low`)
  - `rationale`
  - `confidence` (`high|medium|low`)
  - `assumption`
- `coverageAssessment`
  - `likely_covered[]` (`title`, `matchedTests[]`)
  - `possibly_covered[]` (`title`, `matchedTests[]`)
  - `likely_missing[]` (`title`, `matchedTests[]`)

## Normalization and consistency rules

After parsing, the plugin normalizes contradictory coverage labels:

- If `likely_covered` has no matched tests -> move to `possibly_covered`
- If `likely_missing` has matched tests -> move to `possibly_covered`
- Remove duplicate scenario titles across buckets with precedence:
  - `likely_covered` > `possibly_covered` > `likely_missing`

## Error handling implemented

- Not in Java method -> clear error dialog
- Missing LLM settings -> clear Tool Window error message
- Network/provider failure -> status error message
- Invalid model JSON output ->
  - `The model response could not be parsed. Please try again.`
- No related test class ->
  - `No related test class found. Showing suggestions based on implementation only.`

## Known limitations

- Heuristic by design; does not prove real coverage.
- Existing tests are analyzed by **names only**.
- Upstream validation may exist outside analyzed service method (for example controller/DTO `@Valid`).
- Cross-layer call chain analysis is intentionally out of current MVP scope.

## Cost and local testing options

- API usage usually requires a provider API key and billing.
- For low-cost testing, use a small model (for example `gpt-4o-mini`).
- For zero-cost E2E demo, use a local/mock OpenAI-compatible endpoint.

## Internship-oriented positioning

This is an intentionally scoped, production-minded MVP suitable for a short internship timeline.
