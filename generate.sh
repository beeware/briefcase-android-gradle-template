#!/bin/bash
# Used for developing this repo.
set -eou pipefail

mkdir -p tmp
git archive -0 --format zip HEAD > tmp/$USER-cookie-cutter-android.zip
cd tmp
cookiecutter --replay ./$USER-cookie-cutter-android.zip
diff -urN $USER-cookie-cutter-android gold-standard
