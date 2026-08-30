#!/usr/bin/env python3
"""Dependency-free positive and tamper self-tests for the Morrow client receipt gate."""

from __future__ import annotations

import base64
import hashlib
import json
import subprocess
import tempfile
from datetime import datetime, timedelta, timezone
from pathlib import Path

from check_morrow_client_rehearsal import LANES, REQUIRED_EVENTS, validate


ROOT = Path(__file__).resolve().parents[1]


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def row(run: Path, relative: str, content: bytes) -> dict[str, object]:
    path = run / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(content)
    return {"file": relative, "bytes": len(content), "sha256": sha(path)}


def journal() -> bytes:
    previous = "0" * 64
    lines: list[str] = []
    events = ["morrow_release_initialized", *sorted(REQUIRED_EVENTS)]
    for sequence, event in enumerate(events, 1):
        key = f"synthetic:{sequence}"
        payload = json.dumps({"synthetic": True, "event": event}, sort_keys=True).encode()
        payload_hash = hashlib.sha256(payload).hexdigest()
        material = f"{sequence}\n{previous}\n{key}\n{event}\n{payload_hash}".encode()
        event_hash = hashlib.sha256(material).hexdigest()
        encoded = base64.urlsafe_b64encode(payload).decode().rstrip("=")
        lines.append(f"{sequence}\t{previous}\t{event_hash}\t{key}\t{event}\t{encoded}")
        previous = event_hash
    return ("\n".join(lines) + "\n").encode()


def make_fixture(root: Path) -> Path:
    run = root / "synthetic-pass"
    result = subprocess.run(
        ["python", str(ROOT / "tools" / "new_morrow_client_rehearsal.py"),
         "--run-id", run.name, "--output-root", str(root),
         "--operator-id", "synthetic-operator", "--observer-id", "synthetic-observer"],
        cwd=ROOT, check=True, capture_output=True, text=True,
    )
    assert result.stdout.strip() == str(run)
    data = json.loads((run / "client-rehearsal.json").read_text(encoding="utf-8"))
    now = datetime.now(timezone.utc).replace(microsecond=0)
    data.update({
        "status": "pass",
        "started_at": (now - timedelta(minutes=70)).isoformat().replace("+00:00", "Z"),
        "ended_at": now.isoformat().replace("+00:00", "Z"),
        "operator_narration_used": False,
    })

    for index, name in enumerate(sorted(LANES)):
        data["lanes"][name] = {
            "status": "pass",
            "observer_finding": {"observer_id": "synthetic-observer", "verdict": "pass"},
            "files": [
                row(run, f"media/lane-{index}.png", b"synthetic-png-" + str(index).encode()),
                row(run, f"receipts/lane-{index}.json", b'{"synthetic":true}\n'),
            ],
        }

    for cohort in data["cohorts"]:
        size = cohort["size"]
        players = [f"synthetic-{size}-{index}" for index in range(size)]
        inventory = (json.dumps({
            "inventory_sha256": hashlib.sha256(f"inventory-{size}".encode()).hexdigest(),
            "players": players,
        }, sort_keys=True) + "\n").encode()
        cohort.update({
            "status": "pass",
            "players": players,
            "duration_seconds": 4200,
            "operator_interventions": [],
            "disconnect_rejoin_proven": True,
            "full_inventory_proven": True,
            "safe_spawn_exit_proven": True,
            "cleanup_proven": True,
            "evidence": {
                "server_log": row(run, f"server/cohort-{size}.log",
                                  b"MORROW_RUNTIME_READY\nMORROW_RUNTIME_CLOSED\n"),
                "journal": row(run, f"journal/cohort-{size}.journal", journal()),
                "world_readback": row(run, f"world/cohort-{size}.json", (json.dumps({
                    "occupied_cell_conflicts": 0,
                    "room04_manifest_sha256":
                        "9831893bf03387b6c59b3835b648f056aafd67e6e194f3ade0282a7192fff41a",
                    "safe_spawn_exit": True,
                }, sort_keys=True) + "\n").encode()),
                "inventory_before": row(run, f"inventory/cohort-{size}-before.json", inventory),
                "inventory_after": row(run, f"inventory/cohort-{size}-after.json", inventory),
                "cleanup": row(run, f"receipts/cohort-{size}-cleanup.json",
                               b'{"leaked_entities":0,"leaked_tasks":0,"status":"pass"}\n'),
                "media": [row(run, f"media/cohort-{size}.mp4", b"synthetic-video")],
            },
        })

    data["final_receipts"] = {
        "complete_server_log": row(run, "server/final.log", b"synthetic final server log\n"),
        "complete_journal": row(run, "journal/final.journal", journal()),
        "final_world_readback": row(run, "world/final.json", b'{"synthetic":true}\n'),
        "clean_shutdown_log": row(run, "server/final-shutdown.log", b"MORROW_RUNTIME_CLOSED\n"),
    }
    (run / "client-rehearsal.json").write_text(
        json.dumps(data, indent=2, sort_keys=True) + "\n", encoding="utf-8",
    )
    return run


def main() -> int:
    with tempfile.TemporaryDirectory(prefix="morrow-client-selftest-") as temporary:
        run = make_fixture(Path(temporary))
        validate(run)

        manifest = run / "client-rehearsal.json"
        data = json.loads(manifest.read_text(encoding="utf-8"))
        reused = data["lanes"][sorted(LANES)[0]]["files"][0]
        data["lanes"][sorted(LANES)[1]]["files"][0] = reused
        manifest.write_text(json.dumps(data, indent=2, sort_keys=True) + "\n", encoding="utf-8")
        try:
            validate(run)
        except AssertionError as failure:
            assert "reused evidence" in str(failure)
        else:
            raise AssertionError("reused evidence unexpectedly passed")

    print("MORROW CLIENT REHEARSAL SELFTEST: PASS positive=1 reused-evidence=refused")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
