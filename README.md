# Cute Battery 🐱🔋

A small Android app that puts a cute battery on top of your status bar. You can choose a cat, bunny, bear or heart, and it fills up as your battery charges.

- Dot eyes normally, `^ ^` happy eyes while charging, sleepy eyes below 15%
- Fill colors: Auto (green, then yellow, then red), Pink, Lilac or Mint
- Optional percentage text
- Black or white background pill to cover the real battery icon
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
2. Open **Cute Battery** and tap **Open Accessibility settings**.
3. Go to **Installed apps** (Samsung calls it *Installed apps*, some phones call it *Downloaded apps*), then **Cute Battery**, and turn it **On**.
   - If the switch is greyed out (Android 13 and newer): go to **Settings → Apps → Cute Battery → ⋮ (top-right) → Allow restricted settings**, then try again.
4. Back in the app, use the sliders to move the cute battery over your real battery icon.

## Hiding the real battery icon
The app draws *on top of* the status bar. It can't remove the real icon, so pick one of these:
- **Easiest:** in the app, choose a **Black** or **White** background so the pill covers the real icon.
- **Samsung:** install **Good Lock → QuickStar** and hide the battery icon there.
- **Pixel and most other phones (needs a computer once):** turn on USB debugging, then run:
  ```
  adb shell settings put secure icon_blacklist battery
  ```
  To bring the real icon back, run `adb shell settings delete secure icon_blacklist`.

## Things to know
- The icon may hide when you pull down the notification shade, on the lock screen, or in full-screen videos and games. All overlay apps behave this way.
- If your phone kills background apps aggressively (Xiaomi, Oppo, Vivo, Huawei), set Cute Battery's battery usage to **Unrestricted** or **No restrictions**.
- **Privacy:** the app only *draws* the icon. It does not read your screen (`canRetrieveWindowContent="false"`) and has no internet permission.
- **Publishing:** this is fine to install on your own phone. Google Play restricts apps that use Accessibility for things that aren't accessibility features, so publishing there would need a policy declaration and may be rejected.

## Where to change things
| What | File |
|---|---|
| Shapes, face and colors | `app/src/main/java/com/cute/battery/CuteBatteryView.kt` (`buildShape`, `drawFace`, `levelColor`) |
| Default size and position | `OverlaySettings.kt` |
| Overlay placement | `BatteryOverlayService.kt` (`buildParams`) |
| Settings screen | `res/layout/activity_main.xml` and `MainActivity.kt` |
| App icon | `res/drawable/ic_launcher_foreground.xml` |

To add a new character: add a `STYLE_…` constant, add a branch in `buildShape()` (draw it inside a 1×1 square scaled by `s`), set its eye height in `faceY()`, and add a radio button.
