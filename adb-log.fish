#!/usr/bin/env fish
# Capture filtered logcat output for debugging
# Works on both Linux and macOS

# Detect OS
set -l os_type (uname -s)

if not command -q adb
    echo "adb not found, installing..."
    if test "$os_type" = "Darwin"
        if command -q brew
            brew install --cask android-platform-tools
        else
            echo "✗ Please install Homebrew first: https://brew.sh"
            exit 1
        end
    else
        sudo apt install -y adb
    end
end

# Clear old logs and capture everything from our app + crashes
adb logcat -c
adb logcat "ImageBlur:D" "AndroidRuntime:E" "*:S" 2>&1 | tee logcat_output.txt
