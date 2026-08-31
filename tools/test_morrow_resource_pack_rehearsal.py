#!/usr/bin/env python3
"""Dependency-light contract tests for the Morrow pack decision harness."""

from __future__ import annotations

import tempfile
import urllib.request
from pathlib import Path

import run_morrow_offline_client as client
import run_morrow_resource_pack_rehearsal as rehearsal


class NbtReader:
    def __init__(self, payload: bytes) -> None:
        self.payload = payload
        self.offset = 0

    def take(self, count: int) -> bytes:
        value = self.payload[self.offset:self.offset + count]
        if len(value) != count:
            raise AssertionError("truncated NBT fixture")
        self.offset += count
        return value

    def byte(self) -> int:
        return int.from_bytes(self.take(1), "big", signed=True)

    def ushort(self) -> int:
        return int.from_bytes(self.take(2), "big")

    def integer(self) -> int:
        return int.from_bytes(self.take(4), "big", signed=True)

    def string(self) -> str:
        return self.take(self.ushort()).decode("utf-8")


def read_server(path: Path) -> dict[str, object]:
    reader = NbtReader(path.read_bytes())
    assert reader.byte() == 10 and reader.string() == ""
    assert reader.byte() == 9 and reader.string() == "servers"
    assert reader.byte() == 10 and reader.integer() == 1
    values: dict[str, object] = {}
    while True:
        tag = reader.byte()
        if tag == 0:
            break
        name = reader.string()
        if tag == 1:
            values[name] = reader.byte()
        elif tag == 8:
            values[name] = reader.string()
        else:
            raise AssertionError(f"unexpected tag in server fixture: {tag}")
    assert reader.byte() == 0 and reader.offset == len(reader.payload)
    return values


def test_server_policies(root: Path) -> None:
    for policy, expected in (("prompt", None), ("enabled", 1), ("disabled", 0)):
        path = root / f"servers-{policy}.dat"
        client.write_server_list(path, "127.0.0.1:25592", policy)
        assert path.read_bytes()[0] == 10, "servers.dat must remain uncompressed NBT"
        values = read_server(path)
        assert values["ip"] == "127.0.0.1:25592"
        assert values["hidden"] == 0
        assert values.get("acceptTextures") == expected


def test_windows_option_bytes(root: Path) -> None:
    path = root / "options.txt"
    lines = ["autoJump:false", "fullscreen:false", "narrator:0", "onboardAccessibility:false"]
    path.write_bytes(("\r\n".join(lines) + "\r\n").encode("utf-8"))
    assert rehearsal.paper_harness.sha256(path) == \
        "2c0ac1089e3faea54179243291bc99970a6ab7a819cc8d4464efa03eadb116a7"


def test_config_binding(root: Path) -> None:
    target = root / "target"
    config = target / "plugins" / "Observance" / "config.yml"
    config.parent.mkdir(parents=True)
    config.write_text("""resource-pack:
  url: "https://example.invalid/old.zip"
  sha1: "0000000000000000000000000000000000000000"
  required: true
  prompt: "old"
  delay-ticks: 20

handoff:
  discord-invite-url: "https://example.invalid"

morrow-reboot:
  enabled: true
  projector:
    initial-retry-ms: 100
    maximum-retry-ms: 500
  room04:
    build-enabled: true
""", encoding="utf-8")
    url = rehearsal.enable_pack(target, 29447, "a" * 40)
    text = config.read_text(encoding="utf-8")
    assert url == "http://127.0.0.1:29447/morrow-resourcepack.zip"
    assert 'sha1: "' + "a" * 40 + '"' in text
    assert "resource-pack:\n    enabled: true\n    required: false" in text
    assert "required: true" not in text


def test_pack_server(root: Path) -> None:
    pack = root / "fixture.zip"
    payload = b"PK\x03\x04morrow-pack-fixture"
    pack.write_bytes(payload)
    server, state = rehearsal.start_pack_server(0, pack)
    try:
        port = server.server_address[1]
        with urllib.request.urlopen(  # noqa: S310 - test is pinned to local server address
                f"http://127.0.0.1:{port}/morrow-resourcepack.zip", timeout=5) as response:
            assert response.status == 200 and response.read() == payload
        with state.lock:
            assert len(state.requests) == 1
            assert state.requests[0]["path"] == "/morrow-resourcepack.zip"
    finally:
        server.shutdown()
        server.server_close()


def test_post_status_hold_bounds() -> None:
    assert rehearsal.validate_post_status_hold(0) == 0
    assert rehearsal.validate_post_status_hold(90) == 90
    for invalid in (-1, 91):
        try:
            rehearsal.validate_post_status_hold(invalid)
        except RuntimeError:
            pass
        else:
            raise AssertionError(f"accepted invalid post-status hold: {invalid}")


def main() -> None:
    with tempfile.TemporaryDirectory(prefix="morrow-pack-selftest-") as temporary:
        root = Path(temporary)
        test_server_policies(root)
        test_windows_option_bytes(root)
        test_config_binding(root)
        test_pack_server(root)
        test_post_status_hold_bounds()
    print("MORROW RESOURCE PACK REHEARSAL SELFTEST: PASS policies=3 nbt=uncompressed "
          "loopback_get=1 hold-max=90")


if __name__ == "__main__":
    main()
