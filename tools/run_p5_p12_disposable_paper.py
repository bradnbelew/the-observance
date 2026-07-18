#!/usr/bin/env python3
"""Build, audit, restart, and re-audit the complete Deep Hold on a fresh local Paper target."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import shutil
import zipfile

from run_m3_disposable_paper import PAPER_EXPECTED_SHA256, PaperProcess, sha256, write_text


ROOT = Path(__file__).resolve().parents[1]
WORLD = "observance_campaign_disposable"


def configure(target: Path, paper: Path, plugin: Path, port: int) -> None:
    if target.exists():
        raise FileExistsError(f"refusing to reuse disposable target: {target}")
    (target / "plugins" / "Observance").mkdir(parents=True)
    shutil.copy2(paper, target / "paper.jar")
    shutil.copy2(plugin, target / "plugins" / plugin.name)
    write_text(target / ".observance-disposable-whole-campaign", "private-local-only\n")
    write_text(target / "eula.txt", "eula=true\n")
    write_text(target / "server.properties", "\n".join([
        "accepts-transfers=false", "allow-flight=true", "allow-nether=false", "difficulty=peaceful",
        "enable-command-block=false", "enable-rcon=false", "enable-status=true", "enforce-whitelist=false",
        "force-gamemode=true", "gamemode=adventure", "generate-structures=false", "hardcore=false",
        f"level-name={WORLD}", "level-seed=9137", "level-type=minecraft:flat",
        'generator-settings={"biome":"minecraft:plains","layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:stone","height":262},{"block":"minecraft:grass_block","height":1}]}',
        "max-players=2",
        "motd=PRIVATE WHOLE CAMPAIGN DISPOSABLE AUDIT", "online-mode=false",
        "prevent-proxy-connections=true", "server-ip=127.0.0.1", f"server-port={port}",
        "spawn-animals=false", "spawn-monsters=false", "spawn-npcs=false", "spawn-protection=0",
        "sync-chunk-writes=true", "view-distance=5", "simulation-distance=4", "white-list=false", "",
    ]))
    config = (ROOT / "plugin/src/main/resources/config.yml").read_text(encoding="utf-8")
    replacements = {
        "unlit:\n  enabled: true\n": "unlit:\n  enabled: false\n",
        "  required: true\n  prompt:": "  required: false\n  prompt:",
        "  production-shutdown: true": "  production-shutdown: false",
        "drama:\n  enabled: true": "drama:\n  enabled: false",
    }
    for before, after in replacements.items():
        if before not in config:
            raise RuntimeError(f"offline config replacement anchor drift: {before!r}")
        config = config.replace(before, after, 1)
    write_text(target / "plugins/Observance/config.yml", config)


def package_world(target: Path) -> tuple[Path, str]:
    world = target / WORLD
    if not world.is_dir():
        raise FileNotFoundError(f"Paper did not create {world}")
    package = target / "whole-campaign-world.zip"
    with zipfile.ZipFile(package, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for path in sorted(p for p in world.rglob("*") if p.is_file()):
            relative = path.relative_to(world).as_posix()
            if relative in {"session.lock", "uid.dat"}:
                continue
            info = zipfile.ZipInfo(relative, (1980, 1, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o100644 << 16
            archive.writestr(info, path.read_bytes())
    return package, sha256(package)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--paper-jar", type=Path, required=True)
    parser.add_argument("--plugin-jar", type=Path, required=True)
    parser.add_argument("--target", type=Path, required=True)
    parser.add_argument("--receipt", type=Path, required=True)
    parser.add_argument("--commit", required=True)
    parser.add_argument("--port", type=int, required=True)
    parser.add_argument("--java", default="java")
    args = parser.parse_args()
    paper = args.paper_jar.resolve()
    plugin = args.plugin_jar.resolve()
    target = args.target.resolve()
    if sha256(paper) != PAPER_EXPECTED_SHA256:
        raise ValueError("Paper JAR does not match pinned 1.21.11 build 132")
    configure(target, paper, plugin, args.port)
    command_tail = f"{WORLD} 0 200 0"

    first = PaperProcess(target, args.java)
    try:
        authority = first.wait_for("P5-P12 campaign authority ready", 300)
        first.wait_for("Done (", 300)
        prepare = first.command(f"observance placehold prepare {command_tail}", "PREPARE PASS", 600)
        plan = first.command(f"observance placehold plan {command_tail}", "PLAN ", 300)
        if "PLAN PASS" not in plan:
            raise RuntimeError(f"Deep Hold plan did not pass: {plan}")
        build = first.command(f"observance placehold build {command_tail}",
                              "Deep Hold build complete", 900)
        audit = first.command("observance placehold audit", "physically launch-placeable", 300)
        first.command("save-all flush", "Saved the game", 300)
    finally:
        try:
            first.stop()
        finally:
            write_text(target / "whole-campaign-first.log", "\n".join(first.lines) + "\n")

    second = PaperProcess(target, args.java)
    try:
        restart_authority = second.wait_for("P5-P12 campaign authority ready", 300)
        second.wait_for("Done (", 300)
        restart_audit = second.command("observance placehold audit", "physically launch-placeable", 300)
        second.command("save-all flush", "Saved the game", 300)
    finally:
        try:
            second.stop()
        finally:
            write_text(target / "whole-campaign-restart.log", "\n".join(second.lines) + "\n")

    package, package_hash = package_world(target)
    receipt = {
        "schema_version": "1.0.0-p5-p12-disposable-paper-receipt",
        "source_commit": args.commit,
        "runner_sha256": sha256(Path(__file__)),
        "target": str(target),
        "address": f"127.0.0.1:{args.port}",
        "paper_sha256": sha256(paper),
        "plugin_sha256": sha256(plugin),
        "campaign_projection_sha256": "c0aed5b32b4373c4a23406064763e447ba5390694c4074e654bdb35e52e2ad97",
        "minecraft_binding_sha256": "ef885a396437ec746705f7d9e92946c175a044ff23f364a7d5ce52237e2dce3d",
        "first_start": {"authority": authority, "prepare": prepare, "plan": plan,
                        "build": build, "audit": audit},
        "restart": {"authority": restart_authority, "audit": restart_audit},
        "first_log_sha256": sha256(target / "whole-campaign-first.log"),
        "restart_log_sha256": sha256(target / "whole-campaign-restart.log"),
        "world_package": str(package),
        "world_package_sha256": package_hash,
        "production_mutated": False,
        "pinned_p4_process_mutated": False,
        "fresh_client_visual_receipt": False,
    }
    args.receipt.parent.mkdir(parents=True, exist_ok=True)
    args.receipt.write_text(json.dumps(receipt, indent=2) + "\n", encoding="utf-8", newline="\n")
    print(json.dumps(receipt, indent=2))


if __name__ == "__main__":
    main()
