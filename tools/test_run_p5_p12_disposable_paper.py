#!/usr/bin/env python3
"""Focused self-test for create-only, hash-preserving Paper bootstrap cache copies."""

from __future__ import annotations

import tempfile
from pathlib import Path

from run_p5_p12_disposable_paper import copy_bootstrap_cache, sha256


def main() -> None:
    with tempfile.TemporaryDirectory(prefix="observance-p5-p12-cache-") as directory:
        root = Path(directory)
        source = root / "source"
        target = root / "target"
        source.mkdir()
        target.mkdir()
        payload = source / "mojang_1.21.11.jar"
        payload.write_bytes(b"fixed offline bootstrap fixture\n")
        expected = {payload.name: sha256(payload)}
        assert copy_bootstrap_cache(target, source) == expected
        copied = target / "cache" / payload.name
        assert copied.read_bytes() == payload.read_bytes()
        try:
            copy_bootstrap_cache(target, source)
        except RuntimeError as exc:
            assert "already has a cache" in str(exc)
        else:
            raise AssertionError("cache reuse must fail closed")
    print("P5-P12 Paper bootstrap-cache self-test passed")


if __name__ == "__main__":
    main()
