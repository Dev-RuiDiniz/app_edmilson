# Android Core

## Use This Reference For

- Kotlin Android apps that primarily target phone or tablet.
- Shared app layers that also affect Android TV, such as networking, models, repositories, or playback controllers.
- Refactors involving Activities, Fragments, ViewModels, coroutines, tests, or Gradle configuration.

## Read the Existing Stack First

- Detect whether the project uses Views/XML, Compose, or both.
- Detect whether state is managed with `ViewModel`, MVI-style reducers, direct mutable state, or another pattern already present.
- Detect whether dependency wiring is manual, constructor-based, or framework-driven.
- Detect whether tests are JUnit4, JUnit5, Robolectric, instrumented tests, or a mix.

## Preferred Code Shape

- Keep UI state explicit with `sealed class`, `sealed interface`, or clear immutable data classes.
- Normalize and validate external input close to the boundary layer.
- Put IO work in repositories or data sources, not directly inside the view layer.
- Expose small, intention-revealing methods instead of large "do everything" flows.
- Avoid hidden global state.

## Coroutines and Lifecycle

- Use `viewModelScope`, `lifecycleScope`, or an existing app scope that matches ownership.
- Prefer suspending APIs over callback pyramids.
- Cancel obsolete work when user input or lifecycle state changes.
- When collecting flows from UI, bind them to lifecycle-aware collection instead of collecting forever.

## UI Guidance

- For Views/XML: keep view lookup/binding localized, move logic into helpers or `ViewModel`, and avoid giant `onCreate`.
- For Compose: hoist state, keep composables side-effect light, and isolate navigation or platform APIs outside leaf UI when practical.
- Reuse existing theme, spacing, typography, and string resources instead of hardcoding new values.

## Networking and Parsing

- Keep DTOs separate from domain or UI models when API payloads are unstable.
- Validate required fields before rendering.
- Surface retryable errors distinctly from validation errors.
- Add logging around request or parsing failures when the current project already logs network issues.

## Testing Focus

- Add unit tests for validators, mappers, reducers, repository branching logic, and URL/content parsing.
- Add UI or instrumentation tests only when the behavior is truly view-driven and cannot be covered lower in the stack.
- Prefer targeted tests over snapshot-size test suites for small fixes.

## Verification

- Use `assembleDebug` to catch resource and manifest issues.
- Use unit tests for pure Kotlin logic.
- Use lint when changing resources, accessibility behavior, or manifest declarations.
