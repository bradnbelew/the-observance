#!/usr/bin/env python3
"""Create a fail-closed, source-bound Morrow graphical-client rehearsal packet."""

from __future__ import annotations

import argparse
import hashlib
import ipaddress
import json
import re
import subprocess
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
PAPER_RUNTIME = ROOT / "morrow" / "rehearsal" / "runtime" / "p0-paper-c40f916"
ROOM04_MANIFEST = "9831893bf03387b6c59b3835b648f056aafd67e6e194f3ade0282a7192fff41a"
LANES = (
    "minecraft_visual_entity_interpolation_and_authored_pose",
    "native_dialog_mouse_keyboard_escape_and_readability",
    "resource_pack_decline_fallback_visual_parity",
    "audio_disabled_visual_equivalence",
    "full_inventory_safe_spawn_exit_for_1_2_6",
    "sixty_to_ninety_minute_pacing_without_operator_narration",
)


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def git(*args: str) -> str:
    result = subprocess.run(
        ["git", *args], cwd=ROOT, check=True, capture_output=True, text=True,
    )
    return result.stdout.strip()


def require_loopback(address: str) -> None:
    host = address.rsplit(":", 1)[0].strip("[]")
    if not ipaddress.ip_address(host).is_loopback:
        raise ValueError(f"client rehearsal target must be loopback, got {address!r}")


def empty_evidence() -> dict[str, Any]:
    return {"status": "missing", "files": [], "observer_finding": None}


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--run-id", required=True, help="lowercase create-only identifier")
    parser.add_argument("--output-root", type=Path,
                        default=ROOT / "build" / "morrow-client-rehearsal")
    parser.add_argument("--server", default="127.0.0.1:25589")
    parser.add_argument("--operator-id", required=True, help="pseudonymous operator identifier")
    parser.add_argument("--observer-id", required=True, help="pseudonymous evidence reviewer identifier")
    args = parser.parse_args()

    if not re.fullmatch(r"[a-z0-9][a-z0-9-]{2,63}", args.run_id):
        raise ValueError("run-id must match [a-z0-9][a-z0-9-]{2,63}")
    if args.operator_id == args.observer_id:
        raise ValueError("observer-id must differ from operator-id")
    require_loopback(args.server)

    target = (args.output_root / args.run_id).resolve()
    if target.exists():
        raise FileExistsError(f"refusing to reuse client rehearsal directory: {target}")
    target.mkdir(parents=True)
    for name in ("media", "server", "journal", "world", "inventory", "receipts"):
        (target / name).mkdir()

    runtime_path = PAPER_RUNTIME / "paper-runtime-receipt.json"
    runtime = json.loads(runtime_path.read_text(encoding="utf-8"))
    source = git("rev-parse", "HEAD")
    now = datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")
    cohorts = []
    for size in (1, 2, 6):
        cohorts.append({
            "size": size,
            "players": [],
            "status": "missing",
            "duration_seconds": None,
            "operator_interventions": None,
            "disconnect_rejoin_proven": False,
            "full_inventory_proven": False,
            "safe_spawn_exit_proven": False,
            "cleanup_proven": False,
            "evidence": {
                "server_log": None,
                "journal": None,
                "world_readback": None,
                "inventory_before": None,
                "inventory_after": None,
                "cleanup": None,
                "media": [],
            },
        })

    packet = {
        "schema_version": "1.0.0-morrow-human-client-rehearsal",
        "run_id": args.run_id,
        "status": "in_progress",
        "created_at": now,
        "started_at": None,
        "ended_at": None,
        "source_commit": source,
        "release_id": runtime["release_id"],
        "campaign_id": runtime["campaign_id"],
        "server": {
            "address": args.server,
            "loopback_only": True,
            "online_mode": False,
            "production_credentials_loaded": False,
        },
        "artifact_binding": {
            "paper_runtime_receipt": str(runtime_path.relative_to(ROOT)).replace("\\", "/"),
            "paper_runtime_receipt_sha256": sha(runtime_path),
            "paper_sha256": runtime["paper"]["sha256"],
            "plugin_sha256": runtime["plugin_sha256"],
            "room04_manifest_sha256": ROOM04_MANIFEST,
        },
        "operator_id": args.operator_id,
        "observer_id": args.observer_id,
        "operator_narration_used": None,
        "lanes": {name: empty_evidence() for name in LANES},
        "cohorts": cohorts,
        "final_receipts": {
            "complete_server_log": None,
            "complete_journal": None,
            "final_world_readback": None,
            "clean_shutdown_log": None,
        },
        "notes": [],
    }
    (target / "client-rehearsal.json").write_text(
        json.dumps(packet, indent=2, sort_keys=True) + "\n", encoding="utf-8",
    )
    print(target)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
