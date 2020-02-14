#!/bin/bash
# Used for developing this repo.

git archive -0 --format zip HEAD > tmp/$USER-cookie-cutter-android.zip
cd tmp
cookiecutter --replay ./$USER-cookie-cutter-android.zip
diff -uRN $USER-cookie-cutter-android gold-standard
