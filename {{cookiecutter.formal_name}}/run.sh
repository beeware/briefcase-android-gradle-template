#!/bin/bash
# Strict mode for bash.
set -eou pipefail

APP="{{cookiecutter.package_name}}"
MAIN_ACTIVITY="org.beeware.android.MainActivity"

if [ -z "${ANDROID_SDK_ROOT:-}" ] ; then
    echo 'You must install the Android SDK and configure ANDROID_SDK_ROOT to point to it. Aborting.'
    exit 1
fi

if [ ! -d "$ANDROID_SDK_ROOT" ] ; then
    echo "ANDROID_SDK_ROOT=$ANDROID_SDK_ROOT is not a directory. Aborting."
    exit 1
fi

# Add things like adb to the path for this script, but not children.
PATH="$PATH:$ANDROID_SDK_ROOT/platform-tools"

# Tell bash to kill the logcat when this script exits.
trap "trap - SIGTERM && kill -- -$$" SIGINT SIGTERM EXIT

bash gradlew installDebug --stacktrace

# Clear the the log, then watch the `stdio` log, and `S`ilence everything else.
adb shell logcat -c &
adb shell logcat "stdio:*" "*:S" &

# Stop the app, then launch it.
adb shell am force-stop "$APP" || true
adb shell am start "${APP}/${MAIN_ACTIVITY}" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER || true

# Wait infinitely for logcat.
tail -f /dev/null
