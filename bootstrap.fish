#!/usr/bin/env fish
# Bootstrap script for R3BL Image Blur Android App development environment
# Run this once to set up all dependencies

echo "=== R3BL Image Blur Bootstrap ==="
echo ""

# 1. Install JDK 21 (not just JRE - needed for compilation)
echo "Installing OpenJDK 21..."
if not command -q javac
    sudo apt install -y openjdk-21-jdk
    echo "✓ JDK 21 installed"
else
    echo "✓ JDK already installed ($(javac -version 2>&1))"
end

# 2. Install Android SDK (API 35 for Android 15)
echo ""
echo "Setting up Android SDK..."
set -l ANDROID_HOME "$HOME/Android/Sdk"
set -l CMDLINE_TOOLS_URL "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"

if not test -d "$ANDROID_HOME/platforms/android-35"
    # Create SDK directory
    mkdir -p "$ANDROID_HOME/cmdline-tools"

    # Download command line tools if not present
    if not test -d "$ANDROID_HOME/cmdline-tools/latest"
        echo "Downloading Android command line tools..."
        set -l tmp_zip /tmp/cmdline-tools.zip
        curl -L -o $tmp_zip $CMDLINE_TOOLS_URL
        unzip -q $tmp_zip -d /tmp/cmdline-tools-tmp
        mv /tmp/cmdline-tools-tmp/cmdline-tools "$ANDROID_HOME/cmdline-tools/latest"
        rm -rf $tmp_zip /tmp/cmdline-tools-tmp
    end

    # Accept licenses and install SDK components
    echo "Installing Android SDK platform 35 (Android 15)..."
    yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --licenses >/dev/null 2>&1
    "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" "platforms;android-35" "build-tools;35.0.0" "platform-tools"
    echo "✓ Android SDK installed"
else
    echo "✓ Android SDK already installed"
end

# Set ANDROID_HOME for this session and create local.properties
set -gx ANDROID_HOME "$HOME/Android/Sdk"
set -gx PATH "$ANDROID_HOME/platform-tools" $PATH
echo "sdk.dir=$ANDROID_HOME" > local.properties

# 3. Install ADB for device communication and debugging
echo ""
echo "Installing ADB (via SDK platform-tools)..."
if test -f "$ANDROID_HOME/platform-tools/adb"
    echo "✓ ADB available at $ANDROID_HOME/platform-tools/adb"
else if command -q adb
    echo "✓ ADB already installed (system)"
else
    sudo apt install -y adb
    echo "✓ ADB installed via apt"
end

# 4. Generate/update Gradle wrapper
echo ""
echo "Setting up Gradle wrapper..."

# Check if we need to install gradle to bootstrap the wrapper
if not test -f gradlew
    echo "Installing Gradle to generate wrapper..."
    sudo apt install -y gradle
    gradle wrapper --gradle-version 8.13
    echo "✓ Gradle wrapper generated"
else
    echo "✓ Gradle wrapper already exists"
end

# Ensure wrapper uses correct Gradle version
set -l wrapper_props gradle/wrapper/gradle-wrapper.properties
if test -f $wrapper_props
    if not grep -q "gradle-8.13" $wrapper_props
        echo "Updating Gradle wrapper to 8.13..."
        sed -i 's/gradle-[0-9.]*-bin.zip/gradle-8.13-bin.zip/' $wrapper_props
        echo "✓ Wrapper updated to Gradle 8.13"
    else
        echo "✓ Gradle wrapper already at 8.13"
    end
end

# 5. Test the build
echo ""
echo "Testing build..."
./gradlew assembleDebug --quiet
if test $status -eq 0
    echo "✓ Build successful!"
    echo ""
    echo "APK location: app/build/outputs/apk/debug/app-debug.apk"
else
    echo "✗ Build failed. Check output above for errors."
    exit 1
end

echo ""
echo "=== Bootstrap Complete ==="
echo ""
echo "Useful commands:"
echo "  ./gradlew assembleDebug  - Build debug APK"
echo "  ./gradlew installDebug   - Build and install to device"
echo "  ./adb-log.fish           - View filtered logcat output"
