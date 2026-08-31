#!/usr/bin/env python3
"""Run an owned real-client Morrow cohort join/inventory/restart rehearsal on loopback."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import shutil
import subprocess
import sys
import time
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

import run_morrow_disposable_paper as paper_harness


ROOT = Path(__file__).resolve().parents[1]
EXPECTED_SAFE_CELL = "0,80,-2"
EXPECTED_INVENTORY_ITEMS = 36 * 64


def require(condition: bool, message: str) -> None:
    if not condition:
        raise RuntimeError(message)


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def inventory_commands(usernames: list[str]) -> str:
    commands: list[str] = []
    for username in usernames:
        for slot in range(9):
            commands.append(
                f"item replace entity {username} hotbar.{slot} with minecraft:stone 64")
        for slot in range(27):
            commands.append(
                f"item replace entity {username} inventory.{slot} with minecraft:stone 64")
    return "\n".join(commands)


def count_commands(usernames: list[str]) -> str:
    return "\n".join(f"clear {username} minecraft:stone 0" for username in usernames)


def start_clients(target_root: Path, run_id: str, usernames: list[str], server: str,
                  minecraft_root: Path, javaw: Path, memory_mib: int) \
        -> list[tuple[str, subprocess.Popen[str]]]:
    owned: list[tuple[str, subprocess.Popen[str]]] = []
    for index, username in enumerate(usernames, start=1):
        child_run_id = f"{run_id}-p{index:02d}"
        command = [
            sys.executable, str(ROOT / "tools" / "run_morrow_offline_client.py"),
            "--run-id", child_run_id,
            "--minecraft-root", str(minecraft_root),
            "--target-root", str(target_root),
            "--server", server,
            "--username", username,
            "--server-resource-pack-policy", "disabled",
            "--javaw", str(javaw),
            "--wait-seconds", "120",
            "--terminate-after-wait",
            "--stop-when-requested",
            "--max-memory-mib", str(memory_mib),
            "--disable-tutorial-toast",
        ]
        wrapper = subprocess.Popen(
            command, cwd=ROOT, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
            text=True, encoding="utf-8", errors="replace")
        owned.append((child_run_id, wrapper))
    return owned


def stop_clients(target_root: Path, owned: list[tuple[str, subprocess.Popen[str]]]) \
        -> list[dict[str, Any]]:
    for child_run_id, _ in owned:
        paper_harness.write_text(target_root / child_run_id / "stop.request",
                                 "stop owned cohort rehearsal client\n")
    receipts: list[dict[str, Any]] = []
    for child_run_id, wrapper in owned:
        try:
            output, _ = wrapper.communicate(timeout=45)
        except subprocess.TimeoutExpired:
            wrapper.terminate()
            output, _ = wrapper.communicate(timeout=15)
        receipt_path = target_root / child_run_id / "offline-client-receipt.json"
        require(wrapper.returncode == 0 and receipt_path.is_file(),
                f"owned client {child_run_id} did not finalize: {output[-1000:]}")
        receipt = json.loads(receipt_path.read_text(encoding="utf-8"))
        require(receipt["status"] == "harness_stopped_after_request"
                and receipt["production_credentials_loaded"] is False
                and receipt["loopback_only"] is True,
                f"owned client {child_run_id} crossed its lifecycle boundary")
        receipts.append({
            "run_id": child_run_id,
            "receipt": receipt_path,
            "data": receipt,
            "wrapper_output_sha256": hashlib.sha256(output.encode()).hexdigest(),
        })
    return receipts


def stop_clients_best_effort(target_root: Path,
                             owned: list[tuple[str, subprocess.Popen[str]]]) -> None:
    for child_run_id, wrapper in owned:
        if wrapper.poll() is not None:
            continue
        try:
            paper_harness.write_text(target_root / child_run_id / "stop.request",
                                     "stop failed cohort rehearsal client\n")
            wrapper.wait(timeout=20)
        except Exception:  # noqa: BLE001 - cleanup must continue across every owned client
            if wrapper.poll() is None:
                wrapper.terminate()
                try:
                    wrapper.wait(timeout=10)
                except subprocess.TimeoutExpired:
                    wrapper.kill()
                    wrapper.wait(timeout=10)


def wait_for_cohort(process: paper_harness.PaperProcess, usernames: list[str]) -> list[str]:
    safe_lines = []
    for username in usernames:
        line = process.wait_for(
            f"MORROW_PLAYER_SAFE_ENTRY player={username} reason=join", 120)
        require(f"to={EXPECTED_SAFE_CELL}" in line and "inventory_mutations=0" in line,
                f"{username} did not receive the canonical mutation-free safe entry")
        safe_lines.append(line)
    process.command("list", f"There are {len(usernames)} of a max of 6 players online")
    return safe_lines


def verify_inventory(process: paper_harness.PaperProcess, usernames: list[str], label: str) \
        -> list[str]:
    lines = process.batch(f"{label}_INVENTORY_COUNT", count_commands(usernames))
    matches = [line for line in lines if f"Found {EXPECTED_INVENTORY_ITEMS} matching item(s)" in line]
    require(len(matches) == len(usernames),
            f"{label} inventory count mismatch: expected {len(usernames)} x "
            f"{EXPECTED_INVENTORY_ITEMS}, got {matches}")
    return matches


def retain_clients(receipt_dir: Path, phase: str, rows: list[dict[str, Any]]) \
        -> list[dict[str, Any]]:
    phase_root = receipt_dir / phase
    phase_root.mkdir(parents=True)
    retained: list[dict[str, Any]] = []
    for row in rows:
        destination = phase_root / f"{row['run_id']}.json"
        shutil.copy2(row["receipt"], destination)
        data = row["data"]
        retained.append({
            "run_id": row["run_id"],
            "username": data["identity"]["username"],
            "uuid": data["identity"]["uuid"],
            "receipt": destination.relative_to(receipt_dir).as_posix(),
            "receipt_sha256": sha(destination),
            "client_log_sha256": data["log_sha256"],
            "wrapper_output_sha256": row["wrapper_output_sha256"],
        })
    return retained


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--candidate-root", type=Path, required=True)
    parser.add_argument("--paper-jar", type=Path, required=True)
    parser.add_argument("--bootstrap-root", type=Path, required=True)
    parser.add_argument("--plugin-jar", type=Path, required=True)
    parser.add_argument("--minecraft-root", type=Path, required=True)
    parser.add_argument("--target", type=Path, required=True)
    parser.add_argument("--receipt-dir", type=Path, required=True)
    parser.add_argument("--commit", required=True)
    parser.add_argument("--run-id", required=True)
    parser.add_argument("--cohort-size", type=int, choices=(1, 2, 6), required=True)
    parser.add_argument("--paper-port", type=int, default=25589)
    parser.add_argument("--ingest-port", type=int, default=29443)
    parser.add_argument("--max-memory-mib", type=int, default=768)
    parser.add_argument("--java", default="java")
    parser.add_argument("--javaw", type=Path,
                        default=Path(r"C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot\bin\javaw.exe"))
    parser.add_argument("--launch-acknowledgement", required=True)
    args = parser.parse_args()

    require(re.fullmatch(r"[a-z0-9][a-z0-9-]{2,47}", args.run_id) is not None,
            "run-id must use lowercase letters, digits, and hyphens")
    require(args.launch_acknowledgement
            == f"launch-visible-minecraft-cohort:{args.cohort_size}",
            "visible cohort launch acknowledgement mismatch")
    require(768 <= args.max_memory_mib <= 1024,
            "cohort memory must be between 768 and 1024 MiB per client")
    target, receipt_dir = args.target.resolve(), args.receipt_dir.resolve()
    require(not target.exists() and not receipt_dir.exists(),
            "refusing to reuse disposable cohort target or receipt directory")
    require(not paper_harness.port_open(args.paper_port)
            and not paper_harness.port_open(args.ingest_port),
            "one or more cohort loopback ports are occupied")
    paper, plugin = args.paper_jar.resolve(), args.plugin_jar.resolve()
    require(paper_harness.sha256(paper) == paper_harness.PAPER_SHA256,
            "selected Paper JAR is not exact pinned build 132")
    require(args.minecraft_root.resolve().is_dir() and args.javaw.resolve().is_file(),
            "installed vanilla client root or Java 21 runtime is missing")

    usernames = [f"MorrowW{index:02d}" for index in range(1, args.cohort_size + 1)]
    server_address = f"127.0.0.1:{args.paper_port}"
    cache, seeded = paper_harness.configure(
        target, paper, plugin, args.bootstrap_root.resolve(),
        args.paper_port, args.ingest_port)
    cert, key, truststore, cert_hash = paper_harness.generate_tls(target)
    ingest_server, ingest = paper_harness.start_ingest(args.ingest_port, cert, key)
    first_process: paper_harness.PaperProcess | None = None
    restart_process: paper_harness.PaperProcess | None = None
    first_owned: list[tuple[str, subprocess.Popen[str]]] = []
    restart_owned: list[tuple[str, subprocess.Popen[str]]] = []
    first_root, restart_root = target / "clients-first", target / "clients-rejoin"
    started_at = datetime.now(timezone.utc)
    try:
        first_process = paper_harness.PaperProcess(target, args.java, truststore)
        first_room = first_process.wait_for("MORROW_ROOM04_READY status=BUILT", 300)
        first_ready = first_process.wait_for("MORROW_RUNTIME_READY", 300)
        first_process.wait_for("Done (", 300)
        require(ingest.committed.wait(30), "Morrow projector did not reach loopback ingest")
        first_owned = start_clients(
            first_root, args.run_id, usernames, server_address,
            args.minecraft_root.resolve(), args.javaw.resolve(), args.max_memory_mib)
        first_safe = wait_for_cohort(first_process, usernames)
        fill_lines = first_process.batch("COHORT_FILL", inventory_commands(usernames))
        require(sum("Replaced slot" in line for line in fill_lines) == 36 * len(usernames),
                "Paper did not fill every main-inventory slot")
        first_counts = verify_inventory(first_process, usernames, "FIRST")
        first_rows = stop_clients(first_root, first_owned)
        first_owned = []
        for username in usernames:
            first_process.wait_for(f"{username} left the game", 60)
        first_process.command("save-all flush", "Saved the game")
        attempts_before_restart = len(ingest.attempts)
        first_process.stop()
        paper_harness.write_text(
            target / "morrow-cohort-first.log", "\n".join(first_process.lines) + "\n")
        require(any("MORROW_RUNTIME_CLOSED" in line for line in first_process.lines),
                "first Paper stop omitted Morrow cleanup receipt")
        first_process = None

        restart_process = paper_harness.PaperProcess(target, args.java, truststore)
        restart_room = restart_process.wait_for(
            "MORROW_ROOM04_READY status=ALREADY_PRESENT", 300)
        restart_ready = restart_process.wait_for("MORROW_RUNTIME_READY", 300)
        restart_process.wait_for("Done (", 300)
        time.sleep(1.0)
        require(len(ingest.attempts) == attempts_before_restart,
                "durable projector cursor redelivered after Paper restart")
        restart_owned = start_clients(
            restart_root, args.run_id, usernames, server_address,
            args.minecraft_root.resolve(), args.javaw.resolve(), args.max_memory_mib)
        restart_safe = wait_for_cohort(restart_process, usernames)
        restart_counts = verify_inventory(restart_process, usernames, "RESTART")
        restart_rows = stop_clients(restart_root, restart_owned)
        restart_owned = []
        for username in usernames:
            restart_process.wait_for(f"{username} left the game", 60)
        restart_process.command("save-all flush", "Saved the game")
        restart_process.stop()
        paper_harness.write_text(
            target / "morrow-cohort-restart.log", "\n".join(restart_process.lines) + "\n")
        require(any("MORROW_RUNTIME_CLOSED" in line for line in restart_process.lines),
                "restart Paper stop omitted Morrow cleanup receipt")
        restart_process = None

        receipt_dir.mkdir(parents=True)
        first_log = receipt_dir / "morrow-cohort-first.log"
        restart_log = receipt_dir / "morrow-cohort-restart.log"
        shutil.copy2(target / first_log.name, first_log)
        shutil.copy2(target / restart_log.name, restart_log)
        retained_first = retain_clients(receipt_dir, "first", first_rows)
        retained_restart = retain_clients(receipt_dir, "restart", restart_rows)
        require([row["uuid"] for row in retained_first]
                == [row["uuid"] for row in retained_restart],
                "rejoin client UUIDs differ from first-join identities")
        receipt = {
            "schema_version": "1.0.0-morrow-real-client-cohort-runtime",
            "status": "bounded_join_inventory_restart_cleanup_pass_full_playthrough_open",
            "source_commit": args.commit,
            "started_at": started_at.replace(microsecond=0).isoformat().replace("+00:00", "Z"),
            "completed_at": datetime.now(timezone.utc).replace(
                microsecond=0).isoformat().replace("+00:00", "Z"),
            "cohort_size": args.cohort_size,
            "server": {
                "bind": "127.0.0.1", "paper_port": args.paper_port,
                "ingest_port": args.ingest_port, "online_mode": False,
                "production_credentials_loaded": False,
                "first_room": first_room, "first_ready": first_ready,
                "restart_room": restart_room, "restart_ready": restart_ready,
                "first_log": first_log.name, "first_log_sha256": sha(first_log),
                "restart_log": restart_log.name, "restart_log_sha256": sha(restart_log),
                "tls_certificate_sha256": cert_hash,
                "bootstrap_sections": {
                    name: {key: value for key, value in section.items() if key != "files"}
                    for name, section in cache.items()
                },
            },
            "journal": seeded,
            "clients": {"first": retained_first, "restart": retained_restart},
            "proof": {
                "real_vanilla_clients_joined_concurrently": True,
                "safe_entry_lines": first_safe,
                "safe_rejoin_lines": restart_safe,
                "inventory_fixture": "36 slots x 64 minecraft:stone",
                "inventory_items_per_player_before_disconnect": EXPECTED_INVENTORY_ITEMS,
                "inventory_items_per_player_after_restart": EXPECTED_INVENTORY_ITEMS,
                "first_inventory_count_lines": first_counts,
                "restart_inventory_count_lines": restart_counts,
                "same_uuid_after_restart": True,
                "paper_restart_completed": True,
                "projector_redelivery_after_restart": False,
                "client_processes_closed": True,
                "paper_processes_closed": True,
                "morrow_cleanup_logged_twice": True,
                "input_injected": False,
                "full_human_playthrough_proven": False,
                "production_mutated": False,
            },
            "remaining_gate": "complete the operator-free 60-90 minute cohort slice with "
                              "continuous media and independent observer review",
            "production_enablement": "blocked",
        }
        paper_harness.write_text(
            receipt_dir / "cohort-runtime-rehearsal.json",
            json.dumps(receipt, indent=2, sort_keys=True) + "\n")
        print(json.dumps({
            "status": receipt["status"], "cohort_size": args.cohort_size,
            "receipt": str(receipt_dir / "cohort-runtime-rehearsal.json"),
        }, indent=2))
        return 0
    finally:
        stop_clients_best_effort(first_root, first_owned)
        stop_clients_best_effort(restart_root, restart_owned)
        for process in (first_process, restart_process):
            if process is not None:
                try:
                    process.stop()
                except Exception:
                    pass
        ingest_server.shutdown()
        ingest_server.server_close()


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as failure:  # noqa: BLE001 - bounded runner emits one concise failure
        print(f"MORROW CLIENT COHORT RUNTIME: FAIL: {failure}", file=sys.stderr)
        raise SystemExit(1)
