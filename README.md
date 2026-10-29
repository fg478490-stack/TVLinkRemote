# TV Link Remote (starter project)

A small native Android starter app exploring Bluetooth HID keyboard/mouse input for Android TV / Google TV.

## Current starter features
- Native Android/Kotlin project (min SDK 28 / Android 9)
- Requests Bluetooth permissions on Android 12+
- Attempts to register a Bluetooth HID Device profile
- Basic HID keyboard reports, simple text entry, and touchpad mouse reports
- Minimal dark interface

## Important limitations
- This is a **starter prototype**, not a verified production-ready app or APK.
- Android's `BluetoothHidDevice` profile availability depends on phone hardware, OS build, and vendor restrictions. Some phones do not expose it to third-party apps.
- TV pairing and mouse behavior vary by TV manufacturer and Android TV / Google TV build.
- The current text-entry demo supports a limited set of ASCII characters and should be expanded before daily use.
- Home/Back and media keys may need TV-specific mapping. A Bluetooth HID keyboard does not automatically provide every proprietary TV remote command.
- For a reliable first test, pair the phone from the TV's Bluetooth accessories menu after enabling HID and making the phone discoverable.
- Do not expect a standard app to silently pair or bypass the TV's pairing confirmation.

## Build
1. Install Android Studio on a computer.
2. Open this folder as an existing Gradle project.
3. Let Android Studio install the required Android SDK and sync Gradle.
4. Build > Build APK(s).
5. Install the debug APK on your Android phone and grant Nearby devices permissions.

A Gradle wrapper is not included in this starter archive; Android Studio can sync the project using its configured Gradle tooling.

## Next development milestones
1. Test whether the phone exposes `BluetoothHidDevice`.
2. Improve connection/pairing flow and state/error handling.
3. Add complete key mapping, special characters, media keys, keyboard visibility and repeat behavior.
4. Add a calibrated touchpad, scroll gestures and click controls.
5. Test on the exact TV model and multiple Android versions.


## Build an APK using only your phone (cloud build)

This project includes a GitHub Actions workflow at `.github/workflows/build.yml`.
You can use GitHub's website in your phone browser to upload this project to a repository and run the workflow; no local Android Studio installation is required.

High-level steps:
1. Sign in or create a GitHub account in your phone browser.
2. Create a new repository and upload the extracted project files, including the hidden `.github/workflows/build.yml` path. Uploading folders/hidden files from a phone browser can be awkward.
3. Open the repository's Actions tab and run **Build TV Link Remote APK** (or push to `main`).
4. When the run completes successfully, open that run and download the `TVLinkRemote-debug-apk` artifact.
5. Extract the downloaded artifact ZIP and tap `app-debug.apk` to install. If Android asks, allow installs from that browser or file manager.

Cloud build can fail if source or SDK compatibility issues appear; a successful build does not guarantee Bluetooth HID works with a particular phone or TV. This is a prototype, so test cautiously.
