---
name: kotlin-android-tv
description: Build, refactor, debug, and review Kotlin code for Android apps and Android TV apps. Use when Codex is working in a Gradle-based Android project with Kotlin, Activities, Fragments, ViewModels, coroutines, Retrofit, Media3, Compose, XML layouts, D-pad navigation, Leanback launchers, playback flows, remote-control input, or TV-specific UX constraints.
---

# Kotlin Android TV

## Overview

Use this skill to implement features, fix bugs, and review architecture in Kotlin Android projects without fighting the platform or the existing codebase. Inspect the current project stack first, then extend the established patterns instead of introducing a parallel architecture.

## Workflow

1. Inspect the Android project before editing.
2. Identify the active UI stack, state-management pattern, dependency setup, and testing strategy.
3. Keep new code aligned with the existing stack unless the user explicitly asks for migration.
4. Apply Android TV rules when the app is launched by remote control, uses `LEANBACK_LAUNCHER`, or renders long-distance "10-foot" UI.
5. Validate with the narrowest useful command such as Gradle unit tests, lint, or assemble.

## Inspect First

Read these artifacts before deciding how to implement the change:

- Root `build.gradle.kts`, `settings.gradle.kts`, and module `build.gradle.kts` files for plugin versions, SDK levels, and dependencies.
- `AndroidManifest.xml` for launch mode, permissions, intent filters, and TV-specific capabilities.
- Existing UI entry points such as `Activity`, `Fragment`, composables, adapters, or custom views.
- Existing data flow objects such as repositories, Retrofit services, DTOs, and `ViewModel` classes.
- Existing tests before writing new ones, so coverage style stays consistent.

## Default Implementation Rules

- Prefer Kotlin idioms: null-safety, small immutable models, sealed hierarchies for UI state, and extension functions only when they improve readability.
- Keep business logic out of `Activity` and `Fragment` classes; let them bind UI, lifecycle, and navigation.
- Use coroutines and structured concurrency for async work. Avoid leaking work outside lifecycle-aware scopes.
- Reuse the app's existing DI style. Do not introduce Hilt, Koin, or manual service locators unless the project already uses them or the user requests the change.
- Respect the existing UI toolkit. Stay with Views/XML in a Views project and with Compose in a Compose project unless migration is the task.
- Keep networking, persistence, playback, and rendering concerns separated so they can be tested independently.

## Task Routing

- For phone or tablet UI, lifecycle, state, coroutines, repositories, Retrofit, Room, WorkManager, notifications, or tests, read [references/android-core.md](references/android-core.md).
- For D-pad focus, `LEANBACK_LAUNCHER`, TV layouts, playback surfaces, key handling, banners, or remote-first UX, read [references/android-tv.md](references/android-tv.md).
- For mixed apps that run on both mobile and TV, apply Android core guidance first and then add TV constraints only where device behavior diverges.

## Delivery Checklist

- Confirm whether the change affects phone, tablet, TV, or all device classes.
- Add or update tests when business logic, parsing, validation, focus behavior, or playback state changes.
- Check manifest, resource qualifiers, and input handling when behavior differs by form factor.
- Keep logs and error messages actionable, especially around network and playback failures.
- Run the smallest verification command that proves the change.

## Common Requests

- "Adicionar uma tela em Kotlin usando o padrão já existente."
- "Corrigir navegação por controle remoto no Android TV."
- "Refatorar Activity pesada para ViewModel + repository."
- "Resolver bug de Media3/ExoPlayer ou WebView em app de TV."
- "Escrever testes para validação, parsing ou estado de UI."
