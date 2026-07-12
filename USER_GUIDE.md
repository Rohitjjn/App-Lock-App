# User Guide Updates

## Notification Handling
To completely hide the background app-lock notification without killing the locking service:
1. Open Android **Settings** -> **Apps** -> **SafeLock**.
2. Tap on **Notifications**.
3. Turn off the **App Lock Detector** notification channel.
*(Note: On Android 13+, you can also just swipe the notification away because it is no longer forced as ongoing.)*

## Customizations
- **Clock Font**: Now uses an 'Elegant Serif' stylish premium font by default. You can change this in Settings.
- **Date Font**: Kept as the default system font for better readability.
- **Passcode UI**: The pin dot indicators are now perfectly round and slightly smaller.

## Auto Updates via GitHub Releases
The app now supports checking for updates directly via GitHub Releases.
1. When you push a new release on GitHub (with a tag like `v1.0.1` and an attached APK named `app-release.apk`), the app will automatically detect it.
2. Users will see an "Update Available" popup on the main screen.
3. Clicking **Update Now** downloads the APK and prompts the user to install the update seamlessly.

### Setting up the Auto Updater
1. In the source code, open `app/src/main/java/com/example/util/AutoUpdater.kt`.
2. Change `GITHUB_REPO = "YOUR_GITHUB_USERNAME/YOUR_REPO_NAME"` to your actual GitHub repository.
3. The GitHub Actions workflow (`.github/workflows/build-and-release.yml`) is already set up to build and attach the APK when you push a new tag.

