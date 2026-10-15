"""Isolated behavior checks for the bundled Termux shell script."""

import io
import json
import os
from pathlib import Path
import shutil
import subprocess
import tarfile
import tempfile
import unittest


SCRIPT = Path(__file__).resolve().parents[1] / "app/src/main/assets/lukoa-tavern.sh"


def shell_tools():
    shell = shutil.which("sh")
    cygpath = shutil.which("cygpath")
    if shell:
        return shell, cygpath
    git = shutil.which("git")
    if git and os.name == "nt":
        git_root = Path(git).resolve().parent.parent
        shell = git_root / "usr/bin/sh.exe"
        cygpath = git_root / "usr/bin/cygpath.exe"
        if shell.is_file() and cygpath.is_file():
            return str(shell), str(cygpath)
    raise unittest.SkipTest("A POSIX sh is required for the bundled script checks")


class TavernShellSafetyTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.shell, cls.cygpath = shell_tools()

    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="lukoa-shell-test-")
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.home = self.root / "home"
        self.tavern = self.home / "SillyTavern"
        self.library = self.home / "storage/downloads/LukoaLauncher/backups/sd"
        self.tavern.mkdir(parents=True)
        self.library.mkdir(parents=True)
        (self.tavern / "original.txt").write_text("keep", encoding="utf-8")

    def posix(self, path):
        if self.cygpath:
            return subprocess.check_output(
                [self.cygpath, "-u", str(path)], text=True
            ).strip()
        return str(path)

    def run_script(self, *args):
        environment = os.environ.copy()
        shell_path = self.posix(Path(self.shell).parent)
        node = shutil.which("node")
        node_path = self.posix(Path(node).parent) if node else ""
        environment["PATH"] = ":".join(
            part for part in (shell_path, node_path, "/usr/bin", "/bin") if part
        )
        environment.update(
            HOME=self.posix(self.home),
            LUKOA_HOME=self.posix(self.home),
            LUKOA_STATE_DIR=self.posix(self.root / "state"),
            LUKOA_TAVERN_DIR=self.posix(self.tavern),
            LUKOA_CONFIG_FILE=self.posix(self.root / "absent-config.env"),
            TAVERN_PORT="65534",
        )
        return subprocess.run(
            [self.shell, self.posix(SCRIPT), *map(str, args)],
            env=environment,
            capture_output=True,
            text=True,
            timeout=30,
        )

    def archive(self, name, external_root="none", symlink_root=False, restored_data_root=None):
        archive_path = self.library / name
        manifest = (
            "schema=lukoa-sillytavern-backup-v2\n"
            f"source={self.posix(self.tavern)}\n"
            f"externalDataRoot={external_root}\n"
        )
        with tarfile.open(archive_path, "w:gz") as tar:
            self.add_text(tar, "LUKOA_BACKUP_MANIFEST.txt", manifest)
            if symlink_root:
                link = tarfile.TarInfo("SillyTavern")
                link.type = tarfile.SYMTYPE
                link.linkname = self.posix(self.tavern)
                tar.addfile(link)
            else:
                self.add_directory(tar, "SillyTavern")
                self.add_text(tar, "SillyTavern/restored.txt", "restored")
                self.add_text(
                    tar,
                    "SillyTavern/config.yaml",
                    f"dataRoot: {restored_data_root or external_root}\n"
                    if external_root != "none" or restored_data_root
                    else "dataRoot: data\n",
                )
            if external_root != "none":
                external_base = external_root.rsplit("/", 1)[-1]
                self.add_directory(tar, external_base)
                self.add_text(tar, f"{external_base}/restored-data.txt", "restored")
        return self.posix(archive_path)

    @staticmethod
    def add_directory(tar, name):
        directory = tarfile.TarInfo(name + "/")
        directory.type = tarfile.DIRTYPE
        tar.addfile(directory)

    @staticmethod
    def add_text(tar, name, text):
        payload = text.encode("utf-8")
        entry = tarfile.TarInfo(name)
        entry.size = len(payload)
        tar.addfile(entry, io.BytesIO(payload))

    def test_restore_rejects_external_root_that_would_replace_home(self):
        safe_data = self.root / "safe-data"
        safe_data.mkdir()
        (self.tavern / "config.yaml").write_text(
            f"dataRoot: {self.posix(safe_data)}\n", encoding="utf-8"
        )
        archive = self.archive("hostile-root.tar.gz", self.posix(self.home))

        result = self.run_script("restore", archive, "full")

        self.assertNotEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertEqual("keep", (self.tavern / "original.txt").read_text())
        self.assertTrue(self.library.is_dir())

    def test_restore_rejects_linked_tavern_root_before_moving_original(self):
        archive = self.archive("linked-root.tar.gz", symlink_root=True)

        result = self.run_script("restore", archive, "full")

        self.assertNotEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertEqual("keep", (self.tavern / "original.txt").read_text())
        self.assertIn("unsafe archive entry type", result.stdout)

    def test_restore_rejects_archive_file_symlink(self):
        outside = self.root / "outside.tar.gz"
        library_archive = self.library / "linked-archive.tar.gz"
        self.archive("source.tar.gz")
        (self.library / "source.tar.gz").replace(outside)
        try:
            library_archive.symlink_to(outside)
        except (OSError, NotImplementedError) as error:
            self.skipTest(f"File symlinks are unavailable: {error}")

        result = self.run_script("restore", self.posix(library_archive), "full")

        self.assertEqual(73, result.returncode, result.stdout + result.stderr)
        self.assertEqual("keep", (self.tavern / "original.txt").read_text())
        self.assertIn("Backup archive links cannot be applied", result.stdout)

    def test_valid_external_restore_preserves_previous_directories(self):
        data = self.root / "tavern-data"
        data.mkdir()
        (data / "original-data.txt").write_text("keep", encoding="utf-8")
        (self.tavern / "config.yaml").write_text(
            f"dataRoot: {self.posix(data)}\n", encoding="utf-8"
        )
        archive = self.archive("valid-external.tar.gz", self.posix(data))

        result = self.run_script("restore", archive, "full")

        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertEqual("restored", (self.tavern / "restored.txt").read_text())
        self.assertEqual("restored", (data / "restored-data.txt").read_text())
        self.assertIn("previousDirectory=", result.stdout)
        previous_data = next(self.root.glob("tavern-data.lukoa-before-restore-*"))
        self.assertEqual("keep", (previous_data / "original-data.txt").read_text())

    def test_restore_rejects_archive_whose_config_points_elsewhere(self):
        data = self.root / "tavern-data"
        data.mkdir()
        (self.tavern / "config.yaml").write_text(
            f"dataRoot: {self.posix(data)}\n", encoding="utf-8"
        )
        archive = self.archive(
            "mismatched-config.tar.gz",
            self.posix(data),
            restored_data_root=self.posix(self.root / "other-data"),
        )

        result = self.run_script("restore", archive, "full")

        self.assertNotEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertEqual("keep", (self.tavern / "original.txt").read_text())
        self.assertIn("Restored configuration does not match", result.stdout)

    def test_console_returns_start_script_failure(self):
        if not shutil.which("node"):
            self.skipTest("node is required for the foreground preflight")
        (self.tavern / "start.sh").write_text("exit 23\n", encoding="utf-8")

        result = self.run_script("console")

        self.assertEqual(23, result.returncode, result.stdout + result.stderr)
        status = json.loads(
            (self.root / "state/profiles/main/status.json").read_text(encoding="utf-8")
        )
        self.assertEqual("error", status["status"])
        self.assertEqual(23, status["exitCode"])


if __name__ == "__main__":
    unittest.main()
