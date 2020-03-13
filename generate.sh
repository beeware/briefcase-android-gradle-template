#!/bin/bash
# Used for developing this repo.
set -eou pipefail

mkdir -p tmp
git archive -0 --format zip --prefix=toplevel/ HEAD > tmp/$USER-cookie-cutter-android.zip
cd tmp
rm -rf 'App Name'
cookiecutter --no-input $USER-cookie-cutter-android.zip
diff -urN 'App Name' gold-standard
