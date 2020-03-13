#!/bin/bash
set -eou pipefail

DEFAULT_FORMAL_NAME="App Name"
DEFAULT_MODULE_NAME="my_app"

# This script will use cookiecutter to create and launch a sample Python app.
# It is approximately a shell script version of briefcase. It's intended
# for developing or automatically testing this cookiecutter package.

function createWorkingDirectory() {
    rm -rf tmp
    mkdir -p tmp
    git archive -0 --format zip --prefix=toplevel/ HEAD > tmp/"$USER"-cookie-cutter-android.zip
    cd tmp
    rm -rf ./"$DEFAULT_FORMAL_NAME"
}

function createApp() {
    cookiecutter --no-input "$USER"-cookie-cutter-android.zip
    cd "$DEFAULT_FORMAL_NAME"
}

function addPython() {
    unzip "${PYTHON_ANDROID_SUPPORT_ZIP}"
}

function addPythonCode() {
    app_dir="$PWD/app/src/main/assets/python/$DEFAULT_MODULE_NAME"
    cat > "$app_dir/__init__.py" <<EOF
from rubicon.java import JavaClass, JavaInterface

IPythonApp = JavaInterface('org/beeware/android/IPythonApp')

class Application(IPythonApp):
    def onCreate(self):
        print('called Python onCreate()')

    def onStart(self):
        print('called Python onStart()')

    def onResume(self):
        print('called Python onResume()')
EOF
    cat > "$app_dir/__main__.py" <<EOF
from . import Application
from rubicon.java import JavaClass

activity_class = JavaClass('org/beeware/android/MainActivity')
app = Application()
activity_class.setPythonApp(app)
print('Python app launched & stored in Android Activity class')
EOF
}

function launch() {
    bash run.sh
}

createWorkingDirectory
createApp
addPython
addPythonCode
launch
