#!/usr/bin/env fish

if not command -q adb
    echo "adb not found, installing..."
    sudo apt install -y adb
end

# Clear old logs and capture everything from our app + crashes
adb logcat -c
adb logcat "ImageBlur:D" "AndroidRuntime:E" "*:S" 2>&1 | tee logcat_output.txt
