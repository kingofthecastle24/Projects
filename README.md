# Health Connect Proof (Milestone 1)

A small, standalone Android app that proves weight and workout data recorded in
**Garmin Connect** can be read on-device through **Android Health Connect** —
with no unofficial Garmin API, no scraping, no stored Garmin credentials, and
no browser automation.

This is **not** the full nutrition/weight/workout app. It is a diagnostic
proof-of-concept: it requests read-only Health Connect permissions, reads the
last 30 days of data, and shows exactly what it found (including which app
package each record came from) so the Garmin Connect data source can be
confirmed on a real device.

## What this app does

- Checks whether Health Connect is installed and up to date.
- Explains why access is needed, then requests **read-only** permissions for:
  `WeightRecord`, `BodyFatRecord`, `ExerciseSessionRecord`,
  `ActiveCaloriesBurnedRecord`, `TotalCaloriesBurnedRecord`, `DistanceRecord`,
  `HeartRateRecord`, `SpeedRecord`, `ElevationGainedRecord`, `StepsRecord`,
  `StepsCadenceRecord`.
- Never requests a write permission for any record type.
- Reads the previous 30 days of data on each refresh.
- Shows a diagnostic home screen with three sections: **Connection**,
  **Weight**, **Workouts** — plus a **Data sources** list showing every
  `metadata.dataOrigin.packageName` discovered, so you can confirm which one
  is Garmin Connect on your phone and select it as the preferred source.
- Has a **Run Connection Test** button that produces a short, plain-text
  summary (availability, permissions, sources, latest weight, latest
  workout, any errors) with **Copy** and **Export/Share** buttons, so you
  can hand the result to a developer without typing anything out by hand.
- Keeps everything on-device: no network calls, no analytics, no backend.

## Project structure

```
app/src/main/java/nz/co/ridling/healthproof/
├── MainActivity.kt                  # Compose host, navigation, permission launcher
├── HealthProofApplication.kt
├── data/
│   ├── HealthDataRepository.kt      # Assembles one DiagnosticData snapshot per refresh
│   ├── healthconnect/
│   │   ├── HealthConnectManager.kt  # All Health Connect reads (read-only)
│   │   ├── HealthPermissions.kt     # The full read-only permission set
│   │   ├── HealthConnectAvailability.kt
│   │   └── ExerciseTypeNames.kt
│   └── prefs/
│       └── PreferredSourceStore.kt  # DataStore: remembers the chosen source package
├── domain/
│   ├── Models.kt                    # WeightSummary, WorkoutSession, DiagnosticData, ...
│   ├── WeightSummarizer.kt          # Pure, unit-tested weight trend logic
│   └── DiagnosticsReport.kt         # Pure, unit-tested "Run Connection Test" report text
├── ui/
│   ├── DiagnosticViewModel.kt       # MVVM ViewModel, StateFlow<DiagnosticUiState>
│   ├── state/DiagnosticUiState.kt
│   ├── screens/                     # DiagnosticScreen, ConnectionTestScreen, PrivacyScreen, status screens
│   └── theme/
└── util/                            # Formatting + Health Connect / Play Store intents + sharing
```

A GitHub Actions workflow (`.github/workflows/build-debug-apk.yml`) builds this
project, runs unit tests and lint, and uploads the debug APK as a downloadable
artifact on every push to a `claude/**` branch and every pull request into
`main` — see "Getting the APK" below.

Architecture: simple MVVM. `HealthConnectManager` and `PreferredSourceStore`
are the data layer, `HealthDataRepository` composes them into one
`DiagnosticData` snapshot, `DiagnosticViewModel` exposes that as
`StateFlow<DiagnosticUiState>`, and Compose screens render each state
(loading / unavailable / permission required / content / error).

## What you need on the phone

- A physical Android phone running **Android 10 (API 29) or newer**, with the
  **Health Connect** app installed (pre-installed on Android 14+, otherwise
  installable from the Play Store)
- **Garmin Connect** installed and already syncing weight/activity data into
  Health Connect (Garmin Connect → Settings → Health Connect → enable sync)

You do **not** need Android Studio, a USB cable, or a computer at all to
install and test this app — see below. Android Studio is only useful if
something needs deeper troubleshooting later (covered at the end of this
file).

## Getting the APK (no developer tools needed)

Two ways to get a debug APK to test:

**Option A — you're handed the file directly.** If this came with a `.apk`
file attached (e.g. in this chat), skip to "Installing on your phone" below.

**Option B — download the automated build from GitHub.**

1. Open the pull request for this work on GitHub (or the repo's **Actions**
   tab) in your phone's browser or a computer.
2. Under the PR, look for the "Build debug APK" check, or go to **Actions →
   Build debug APK** and open the latest run for this branch.
3. Scroll to **Artifacts** and tap/click **app-debug-apk** to download it.
   It downloads as a `.zip` file containing one file: `app-debug.apk`.
4. Unzip it. On a phone: your Files app (or "Files by Google") can usually
   open/extract a `.zip` directly — look for an "Extract" or "Unzip" option
   when you tap the downloaded file. On a computer: unzip it normally, then
   transfer the `.apk` to your phone (e.g. email it to yourself, or upload
   to Google Drive/Dropbox and open the link on the phone).

Either way, you end up with a single `app-debug.apk` file somewhere on your
phone (Downloads, or wherever your Files app saved it).

## Installing on your phone (no Android Studio, no cable)

1. Using your phone's Files app (or whichever app you saved it with), find
   `app-debug.apk` and tap it.
2. Android will likely block the install the first time and show a prompt
   like **"For your security, your phone is not allowed to install unknown
   apps from this source."** Tap **Settings** on that prompt, then turn on
   **"Allow from this source"** for the app you opened the file with (e.g.
   Files, Chrome, Gmail, Drive). This is a one-time step per app.
3. Go back and tap `app-debug.apk` again → **Install** → **Install anyway**
   if a "Play Protect" warning appears (expected for an app not from the
   Play Store) → **Open**.
4. That's it — no Developer options, no USB debugging, no cable required.

If a newer version needs testing later, just repeat these steps with the new
`.apk` — Android will update the existing install in place as long as the
package name hasn't changed.

## Granting Health Connect access

1. Launch **Health Connect Proof** on the phone.
2. On first launch you'll see a screen explaining why access is needed. Tap
   **Grant Health Connect access**.
3. The system Health Connect permission screen opens, listing every record
   type this app can read. Turn on the ones you're happy to share (or "Allow
   all"), then confirm.
4. You're returned to the app, which immediately reads the last 30 days of
   data and shows the diagnostic screen.
5. You can revoke access at any time from **Health Connect → App
   permissions → Health Connect Proof**, or via the "Open Health Connect
   settings" button shown on this app's permission screen. The app's
   **Privacy information** screen (the "i" icon, top right) explains exactly
   what is read, why, and confirms no Garmin password is ever collected and
   no data leaves the phone.

## Using "Run Connection Test" to report a problem

If anything looks wrong once you've granted access and selected Garmin as
the preferred source, use this instead of describing it from memory or
taking screenshots of every field:

1. On the diagnostic home screen, tap **Run Connection Test**.
2. Tap **Copy** (puts it on your clipboard — paste it into a message) or
   **Export / Share** (opens Android's share sheet so you can send it via
   whatever app you like — Messages, email, back into this chat, etc.).
3. Send that text over. It's a short plain-text report — Health Connect
   availability, permission status, every data source found, your selected
   preferred source, your **latest** weight and workout only (not your full
   30-day history), and any read errors — so there's nothing to manually
   transcribe and no detailed health history leaves the phone beyond that
   single latest weight/workout snapshot.

## Diagnosing missing Garmin data

If the **Weight** or **Workouts** sections are empty, or Garmin doesn't
appear in **Data sources**, work through this checklist on the phone:

1. **Confirm Garmin Connect is syncing to Health Connect at all.**
   Garmin Connect → **☰ menu → Settings → Health Connect** (or search
   "Health Connect" in Garmin Connect's settings) → make sure sync is
   turned on and each relevant data type (weight, activities, etc.) is
   enabled.
2. **Check Health Connect directly**, independent of this app: open the
   **Health Connect** app → **Data and access** → **Weight** /
   **Exercise**, and confirm records with a recent date exist and that
   **Garmin Connect** is listed as a contributing app.
3. **Check this app's own permission grants.** Open Health Connect →
   **App permissions** → **Health Connect Proof**, and confirm the record
   types you expect data for are switched on. If you denied a permission
   the first time, you can grant it here later — no need to reinstall.
4. **Look at the "Data sources" card** on this app's diagnostic screen.
   It lists every `metadata.dataOrigin.packageName` that Health Connect
   returned in the last 30 days, with a record count for each. Garmin
   Connect's package is `com.garmin.android.apps.connectmobile` on most
   devices — confirm it appears here before selecting it as the preferred
   source. If it's missing entirely, the problem is upstream of this app
   (Garmin Connect isn't writing to Health Connect yet, or the sync hasn't
   run since the weight/activity was recorded).
5. **Select Garmin as the preferred source.** Once you've confirmed its
   package name in the Data sources list, tap **Set as preferred** on that
   row. Weight and workouts are then filtered to that source only, which
   avoids a second app (e.g. a phone's own Health app) double-reporting the
   same workout or a conflicting weight value.
6. **Pull-to-refresh isn't implemented** — use the refresh icon (top right)
   after making any change in Garmin Connect or Health Connect, since data
   is only re-read on demand or on app start.
7. **30-day window.** This proof only reads the last 30 days. A weight or
   workout entry older than that intentionally won't show up.

## If something needs troubleshooting: opening in Android Studio

You shouldn't need this for normal testing — it's only for digging into a
bug that the Connection Test report and README checklist above can't
explain.

1. Install Android Studio (Ladybug/2024.2 or newer) and JDK 17.
2. **File → Open** → select this repository's root folder (the one
   containing `settings.gradle.kts`) → let Gradle sync. The project uses
   the Gradle wrapper (`./gradlew`), so no separate Gradle install is
   needed; if Android Studio asks to install missing SDK platforms/build
   tools (`compileSdk 36` / `minSdk 26`), accept.
3. Connect the phone by USB, enable Developer options (**Settings → About
   phone** → tap "Build number" 7 times) and **USB debugging** under
   **Settings → Developer options**, accept the "Allow USB debugging?"
   prompt, then use Android Studio's **Run ▶** button with the phone
   selected as the target — this gives you Logcat output live, which is
   the main reason to drop into Android Studio at all.
4. Or, from the command line: `./gradlew assembleDebug` writes the APK to
   `app/build/outputs/apk/debug/app-debug.apk`, and
   `adb install -r app/build/outputs/apk/debug/app-debug.apk` installs it
   (`adb` lives in `<Android SDK>/platform-tools`).

## Running checks locally

```bash
./gradlew assembleDebug        # build a debug APK
./gradlew testDebugUnitTest    # unit tests (pure Kotlin logic: formatting, weight trend, diagnostics report)
./gradlew lintDebug            # Android lint
```

All three run automatically on every push via
`.github/workflows/build-debug-apk.yml`; see the PR description or the
Actions tab for results on the current commit.

## What still needs verifying on a real device

Everything above the Health Connect client boundary was exercised with
Android's build tooling only (this development environment has no physical
Android hardware or emulator attached). Before treating Milestone 1 as fully
signed off, verify on an actual phone with Garmin Connect installed:

- The permission rationale/request screen actually renders the Health
  Connect system dialog listing all 11 record types, and granting/denying
  is reflected correctly back in this app.
- Garmin Connect's real `dataOrigin.packageName` value appears in the
  **Data sources** card (expected to be
  `com.garmin.android.apps.connectmobile`, but this must be confirmed, not
  assumed — the app deliberately does not hard-code it).
- Weight, exercise session, heart rate, distance, speed, elevation and
  active-calorie values displayed match what Garmin Connect itself shows,
  once Garmin is selected as the preferred source.
- Behaviour when Health Connect is not installed / needs updating, on a
  device that actually exhibits that state.
- Behaviour after revoking permissions from the Health Connect app while
  this app is running, then returning to it and refreshing.

## What Milestone 1 deliberately excludes

Per scope, this build does not include: food logging, Supabase, any
Anthropic/AI integration, or final visual design. It also does not write
anything back to Health Connect, and makes no network requests of any kind.
