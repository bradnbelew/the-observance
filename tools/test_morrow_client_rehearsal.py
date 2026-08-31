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

from check_morrow_client_rehearsal import (
    INVESTIGATION_CONTRACTS,
    INVESTIGATION_IDS,
    LANES,
    PATH_PROOFS,
    REQUIRED_EVENTS,
    validate,
)


ROOT = Path(__file__).resolve().parents[1]
PAPER_RUNTIME_RECEIPT = (
    ROOT / "morrow" / "rehearsal" / "runtime" / "fedac89-current"
    / "paper-runtime-receipt.json"
)


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
         "--paper-runtime-receipt", str(PAPER_RUNTIME_RECEIPT),
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

    for index, investigation_id in enumerate(INVESTIGATION_IDS):
        contract = INVESTIGATION_CONTRACTS[investigation_id]
        data["investigations"][investigation_id] = {
            "title": contract["title"],
            "status": "pass",
            "surfaces_observed": contract["surfaces"],
            "path_proofs": {name: True for name in PATH_PROOFS},
            "observer_finding": {
                "observer_id": "synthetic-observer",
                "verdict": "pass",
            },
            "files": [
                row(
                    run, f"media/investigation-{investigation_id.lower()}.png",
                    b"synthetic-investigation-png-" + str(index).encode(),
                ),
                row(
                    run, f"receipts/investigation-{investigation_id.lower()}.json",
                    json.dumps(
                        {"synthetic": True, "investigation": investigation_id},
                        sort_keys=True,
                    ).encode() + b"\n",
                ),
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
            "completed_investigations": list(INVESTIGATION_IDS),
            "ending": {
                1: "certify",
                2: "preserve_audit",
                6: "create_new_branch",
            }[size],
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
        "complete_server_log": row(
            run, "server/final.log", b"MORROW_RUNTIME_READY\nsynthetic final server log\n",
        ),
        "complete_journal": row(run, "journal/final.journal", journal()),
        "final_world_readback": row(run, "world/final.json", b'{"synthetic":true}\n'),
        "clean_shutdown_log": row(run, "server/final-shutdown.log", b"MORROW_RUNTIME_CLOSED\n"),
    }
    (run / "client-rehearsal.json").write_text(
        json.dumps(data, indent=2, sort_keys=True) + "\n", encoding="utf-8",
    )
    return run


def main() -> int:
    # Use the host temp root rather than the repository build directory. On
    # sandboxed Windows, mkdir can be denied while os.access still reports the
    # parent writable, causing tempfile.mkdtemp to retry every candidate name.
    with tempfile.TemporaryDirectory(prefix="morrow-client-selftest-") as temporary:
        run = make_fixture(Path(temporary))
        validate(run)

        manifest = run / "client-rehearsal.json"
        data = json.loads(manifest.read_text(encoding="utf-8"))
        passed = json.loads(json.dumps(data))

        del data["investigations"]["M12"]
        manifest.write_text(json.dumps(data, indent=2, sort_keys=True) + "\n", encoding="utf-8")
        try:
            validate(run)
        except AssertionError as failure:
            assert "investigation set/order" in str(failure)
        else:
            raise AssertionError("M01-M11-only evidence unexpectedly passed")

        data = json.loads(json.dumps(passed))
        reused = data["lanes"][sorted(LANES)[0]]["files"][0]
        data["lanes"][sorted(LANES)[1]]["files"][0] = reused
        manifest.write_text(json.dumps(data, indent=2, sort_keys=True) + "\n", encoding="utf-8")
        try:
            validate(run)
        except AssertionError as failure:
            assert "reused evidence" in str(failure)
        else:
            raise AssertionError("reused evidence unexpectedly passed")

    print(
        "MORROW CLIENT REHEARSAL SELFTEST: PASS "
        "m01-m12=required positive=1 omitted-investigation=refused reused-evidence=refused"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
