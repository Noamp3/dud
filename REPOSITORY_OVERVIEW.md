# SmartBoiler Repository Overview

## What this repository is

This repository contains an Android app (`SmartBoiler`) that schedules electric boiler heating around expected solar contribution, user shower plans, and weather forecasts.

Primary implementation is in `app/` (single Android application module), with SmartThings registration/update payloads in `smartthings/`.

---

## How the project is built

## Build system
- **Gradle Kotlin DSL** (`build.gradle.kts`, `settings.gradle.kts`)
- **Single module**: `:app`
- **Gradle wrapper**: `gradlew`, `gradlew.bat`, wrapper set to **Gradle 9.1.0**
- **Android plugin**: `com.android.application` **9.0.1**
- **Kotlin**: **2.2.10**
- **Java target**: **17**
- **Android SDK levels**: `minSdk=26`, `targetSdk=34`, `compileSdk=34`

## Important build plugins in `app`
- `com.android.application`
- `org.jetbrains.kotlin.android`
- `org.jetbrains.kotlin.plugin.compose`
- `com.google.dagger.hilt.android` (DI)
- `com.google.devtools.ksp` (annotation/code generation for Hilt + Room)

## Build commands (Windows)
From repository root:
- Debug APK: `./gradlew.bat :app:assembleDebug`
- Release APK: `./gradlew.bat :app:assembleRelease`
- Unit tests: `./gradlew.bat :app:testDebugUnitTest`
- Instrumentation tests: `./gradlew.bat :app:connectedDebugAndroidTest`

---

## Runtime architecture and flow

## High-level architecture style
- **MVVM + Clean-ish layering**
  - `ui/*` → Compose screens + ViewModels
  - `domain/*` → models, repository contracts, use-cases, thermal math
  - `data/*` → Room, Retrofit, repository implementations, device controller
  - `workers/*` → WorkManager background execution for on/off commands + notifications

## App startup
1. `SmartBoilerApp` is `@HiltAndroidApp` and provides WorkManager config via `HiltWorkerFactory`.
2. Manifest disables default WorkManager initializer and routes through Hilt-provided worker factory.
3. `MainActivity` sets Compose content and loads `SmartBoilerNavHost`.
4. Nav host checks onboarding completion and chooses onboarding vs home as start destination.

## Planning + scheduling flow
1. User chooses people/date/time in `ScheduleViewModel`.
2. ViewModel fetches config + baselines (Room) and weather (Open-Meteo).
3. `CalculateHeatingPlanUseCase` computes if heating is needed, duration, and start time.
4. Schedule is persisted in Room.
5. If heating is required, `BoilerScheduleWorker.schedule(...)` enqueues turn-on and turn-off jobs.
6. Worker controls device through `SmartSwitchController` implementation (`SmartThingsController`) and emits notifications.

## Learning/feedback flow
1. After shower time, app can request feedback (`TOO_COLD`, `JUST_RIGHT`, `TOO_HOT`).
2. Feedback is saved with weather/day-type context.
3. `AdjustBaselinesUseCase` updates only the relevant baseline bucket (sunny/partly/cloudy) by a fixed step.
4. Updated baselines influence future temperature estimation/planning.

---

## Rationale behind key technical decisions

- **Single `:app` module** keeps delivery and iteration fast while product logic is still evolving.
- **Compose UI + Navigation Compose** reduces XML/UI boilerplate and fits state-driven ViewModel flows.
- **Hilt + KSP** gives compile-time DI wiring and direct WorkManager worker injection.
- **Room local-first persistence** matches offline-first product behavior and privacy goals from requirements.
- **Open-Meteo** provides required weather/solar data without API key friction.
- **In-memory weather caching** (rounded lat/lon + date + TTL + stale fallback) prevents redundant API calls and degrades gracefully on network issues.
- **Thermal formulas centralized in `ThermalMath`** avoids duplicated/fragmented energy math across features.
- **Baseline-calibrated estimation model** balances physical modeling with user feedback personalization, rather than relying only on static constants.
- **WorkManager orchestration** ensures scheduled heating commands survive app process death/background restrictions.
- **SmartThings abstraction via `SmartSwitchController`** decouples core planning logic from vendor-specific device APIs.
- **Pinned Compose versions** (UI 1.6.0 + Material3 1.2.0) avoid runtime incompatibility from mismatched transitive versions.
- **Room 2.7.2 + Kotlin 2.2 compatible KSP pairing** reflects practical build stability constraints in this codebase.

---

## Technology stack (actual, current)

## Language & platform
- Kotlin
- Android (min 26 / target 34)
- Java 17 toolchain target

## UI
- Jetpack Compose (`ui`, `material3`, animations, tooling)
- Navigation Compose

## Architecture & state
- ViewModel + lifecycle runtime/compose
- Hilt DI (including hilt-navigation-compose)

## Data
- Room (`runtime`, `ktx`, `compiler` via KSP)
- DataStore Preferences (dependency present; minimal current footprint)

## Networking
- Retrofit 2 + Gson converter
- OkHttp + logging interceptor
- APIs:
  - Open-Meteo forecast API
  - SmartThings REST API v1

## Background & device orchestration
- WorkManager (`work-runtime-ktx`)
- `androidx.hilt:hilt-work`
- Push/local notifications for schedule lifecycle events

## Google/Android services
- Google Play Services Location

## Testing
- JUnit4
- AndroidX test ext + Espresso
- Compose UI test artifacts

---

## Repository layout notes

- `app/src/main/java/com/smartboiler/`
  - `ui/` screens, nav graph, viewmodels
  - `domain/` use-cases, models, interfaces, device abstraction
  - `data/` room/retrofit/repositories/controllers
  - `workers/` scheduled heating + notifications
  - `di/` module wiring (DB, network, bindings)
- `smartthings/`
  - CLI payloads for SmartThings app + OAuth update
  - operational README for updating app registration
- `smart_boiler_app_requirements.md`
  - product/functional rationale and intended behavior
- `build/`, `app/build/`
  - generated artifacts (not source-of-truth for architecture)

---

## Current external integration boundary

The Android app uses SmartThings REST APIs directly at runtime (PAT + selected device ID saved locally). The `smartthings/*.json` files are deployment/registration metadata for SmartThings app configuration and are managed outside the Android Gradle build itself.
