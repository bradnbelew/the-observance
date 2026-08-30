#!/usr/bin/env python3
"""Create a deterministic 1/2/6-player vanilla-client rehearsal cohort."""

from __future__ import annotations

import argparse
import hashlib
import ipaddress
import json
import re
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_MINECRAFT = Path.home() / "AppData" / "Roaming" / ".minecraft"


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def require(condition: bool, message: str) -> None:
    if not condition:
        raise RuntimeError(message)


def write_receipt(path: Path, value: dict[str, Any]) -> None:
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--run-id", required=True)
    parser.add_argument("--cohort-size", required=True, type=int, choices=(1, 2, 6))
    parser.add_argument("--minecraft-root", type=Path, default=DEFAULT_MINECRAFT)
    parser.add_argument("--target-root", type=Path,
                        default=ROOT / "build" / "morrow-offline-cohorts")
    parser.add_argument("--server", default="127.0.0.1:25589")
    parser.add_argument("--javaw", type=Path,
                        default=Path(r"C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot\bin\javaw.exe"))
    parser.add_argument("--audio-disabled", action="store_true")
    parser.add_argument("--max-memory-mib", type=int, default=1024)
    parser.add_argument("--launch", action="store_true")
    parser.add_argument("--launch-acknowledgement", default="")
    args = parser.parse_args()

    if not re.fullmatch(r"[a-z0-9][a-z0-9-]{2,47}", args.run_id):
        raise ValueError("run-id must match [a-z0-9][a-z0-9-]{2,47}")
    host = args.server.rsplit(":", 1)[0].strip("[]")
    require(ipaddress.ip_address(host).is_loopback, "cohort target must be loopback")
    require(768 <= args.max_memory_mib <= 2048,
            "max-memory-mib must be between 768 and 2048")
    expected_ack = f"launch-visible-vanilla-clients:{args.cohort_size}:{args.server}"
    if args.launch:
        require(args.launch_acknowledgement == expected_ack,
                f"launch refused; acknowledgement must be exactly {expected_ack}")
    else:
        require(not args.launch_acknowledgement,
                "launch acknowledgement is invalid without --launch")

    run_root = (args.target_root / args.run_id).resolve()
    require(not run_root.exists(), f"refusing to reuse cohort target: {run_root}")
    clients_root = run_root / "clients"
    clients_root.mkdir(parents=True)
    receipt_path = run_root / "cohort-receipt.json"
    receipt: dict[str, Any] = {
        "schema_version": "1.0.0-morrow-offline-cohort",
        "status": "preparing",
        "created_at": datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z"),
        "run_id": args.run_id,
        "cohort_size": args.cohort_size,
        "server": args.server,
        "loopback_only": True,
        "visible_clients_launched": args.launch,
        "audio_disabled": args.audio_disabled,
        "max_memory_mib_per_client": args.max_memory_mib,
        "source_commit": subprocess.run(
            ["git", "rev-parse", "HEAD"], cwd=ROOT, check=True, capture_output=True, text=True,
        ).stdout.strip(),
        "launcher": {
            "path": "tools/run_morrow_offline_client.py",
            "sha256": sha(ROOT / "tools" / "run_morrow_offline_client.py"),
        },
        "clients": [],
    }
    write_receipt(receipt_path, receipt)

    launcher = ROOT / "tools" / "run_morrow_offline_client.py"
    for index in range(1, args.cohort_size + 1):
        child_run_id = f"{args.run_id}-p{index:02d}"
        username = f"MorrowW{index:02d}"
        command = [
            sys.executable, str(launcher),
            "--run-id", child_run_id,
            "--minecraft-root", str(args.minecraft_root),
            "--target-root", str(clients_root),
            "--server", args.server,
            "--username", username,
            "--javaw", str(args.javaw),
            "--max-memory-mib", str(args.max_memory_mib),
        ]
        if args.audio_disabled:
            command.append("--audio-disabled")
        if args.launch:
            command.extend(("--wait-seconds", "0"))
        else:
            command.append("--prepare-only")
        result = subprocess.run(command, cwd=ROOT, capture_output=True, text=True)
        if result.returncode != 0:
            receipt["status"] = "partial_failure"
            receipt["failed_client_index"] = index
            receipt["failure"] = result.stderr.strip()[-1000:]
            write_receipt(receipt_path, receipt)
            raise RuntimeError(f"client {index} preparation failed; retained partial receipt")
        child_receipt = clients_root / child_run_id / "offline-client-receipt.json"
        require(child_receipt.is_file(), f"client {index} emitted no receipt")
        child = json.loads(child_receipt.read_text(encoding="utf-8"))
        require(child["identity"]["username"] == username,
                f"client {index} identity receipt mismatch")
        require(child["server"] == args.server and child["loopback_only"] is True,
                f"client {index} target receipt mismatch")
        require(child["max_memory_mib"] == args.max_memory_mib,
                f"client {index} memory receipt mismatch")
        require(child["accessibility_profile"]["audio_disabled"] is args.audio_disabled,
                f"client {index} accessibility receipt mismatch")
        receipt["clients"].append({
            "index": index,
            "username": username,
            "uuid": child["identity"]["uuid"],
            "process_id": child["process_id"],
            "status": child["status"],
            "receipt": str(child_receipt.relative_to(run_root)).replace("\\", "/"),
            "receipt_sha256": sha(child_receipt),
        })
        write_receipt(receipt_path, receipt)

    identities = {row["uuid"] for row in receipt["clients"]}
    require(len(identities) == args.cohort_size, "cohort dummy identities collided")
    expected_status = "running" if args.launch else "prepared"
    require(all(row["status"] == expected_status for row in receipt["clients"]),
            "one or more clients did not reach the expected state")
    receipt["status"] = "running" if args.launch else "prepared"
    receipt["distinct_identity_count"] = len(identities)
    write_receipt(receipt_path, receipt)
    print(receipt_path)
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as failure:  # noqa: BLE001 - create-only runner emits one concise failure
        print(f"MORROW OFFLINE COHORT: FAIL: {failure}", file=sys.stderr)
        raise SystemExit(1)
