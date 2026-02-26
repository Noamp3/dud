# SmartBoiler (DUD)

SmartBoiler is an Android app that helps you heat water only when needed.

It is designed for homes with a **hybrid solar + electric boiler**. The app looks at your shower plan and today’s weather, then decides whether solar should be enough or if it should turn on electric heating.

## At a glance

- **What it does:** Plans boiler heating based on shower schedule + weather.
- **Why it matters:** Reduces unnecessary electricity use while avoiding cold showers.
- **Who it helps:** Home users, and developers building solar-aware home automation.
- **Platform:** Android (Kotlin, Jetpack Compose).

## Quick links

- [Quick start (run the app)](#quick-start-run-the-app)
- [First-time setup inside the app](#first-time-setup-inside-the-app)
- [Repository guide (where things are)](#repository-guide-where-things-are)
- [Tech stack](#tech-stack)
- [SmartThings files in this repo](#smartthings-files-in-this-repo)

## For different readers

### If you are using the app

Start with:
1. [Quick start (run the app)](#quick-start-run-the-app)
2. [First-time setup inside the app](#first-time-setup-inside-the-app)

### If you are developing in this repository

Start with:
1. [Repository guide (where things are)](#repository-guide-where-things-are)
2. [Tech stack](#tech-stack)
3. [REPOSITORY_OVERVIEW.md](REPOSITORY_OVERVIEW.md)

## What problem this app solves

Without automation, people often turn the boiler on “just in case”, which wastes electricity.

SmartBoiler tries to prevent that by answering one question every day:

**“Do I really need electric heating today, and if yes, for how long?”**

## How it works in real life

1. You enter how many people will shower and when.
2. The app checks weather/solar forecast for your location.
3. It estimates water temperature in the tank.
4. If solar should be enough, it does not schedule heating.
5. If not, it calculates duration and schedules the smart switch so water is ready on time.
6. After shower time, you can rate the result (too cold / just right / too hot), and the app improves future plans.

## Main app capabilities

- **Daily planning:** set people count + shower time in a few taps.
- **Home dashboard:** view estimated temperature, weather summary, and boiler status.
- **Automatic scheduling:** uses WorkManager to execute ON/OFF at the planned times.
- **Feedback learning:** adjusts baseline heating durations by day type over time.
- **SmartThings control:** stores token + selected device and controls the switch via API.
- **Local-first data:** config/history are stored on-device with Room.

---

## Typical daily flow

1. Open the app and enter people count + shower time.
2. App fetches forecast and estimates expected tank temperature.
3. App either:
	- skips electric heating (solar likely enough), or
	- schedules ON/OFF heating window for your selected device.
4. You shower and send feedback.
5. Next similar day, planning is slightly more accurate.

---

## Quick start (run the app)

### Prerequisites

- Android Studio (latest stable)
- JDK 17
- Android SDK 34
- Device/emulator with API 26+

### Open and run

1. Clone this repository.
2. Open the repository root in Android Studio.
3. Wait for Gradle sync.
4. Run the `app` configuration.

### Useful terminal commands (Windows)

From the repo root:

```powershell
./gradlew.bat :app:assembleDebug
./gradlew.bat :app:testDebugUnitTest
./gradlew.bat :app:connectedDebugAndroidTest
```

---

## First-time setup inside the app

1. Complete onboarding (boiler size, target temp, location, baseline durations).
2. Open device setup.
3. Paste your SmartThings Personal Access Token (PAT).
4. Select the smart plug/switch connected to the boiler.

The app then uses this device for scheduled ON/OFF actions.

---

## Repository guide (where things are)

### Top level

- `app/` — Android application module (all runtime code).
- `smartthings/` — SmartThings registration/update payloads and operational notes.
- `smart_boiler_app_requirements.md` — product requirements and UX/logic goals.
- `REPOSITORY_OVERVIEW.md` — deeper technical architecture walkthrough.

### Inside `app/src/main/java/com/smartboiler/`

- `ui/` — Compose screens, navigation, and ViewModels.
- `domain/` — business models, interfaces, thermal calculations, and use cases.
- `data/` — Room entities/DAOs, Retrofit services, repository implementations, device control.
- `workers/` — WorkManager jobs for heating events and notifications.
- `di/` — Hilt dependency wiring.

If you are new to the codebase, start with:
1. `ui/nav` (entry navigation flow)
2. `ui/plan` + `domain/usecase/CalculateHeatingPlanUseCase`
3. `workers/BoilerScheduleWorker`

---

## Tech stack

- **Kotlin**, **Jetpack Compose**, **Navigation Compose**
- **MVVM** with clean layered separation (`ui` / `domain` / `data`)
- **Hilt** (including worker injection)
- **Room** (local persistence)
- **Retrofit + OkHttp** (network)
- **WorkManager** (background scheduling)
- **Open-Meteo** (forecast source, no API key)
- **SmartThings REST API v1** (device control)

Build/runtime versions:
- `minSdk = 26`
- `targetSdk = 34`
- `compileSdk = 34`
- Kotlin `2.2.10`
- Gradle wrapper `9.1.0`

---

## SmartThings files in this repo

For SmartThings app registration update JSONs and CLI usage, see:
- [smartthings/README.md](smartthings/README.md)
- [smartthings/app-update.json](smartthings/app-update.json)
- [smartthings/oauth-update.json](smartthings/oauth-update.json)

---

## Project status notes

- The project currently uses a single Android module: `:app`.
- Internet access is required for weather and SmartThings control.
- A dedicated relay/smart plug is recommended for safe testing.

## License

This repository is proprietary and released under an **All Rights Reserved** license.

See [LICENSE](LICENSE) for details.
