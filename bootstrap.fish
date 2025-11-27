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

# 2. Install ADB for device communication and debugging
echo ""
echo "Installing ADB..."
if not command -q adb
    sudo apt install -y adb
    echo "✓ ADB installed"
else
    echo "✓ ADB already installed"
end

# 3. Generate/update Gradle wrapper
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

# 4. Test the build
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
