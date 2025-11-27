# R3BL Image Blur

A minimal Android app that blurs and darkens images for use as wallpaper backgrounds. No
launcher icon — the app only appears in the system share menu.

## What It Does

1. Share any image from your gallery or file manager
2. Select "R3BL Image Blur" from the share menu
3. The app processes the image in the background (blur + darken)
4. A notification appears when complete — tap to view the result
5. Find the processed image in your Downloads folder with `_blur` suffix

The blur effect creates a "frosted glass" look that obscures details while preserving
colors — perfect for wallpaper backgrounds that won't distract from icons and widgets.

## Requirements

- Android 15+ (API 35)
- Tested on Pixel 10

## Installation

Download the APK from `releases/app-debug.apk` and sideload it to your device.

## Scripts

| Script           | Description                                                                                |
| ---------------- | ------------------------------------------------------------------------------------------ |
| `bootstrap.fish` | First-time setup. Installs JDK 21, ADB, and Gradle wrapper, then runs a test build.        |
| `build.fish`     | Compiles the app and copies the APK to `releases/` for distribution.                       |
| `adb-log.fish`   | Captures filtered logcat output for debugging. Shows only `ImageBlur` logs and crash info. |

## Building From Source

```bash
# First time setup
./bootstrap.fish

# Build APK
./build.fish

# Or use gradle directly
./gradlew assembleDebug
```

## Configuration

Blur settings can be adjusted in `app/src/main/kotlin/com/r3bl/imageblur/Config.kt`:

```kotlin
object Config {
    const val BLUR_RADIUS = 20f      // 0-25, higher = more blur
    const val SCALE_FACTOR = 0.2f    // downscale before blur for performance
    const val DARKEN_ALPHA = 0.18f   // 0-1, higher = darker
}
```

## License

Copyright (c) 2025 R3BL LLC. Licensed under the MIT License.
