"""
To run this tests:

python3 -m venv .venv
.venv/bin/pip install -U pip
.venv/bin/pip install -U cookiecutter
.venv/bin/python -m unittest
"""

import pathlib
import unittest

from jinja2 import Environment, FileSystemLoader


def get_template():
    MANIFEST_DIR = pathlib.Path(__file__).parent.parent / "{{ cookiecutter.format }}" / "app" / "src" / "main"
    env = Environment(loader=FileSystemLoader(MANIFEST_DIR))
    env.filters['bool_attr'] = lambda arg: "true" if arg else "false"
    return env.get_template('AndroidManifest.xml')


def render(context):
    return get_template().render(cookiecutter=context)


class TestAndroidManifest(unittest.TestCase):
    def test_permissions_with_dicts(self):
        output = render({
            "permissions": {
                "android.permission.ACCESS_COARSE_LOCATION": {
                    "android:maxSdkVersion": "30"
                },
                "android.permission.INTERNET": {},
            },
            "features": {},
        })
        self.assertIn('android:maxSdkVersion="30"', output)

    def test_permissions_with_false(self):
        output = render({
            "permissions": {
                "android.permission.CAMERA": False,
                "android.permission.READ_CONTACTS": {},
            },
            "features": {},
        })
        self.assertNotIn('CAMERA', output)
