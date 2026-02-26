# Smart Boiler Controller App — Requirements Document

## 1. Overview

An Android application that intelligently controls a home solar/electric hybrid boiler. The app minimizes electricity usage by leveraging solar energy data, weather forecasts, and user shower schedules to determine if and when the electric boiler needs to be activated. The user simply tells the app **how many people will shower and when**, and the app handles the rest.

---

## 2. Core Concept

| Aspect | Detail |
|---|---|
| **Boiler type** | Hybrid — solar collector + electric heating element |
| **Goal** | Avoid unnecessary electric heating on days when solar energy is sufficient |
| **User input** | Number of showers + desired time |
| **App output** | Automatic boiler on/off schedule (or recommendation) |
| **Control method** | Google Home smart plug / alternative smart switch API |

---

## 3. Onboarding Flow

During first launch the user provides baseline information that the algorithm needs:

### 3.1 Boiler Setup
- **Boiler capacity** (liters) — e.g. 150L, 200L
- **Electric heating power** (kW) — optional, for more accurate heating-time estimates
- **Desired shower temperature** (°C) — default ~40 °C

### 3.2 Location
- **Auto-detect GPS location** (with permission) or manual city entry
- Used to fetch real weather & solar irradiance data

### 3.3 Baseline Heating Durations
The user provides how long they **currently** turn on the electric boiler on different day types to get usable hot water. This seeds the algorithm before it has enough feedback data:

| Day Type | User Input |
|---|---|
| ☀️ Sunny day | Duration in minutes (e.g. 0 min — no need) |
| ⛅ Partly cloudy day | Duration in minutes (e.g. 30 min) |
| ☁️ Cloudy / rainy day | Duration in minutes (e.g. 90 min) |

### 3.4 Household Defaults (optional)
- Default number of people showering per day
- Typical shower time per person (minutes)
- Preferred shower time window (e.g. 18:00–20:00)

---

## 4. Main User Flow

### 4.1 Daily Input Screen
- **"How many people are showering?"** — numeric picker
- **"When?"** — time picker (or time window)
- One-tap confirmation → app calculates and schedules

### 4.2 Dashboard / Home Screen
- Current estimated water temperature 🌡️
- Today's weather summary (sunny / cloudy / rain)
- Boiler status: **Off** · **Scheduled** · **Heating** · **Ready**
- Next scheduled heating event (if any)
- Solar contribution estimate (e.g. "Solar is expected to heat water to ~42 °C today")

### 4.3 Schedule View
- Calendar/timeline showing upcoming boiler events
- Ability to add, edit, or cancel scheduled heating sessions

---

## 5. Smart Calculation Engine

### 5.1 Inputs to the Algorithm
| Input | Source |
|---|---|
| Boiler capacity (L) | Onboarding |
| Current water temperature estimate | Calculated (see §5.3) |
| Target temperature | User preference (default 40 °C) |
| Number of showers | Daily user input |
| Avg. water per shower (~50L default) | Configurable |
| Weather forecast (cloud cover, sun hours, ambient temp) | Weather API |
| Solar irradiance | Weather/solar API or derived from forecast |
| Heating element power (kW) | Onboarding |
| Historical feedback corrections | Feedback loop (§6) |

### 5.2 Decision Logic
1. Fetch weather forecast for today (or next N hours).
2. Estimate solar energy contribution → predicted solar-heated temperature.
3. Compare predicted temp vs. required temp for the requested showers.
4. **If solar alone is enough** → no electric heating, notify user "No heating needed today ☀️".
5. **If solar falls short** → calculate the deficit in kWh → convert to heating duration → schedule the boiler to turn on at the right time so water is ready by the requested shower time.
6. Add a safety margin (configurable, e.g. +10 min).

### 5.3 Water Temperature Estimation Model
The app should maintain a **running estimate** of the water temperature in the tank:

- **Solar gain**: Based on solar irradiance, collector area (if known), and hours of sunlight.
- **Standby heat loss**: Modeled as exponential decay toward ambient temperature.
- **Electric heating gain**: `ΔT = (Power × Time) / (Mass × Specific Heat)`.
- **Draw-down from usage**: When hot water is used, cold water enters the tank, reducing average temperature.

> [!NOTE]
> The initial model will use the user's baseline durations as a proxy. Over time, feedback data refines the model (see §6).

---

## 6. Feedback & Learning System

### 6.1 Post-Shower Feedback
After the scheduled shower time, the app prompts the user:

> **"How was the water?"**
> - 🥶 Not enough hot water
> - 👍 Just right
> - 🔥 Too hot — could have heated less

### 6.2 How Feedback Is Used
| Feedback | Action |
|---|---|
| 🥶 Not enough | Increase heating duration for similar conditions by X minutes |
| 👍 Just right | Reinforce current parameters |
| 🔥 Too hot | Decrease heating duration for similar conditions by X minutes |

- Feedback is tagged with the weather conditions of that day, so corrections apply to similar future days.
- Over time the model builds a personalized profile that improves accuracy.

### 6.3 Learning Goals
- Reduce unnecessary electricity usage
- Eliminate cold-shower surprises
- Adapt to seasonal changes (summer vs. winter solar gain)

---

## 7. Boiler Control Integration

### 7.1 Google Home / Smart Plug
- Control the boiler's power via a **smart plug** connected to Google Home (e.g. TP-Link Kasa, Sonoff, Shelly).
- Use the **Google Home API** or **device-specific API** (e.g. Shelly Cloud API) to send on/off commands.
- The app schedules routines or sends direct commands at the calculated times.

### 7.2 Alternative Control Methods
| Method | Pros | Cons |
|---|---|---|
| Google Home Routines API | Ecosystem integration | Limited API access for 3rd-party apps |
| Shelly / Sonoff direct API | Full control, local + cloud | Requires specific hardware |
| MQTT (Home Assistant) | Maximum flexibility | Requires Home Assistant setup |
| IFTTT webhooks | Easy integration | Latency, reliability |

> [!IMPORTANT]
> The recommended approach is to support **Shelly smart relays** as the primary integration (direct REST API, local network control, reliable). Google Home can be offered as a secondary/manual option.

---

## 8. Weather & Solar Data

### 8.1 Weather API
- Use a free/freemium weather API (e.g. **OpenWeatherMap**, **Open-Meteo**, **WeatherAPI**).
- Required data points:
  - Cloud cover (%)
  - Sunshine hours
  - Ambient temperature
  - Precipitation probability

### 8.2 Solar Irradiance (optional enhancement)
- Use a solar API (e.g. **Open-Meteo Solar**, **Solcast**) for more accurate solar energy estimates.
- Alternatively, derive solar contribution from cloud cover + location latitude + time of year.

---

## 9. Notifications

| Event | Notification |
|---|---|
| Boiler scheduled to turn on | "Boiler will start heating at 16:30 for 45 min" |
| Boiler started heating | "Boiler is now heating 🔥" |
| Water is ready | "Hot water is ready! Estimated temp: 43 °C ☀️" |
| No heating needed | "Sunny day! Solar should be enough ☀️ No heating scheduled" |
| Feedback reminder | "How was your shower? Tap to rate" |

---

## 10. Data & Privacy

- **Local-first**: All personal data (boiler size, location, history) stored on-device.
- **Weather API calls**: Only the user's city/coordinates are sent to the weather provider.
- **No account required** for basic functionality.
- Optional cloud backup/sync for multi-device use.

---

## 11. Tech Stack (Proposed)

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Architecture | MVVM + Clean Architecture |
| Local DB | Room |
| Networking | Retrofit / Ktor |
| DI | Hilt |
| Scheduling | WorkManager |
| Smart device control | Shelly API / Google Home API |
| Weather data | Open-Meteo (free, no API key) |

---

## 12. Screens Summary

| Screen | Purpose |
|---|---|
| **Onboarding** (multi-step) | Boiler setup, location, baselines |
| **Home / Dashboard** | Status, estimated temp, today's plan |
| **Schedule Shower** | Input number of people + time |
| **Schedule View** | Upcoming heating events timeline |
| **Feedback** | Post-shower rating prompt |
| **History** | Past events, feedback log, savings estimate |
| **Settings** | Edit boiler info, location, notification prefs, integrations |

---

## 13. Future Enhancements (v2+)

- **Actual temperature sensor** integration (e.g. Shelly temperature add-on on the boiler tank) for real temp readings instead of estimates.
- **Multi-boiler support**.
- **Electricity cost tracking** — show money saved by using solar.
- **Wear OS companion** — quick schedule from smartwatch.
- **Voice commands** — "Hey Google, schedule hot water for 4 people at 7 PM."
- **Guest mode** — quick add extra showers when guests visit.
- **Weekly auto-schedule** — recurring patterns (e.g. every weekday at 18:00 for 3 people).
- **Widget** — home screen widget showing boiler status + quick schedule.

---

## 14. Success Metrics

- ⚡ **Reduction in electric boiler usage** (kWh saved vs. manual control)
- 🥶 **Zero cold-shower incidents** after the learning period
- 📉 **Feedback convergence** — "Just right" ratings increase over time
- ⏱️ **User effort** — schedule a shower in < 5 seconds
