#!/usr/bin/env python3
"""Create-only Paper 1.21.11 validation and localhost review targets for M3 v3."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import queue
import shutil
import subprocess
import threading
import time
import zipfile


ROOT = Path(__file__).resolve().parents[1]
PAPER_EXPECTED_SHA256 = "5ffef465eeeb5f2a3c23a24419d97c51afd7dbb4923ff42df9a3f58bba1ccfba"
PAPER_VERSION = "1.21.11"
PAPER_BUILD = 132
PREDICATE_RAW_SHA256 = "16de527496a6c4e3ae0fc093db07b74754be55193059f1c8d3fe9ab0c29a595a"
MANIFEST_VERSION = "2.0.0-m2"
COARSE_AUTHORITY_SHA256 = "564225c0d9a2d015437f9722e5741e509b46837b6e194e6ca1cda860cea7f962"
SLICE_AUTHORITY_SHA256 = "316cedac5c1673e8fba913957d4c0c71bd899da47d8d95613f5e81da88c7ae2b"
AUTHORITY_ID = "observance-p4-private-slice-v3"


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def canonical_sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes().replace(b"\r\n", b"\n")).hexdigest()


def write_text(path: Path, body: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(body, encoding="utf-8", newline="\n")


class PaperProcess:
    def __init__(self, target: Path, java: str) -> None:
        self.target = target
        self.lines: list[str] = []
        self.events: queue.Queue[str] = queue.Queue()
        self.process = subprocess.Popen(
            [java, "-Xms1G", "-Xmx2G", "-jar", "paper.jar", "--nogui"],
            cwd=target, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
            text=True, encoding="utf-8", errors="replace", bufsize=1,
        )
        threading.Thread(target=self._read, daemon=True).start()

    def _read(self) -> None:
        assert self.process.stdout is not None
        for line in self.process.stdout:
            clean = line.rstrip("\r\n")
            self.lines.append(clean)
            self.events.put(clean)

    def wait_for(self, token: str, timeout: float = 180.0) -> str:
        deadline = time.monotonic() + timeout
        while time.monotonic() < deadline:
            if self.process.poll() is not None:
                raise RuntimeError(f"Paper exited before {token!r}; exit={self.process.returncode}")
            try:
                line = self.events.get(timeout=0.5)
            except queue.Empty:
                continue
            if token in line:
                return line
        raise TimeoutError(f"timed out waiting for Paper output: {token}")

    def command(self, command: str, expect: str, timeout: float = 120.0) -> str:
        assert self.process.stdin is not None
        self.process.stdin.write(command + "\n")
        self.process.stdin.flush()
        return self.wait_for(expect, timeout)

    def stop(self) -> None:
        if self.process.poll() is None:
            assert self.process.stdin is not None
            self.process.stdin.write("stop\n")
            self.process.stdin.flush()
        try:
            self.process.wait(timeout=120)
        except subprocess.TimeoutExpired as error:
            raise RuntimeError("Paper did not stop cleanly") from error
        if self.process.returncode != 0:
            raise RuntimeError(f"Paper exited nonzero: {self.process.returncode}")


def verify_authorities() -> None:
    coarse = canonical_sha256(ROOT / "design" / "m3" / "coarse-adjacency-v1.json")
    sliced = canonical_sha256(ROOT / "design" / "m3" / "vertical-slice-v3.json")
    if coarse != COARSE_AUTHORITY_SHA256:
        raise ValueError(f"coarse authority hash drift: {coarse}")
    if sliced != SLICE_AUTHORITY_SHA256:
        raise ValueError(f"v3 slice authority hash drift: {sliced}")


def configure(target: Path, paper: Path, plugin: Path, target_id: str, commit: str, port: int) -> None:
    if target.exists():
        raise FileExistsError(f"refusing to reuse existing disposable target: {target}")
    (target / "plugins" / "Observance").mkdir(parents=True)
    shutil.copy2(paper, target / "paper.jar")
    shutil.copy2(plugin, target / "plugins" / plugin.name)
    write_text(target / ".observance-disposable-paper-target", target_id + "\n")
    write_text(target / "eula.txt", "eula=true\n")
    write_text(target / "server.properties", "\n".join([
        "accepts-transfers=false", "allow-flight=false", "allow-nether=false", "difficulty=peaceful",
        "enable-command-block=false", "enable-rcon=false", "enable-status=true", "enforce-whitelist=false",
        "force-gamemode=true", "gamemode=adventure", "generate-structures=false", "hardcore=false",
        "level-name=m3_private_slice", "level-seed=9137", "level-type=minecraft:flat", "max-players=4",
        "motd=PRIVATE M3 V3 DISPOSABLE REVIEW", "online-mode=false", "prevent-proxy-connections=true",
        "server-ip=127.0.0.1", f"server-port={port}", "spawn-animals=false", "spawn-monsters=false",
        "spawn-npcs=false", "spawn-protection=0", "sync-chunk-writes=true", "view-distance=5",
        "simulation-distance=4", "white-list=false", "",]))
    write_text(target / "plugins" / "Observance" / "config.yml", "\n".join([
        "m3-review:", "  enabled: true", f'  target-id: "{target_id}"',
        '  world: "m3_private_slice"', "  origin-x: 0", "  origin-y: 80", "  origin-z: 0",
        f'  source-git-commit: "{commit}"', "resource-pack:", "  required: false",
        "finale:", "  production-shutdown: false", "drama:", "  enabled: false", "",]))


def observe(process: PaperProcess, finding: str, source: str) -> str:
    return process.command(f"obsm3 observe {finding} {source} harness readback", "M3_OBSERVATION_COMMITTED")


def exercise_validation(target: Path, java: str) -> tuple[dict[str, object], list[str], list[str]]:
    first = PaperProcess(target, java)
    try:
        confirmation = first.wait_for("M3_TARGET_CONFIRMED", 300)
        first.wait_for("Done (", 300)
        status = first.command("obsm3 status", "M3_STATUS")
        build = first.command("obsm3 build", "M3_BUILD_COMPLETE", 300)
        closed = first.command("obsm3 audit", "M3_AUDIT PASS")
        security_closed = first.command("obsm3 security", "M3_SECURITY_PASS")
        observations = [
            observe(first, "P4.F3", "population_board"), observe(first, "P4.F3", "ration_ledger"),
            observe(first, "P4.F1", "cart_wear"), observe(first, "P4.F1", "drainage_map"),
            observe(first, "P4.F4", "descent_heat_marks"), observe(first, "P4.F4", "founding_minutes"),
            observe(first, "P4.F2", "material_join_civic"), observe(first, "P4.F2", "survey_revisions"),
        ]
        first.command("obsm3 finding P4.F3 harness population_board,ration_ledger", "M3_FINDING_COMMITTED")
        first.command("obsm3 finding P4.F1 harness cart_wear,drainage_map", "M3_FINDING_COMMITTED")
        first.command("obsm3 finding P4.F4 harness descent_heat_marks,founding_minutes", "M3_FINDING_COMMITTED")
        first.command("obsm3 finding P4.F2 harness material_join_civic,survey_revisions", "M3_FINDING_COMMITTED")
        first.command("obsm3 finding P4.F5 harness P4.F1,P4.F2,P4.F3,P4.F4", "gate=open")
        opened = first.command("obsm3 audit", "M3_AUDIT PASS")
        security_open = first.command("obsm3 security", "M3_SECURITY_PASS")
        replay = first.command("obsm3 replay", "M3_REPLAY_PASS")
        first.command("save-all flush", "Saved the game")
    finally:
        first.stop()
    write_text(target / "m3-first-start.log", "\n".join(first.lines) + "\n")

    second = PaperProcess(target, java)
    try:
        restart_confirmation = second.wait_for("M3_TARGET_CONFIRMED", 300)
        second.wait_for("Done (", 300)
        restarted = second.command("obsm3 audit", "M3_AUDIT PASS")
        restart_security = second.command("obsm3 security", "M3_SECURITY_PASS")
        restart_replay = second.command("obsm3 replay", "M3_REPLAY_PASS")
        final = second.command("obsm3 audit", "M3_AUDIT PASS")
        second.command("save-all flush", "Saved the game")
    finally:
        second.stop()
    write_text(target / "m3-restart.log", "\n".join(second.lines) + "\n")
    evidence: dict[str, object] = {
        "platform_confirmation": confirmation, "status": status, "build": build,
        "closed_audit": closed, "security_closed": security_closed, "observations": observations,
        "open_audit": opened, "security_open": security_open, "replay": replay,
        "restart_confirmation": restart_confirmation, "restart_audit": restarted,
        "restart_security": restart_security, "restart_replay": restart_replay, "final_audit": final,
    }
    return evidence, first.lines, second.lines


def prepare_review(target: Path, java: str) -> tuple[dict[str, object], list[str], list[str]]:
    process = PaperProcess(target, java)
    try:
        confirmation = process.wait_for("M3_TARGET_CONFIRMED", 300)
        process.wait_for("Done (", 300)
        status = process.command("obsm3 status", "M3_STATUS")
        build = process.command("obsm3 build", "M3_BUILD_COMPLETE", 300)
        audit = process.command("obsm3 audit", "M3_AUDIT PASS")
        security = process.command("obsm3 security", "M3_SECURITY_PASS")
        process.command("save-all flush", "Saved the game")
    finally:
        process.stop()
    write_text(target / "m3-review-prepare.log", "\n".join(process.lines) + "\n")
    return {"platform_confirmation": confirmation, "status": status, "build": build,
            "closed_audit": audit, "security": security}, process.lines, []


def package_world(target: Path) -> tuple[str, str, Path]:
    roots = [target / "m3_private_slice", target / "m3_private_slice_nether", target / "m3_private_slice_the_end"]
    files = sorted(path for root in roots if root.exists() for path in root.rglob("*")
                   if path.is_file() and path.name != "session.lock")
    digest = hashlib.sha256()
    for path in files:
        relative = path.relative_to(target).as_posix()
        digest.update((relative + "\n" + sha256(path) + "\n").encode())
    world_tree_hash = digest.hexdigest()
    package = target / "m3-private-slice-v3-world.zip"
    with zipfile.ZipFile(package, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for path in files:
            info = zipfile.ZipInfo(path.relative_to(target).as_posix(), (1980, 1, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o100644 << 16
            archive.writestr(info, path.read_bytes(), compress_type=zipfile.ZIP_DEFLATED, compresslevel=9)
    return world_tree_hash, sha256(package), package


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--paper-jar", type=Path, required=True)
    parser.add_argument("--plugin-jar", type=Path, required=True)
    parser.add_argument("--target", type=Path, required=True)
    parser.add_argument("--target-id", required=True)
    parser.add_argument("--commit", required=True)
    parser.add_argument("--receipt", type=Path, required=True)
    parser.add_argument("--mode", choices=("validate", "prepare-review"), default="validate")
    parser.add_argument("--port", type=int, default=25579)
    parser.add_argument("--java", default="java")
    args = parser.parse_args()
    paper = args.paper_jar.resolve()
    plugin = args.plugin_jar.resolve()
    target = args.target.resolve()
    if sha256(paper) != PAPER_EXPECTED_SHA256:
        raise ValueError("Paper JAR does not match platform-confirmed stable build 132")
    verify_authorities()
    configure(target, paper, plugin, args.target_id, args.commit, args.port)
    evidence, first_lines, second_lines = (exercise_validation(target, args.java)
                                           if args.mode == "validate" else prepare_review(target, args.java))
    world_hash, package_hash, package = package_world(target)
    journal = target / "plugins" / "Observance" / "m3-private-slice-v3.journal"
    if args.mode == "validate" and not journal.is_file():
        raise FileNotFoundError("validation completed without the required durable M3 journal")
    receipt = {
        "schema_version": "3.0.0-m3-paper-receipt" if args.mode == "validate"
            else "3.0.0-m3-review-server-receipt",
        "scope": "disposable local private Paper target; never player-facing or production",
        "mode": args.mode,
        "target_id": args.target_id,
        "target_path": str(target),
        "source_git_commit": args.commit,
        "manifest_version": MANIFEST_VERSION,
        "authority_id": AUTHORITY_ID,
        "predicate_raw_sha256": PREDICATE_RAW_SHA256,
        "coarse_authority_sha256": COARSE_AUTHORITY_SHA256,
        "slice_authority_sha256": SLICE_AUTHORITY_SHA256,
        "paper": {"version": PAPER_VERSION, "build": PAPER_BUILD, "jar_sha256": sha256(paper)},
        "plugin_jar_sha256": sha256(plugin),
        "world_tree_sha256": world_hash,
        "world_package_sha256": package_hash,
        "world_package_name": package.name,
        "journal_sha256": sha256(journal) if journal.is_file() else None,
        "journal_state": "present" if journal.is_file() else "absent_pristine_review_target",
        "log_sha256": sha256(target / ("m3-first-start.log" if args.mode == "validate" else "m3-review-prepare.log")),
        "restart_log_sha256": sha256(target / "m3-restart.log") if second_lines else None,
        "evidence": evidence,
        "server_configuration": {
            "bind": "127.0.0.1", "port": args.port, "online_mode": False, "whitelist": False,
            "force_gamemode": True, "gamemode": "adventure", "default_op": False,
            "inventory_escrow": False, "production_credentials_loaded": False,
        },
        "client_receipts": {
            "non_op_adventure_join": None, "protected_region_bypass": None,
            "two_client_asymmetry": None, "solo_accessibility": None,
            "reason": "Server configuration, pure state tests, and Paper security readback exist; no authenticated Minecraft client was automated.",
        },
        "brad_visual_approval": None,
        "brad_visual_status": "pending_v3_re_review_after_revision",
        "m4_authority": "closed",
    }
    write_text(args.receipt.resolve(), json.dumps(receipt, indent=2, sort_keys=True) + "\n")
    print(json.dumps(receipt, indent=2, sort_keys=True))


if __name__ == "__main__":
    main()
