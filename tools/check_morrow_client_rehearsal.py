#!/usr/bin/env python3
"""Validate a real Morrow graphical-client rehearsal without accepting prose-only proof."""

from __future__ import annotations

import argparse
import base64
import hashlib
import ipaddress
import json
import subprocess
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
PAPER_RUNTIME = ROOT / "morrow" / "rehearsal" / "runtime" / "p0-paper-c40f916"
ROOM04_MANIFEST = "9831893bf03387b6c59b3835b648f056aafd67e6e194f3ade0282a7192fff41a"
REQUIRED_EVENTS = {
    "morrow.act1.room04_witnessed",
    "morrow.act1.static_proposal_authenticated",
    "morrow.act1.intention_error_proven",
    "morrow.act1.entity_replay_authorized",
    "morrow.act2.missing_role_completed",
    "morrow.act2.live_test_recorded",
    "morrow.act2.behavior_reuse_proven",
}
LANES = {
    "minecraft_visual_entity_interpolation_and_authored_pose",
    "native_dialog_mouse_keyboard_escape_and_readability",
    "resource_pack_decline_fallback_visual_parity",
    "audio_disabled_visual_equivalence",
    "full_inventory_safe_spawn_exit_for_1_2_6",
    "sixty_to_ninety_minute_pacing_without_operator_narration",
}
MEDIA_SUFFIXES = {".png", ".jpg", ".jpeg", ".webp", ".mp4", ".webm", ".mkv"}


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def parse_time(value: str) -> datetime:
    parsed = datetime.fromisoformat(value.replace("Z", "+00:00"))
    require(parsed.tzinfo is not None, "timestamps must include UTC offset")
    return parsed.astimezone(timezone.utc)


def git(*args: str) -> str:
    return subprocess.run(
        ["git", *args], cwd=ROOT, check=True, capture_output=True, text=True,
    ).stdout.strip()


def journal_event_types(path: Path) -> set[str]:
    events: set[str] = set()
    previous = "0" * 64
    expected_sequence = 1
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line.strip():
            continue
        fields = line.split("\t")
        require(len(fields) == 6, "journal record must have six TSV fields")
        sequence = int(fields[0])
        padding = "=" * ((4 - len(fields[5]) % 4) % 4)
        payload = base64.urlsafe_b64decode(fields[5] + padding)
        payload_hash = hashlib.sha256(payload).hexdigest()
        material = f"{sequence}\n{fields[1]}\n{fields[3]}\n{fields[4]}\n{payload_hash}".encode()
        event_hash = hashlib.sha256(material).hexdigest()
        require(sequence == expected_sequence and fields[1] == previous and fields[2] == event_hash,
                f"journal hash chain failed at sequence {sequence}")
        events.add(fields[4])
        previous = fields[2]
        expected_sequence += 1
    return events


def evidence_file(run: Path, row: dict[str, Any], kind: str,
                  started: datetime, ended: datetime, seen: set[Path]) -> Path:
    require(isinstance(row, dict), f"{kind} evidence row missing")
    relative = Path(row.get("file", ""))
    require(relative.parts and not relative.is_absolute() and ".." not in relative.parts,
            f"{kind} evidence path escapes run directory")
    path = (run / relative).resolve()
    require(path.is_relative_to(run), f"{kind} evidence escaped run directory")
    require(path not in seen, f"{kind} reused evidence already assigned to another proof")
    seen.add(path)
    require(path.is_file() and path.stat().st_size > 0, f"{kind} evidence file missing or empty")
    require(row.get("bytes") == path.stat().st_size, f"{kind} evidence size mismatch")
    require(row.get("sha256") == sha(path), f"{kind} evidence hash mismatch")
    modified = datetime.fromtimestamp(path.stat().st_mtime, timezone.utc)
    require(started - timedelta(minutes=5) <= modified <= ended + timedelta(minutes=5),
            f"{kind} evidence is stale or outside run time")
    return path


def validate(run: Path) -> None:
    run = run.resolve()
    manifest_path = run / "client-rehearsal.json"
    data = load(manifest_path)
    require(data["schema_version"] == "1.0.0-morrow-human-client-rehearsal",
            "client rehearsal schema drifted")
    require(data["status"] == "pass", "client rehearsal is not marked pass")
    require(data["operator_id"] != data["observer_id"], "operator cannot review their own run")
    require(data["operator_narration_used"] is False,
            "operator narration was used or not explicitly denied")
    started, ended = parse_time(data["started_at"]), parse_time(data["ended_at"])
    require(started < ended and ended - started <= timedelta(hours=3), "invalid run time window")
    seen_evidence: set[Path] = set()

    server = data["server"]
    host = server["address"].rsplit(":", 1)[0].strip("[]")
    require(ipaddress.ip_address(host).is_loopback and server["loopback_only"],
            "client rehearsal used a non-loopback target")
    require(server["online_mode"] is False and server["production_credentials_loaded"] is False,
            "client rehearsal loaded production identity or credentials")

    source = data["source_commit"]
    require(len(source) == 40 and git("cat-file", "-t", source) == "commit",
            "source commit is missing")
    require(subprocess.run(["git", "merge-base", "--is-ancestor", source, "HEAD"], cwd=ROOT).returncode == 0,
            "client rehearsal source is not an ancestor of current HEAD")

    runtime_path = PAPER_RUNTIME / "paper-runtime-receipt.json"
    runtime = load(runtime_path)
    binding = data["artifact_binding"]
    require(binding["paper_runtime_receipt"] == str(runtime_path.relative_to(ROOT)).replace("\\", "/"),
            "Paper runtime receipt path drifted")
    require(binding["paper_runtime_receipt_sha256"] == sha(runtime_path),
            "Paper runtime receipt hash drifted")
    require(binding["paper_sha256"] == runtime["paper"]["sha256"], "Paper artifact mismatch")
    require(binding["plugin_sha256"] == runtime["plugin_sha256"], "plugin artifact mismatch")
    require(binding["room04_manifest_sha256"] == ROOM04_MANIFEST, "Room 04 manifest mismatch")
    require(data["release_id"] == runtime["release_id"] and data["campaign_id"] == runtime["campaign_id"],
            "release or campaign binding mismatch")

    lanes = data["lanes"]
    require(set(lanes) == LANES, "graphical-client lane set drifted")
    for name, lane in lanes.items():
        require(lane["status"] == "pass", f"client lane did not pass: {name}")
        finding = lane.get("observer_finding")
        require(isinstance(finding, dict) and finding.get("verdict") == "pass"
                and finding.get("observer_id") == data["observer_id"],
                f"independent observer finding missing for {name}")
        files = lane.get("files", [])
        require(len(files) >= 2, f"{name} needs synchronized media, not prose")
        paths = [evidence_file(run, row, f"lane {name}", started, ended, seen_evidence)
                 for row in files]
        require(any(path.suffix.lower() in MEDIA_SUFFIXES for path in paths),
                f"{name} lacks visual evidence")

    cohorts = data["cohorts"]
    require([row["size"] for row in cohorts] == [1, 2, 6], "cohort coverage must be exactly 1/2/6")
    all_players: set[str] = set()
    for cohort in cohorts:
        size = cohort["size"]
        players = cohort["players"]
        require(cohort["status"] == "pass" and len(players) == size and len(set(players)) == size,
                f"cohort {size} identity coverage failed")
        require(not all_players.intersection(players), f"player identity reused across cohort {size}")
        all_players.update(players)
        require(3600 <= cohort["duration_seconds"] <= 5400,
                f"cohort {size} pacing is outside 60-90 minutes")
        require(cohort["operator_interventions"] == [], f"cohort {size} required operator intervention")
        for flag in ("disconnect_rejoin_proven", "full_inventory_proven",
                     "safe_spawn_exit_proven", "cleanup_proven"):
            require(cohort[flag] is True, f"cohort {size} did not prove {flag}")
        evidence = cohort["evidence"]
        machine_paths = {
            kind: evidence_file(run, evidence[kind], f"cohort {size} {kind}", started, ended,
                                seen_evidence)
            for kind in ("server_log", "journal", "world_readback", "inventory_before",
                         "inventory_after", "cleanup")
        }
        media = [evidence_file(run, row, f"cohort {size} media", started, ended, seen_evidence)
                 for row in evidence["media"]]
        require(any(path.suffix.lower() in MEDIA_SUFFIXES for path in media),
                f"cohort {size} lacks client media")
        log_text = machine_paths["server_log"].read_text(encoding="utf-8", errors="replace")
        require("MORROW_RUNTIME_READY" in log_text and "MORROW_RUNTIME_CLOSED" in log_text,
                f"cohort {size} server lifecycle receipt incomplete")
        journal_events = journal_event_types(machine_paths["journal"])
        require(REQUIRED_EVENTS <= journal_events, f"cohort {size} journal misses required progression")
        world = load(machine_paths["world_readback"])
        require(world["room04_manifest_sha256"] == ROOM04_MANIFEST
                and world["safe_spawn_exit"] is True and world["occupied_cell_conflicts"] == 0,
                f"cohort {size} world read-back failed")
        cleanup = load(machine_paths["cleanup"])
        require(cleanup == {"leaked_entities": 0, "leaked_tasks": 0, "status": "pass"},
                f"cohort {size} cleanup receipt failed")
        before, after = load(machine_paths["inventory_before"]), load(machine_paths["inventory_after"])
        require(before["players"] == after["players"] == players
                and before["inventory_sha256"] == after["inventory_sha256"],
                f"cohort {size} inventory changed")

    final = data["final_receipts"]
    for kind, row in final.items():
        evidence_file(run, row, f"final {kind}", started, ended, seen_evidence)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("run", type=Path)
    args = parser.parse_args()
    try:
        validate(args.run)
    except Exception as failure:  # noqa: BLE001 - audit emits one concise receipt
        print(f"MORROW CLIENT REHEARSAL: FAIL: {failure}", file=sys.stderr)
        return 1
    print("MORROW CLIENT REHEARSAL: PASS loopback=1 cohorts=1/2/6 media+runtime=bound")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
