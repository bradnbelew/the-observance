#!/usr/bin/env python3
"""Build, stop, restart, and independently audit the integrated Unlit village candidate."""
from __future__ import annotations

import argparse
import json
from pathlib import Path
import shutil
import socket
import stat
import zipfile

from run_m3_disposable_paper import PAPER_EXPECTED_SHA256, PaperProcess, sha256, write_text


ROOT = Path(__file__).resolve().parents[1]
SURFACE_WORLD = "observance_unlit_surface_disposable"
UNLIT_WORLD = "observance_unlit_candidate_disposable"


def copy_bootstrap_cache(target: Path, source: Path | None) -> dict[str, str]:
    if source is None:
        return {}
    source = source.resolve()
    if not source.is_dir():
        raise FileNotFoundError(f"bootstrap cache is not a directory: {source}")
    files = sorted(path for path in source.rglob("*") if path.is_file())
    if not files:
        raise RuntimeError("bootstrap cache is empty")
    inventory = {path.relative_to(source).as_posix(): sha256(path) for path in files}
    destination = target / "cache"
    shutil.copytree(source, destination, copy_function=shutil.copyfile)
    for path in sorted(candidate for candidate in destination.rglob("*") if candidate.is_file()):
        path.chmod(path.stat().st_mode | stat.S_IWUSR)
    copied = {
        path.relative_to(destination).as_posix(): sha256(path)
        for path in sorted(candidate for candidate in destination.rglob("*") if candidate.is_file())
    }
    if copied != inventory:
        raise RuntimeError("bootstrap cache copy changed bytes")
    return inventory


def configure(target: Path, paper: Path, plugin: Path, port: int,
              bootstrap_cache: Path | None) -> dict[str, str]:
    if target.exists():
        raise FileExistsError(f"refusing to reuse disposable target: {target}")
    (target / "plugins" / "Observance").mkdir(parents=True)
    shutil.copy2(paper, target / "paper.jar")
    shutil.copy2(plugin, target / "plugins" / plugin.name)
    cache_inventory = copy_bootstrap_cache(target, bootstrap_cache)
    write_text(target / ".observance-disposable-unlit-candidate", "private-local-only\n")
    write_text(target / "eula.txt", "eula=true\n")
    write_text(target / "server.properties", "\n".join([
        "accepts-transfers=false", "allow-flight=true", "allow-nether=false", "difficulty=peaceful",
        "enable-command-block=false", "enable-rcon=false", "enable-status=true", "enforce-whitelist=false",
        "force-gamemode=true", "gamemode=adventure", "generate-structures=false", "hardcore=false",
        f"level-name={SURFACE_WORLD}", "level-seed=713071", "level-type=minecraft:flat",
        'generator-settings={"biome":"minecraft:plains","layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:stone","height":126},{"block":"minecraft:grass_block","height":1}]}',
        "max-players=2", "motd=PRIVATE UNLIT CANDIDATE AUDIT", "online-mode=false",
        "prevent-proxy-connections=true", "server-ip=127.0.0.1", f"server-port={port}",
        "spawn-animals=false", "spawn-monsters=false", "spawn-npcs=false", "spawn-protection=0",
        "sync-chunk-writes=true", "view-distance=6", "simulation-distance=4", "white-list=false", "",
    ]))
    config = (ROOT / "plugin/src/main/resources/config.yml").read_text(encoding="utf-8")
    replacements = {
        "  candidate-build-enabled: false": "  candidate-build-enabled: true",
        "    disposable-audit-enabled: false": "    disposable-audit-enabled: true",
        "  required: true\n  prompt:": "  required: false\n  prompt:",
        "  production-shutdown: true": "  production-shutdown: false",
        "drama:\n  enabled: true": "drama:\n  enabled: false",
    }
    for before, after in replacements.items():
        if before not in config:
            raise RuntimeError(f"offline config replacement anchor drift: {before!r}")
        config = config.replace(before, after, 1)
    write_text(target / "plugins/Observance/config.yml", config)
    return cache_inventory


def port_open(port: int) -> bool:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as probe:
        probe.settimeout(0.25)
        return probe.connect_ex(("127.0.0.1", port)) == 0


def package_worlds(target: Path) -> tuple[Path, str, dict[str, int]]:
    worlds = [target / SURFACE_WORLD, target / UNLIT_WORLD]
    if any(not world.is_dir() for world in worlds):
        raise FileNotFoundError("Paper did not create both integrated Unlit worlds")
    package = target / "unlit-candidate-worlds.zip"
    counts: dict[str, int] = {}
    with zipfile.ZipFile(package, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for world in worlds:
            count = 0
            for path in sorted(candidate for candidate in world.rglob("*") if candidate.is_file()):
                relative = path.relative_to(world).as_posix()
                if relative in {"session.lock", "uid.dat"}:
                    continue
                info = zipfile.ZipInfo(f"{world.name}/{relative}", (1980, 1, 1, 0, 0, 0))
                info.compress_type = zipfile.ZIP_DEFLATED
                info.external_attr = 0o100644 << 16
                archive.writestr(info, path.read_bytes())
                count += 1
            counts[world.name] = count
    return package, sha256(package), counts


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--paper-jar", type=Path, required=True)
    parser.add_argument("--plugin-jar", type=Path, required=True)
    parser.add_argument("--target", type=Path, required=True)
    parser.add_argument("--receipt", type=Path, required=True)
    parser.add_argument("--commit", required=True)
    parser.add_argument("--port", type=int, required=True)
    parser.add_argument("--java", default="java")
    parser.add_argument("--bootstrap-cache", type=Path)
    args = parser.parse_args()

    paper = args.paper_jar.resolve()
    plugin = args.plugin_jar.resolve()
    target = args.target.resolve()
    if sha256(paper) != PAPER_EXPECTED_SHA256:
        raise ValueError("Paper JAR does not match pinned 1.21.11 build 132")
    cache_inventory = configure(target, paper, plugin, args.port, args.bootstrap_cache)

    first = PaperProcess(target, args.java)
    try:
        first.wait_for("UNLIT_COPY_PROOF_READY", 300)
        first.wait_for("Done (", 300)
        build = first.command(
            f"observance unlit candidate build {UNLIT_WORLD} 0 72 0",
            "UNLIT_CANDIDATE_BUILD PASS", 900)
        ready = first.command("observance unlit candidate audit", "Gate: READY", 300)
        copy_install = first.command(
            f"obscopyproof install {SURFACE_WORLD} 20 130 20 {UNLIT_WORLD} 10 73 8",
            "UNLIT_COPY_PROOF_INSTALL PASS", 300)
        copy_exercise = first.command(
            "obscopyproof exercise-default", "UNLIT_COPY_PROOF_EXERCISE PASS", 120)
        copy_audit = first.command("obscopyproof audit", "UNLIT_COPY_PROOF_AUDIT PASS", 120)
        post_copy_ready = first.command("observance unlit candidate audit", "Gate: READY", 300)
        first.command("save-all flush", "Saved the game", 300)
    finally:
        try:
            first.stop()
        finally:
            write_text(target / "unlit-candidate-first.log", "\n".join(first.lines) + "\n")

    second = PaperProcess(target, args.java)
    try:
        second.wait_for("UNLIT_COPY_PROOF_READY", 300)
        second.wait_for("Done (", 300)
        restart_ready = second.command("observance unlit candidate audit", "Gate: READY", 300)
        restart_copy = second.command("obscopyproof audit", "UNLIT_COPY_PROOF_AUDIT PASS", 120)
        overwrite_refusal = second.command(
            f"observance unlit candidate build {UNLIT_WORLD} 0 72 0",
            "UNLIT_CANDIDATE_BUILD BLOCKED", 300)
        post_refusal_ready = second.command("observance unlit candidate audit", "Gate: READY", 300)
        second.command("save-all flush", "Saved the game", 300)
    finally:
        try:
            second.stop()
        finally:
            write_text(target / "unlit-candidate-restart.log", "\n".join(second.lines) + "\n")

    if port_open(args.port):
        raise RuntimeError("disposable Paper port remained open after graceful stop")
    data = target / "plugins" / "Observance"
    for required in (data / "sites.yml", data / "unlit-copy-proof.journal",
                     data / "unlit-copy-proof-layout.txt"):
        if not required.is_file():
            raise RuntimeError(f"integrated Unlit durable artifact is missing: {required.name}")
    sites_text = (data / "sites.yml").read_text(encoding="utf-8")
    required_sites = [
        "unlit_entry", "unlit_spawn_mirror", "unlit_exit", "unlit_house_lamp",
        "unlit_house_cairn", "unlit_house_coop", "unlit_house_well",
        "unlit_house_watch", "unlit_house_warm", "unlit_house_threshold", "unlit_house_base",
    ]
    missing_sites = [site for site in required_sites if f"  {site}:" not in sites_text]
    if missing_sites:
        raise RuntimeError(f"sites.yml lost required Unlit sites: {missing_sites}")
    package, package_hash, world_file_counts = package_worlds(target)
    receipt = {
        "schema_version": "1.0.0-unlit-candidate-disposable-paper-receipt",
        "source_commit": args.commit,
        "runner_sha256": sha256(Path(__file__)),
        "target": str(target),
        "address": f"127.0.0.1:{args.port}",
        "paper_version": "1.21.11 build 132",
        "paper_sha256": sha256(paper),
        "plugin_sha256": sha256(plugin),
        "bootstrap_cache": {
            "source": str(args.bootstrap_cache.resolve()) if args.bootstrap_cache else None,
            "files": cache_inventory,
        },
        "first_start": {
            "build": build, "ready": ready, "copy_install": copy_install,
            "copy_exercise": copy_exercise, "copy_audit": copy_audit,
            "post_copy_ready": post_copy_ready,
        },
        "restart": {
            "ready": restart_ready, "copy_audit": restart_copy,
            "overwrite_refusal": overwrite_refusal, "post_refusal_ready": post_refusal_ready,
        },
        "sites_sha256": sha256(data / "sites.yml"),
        "copy_journal_sha256": sha256(data / "unlit-copy-proof.journal"),
        "copy_layout_sha256": sha256(data / "unlit-copy-proof-layout.txt"),
        "first_log_sha256": sha256(target / "unlit-candidate-first.log"),
        "restart_log_sha256": sha256(target / "unlit-candidate-restart.log"),
        "world_package": str(package),
        "world_package_sha256": package_hash,
        "world_file_counts": world_file_counts,
        "proof": {
            "required_sites": 11,
            "house_workplaces": 7,
            "base_synthesis": 1,
            "distinct_structural_palettes": 8,
            "bounded_build_radius": 58,
            "border_diameter": 128,
            "fresh_install_passed": True,
            "exact_mechanics_audit_passed": True,
            "bounded_copy_integrated": True,
            "graceful_stop_passed": True,
            "restart_independent_audit_passed": True,
            "occupied_rebuild_refused": True,
            "port_listener_after_stop": 0,
        },
        "scope": "Automated physical village/mechanics/copy/restart proof only; player route, dread, investigation depth, and visual experience remain human-gated.",
        "production_mutated": False,
        "brad_approval": None,
    }
    args.receipt.parent.mkdir(parents=True, exist_ok=True)
    args.receipt.write_text(json.dumps(receipt, indent=2) + "\n", encoding="utf-8", newline="\n")
    print(json.dumps(receipt, indent=2))


if __name__ == "__main__":
    main()
