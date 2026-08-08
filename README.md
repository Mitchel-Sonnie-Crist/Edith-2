# J.A.R.V.I.S. OS — Boot HUD Overlay for Niagara Launcher

A high-tech, Iron Man **J.A.R.V.I.S.**-inspired HUD boot sequence for Android,
built to sit cleanly on top of [Niagara Launcher](https://niagaralauncher.app/).
On device boot (or unlock), it plays a ~3.6s cyan/electric-blue heads-up display
— a pulsing Arc Reactor, rotating diagnostic rings, and scrolling system
telemetry — synchronized with Stark-style diagnostic sound effects, then fades
crisply back to the desktop.

The HUD is anchored **center-right** and is a **non-interactive** overlay
(touches pass straight through), so Niagara's left-aligned vertical app list is
never obstructed and stays fully swipeable while the animation plays.

---

## Feature checklist

| Requirement | Implementation |
|---|---|
| Cyan/electric-blue HUD, Arc Reactor, diagnostic rings, telemetry | `ui/hud/*` (Compose Canvas) |
| 60/120 FPS smooth animation | Compose `Animatable` + `InfiniteTransition` (frame-driven) |
| ~3–4s duration + crisp fade-out | `JarvisHud` master timeline (`durationMillis = 4000`) |
| Low-latency audio | `audio/SoundManager` — SoundPool (SFX) + Media3/ExoPlayer (hum) |
| Spoken J.A.R.V.I.S. voice | `audio/JarvisVoice` — built-in Text-to-Speech, UK English, no audio files needed |
| Boot trigger | `boot/BootCompletedReceiver` → `service/JarvisOverlayService` |
| Unlock trigger | dynamic `ACTION_USER_PRESENT` receiver in the service |
| J.A.R.V.I.S. launcher / Home screen | `ui/home/HomeActivity` + `JarvisHomeScreen` — persistent HUD, live clock, real battery, app list, voice |
| Niagara-safe layout | center-right anchor + `FLAG_NOT_TOUCHABLE` overlay |
| Dark base + neon highlights | `ui/theme/*` (dark-only Material 3 scheme) |
| Manifest permissions | `RECEIVE_BOOT_COMPLETED`, `SYSTEM_ALERT_WINDOW`, `FOREGROUND_SERVICE(_SPECIAL_USE)` |
| Gradle (KTS) with version catalog | `build.gradle.kts`, `gradle/libs.versions.toml` |

---

## Project structure

```
Edith-2/
├── build.gradle.kts                 # root plugins (apply false)
├── settings.gradle.kts              # repos + module include
├── gradle.properties
├── gradle/
│   ├── libs.versions.toml           # version catalog (Compose, Lottie, Media3…)
│   └── wrapper/                      # Gradle 8.9 wrapper
├── app/
│   ├── build.gradle.kts             # Android + Compose config, dependencies
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml      # permissions, receiver, service, activity
│       ├── java/com/stark/jarvis/
│       │   ├── JarvisApplication.kt         # notification channel
│       │   ├── MainActivity.kt              # control panel + in-app preview
│       │   ├── boot/
│       │   │   └── BootCompletedReceiver.kt # BOOT_COMPLETED → service
│       │   ├── service/
│       │   │   └── JarvisOverlayService.kt  # foreground service owns overlay+audio
│       │   ├── audio/
│       │   │   └── SoundManager.kt          # SoundPool + ExoPlayer
│       │   ├── ui/
│       │   │   ├── hud/
│       │   │   │   ├── JarvisHud.kt         # orchestrates the whole sequence
│       │   │   │   ├── ArcReactor.kt        # glowing reactor core (Canvas)
│       │   │   │   ├── DiagnosticRings.kt   # sweeping diagnostic rings (Canvas)
│       │   │   │   ├── TelemetryText.kt     # typewriter console readout
│       │   │   │   └── HudSound.kt          # sound-cue contract
│       │   │   ├── overlay/
│       │   │   │   ├── ComposeOverlay.kt        # Compose in a WindowManager window
│       │   │   │   └── OverlayLifecycleOwner.kt # lifecycle/vm/savedstate owner
│       │   │   └── theme/                    # Color / Type / Theme
│       │   └── util/
│       │       ├── OverlayPermission.kt      # SYSTEM_ALERT_WINDOW helpers
│       │       └── JarvisPreferences.kt      # boot/unlock toggles
│       └── res/
│           ├── drawable/ic_arc_reactor_foreground.xml
│           ├── mipmap-anydpi-v26/            # adaptive launcher icon
│           ├── raw/                           # drop sound assets here (see docs/SOUND_ASSETS.md)
│           └── values/                       # strings, colors, themes
```

## Architecture at a glance

```
BOOT_COMPLETED ─▶ BootCompletedReceiver ─▶ JarvisOverlayService (foreground)
                                                │
USER_PRESENT ─▶ (dynamic receiver) ────────────┤
                                                ▼
                                   ComposeOverlay (WindowManager, non-touchable)
                                                ▼
                                   JarvisHud ── onSound ──▶ SoundManager
                                        │                    (SoundPool + ExoPlayer)
                                        └── onFinished ──▶ remove overlay + stop
```

The receiver stays lightweight (it can't draw UI); all rendering and audio live
in the foreground service so the OS won't kill the animation mid-play.

---

## Download the APK

Every push is built in CI and published to a rolling release with a **permanent
download link**:

**➡️ [Download `jarvis-os.apk`](https://github.com/Mitchel-Sonnie-Crist/Edith-2/releases/download/latest-build/jarvis-os.apk)**

Install it on your phone (allow "install from unknown sources" when prompted),
then follow the on-device setup below.

## Build & run

Requires JDK 17 and the Android SDK (compileSdk 34).

```bash
./gradlew :app:assembleDebug        # build the APK
./gradlew :app:installDebug         # install on a connected device
```

> **Note:** dependencies are fetched from Google's Maven (`dl.google.com`) and
> Maven Central. Build on a machine with access to those; some locked-down CI
> networks block `dl.google.com`.

### First-run setup on device

1. Launch **J.A.R.V.I.S. OS**.
2. Tap **Grant display-over-apps permission** (SYSTEM_ALERT_WINDOW must be
   enabled in system Settings — it can't be granted by a normal prompt).
3. Toggle **Play on device boot** and/or **Play on unlock**.
4. Tap **Run boot sequence now** for an in-app preview (no permission needed),
   or **Trigger as system overlay** to test the real overlay path.

### Use it as your home screen (the full J.A.R.V.I.S. UI)

Beyond the boot overlay, the app ships a **launcher**: set it as your Home app and
your entire home screen becomes a persistent J.A.R.V.I.S. HUD — live clock, real
battery telemetry, an arc reactor (tap it for a spoken status report), and your
full app list. It greets you by voice on each launch.

1. In the app, tap **"Set J.A.R.V.I.S. as Home screen"** (or Settings → Apps →
   Default apps → Home app).
2. Choose **J.A.R.V.I.S. OS**.
3. Press Home — you're now on the HUD. Tap any app to launch it; tap the reactor
   for a status report; tap **J.A.R.V.I.S. SETTINGS** to return to this panel.

To go back to your normal launcher, just pick it again under Default apps → Home.

### Voice & sound

The app **speaks** the diagnostics out of the box using the device's built-in
Text-to-Speech engine (`audio/JarvisVoice`) — a calm UK-English "AI butler"
delivery ending in *"All systems online. Welcome back, sir."* No audio files are
required; the voice is part of the APK. For the best result, install a British
English voice under **Settings → Accessibility → Text-to-speech** on the phone.

The short SFX layer (metallic chimes, power-up hum, UI beeps) is **optional and
additive**: drop your files into `app/src/main/res/raw/` — see
[`docs/SOUND_ASSETS.md`](docs/SOUND_ASSETS.md) for the expected filenames — and
they play alongside the voice. Without them, the voice still works.

---

## Notes & tuning

- **Duration / beats:** adjust `durationMillis` and the timeline fractions
  (`FADE_IN`, `CONTENT_END`, `FADE_OUT_START`) at the bottom of `JarvisHud.kt`.
- **Telemetry copy:** edit the `telemetry_*` strings in `res/values/strings.xml`.
- **HUD position:** pass a different `alignment` to `JarvisHud` (default
  `Alignment.CenterEnd` keeps Niagara's left column clear).
- **Lottie:** `lottie-compose` is wired up if you'd rather drop in a designer's
  `.json` HUD animation alongside (or instead of) the Canvas art.
- **Battery:** if "Play on unlock" is **off**, the service stops itself the
  instant the boot animation finishes — no persistent background cost.

## OEM caveats

Auto-start on boot and drawing overlays are aggressively restricted by some
OEM skins (MIUI, ColorOS, One UI, etc.). On those devices the user may need to
whitelist the app for **auto-start** and **display-over-other-apps** in the
manufacturer's battery/permission settings for the boot trigger to fire.
