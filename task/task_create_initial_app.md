# Image Blur App - Minimal Android Implementation Plan

## Goal
Create the simplest possible Android app that blurs and darkens images via the system share menu, with no visible UI.

Name this app "R3BL Image Blur".

Create a folder in "~/github/r3bl-image-blur-android-app" in which the code should be written.

## Recommended Architecture

**Transparent Activity + WorkManager (No Launcher Icon)**

```
Share Menu → Transparent Activity (invisible)
           → Extracts image URI
           → Enqueues WorkManager task
           → Finishes immediately (no UI shown)
           → Worker processes image in background
           → Saves to same folder as original
           → Notification shown when complete
```

### Why No Launcher Icon?
A launcher icon would require a main activity that shows something when tapped. The simplest option is:
- **No launcher** = app only appears in share menu
- To "uninstall" or manage: Settings → Apps → find it there

This is the minimal approach - no help screen to maintain, no UI code at all.

## File Structure (5 kotlin files + manifest + build files)

```
WallpaperBlur/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── res/
│   │   │   ├── values/strings.xml
│   │   │   └── drawable/ic_launcher.xml  (from /home/nazmul/Downloads/visual/r3bl-logo.svg)
│   │   └── kotlin/com/wallpaperblur/
│   │       ├── ShareActivity.kt          (~25 lines)
│   │       ├── ImageProcessWorker.kt     (~100 lines)
│   │       ├── ImageProcessor.kt         (~40 lines)
│   │       └── Config.kt                 (~15 lines) - tunable constants
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## Implementation Details

### 1. AndroidManifest.xml
- Register transparent ShareActivity for `ACTION_SEND` with `image/*` mime type
- Use `Theme.NoDisplay` for zero UI
- Single permission: `POST_NOTIFICATIONS`
- **No launcher activity** - app hidden from app drawer

### 2. ShareActivity.kt
- Receives share intent
- Extracts image URI and original filename
- Enqueues WorkManager job with URI + original path info
- Calls `finish()` immediately

### 3. ImageProcessWorker.kt
- Loads bitmap from URI
- Applies blur + darken using ImageProcessor
- **Saves to Downloads folder** with `_blur` suffix, preserving the original filename extension
- Posts completion notification with tap-to-open action

### 4. ImageProcessor.kt
- `blur()`: Medium-heavy frosted glass effect
- `darken()`: 30% black overlay for contrast

## Technical Choices

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Background processing | WorkManager | Survives app death, auto-retry |
| Blur algorithm | RenderEffect (Android 12+) | Modern API, no deprecated RenderScript |
| Blur strength | Radius 20, scale 0.2 | Medium-heavy frosted glass effect |
| Darken amount | 30% black overlay | Good for wallpaper contrast |
| Output location | Downloads folder | No permission issues, always accessible |
| Output format | PNG | Lossless quality |
| Filename | `{original}_blur.png` | Clear naming |

### 5. Config.kt
All tunable constants in one place:
```kotlin
object Config {
    const val BLUR_RADIUS = 20f      // 0-25, higher = more blur
    const val SCALE_FACTOR = 0.2f    // downscale before blur for performance
    const val DARKEN_ALPHA = 0.3f    // 0-1, higher = darker
}
```

## Dependencies

```kotlin
dependencies {
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.core:core-ktx:1.12.0")
}
```

## User Flow

1. Open any image in gallery/files
2. Tap Share → select "Wallpaper Blur"
3. Nothing visible happens (processing in background)
4. Notification appears: "Saved: photo_blur.png"
5. Tap notification to open the image, or find it in Downloads folder

## Permissions Summary

| Permission | Required | Notes |
|------------|----------|-------|
| POST_NOTIFICATIONS | Yes | Android 13+ for completion notification |

**No storage permissions needed!** Downloads folder is accessible without special permissions via MediaStore on Android 10+.

## Build Configuration

- **minSdk: 35 (Android 15 only)** - Pixel 10 and newer only, no backward compat
- targetSdk: 35 (Android 15)
- Kotlin + Gradle KTS

### Simplifications from Android 15-only target:
- **No RenderScript** - use modern `RenderEffect.createBlurEffect()` instead (added in Android 12)
- **No permission version checks** - just use latest APIs directly
- **Simpler MediaStore** - no legacy storage path fallbacks
- **Cleaner code** - no `Build.VERSION.SDK_INT` checks anywhere

## Blur Effect Settings

For the "frosted glass" effect you want:
- **Scale factor**: 0.2 (scale down to 20%, blur, scale back up)
- **Blur radius**: 20 (near max of 25)
- **Darken**: 30% black overlay

This creates a heavy blur that obscures details while preserving colors - perfect for wallpaper backgrounds.