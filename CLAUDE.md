# R3BL Image Blur Android App

## Project Overview

A minimal Android app that applies blur and darken effects to images shared via the Android share menu. No launcher icon - the app only appears in the share sheet.

## Build & Run

- Open in Android Studio and run on device
- Or use: `./gradlew installDebug` (requires Gradle wrapper)

## Debug Logging

Run `./adb-log.fish` to capture filtered logcat output. Logs are tagged with `ImageBlur`.

## Architecture

- `ShareActivity` - Receives share intents, handles permissions, enqueues work
- `ImageProcessWorker` - Background worker that processes images
- `ImageProcessor` - Pure functions for blur (stack blur algorithm) and darken effects
- `Permissions` - Runtime permission handling for notifications (Android 13+)
- `Config` - Tunable constants (blur radius, scale factor, darken alpha)

## Key Files

```
app/src/main/kotlin/com/r3bl/imageblur/
├── Config.kt           # Configuration constants
├── ImageProcessWorker.kt # WorkManager worker
├── ImageProcessor.kt   # Blur & darken algorithms
├── Permissions.kt      # Notification permission handling
└── ShareActivity.kt    # Entry point for share intents
```

## Building a Release APK

1. In Android Studio: Build → Build Bundle(s) / APK(s) → Build APK(s)
2. Copy the APK to `releases/` folder in the repo
3. Commit and push

The `releases/` folder is tracked in git for sideloading to other devices.

## Commit Guidelines

Do not include the following in commit messages:
```
🤖 Generated with [Claude Code](https://claude.com/claude-code)

Co-Authored-By: Claude <noreply@anthropic.com>
```
