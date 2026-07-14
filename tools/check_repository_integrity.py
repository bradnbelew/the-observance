#!/usr/bin/env python3
"""Read and validate every project-owned file in the checkout.

This is deliberately broader than the story/runtime checks. It catches the quiet release
failures that otherwise survive compilation: unreadable UTF-8, mojibake, conflict markers,
broken relative Markdown links, malformed JSON/TOML/CSV/XML, damaged archives, path traversal
inside zips, duplicate case-insensitive paths, and accidentally committed credential shapes.

Ignored dependency/build trees are not project-owned and are excluded. Frozen release artifacts
are added explicitly even when ignored by Git.
"""

from __future__ import annotations

import csv
import hashlib
import io
import json
import re
import subprocess
import sys
import tomllib
import urllib.parse
import zipfile
from pathlib import Path
from xml.etree import ElementTree


ROOT = Path(__file__).resolve().parents[1]

TEXT_EXTENSIONS = {
    ".bat", ".css", ".gradle", ".html", ".java", ".js", ".json", ".md",
    ".mcfunction", ".mjs", ".properties", ".ps1", ".py", ".sh", ".sql",
    ".svg", ".toml", ".ts", ".tsx", ".txt", ".yml", ".yaml",
}
TEXT_NAMES = {".gitignore", "gradlew", "LICENSE"}
JSON_EXTENSIONS = {".json", ".mcmeta"}
ARCHIVE_EXTENSIONS = {".jar", ".zip"}
MOJIBAKE = (
    chr(0xE2) + chr(0x20AC),
    chr(0xE2) + chr(0x2020),
    chr(0xC3) + chr(0x0192),
    chr(0xC3) + chr(0xA2),
    chr(0xC2) + chr(0xA7),
    chr(0xC2) + chr(0xB7),
    chr(0xEF) + chr(0xBB) + chr(0xBF),
    chr(0xFFFD),
)
CONFLICT_MARKERS = re.compile(r"(?m)^(?:<<<<<<< |=======\s*$|>>>>>>> )")
MARKDOWN_LINK = re.compile(r"(?<!!)\[[^\]]*\]\(([^)]+)\)")
DISCORD_TOKEN = re.compile(r"(?<![A-Za-z0-9_-])[A-Za-z0-9_-]{24}\.[A-Za-z0-9_-]{6}\.[A-Za-z0-9_-]{25,}(?![A-Za-z0-9_-])")
JWT_SECRET = re.compile(r"(?<![A-Za-z0-9_-])eyJ[A-Za-z0-9_-]{40,}\.[A-Za-z0-9_-]{20,}\.[A-Za-z0-9_-]{20,}(?![A-Za-z0-9_-])")


def git_paths() -> list[Path]:
    raw = subprocess.check_output(
        ["git", "ls-files", "-co", "--exclude-standard", "-z"], cwd=ROOT
    )
    # ``git ls-files -c`` also reports tracked paths that are intentionally deleted in
    # the working tree.  They have no checkout bytes to validate and treating them as
    # malformed files makes an otherwise valid rewrite fail until it is committed.
    # Keep broken symlinks so the dedicated check below can still diagnose them.
    candidates = [ROOT / item.decode("utf-8") for item in raw.split(b"\0") if item]
    paths = [path for path in candidates if path.exists() or path.is_symlink()]
    release = [
        ROOT / "observance-datapack.zip",
        ROOT / "observance-resourcepack.zip",
        ROOT / "observance-deploy-manifest.json",
    ]
    release.extend((ROOT / "plugin" / "build" / "libs").glob("observance-*.jar"))
    for path in release:
        if path.exists() and path not in paths:
            paths.append(path)
    return sorted(paths, key=lambda p: p.relative_to(ROOT).as_posix().lower())


def is_text(path: Path) -> bool:
    return path.suffix.lower() in TEXT_EXTENSIONS or path.name in TEXT_NAMES or path.suffix.lower() == ".mcmeta"


def validate_markdown_links(path: Path, text: str, failures: list[str]) -> None:
    for raw_target in MARKDOWN_LINK.findall(text):
        target = raw_target.strip().strip("<>")
        if target.startswith(("http:", "https:", "mailto:", "app:", "#")):
            continue
        target = urllib.parse.unquote(target.split("#", 1)[0].split("?", 1)[0])
        if not target:
            continue
        resolved = (path.parent / target.replace("\\", "/")).resolve()
        if not resolved.exists():
            failures.append(f"{rel(path)}: broken Markdown link -> {target}")


def validate_csv(path: Path, text: str, failures: list[str]) -> None:
    try:
        rows = list(csv.reader(io.StringIO(text)))
    except csv.Error as exc:
        failures.append(f"{rel(path)}: malformed CSV: {exc}")
        return
    if not rows:
        failures.append(f"{rel(path)}: empty CSV")
        return
    width = len(rows[0])
    for index, row in enumerate(rows[1:], start=2):
        if len(row) != width:
            failures.append(f"{rel(path)}:{index}: CSV width {len(row)} != header width {width}")


def validate_archive(path: Path, failures: list[str]) -> None:
    try:
        with zipfile.ZipFile(path) as archive:
            names = archive.namelist()
            if not names:
                failures.append(f"{rel(path)}: archive is empty")
            folded: set[str] = set()
            for name in names:
                normalized = name.replace("\\", "/")
                if normalized.startswith("/") or ".." in Path(normalized).parts:
                    failures.append(f"{rel(path)}: unsafe archive member {name}")
                key = normalized.casefold()
                if key in folded:
                    failures.append(f"{rel(path)}: duplicate case-insensitive archive member {name}")
                folded.add(key)
            corrupt = archive.testzip()
            if corrupt:
                failures.append(f"{rel(path)}: corrupt archive member {corrupt}")
    except (OSError, zipfile.BadZipFile) as exc:
        failures.append(f"{rel(path)}: unreadable archive: {exc}")


def validate_signature(path: Path, data: bytes, failures: list[str]) -> None:
    ext = path.suffix.lower()
    expected = {
        ".png": b"\x89PNG\r\n\x1a\n",
        ".ogg": b"OggS",
        ".ttf": b"\x00\x01\x00\x00",
    }.get(ext)
    if expected is not None and not data.startswith(expected):
        failures.append(f"{rel(path)}: invalid {ext} file signature")


def rel(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def main() -> int:
    failures: list[str] = []
    paths = git_paths()
    folded_paths: dict[str, str] = {}
    bytes_read = 0
    text_count = 0
    binary_count = 0

    for path in paths:
        relative = rel(path)
        folded = relative.casefold()
        if folded in folded_paths and folded_paths[folded] != relative:
            failures.append(f"case-insensitive path collision: {folded_paths[folded]} / {relative}")
        folded_paths[folded] = relative

        if path.is_symlink() and not path.exists():
            failures.append(f"{relative}: broken symlink")
            continue
        if not path.is_file():
            failures.append(f"{relative}: listed project path is not a file")
            continue
        try:
            data = path.read_bytes()
        except OSError as exc:
            failures.append(f"{relative}: unreadable: {exc}")
            continue
        bytes_read += len(data)
        if not data:
            failures.append(f"{relative}: empty project file")
            continue

        if path.suffix.lower() in ARCHIVE_EXTENSIONS:
            binary_count += 1
            validate_archive(path, failures)
            continue

        validate_signature(path, data, failures)
        if not is_text(path):
            binary_count += 1
            continue

        text_count += 1
        try:
            text = data.decode("utf-8-sig")
        except UnicodeDecodeError as exc:
            failures.append(f"{relative}: not valid UTF-8: {exc}")
            continue
        if "\x00" in text:
            failures.append(f"{relative}: NUL byte in text file")
        if CONFLICT_MARKERS.search(text):
            failures.append(f"{relative}: unresolved merge-conflict marker")
        found_mojibake = sorted(token for token in MOJIBAKE if token in text)
        if found_mojibake:
            failures.append(f"{relative}: mojibake sequence(s) {found_mojibake}")
        if DISCORD_TOKEN.search(text) or JWT_SECRET.search(text):
            failures.append(f"{relative}: committed credential-shaped secret")

        suffix = path.suffix.lower()
        try:
            if suffix in JSON_EXTENSIONS:
                json.loads(text)
            elif suffix == ".toml":
                tomllib.loads(text)
            elif suffix == ".csv":
                validate_csv(path, text, failures)
            elif suffix == ".svg":
                ElementTree.fromstring(text)
        except (ValueError, tomllib.TOMLDecodeError, ElementTree.ParseError) as exc:
            failures.append(f"{relative}: structured-text parse failed: {exc}")

        if suffix == ".md":
            validate_markdown_links(path, text, failures)

    # Force a complete content read into an aggregate receipt. The digest is informational rather
    # than pinned: any legitimate project edit changes it, while the file count stays auditable.
    digest = hashlib.sha256()
    for path in paths:
        if path.is_file():
            digest.update(rel(path).encode("utf-8"))
            digest.update(b"\0")
            digest.update(path.read_bytes())

    if failures:
        print(f"repository integrity check: FAILED ({len(failures)} issue(s))")
        for failure in failures:
            print(f"  - {failure}")
        return 1

    print(
        "repository integrity check: OK - "
        f"{len(paths)} project-owned files read ({text_count} text, {binary_count} binary/archive; "
        f"{bytes_read} bytes; aggregate {digest.hexdigest()[:16]})"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
