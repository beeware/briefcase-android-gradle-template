import tomllib
from pathlib import Path

import pytest
from cookiecutter.main import cookiecutter


@pytest.mark.parametrize("python_version", ["3.11.17", "3.12.15", "3.13.16"])
def test_preinstalled_requirements(tmp_path, python_version):
    """The rendered project accepts dependency directories prepared by Briefcase."""
    project = Path(
        cookiecutter(
            str(Path(__file__).resolve().parents[1]),
            no_input=True,
            default_config={"replay_dir": str(tmp_path / "replay")},
            output_dir=str(tmp_path),
            extra_context={
                "python_version": python_version,
                "permissions": {},
                "features": {},
            },
        )
    )
    config = tomllib.loads((project / "briefcase.toml").read_text(encoding="utf-8"))
    assert config["paths"]["app_packages_path"] == "app/app_packages"
    assert "app_requirements_path" not in config["paths"]
    assert "app_requirement_installer_args_path" not in config["paths"]
    gradle = (project / "app/build.gradle").read_text(encoding="utf-8")
    assert 'install "-r", "requirements.txt"' not in gradle
    assert 'outputDir.set(layout.projectDirectory.dir("app_packages"))' in gradle
    assert "enabled = false" in gradle
    assert "afterEvaluate {" in gradle
    assert 'abiFilters "arm64-v8a", "x86_64"' in gradle
    assert "armeabi-v7a" not in gradle
