## About this repo

_Status: alpha_

This repo contains a sample `cookiecutter` that demonstrates the progress
so far to create Android apps easily using BeeWare-related tools.

## How to use

Requirements:

- Make sure you have an Android device connected such that `adb` works,
  i.e., try `adb shell` and/or launch an emulator.

- Make sure you have `cookiecutter` already installed.

Run the following commands.

```
git clone https://github.com/paulproteus/cookiecutter-beeware-android.git
cd cookiecutter-beeware-android
export PYTHON_ANDROID_SUPPORT_ZIP=/path/to/3.7.zip
export ANDROID_SDK_ROOT=/path/to/android/sdk
./create_sample_app.sh
```

This will use `cookiecutter` to create, and build, a sample Python app.

If you don't know how to get 3.7.zip, then talk to Asheesh.
