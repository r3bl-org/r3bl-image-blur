#!/usr/bin/env fish
# Build script for R3BL Image Blur Android App
# Compiles the app and copies APK to releases/ folder

echo "=== Building R3BL Image Blur ==="
echo ""

# Build the debug APK
echo "Compiling..."
./gradlew assembleDebug --quiet

if test $status -ne 0
    echo "✗ Build failed"
    exit 1
end

# Copy to releases folder
echo "Copying APK to releases/..."
cp app/build/outputs/apk/debug/app-debug.apk releases/

echo ""
echo "✓ Build complete!"
echo "  APK: releases/app-debug.apk"
echo ""
echo "To commit and push:"
echo "  git add releases/app-debug.apk && git commit -m 'Update release APK' && git push"
