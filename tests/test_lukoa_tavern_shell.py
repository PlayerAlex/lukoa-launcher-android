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
OPS_SCRIPT = Path(__file__).resolve().parents[1] / "app/src/main/assets/lukoa-ops.sh"


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

    def test_chat_guard_config_roundtrip_and_history_clear(self):
        set_res = self.run_script("chat-guard-set", "1", "0", "512", "20")
        self.assertEqual(0, set_res.returncode, set_res.stdout + set_res.stderr)
        self.assertIn("chatGuard.enabled=1", set_res.stdout)
        self.assertIn("chatGuard.wakeLock=0", set_res.stdout)
        self.assertIn("chatGuard.minKb=512", set_res.stdout)
        self.assertIn("chatGuard.intervalSec=20", set_res.stdout)

        guard_dir = self.root / "state/profiles/main/chat-guard"
        guard_dir.mkdir(parents=True, exist_ok=True)
        rescued_file = self.tavern / "chat [露科亚救回 1123-100000].jsonl"
        rescued_file.write_text("{\"mes\":\"hello\"}\n", encoding="utf-8")
        (guard_dir / "rescued.tsv").write_text(
            f"2026-11-23T10:00:00Z\t{self.posix(self.tavern / 'chat.jsonl')}\t{self.posix(rescued_file)}\n",
            encoding="utf-8",
        )

        status_res = self.run_script("chat-guard-status")
        self.assertEqual(0, status_res.returncode, status_res.stdout + status_res.stderr)
        self.assertIn("chatGuard.rescueCount=1", status_res.stdout)
        self.assertIn("chatGuard.rescue=2026-11-23T10:00:00Z", status_res.stdout)

        clear_res = self.run_script("chat-guard-clear-history")
        self.assertEqual(0, clear_res.returncode, clear_res.stdout + clear_res.stderr)
        self.assertIn("chatGuard.rescueCount=0", clear_res.stdout)
        # Clearing history must never delete the rescued chat file itself
        self.assertTrue(rescued_file.is_file())

    def run_ops_script(self, *args):
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
            [self.shell, self.posix(OPS_SCRIPT), *map(str, args)],
            env=environment,
            capture_output=True,
            text=True,
            timeout=30,
        )

    def test_ops_storage_clean_preserves_chats_and_characters(self):
        chats = self.tavern / "data/default-user/chats/Alice"
        chars = self.tavern / "data/default-user/characters"
        thumbs = self.tavern / "data/default-user/thumbnails"
        leftover = self.tavern / ".lukoa-before-repair-123"
        state_profile = self.root / "state/profiles/main"
        for d in (chats, chars, thumbs, leftover, state_profile):
            d.mkdir(parents=True, exist_ok=True)
        chat_file = chats / "story.jsonl"
        char_file = chars / "alice.png"
        thumb_file = thumbs / "alice.jpg"
        old_log = state_profile / "tavern.log.old"
        chat_file.write_text("important chat\n", encoding="utf-8")
        char_file.write_text("important card\n", encoding="utf-8")
        thumb_file.write_text("x" * 4096, encoding="utf-8")
        (leftover / "junk.txt").write_text("y" * 4096, encoding="utf-8")
        old_log.write_text("old log\n", encoding="utf-8")

        scan_res = self.run_ops_script("storage-scan")
        self.assertEqual(0, scan_res.returncode, scan_res.stdout + scan_res.stderr)
        self.assertIn("storage.leftoverDirsCount=1", scan_res.stdout)

        clean_res = self.run_ops_script("storage-clean", "old_logs,thumbnails,leftover_dirs")
        self.assertEqual(0, clean_res.returncode, clean_res.stdout + clean_res.stderr)
        self.assertIn("storage.freedKb=", clean_res.stdout)
        self.assertFalse(old_log.exists())
        self.assertFalse(thumb_file.exists())
        self.assertFalse(leftover.exists())
        self.assertTrue(chat_file.is_file())
        self.assertTrue(char_file.is_file())

    def test_ops_quick_import_avoids_overwrite_and_rejects_outside_paths(self):
        imports_dir = self.home / "storage/downloads/LukoaLauncher/imports"
        imports_dir.mkdir(parents=True, exist_ok=True)
        chars_dir = self.tavern / "data/default-user/characters"
        chars_dir.mkdir(parents=True, exist_ok=True)
        existing = chars_dir / "hero.png"
        existing.write_text("original", encoding="utf-8")

        staged = imports_dir / "hero.png"
        staged.write_text("new-version", encoding="utf-8")
        res = self.run_ops_script("quick-import", "character", self.posix(staged), "default-user")
        self.assertEqual(0, res.returncode, res.stdout + res.stderr)
        self.assertEqual("original", existing.read_text(encoding="utf-8"))
        imported_copies = [p for p in chars_dir.glob("hero (导入 *).png")]
        self.assertEqual(1, len(imported_copies))
        self.assertEqual("new-version", imported_copies[0].read_text(encoding="utf-8"))
        self.assertFalse(staged.exists())

        outside = self.root / "evil.png"
        outside.write_text("evil", encoding="utf-8")
        bad = self.run_ops_script("quick-import", "character", self.posix(outside), "default-user")
        self.assertEqual(64, bad.returncode, bad.stdout + bad.stderr)


if __name__ == "__main__":
    unittest.main()
