#!/usr/bin/env python3
"""Fail closed over the latest-plugin d738788 disposable Paper lifecycle."""

from __future__ import annotations

import hashlib
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
EVIDENCE = ROOT / "morrow/rehearsal/runtime/d738788-current"
RECEIPT = EVIDENCE / "paper-runtime-receipt.json"
SOURCE = "d7387880d8fe2e2d74235160fab2acd109bccca9"
PLUGIN_SHA256 = "ea2cbdfd56eca83580c1dcf71722c3391f85e556c3ea4c25f4cc69561e0bf342"
PAPER_SHA256 = "5ffef465eeeb5f2a3c23a24419d97c51afd7dbb4923ff42df9a3f58bba1ccfba"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit(f"MORROW LATEST-PLUGIN PAPER: FAIL {message}")


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


require(RECEIPT.is_file(), "receipt missing")
receipt = json.loads(RECEIPT.read_text(encoding="utf-8"))
require(receipt.get("status") == "pass", "runtime status")
require(receipt.get("source_commit") == SOURCE, "source commit")
require(receipt.get("plugin_sha256") == PLUGIN_SHA256, "plugin hash")
paper = receipt.get("paper", {})
require(
    paper.get("version") == "1.21.11"
    and paper.get("build") == 132
    and paper.get("sha256") == PAPER_SHA256,
    "Paper identity",
)
server = receipt.get("server", {})
require(
    server.get("bind") == "127.0.0.1"
    and server.get("port") == 25598
    and server.get("online_mode") is False
    and server.get("production_credentials_loaded") is False
    and server.get("jvm_non_loopback_proxy") == "127.0.0.1:1",
    "disposable server boundary",
)

first_log = EVIDENCE / "morrow-first-start.log"
restart_log = EVIDENCE / "morrow-restart.log"
require(first_log.is_file() and restart_log.is_file(), "lifecycle logs missing")
logs = receipt.get("logs", {})
require(sha256(first_log) == logs.get("first_sha256"), "first-start log hash")
require(sha256(restart_log) == logs.get("restart_sha256"), "restart log hash")
first_text = first_log.read_text(encoding="utf-8", errors="replace")
restart_text = restart_log.read_text(encoding="utf-8", errors="replace")
for marker in (
    "MORROW_M05_READY",
    "MORROW_M06_READY",
    "MORROW_M07_READY",
    "MORROW_M08_READY",
    "MORROW_M09_READY",
    "MORROW_M10_READY",
    "MORROW_M11_READY",
    "MORROW_M12_READY",
    "MORROW_RUNTIME_READY",
    "MORROW_RUNTIME_CLOSED entities_and_tasks=cleaned",
):
    require(marker in first_text, f"first-start log omitted {marker}")
require("copper_aging_repairs=0" in first_text, "first-start copper repair disclosure")
for marker in (
    "status=ALREADY_PRESENT",
    "MORROW_RUNTIME_READY",
    "MORROW_RUNTIME_CLOSED entities_and_tasks=cleaned",
):
    require(marker in restart_text, f"restart log omitted {marker}")
require("copper_aging_repairs=0" in restart_text, "restart copper repair disclosure")

projection = receipt.get("projection", {})
attempts = projection.get("attempts", [])
require(
    [row.get("response_status") for row in attempts] == [503, 503, 200]
    and all(row.get("signature_valid") is True for row in attempts)
    and projection.get("no_restart_redelivery") is True,
    "projector outage/recovery sequence",
)
proof = receipt.get("proof", {})
require(
    proof.get("exact_paper_match") is True
    and proof.get("restart_already_present_audit") is True
    and proof.get("entity_counts_stable_across_restart") is True
    and proof.get("graceful_cleanup_logged") is True
    and proof.get("production_mutated") is False
    and proof.get("graphical_client_automated") is False,
    "proof boundaries",
)

print(
    "MORROW LATEST-PLUGIN PAPER: PASS "
    "source=d738788 paper=1.21.11-132 restart=stable projection=503/503/200"
)
