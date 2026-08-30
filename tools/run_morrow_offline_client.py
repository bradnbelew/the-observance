#!/usr/bin/env python3
"""Prepare or launch the installed vanilla 1.21.11 client against Morrow loopback only."""

from __future__ import annotations

import argparse
import hashlib
import ipaddress
import json
import os
import platform
import re
import struct
import subprocess
import sys
import time
import uuid
import zipfile
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_MINECRAFT = Path.home() / "AppData" / "Roaming" / ".minecraft"
VERSION = "1.21.11"
SOUND_CATEGORIES = (
    "master", "music", "record", "weather", "block", "hostile",
    "neutral", "player", "ambient", "voice", "ui",
)


def nbt_name(value: str) -> bytes:
    encoded = value.encode("utf-8")
    return struct.pack(">H", len(encoded)) + encoded


def nbt_string(name: str, value: str) -> bytes:
    encoded = value.encode("utf-8")
    return b"\x08" + nbt_name(name) + struct.pack(">H", len(encoded)) + encoded


def nbt_byte(name: str, value: int) -> bytes:
    return b"\x01" + nbt_name(name) + struct.pack(">b", value)


def write_server_list(path: Path, server: str, policy: str) -> None:
    compound = b"".join([
        nbt_string("name", "Morrow loopback rehearsal"),
        nbt_string("ip", server),
        nbt_byte("hidden", 0),
        b"" if policy == "prompt" else nbt_byte("acceptTextures", 1 if policy == "enabled" else 0),
        b"\x00",
    ])
    root = b"".join([
        b"\x0a\x00\x00",
        b"\x09" + nbt_name("servers") + b"\x0a" + struct.pack(">i", 1),
        compound,
        b"\x00",
    ])
    path.write_bytes(root)


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def sha1(path: Path) -> str:
    return hashlib.sha1(path.read_bytes()).hexdigest()  # noqa: S324 - Mojang manifest identity


def require(condition: bool, message: str) -> None:
    if not condition:
        raise RuntimeError(message)


def rule_matches(rule: dict[str, Any], features: dict[str, bool]) -> bool:
    os_rule = rule.get("os", {})
    if os_rule.get("name") not in (None, "windows"):
        return False
    arch = os_rule.get("arch")
    if arch == "x86" or (arch not in (None, "x86_64", "amd64")):
        return False
    version = os_rule.get("version")
    if version and re.search(version, platform.version()) is None:
        return False
    return all(features.get(name) == value for name, value in rule.get("features", {}).items())


def allowed(item: dict[str, Any], features: dict[str, bool]) -> bool:
    rules = item.get("rules")
    if not rules:
        return True
    result = False
    for rule in rules:
        if rule_matches(rule, features):
            result = rule["action"] == "allow"
    return result


def extract_natives(jars: list[Path], target: Path) -> dict[str, str]:
    inventory: dict[str, str] = {}
    for jar in jars:
        with zipfile.ZipFile(jar) as archive:
            for member in archive.infolist():
                if member.is_dir() or not member.filename.lower().endswith(".dll"):
                    continue
                name = Path(member.filename).name
                destination = target / name
                payload = archive.read(member)
                if destination.exists() and destination.read_bytes() != payload:
                    raise RuntimeError(f"native collision with different bytes: {name}")
                destination.write_bytes(payload)
                inventory[name] = hashlib.sha256(payload).hexdigest()
    return dict(sorted(inventory.items()))


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--run-id", required=True)
    parser.add_argument("--minecraft-root", type=Path, default=DEFAULT_MINECRAFT)
    parser.add_argument("--target-root", type=Path,
                        default=ROOT / "build" / "morrow-offline-client")
    parser.add_argument("--server", default="127.0.0.1:25589")
    parser.add_argument("--username", default="MorrowWitness")
    parser.add_argument(
        "--server-resource-pack-policy",
        choices=("prompt", "enabled", "disabled"),
        default="prompt",
        help="fresh servers.dat preference for this exact loopback server",
    )
    parser.add_argument("--javaw", type=Path,
                        default=Path(r"C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot\bin\javaw.exe"))
    parser.add_argument("--prepare-only", action="store_true")
    parser.add_argument(
        "--audio-disabled",
        action="store_true",
        help="start with every vanilla sound category at zero for accessibility parity rehearsal",
    )
    parser.add_argument("--wait-seconds", type=int, default=20)
    parser.add_argument("--terminate-after-wait", action="store_true")
    parser.add_argument(
        "--stop-when-requested",
        action="store_true",
        help="also stop when this run's create-only game directory receives stop.request",
    )
    parser.add_argument("--max-memory-mib", type=int, default=2048)
    args = parser.parse_args()

    if not re.fullmatch(r"[a-z0-9][a-z0-9-]{2,63}", args.run_id):
        raise ValueError("run-id must match [a-z0-9][a-z0-9-]{2,63}")
    if not re.fullmatch(r"[A-Za-z0-9_]{3,16}", args.username):
        raise ValueError("username must be a vanilla-safe 3-16 character local pseudonym")
    host = args.server.rsplit(":", 1)[0].strip("[]")
    require(ipaddress.ip_address(host).is_loopback, "offline client target must be loopback")
    require(0 <= args.wait_seconds <= 120, "wait-seconds must be between 0 and 120")
    require(768 <= args.max_memory_mib <= 2048,
            "max-memory-mib must be between 768 and 2048")
    require(not (args.prepare_only and args.terminate_after_wait),
            "prepare-only cannot terminate a process")
    require(not args.stop_when_requested or args.terminate_after_wait,
            "stop-when-requested requires terminate-after-wait")

    minecraft = args.minecraft_root.resolve()
    target = (args.target_root / args.run_id).resolve()
    require(not target.exists(), f"refusing to reuse offline client target: {target}")
    require(args.javaw.is_file(), f"Java 21 executable missing: {args.javaw}")
    version_root = minecraft / "versions" / VERSION
    manifest_path = version_root / f"{VERSION}.json"
    client_jar = version_root / f"{VERSION}.jar"
    require(manifest_path.is_file() and client_jar.is_file(), "vanilla 1.21.11 client is incomplete")
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    require(manifest["id"] == VERSION and manifest["mainClass"] == "net.minecraft.client.main.Main",
            "unexpected vanilla client manifest")

    target.mkdir(parents=True)
    game = target / "game"
    natives = target / "natives"
    game.mkdir()
    natives.mkdir()
    option_lines = [
        "autoJump:false",
        "fullscreen:false",
        "narrator:0",
        "onboardAccessibility:false",
    ]
    if args.audio_disabled:
        option_lines.extend(f"soundCategory_{category}:0.0" for category in SOUND_CATEGORIES)
    options_path = game / "options.txt"
    options_path.write_text("\n".join(option_lines) + "\n", encoding="utf-8")
    servers_path = game / "servers.dat"
    write_server_list(servers_path, args.server, args.server_resource_pack_policy)

    features = {
        "has_custom_resolution": True,
        "has_quick_plays_support": False,
        "is_demo_user": False,
        "is_quick_play_multiplayer": True,
        "is_quick_play_realms": False,
        "is_quick_play_singleplayer": False,
    }
    classpath: list[Path] = []
    native_jars: list[Path] = []
    for library in manifest["libraries"]:
        if not allowed(library, features):
            continue
        artifact = library.get("downloads", {}).get("artifact")
        if not artifact:
            continue
        path = minecraft / "libraries" / artifact["path"]
        require(path.is_file(), f"required local library missing: {artifact['path']}")
        require(path.stat().st_size == artifact["size"] and sha1(path) == artifact["sha1"],
                f"local library failed manifest hash: {artifact['path']}")
        classpath.append(path)
        if library["name"].endswith(":natives-windows"):
            native_jars.append(path)
    classpath.append(client_jar)
    native_inventory = extract_natives(native_jars, natives)
    require(native_inventory, "no Windows native libraries were extracted")

    assets = minecraft / "assets"
    asset_index = assets / "indexes" / f"{manifest['assetIndex']['id']}.json"
    require(asset_index.is_file(), "asset index 29 is missing")
    dummy_uuid = uuid.uuid5(uuid.NAMESPACE_DNS, f"morrow-rehearsal:{args.run_id}:{args.username}").hex
    jvm = [
        "-Xms512M", f"-Xmx{args.max_memory_mib}M",
        f"-Djava.library.path={natives}",
        f"-Djna.tmpdir={natives}",
        f"-Dorg.lwjgl.system.SharedLibraryExtractPath={natives}",
        f"-Dio.netty.native.workdir={natives}",
        "-Dminecraft.launcher.brand=morrow-rehearsal",
        "-Dminecraft.launcher.version=1",
        "-Dhttps.proxyHost=127.0.0.1", "-Dhttps.proxyPort=1",
        "-Dhttp.proxyHost=127.0.0.1", "-Dhttp.proxyPort=1",
        "-Dhttp.nonProxyHosts=localhost|127.*|[::1]",
        "-cp", os.pathsep.join(str(path) for path in classpath),
    ]
    game_args = [
        manifest["mainClass"],
        "--username", args.username,
        "--version", VERSION,
        "--gameDir", str(game),
        "--assetsDir", str(assets),
        "--assetIndex", manifest["assetIndex"]["id"],
        "--uuid", dummy_uuid,
        "--accessToken", "0",
        "--clientId", "0",
        "--xuid", "0",
        "--versionType", "morrow-rehearsal",
        "--width", "1280", "--height", "720",
        "--quickPlayMultiplayer", args.server,
    ]
    command = [str(args.javaw.resolve()), *jvm, *game_args]
    receipt = {
        "schema_version": "1.0.0-morrow-offline-client-launch",
        "status": "prepared" if args.prepare_only else "launching",
        "created_at": datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z"),
        "run_id": args.run_id,
        "source_commit": subprocess.run(
            ["git", "rev-parse", "HEAD"], cwd=ROOT, check=True, capture_output=True, text=True,
        ).stdout.strip(),
        "server": args.server,
        "server_resource_pack_policy": args.server_resource_pack_policy,
        "servers_dat_sha256": sha(servers_path),
        "loopback_only": True,
        "identity": {"kind": "dummy_offline", "username": args.username, "uuid": dummy_uuid},
        "account_files_read": False,
        "production_credentials_loaded": False,
        "outbound_proxy": "127.0.0.1:1",
        "max_memory_mib": args.max_memory_mib,
        "accessibility_profile": {
            "audio_disabled": args.audio_disabled,
            "sound_categories_zeroed": list(SOUND_CATEGORIES) if args.audio_disabled else [],
            "options_sha256": sha(options_path),
        },
        "manifest": {"path": str(manifest_path), "sha256": sha(manifest_path)},
        "client": {"path": str(client_jar), "sha256": sha(client_jar)},
        "asset_index": {"path": str(asset_index), "sha256": sha(asset_index)},
        "libraries": {"count": len(classpath) - 1,
                      "aggregate_sha256": hashlib.sha256("\n".join(sha(path) for path in classpath[:-1]).encode()).hexdigest()},
        "natives": {"count": len(native_inventory), "files": native_inventory},
        "game_directory": str(game),
        "control_stop_file": str(target / "stop.request") if args.stop_when_requested else None,
        "process_id": None,
    }
    receipt_path = target / "offline-client-receipt.json"
    receipt_path.write_text(json.dumps(receipt, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    if args.prepare_only:
        print(receipt_path)
        return 0

    log_path = target / "client.log"
    log = log_path.open("wb")
    process = subprocess.Popen(command, cwd=game, stdout=log, stderr=subprocess.STDOUT)
    receipt["process_id"] = process.pid
    receipt["status"] = "running"
    receipt["log"] = str(log_path)
    receipt_path.write_text(json.dumps(receipt, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    stopped_reason = None
    try:
        if args.stop_when_requested:
            deadline = time.monotonic() + args.wait_seconds
            stop_file = target / "stop.request"
            while process.poll() is None and time.monotonic() < deadline:
                if stop_file.is_file():
                    stopped_reason = "request"
                    break
                time.sleep(.25)
            if process.poll() is None and stopped_reason is None:
                stopped_reason = "wait"
            if process.poll() is None and stopped_reason is not None:
                raise subprocess.TimeoutExpired(command, args.wait_seconds)
            return_code = process.returncode
        else:
            return_code = process.wait(timeout=args.wait_seconds)
    except subprocess.TimeoutExpired:
        if args.terminate_after_wait:
            process.terminate()
            try:
                return_code = process.wait(timeout=15)
            except subprocess.TimeoutExpired:
                process.kill()
                return_code = process.wait(timeout=15)
            log.close()
            receipt["status"] = ("harness_stopped_after_request"
                                 if stopped_reason == "request"
                                 else "harness_stopped_after_wait")
            receipt["exit_code"] = return_code
            receipt["wait_seconds"] = args.wait_seconds
            receipt["stop_reason"] = stopped_reason or "wait"
            receipt["log_sha256"] = sha(log_path)
            receipt_path.write_text(json.dumps(receipt, indent=2, sort_keys=True) + "\n", encoding="utf-8")
            print(json.dumps({"exit_code": return_code, "receipt": str(receipt_path),
                              "status": receipt["status"]}))
            return 0
        print(json.dumps({"process_id": process.pid, "receipt": str(receipt_path), "status": "running"}))
        return 0
    finally:
        log.close()
    receipt["status"] = "exited"
    receipt["exit_code"] = return_code
    receipt["log_sha256"] = sha(log_path)
    receipt_path.write_text(json.dumps(receipt, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(json.dumps({"exit_code": return_code, "receipt": str(receipt_path), "status": "exited"}))
    return 1 if return_code else 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as failure:  # noqa: BLE001 - create-only launcher emits one concise failure
        print(f"MORROW OFFLINE CLIENT: FAIL: {failure}", file=sys.stderr)
        raise SystemExit(1)
