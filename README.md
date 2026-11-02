# Cute Battery 🐱🔋

A small Android app that puts a cute battery at the top of your screen. You can choose an anime-style eye, a cat, bunny, bear or heart, and it fills up as your battery charges.

**Version 2 doesn't need Accessibility.** It uses "Display over other apps" instead, so Google Play Protect doesn't block the install.

- **Eye:** the iris fills with the battery level, the pupil turns into a ✦ star while charging, and the lid droops below 15%
- **Animals:** dot eyes normally, `^ ^` happy eyes while charging, sleepy eyes below 15%
- Fill colors: Auto (green, then yellow, then red), Pink, Lilac or Mint
- Optional percentage text
- Optional black or white background bubble
- Optional hiding during full-screen videos and games
- Turns itself back on after a restart
- Size and position sliders, with a live preview
- No root needed. Works on Android 8.0 and newer.

## Build the APK

### Option A: from your phone, using GitHub (no computer needed)
1. Create a free account at github.com and make a new repository (it can be private).
2. Upload all the files from this folder, keeping the folder structure. The hidden `.github` folder is the one that does the build. If your phone won't upload it, use **Add file → Create new file**, type `.github/workflows/build.yml` as the name, and paste in that file's contents.
3. Open the **Actions** tab. The **Build APK** job runs on its own, or you can press **Run workflow**.
4. When it shows a green check (about 3–5 minutes), open the run and download **CuteBattery-apk** at the bottom. Unzip it to get `app-debug.apk`.

### Option B: on a computer, using Android Studio
1. Install Android Studio (free).
2. Choose **File → Open** and pick this `CuteBattery` folder. Let it sync, and accept any upgrade prompts.
3. Plug in your phone with USB debugging on and press ▶ Run, or use **Build → Build APK(s)**.

## Install and turn it on
1. Open `app-debug.apk` on your phone and allow installing from this source.
2. Open **Cute Battery** and allow notifications. The notification is a quiet "Cute Battery is on" message that keeps it running.
3. Tap **Allow display over other apps**, find Cute Battery, and turn it on.
4. Go back to the app. The cute battery appears at the top of the screen.
5. Use the sliders to place it. Android always draws the real status bar icons **on top of** overlay apps, so put it just left of the real battery icon, or hide the real one (see below).

## Hiding the real battery icon (optional)
- **Samsung:** use **Good Lock → QuickStar** to hide the battery icon.
- **Pixel and most other phones (needs a computer once):** turn on USB debugging, then run:
  ```
  adb shell settings put secure icon_blacklist battery
  ```
  To bring the real icon back, run `adb shell settings delete secure icon_blacklist`.

## Things to know
- The cute battery sits *under* the status bar and notification shade, and may not show on the lock screen. That's how Android handles overlay apps.
- Turn on **Hide during full-screen videos and games** if it gets in the way. That option needs Android 11 or newer.
- If your phone kills background apps aggressively (Xiaomi, Oppo, Vivo, Huawei), set Cute Battery's battery usage to **Unrestricted** or **No restrictions**.
- **Privacy:** the app only *draws* the icon. It can't read your screen and has no internet permission.

## Where to change things
| What | File |
|---|---|
| Shapes, face and colors | `app/src/main/java/com/cute/battery/CuteBatteryView.kt` (`drawEye`, `buildShape`, `drawFace`, `levelColor`) |
| Default size and position | `OverlaySettings.kt` |
| Overlay placement | `OverlayService.kt` (`buildParams`) |
| Settings screen | `res/layout/activity_main.xml` and `MainActivity.kt` |
| App icon | `res/drawable/ic_launcher_foreground.xml` |

To add a new character: add a `STYLE_…` constant, add a branch in `buildShape()` (draw it inside a 1×1 square scaled by `s`), set its eye height in `faceY()`, and add a radio button.
