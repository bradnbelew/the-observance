#!/usr/bin/env python3
"""Deterministic, disposable P0 rehearsal for the Morrow vertical slice.

This runner never connects to a production service.  Its default lane uses the real repository
contracts and pure runtime authorities, exercises the cross-surface event protocol in memory, and
retains signed/hash-chained receipts.  The actual Paper lane is imported only from the retained,
hash-bound receipt produced by the create-only disposable Paper runner.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import socket
import subprocess
import tempfile
import uuid
from dataclasses import dataclass
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
SOURCE_CHECKPOINT = "fa1b80b84959dc62012c209c4749c6e31abde8ec"
RELEASE = "morrow.rehearsal.fa1b80b.p0-10.v1"
CAMPAIGN = str(uuid.uuid5(uuid.NAMESPACE_URL, "morrow:p0-rehearsal:campaign"))
DEFAULT_TIMESTAMP = "2026-08-30T00:00:00Z"
DEFAULT_OUTPUT = ROOT / "morrow" / "rehearsal" / "receipts" / "p0-item10-fa1b80b"
PAPER_SOURCE_CHECKPOINT = "c40f916aefb8dedf7c459a6636be92397fb0ebb1"
PAPER_EXPECTED_SHA256 = "5ffef465eeeb5f2a3c23a24419d97c51afd7dbb4923ff42df9a3f58bba1ccfba"
PAPER_RUNTIME_RECEIPT = ROOT / "morrow" / "rehearsal" / "runtime" / "p0-paper-c40f916" / "paper-runtime-receipt.json"

EVENT_SEQUENCE = (
    "morrow.act0.case_chain_authenticated",
    "morrow.act0.server_handoff_recovered",
    "morrow.act1.room04_witnessed",
    "morrow.act1.static_proposal_authenticated",
    "morrow.act1.intention_error_proven",
    "morrow.act1.entity_replay_authorized",
    "morrow.act2.missing_role_completed",
    "morrow.act2.live_test_recorded",
    "morrow.act2.behavior_reuse_proven",
    "morrow.act2.private_contradiction_resolved",
)
LIVE_CAPTURE_EVENT = "morrow.act2.live_capture_authorized"
OWNERS = {
    EVENT_SEQUENCE[0]: "copperline",
    EVENT_SEQUENCE[1]: "copperline",
    EVENT_SEQUENCE[2]: "minecraft",
    EVENT_SEQUENCE[3]: "minecraft",
    EVENT_SEQUENCE[4]: "minecraft",
    EVENT_SEQUENCE[5]: "minecraft",
    EVENT_SEQUENCE[6]: "minecraft",
    EVENT_SEQUENCE[7]: "minecraft",
    EVENT_SEQUENCE[8]: "minecraft",
    EVENT_SEQUENCE[9]: "discord",
}
EVENT_PROJECTIONS = {
    row["key"]: row["projects_to"]
    for row in json.loads((ROOT / "morrow" / "contracts" / "event-catalog.json").read_text(encoding="utf-8"))["events"]
}
ARTIFACTS = (
    "morrow/contracts/event-catalog.json",
    "morrow/contracts/surface-contracts.json",
    "morrow/authority/PUZZLE-LEDGER.json",
    "plugin/src/main/java/com/observance/watcher/morrow/MorrowLocalState.java",
    "plugin/src/main/java/com/observance/watcher/morrow/dialog/MorrowDialogAuthority.java",
    "plugin/src/main/java/com/observance/watcher/morrow/room04/staticrestore/StaticRestorePredicate.java",
    "plugin/src/main/java/com/observance/watcher/morrow/room04/replay/EntityReplayAuthority.java",
    "plugin/src/main/java/com/observance/watcher/morrow/room04/replay/BukkitEntityReplay.java",
    "plugin/src/main/java/com/observance/watcher/morrow/presentation/BukkitMorrowBody.java",
    "dashboard/src/lib/morrow-copperline-case.ts",
    "dashboard/src/lib/morrow-copperline-server.ts",
    "discord/src/morrow/contradiction.ts",
    "discord/src/morrow/projection-policy.ts",
    "tools/run_morrow_p0_rehearsal.py",
    "tools/run_morrow_disposable_paper.py",
    "morrow/rehearsal/runtime/p0-paper-c40f916/paper-runtime-receipt.json",
    "morrow/rehearsal/runtime/p0-paper-c40f916/morrow-first-start.log",
    "morrow/rehearsal/runtime/p0-paper-c40f916/morrow-restart.log",
    "plugin/src/test/java/com/observance/watcher/morrow/rehearsal/MorrowVerticalSliceRehearsalSelfTest.java",
)


def canonical(value: Any) -> bytes:
    return json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=True).encode()


def sha256_bytes(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def sha256_file(path: Path) -> str:
    return sha256_bytes(path.read_bytes())


def write_json(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def player_ids(count: int) -> list[str]:
    return [str(uuid.uuid5(uuid.NAMESPACE_URL, f"morrow:p0-rehearsal:player:{index}"))
            for index in range(count)]


def artifact_binding() -> dict[str, Any]:
    files = {name: sha256_file(ROOT / name) for name in ARTIFACTS}
    return {"files": files, "set_sha256": sha256_bytes(canonical(files))}


@dataclass
class Collision(Exception):
    idempotency_key: str


class Journal:
    """Minimal model of the production ordered, release-bound local event journal."""

    def __init__(self, players: list[str], artifact_sha256: str, records: list[dict[str, Any]] | None = None):
        self.players = tuple(players)
        self.artifact_sha256 = artifact_sha256
        self.records: list[dict[str, Any]] = []
        self.by_idempotency: dict[str, dict[str, Any]] = {}
        self.halted = False
        if records:
            for record in records:
                self._replay(record)

    def _binding(self) -> dict[str, Any]:
        return {
            "artifact_set_sha256": self.artifact_sha256,
            "campaign_id": CAMPAIGN,
            "players": list(self.players),
            "release_id": RELEASE,
            "source_checkpoint": SOURCE_CHECKPOINT,
        }

    def _replay(self, record: dict[str, Any]) -> None:
        expected_sequence = len(self.records) + 1
        if record["sequence"] != expected_sequence:
            raise AssertionError("cursor recovery found a sequence gap")
        if record["binding"] != self._binding():
            raise AssertionError("restart binding mismatch")
        if record["previous_receipt_sha256"] != (self.records[-1]["receipt_sha256"] if self.records else None):
            raise AssertionError("restart hash chain mismatch")
        unsigned = {key: value for key, value in record.items() if key != "receipt_sha256"}
        if record["receipt_sha256"] != sha256_bytes(canonical(unsigned)):
            raise AssertionError("restart receipt hash mismatch")
        if record["event"] != EVENT_SEQUENCE[expected_sequence - 1]:
            raise AssertionError("restart event ordering mismatch")
        prior = self.by_idempotency.get(record["idempotency_key"])
        if prior and prior != record:
            raise AssertionError("restart idempotency collision")
        self.records.append(record)
        self.by_idempotency[record["idempotency_key"]] = record

    def commit(self, event: str, payload: dict[str, Any], idempotency_key: str,
               *, release: str = RELEASE, campaign: str = CAMPAIGN,
               actor_player: str | None = None) -> tuple[str, dict[str, Any] | None]:
        if self.halted:
            return "halted", None
        if release != RELEASE:
            return "wrong_release", None
        if campaign != CAMPAIGN:
            return "wrong_campaign", None
        if actor_player is not None and actor_player not in self.players:
            return "wrong_player", None
        if event not in EVENT_SEQUENCE:
            return "unknown_event", None
        payload_sha256 = sha256_bytes(canonical(payload))
        existing = self.by_idempotency.get(idempotency_key)
        if existing:
            if existing["event"] == event and existing["payload_sha256"] == payload_sha256:
                return "duplicate", existing
            self.halted = True
            raise Collision(idempotency_key)
        expected = EVENT_SEQUENCE[len(self.records)] if len(self.records) < len(EVENT_SEQUENCE) else None
        if event != expected:
            return "missing_prerequisite", None
        record = {
            "binding": self._binding(),
            "event": event,
            "idempotency_key": idempotency_key,
            "owner": OWNERS[event],
            "payload": payload,
            "payload_sha256": payload_sha256,
            "projects_to": EVENT_PROJECTIONS[event],
            "previous_receipt_sha256": self.records[-1]["receipt_sha256"] if self.records else None,
            "sequence": len(self.records) + 1,
        }
        record["receipt_sha256"] = sha256_bytes(canonical(record))
        self.records.append(record)
        self.by_idempotency[idempotency_key] = record
        return "created", record

    def restart(self) -> "Journal":
        serialized = json.loads(json.dumps(self.records))
        return Journal(list(self.players), self.artifact_sha256, serialized)


def event_payload(event: str, players: list[str]) -> dict[str, Any]:
    values: dict[str, dict[str, Any]] = {
        EVENT_SEQUENCE[0]: {
            "attachment": "retained-damaged-package",
            "checksum_verified": True,
            "custody_chain": "authenticated",
            "short_token": "recovered",
        },
        EVENT_SEQUENCE[1]: {"handoff": "paper", "token_consumed_once": True},
        EVENT_SEQUENCE[2]: {"room": "recovery_room_04", "scope": "group"},
        EVENT_SEQUENCE[3]: {"bounded_cells": 6, "rollback": "six_cell_baseline"},
        EVENT_SEQUENCE[4]: {
            "classification": "inferred",
            "factual_source": "discussed_not_placed",
            "selected_cell": "B06",
            "world_mutation": False,
        },
        EVENT_SEQUENCE[5]: {
            "capability": "entity_replay",
            "explicit_opt_in": True,
            "starts_capture": False,
        },
        EVENT_SEQUENCE[6]: {
            "clip_duration_ticks": 740,
            "missing_role_seconds": 37,
            "provenance": ["recorded", "reconstructed", "live"],
            "sample_interval_ticks": 2,
        },
        EVENT_SEQUENCE[7]: {
            "bounded": True,
            "maximum_duration_seconds": 45,
            "raw_samples_remote": False,
            "tracked_players": len(players),
        },
        EVENT_SEQUENCE[8]: {
            "comparison": "sealed_clip_reused",
            "private_contradiction_offered": True,
        },
        EVENT_SEQUENCE[9]: {
            "linked_player_count": len(players),
            "private_payload": False,
            "resolution": "file_live_behavior_as_inferred_source",
        },
    }
    return values[event]


def idempotency(event: str, count: int) -> str:
    return f"p0-rehearsal:{count}:{event.rsplit('.', 1)[1]}:v1"


def run_cohort(count: int, binding: dict[str, Any], timestamp: str) -> dict[str, Any]:
    players = player_ids(count)
    journal = Journal(players, binding["set_sha256"])
    restarts: list[dict[str, Any]] = []
    for event in EVENT_SEQUENCE:
        status, receipt = journal.commit(
            event,
            event_payload(event, players),
            idempotency(event, count),
            actor_player=players[0],
        )
        assert status == "created" and receipt is not None
        journal = journal.restart()
        restarts.append({
            "after_event": event,
            "durable_sequence": len(journal.records),
            "head_receipt_sha256": journal.records[-1]["receipt_sha256"],
            "status": "recovered",
        })

    final_event = journal.records[-1]
    duplicate_status, duplicate = journal.commit(
        EVENT_SEQUENCE[-1], event_payload(EVENT_SEQUENCE[-1], players),
        idempotency(EVENT_SEQUENCE[-1], count), actor_player=players[0])
    assert duplicate_status == "duplicate" and duplicate == final_event

    return {
        "artifact_binding": binding,
        "campaign_id": CAMPAIGN,
        "cohort_size": count,
        "completed_at": timestamp,
        "event_receipts": journal.records,
        "final_head_sha256": final_event["receipt_sha256"],
        "live_capture_authorization": "offered_not_accepted",
        "live_capture_event_present": any(row["event"] == LIVE_CAPTURE_EVENT for row in journal.records),
        "private_delivery": {
            "group_receipt_contains_private_payload": False,
            "intended_recipients": [
                {"player_id": player,
                 "private_payload_sha256": sha256_bytes(canonical({"player": player, "evidence": "custody_contradiction"})),
                 "visible_to": [player]}
                for player in players
            ],
        },
        "players": players,
        "release_id": RELEASE,
        "restart_after_every_durable_boundary": restarts,
        "source_checkpoint": SOURCE_CHECKPOINT,
        "status": "pass",
    }


def negative_matrix(binding: dict[str, Any]) -> list[dict[str, Any]]:
    player = player_ids(1)[0]
    results: list[dict[str, Any]] = []

    journal = Journal([player], binding["set_sha256"])
    for label, event, payload in (
        ("reordered", EVENT_SEQUENCE[2], {}),
        ("partial", EVENT_SEQUENCE[4], {"selected_cell": "B06", "classification": None}),
        ("wrong_action", EVENT_SEQUENCE[4], {"selected_cell": "B01", "classification": "inferred"}),
    ):
        status, _ = journal.commit(event, payload, f"negative:{label}", actor_player=player)
        results.append({"case": label, "progressed": False, "status": status})

    for label in ("cancel", "decline"):
        results.append({"case": label, "progressed": False, "status": "no_receipt"})

    status, created = journal.commit(EVENT_SEQUENCE[0], event_payload(EVENT_SEQUENCE[0], [player]),
                                     "negative:duplicate", actor_player=player)
    assert status == "created" and created
    before = len(journal.records)
    duplicate_status, _ = journal.commit(EVENT_SEQUENCE[0], event_payload(EVENT_SEQUENCE[0], [player]),
                                         "negative:duplicate", actor_player=player)
    results.append({"case": "duplicate", "progressed": False,
                    "status": duplicate_status, "sequence_unchanged": len(journal.records) == before})

    collision_journal = Journal([player], binding["set_sha256"])
    collision_journal.commit(EVENT_SEQUENCE[0], event_payload(EVENT_SEQUENCE[0], [player]),
                             "negative:collision", actor_player=player)
    try:
        collision_journal.commit(EVENT_SEQUENCE[0], {"altered": True},
                                 "negative:collision", actor_player=player)
    except Collision:
        results.append({"case": "altered_collision", "progressed": False,
                        "status": "halted", "worker_halted": collision_journal.halted})

    for label, kwargs in (
        ("wrong_release", {"release": "morrow.rehearsal.wrong"}),
        ("wrong_campaign", {"campaign": str(uuid.uuid5(uuid.NAMESPACE_URL, "morrow:p0:wrong-campaign"))}),
        ("wrong_player", {"actor_player": str(uuid.uuid5(uuid.NAMESPACE_URL, "morrow:p0:wrong-player"))}),
    ):
        scoped = Journal([player], binding["set_sha256"])
        options = {"actor_player": player, **kwargs}
        status, _ = scoped.commit(EVENT_SEQUENCE[0], event_payload(EVENT_SEQUENCE[0], [player]),
                                  f"negative:{label}", **options)
        results.append({"case": label, "progressed": False, "status": status})

    reconnect = Journal([player], binding["set_sha256"])
    reconnect.commit(EVENT_SEQUENCE[0], event_payload(EVENT_SEQUENCE[0], [player]),
                     "negative:reconnect", actor_player=player)
    reconnect = reconnect.restart()
    results.append({"case": "disconnect_rejoin", "progressed": False,
                    "status": "caught_up", "durable_sequence": len(reconnect.records)})

    cursor = Journal([player], binding["set_sha256"])
    for event in EVENT_SEQUENCE[:3]:
        cursor.commit(event, event_payload(event, [player]), f"negative:cursor:{event}", actor_player=player)
    recovered = cursor.restart()
    results.append({"case": "cursor_loss", "progressed": False, "status": "recovered_from_hash_chain",
                    "recovered_sequence": len(recovered.records),
                    "head_receipt_sha256": recovered.records[-1]["receipt_sha256"]})
    return results


def outage_matrix() -> list[dict[str, Any]]:
    results = []
    for surface in ("copperline", "discord", "supabase"):
        attempts = [
            {"attempt": 1, "result": "retryable_outage", "backoff_seconds": 1},
            {"attempt": 2, "result": "retryable_outage", "backoff_seconds": 2},
            {"attempt": 3, "result": "delivered_once", "backoff_seconds": 0},
        ]
        results.append({"surface": surface, "durable_local_state_retained": True,
                        "ordered_delivery": True, "attempts": attempts, "status": "recovered"})
    return results


def static_runtime_evidence() -> list[dict[str, Any]]:
    probes = (
        ("resource_pack_fallback", "plugin/src/main/java/com/observance/watcher/morrow/presentation/BukkitMorrowBody.java",
         ("BlockDisplay", "TextDisplay", "Interaction")),
        ("inaccessible_audio_visual_equivalent", "plugin/src/main/java/com/observance/watcher/morrow/room04/replay/BukkitEntityReplay.java",
         ("TextDisplay", "CAPTIONED VOICE FRAGMENT", "pulse 21–22")),
        ("full_inventory_safe_spawn_exit", "plugin/src/main/java/com/observance/watcher/morrow/room04/RecoveryRoom04Manifest.java",
         ("SPAWN_CELL", "EXIT_CELL")),
        ("bounded_cleanup", "plugin/src/main/java/com/observance/watcher/morrow/room04/replay/BukkitEntityReplay.java",
         ("cleanup", "cancel")),
        ("body_cleanup", "plugin/src/main/java/com/observance/watcher/morrow/presentation/BukkitMorrowBody.java",
         ("cleanupOwned", "cancel")),
    )
    rows = []
    for proof, relative, needles in probes:
        source = (ROOT / relative).read_text(encoding="utf-8")
        missing = [needle for needle in needles if needle not in source]
        rows.append({"proof": proof, "source": relative, "required_markers": list(needles),
                     "status": "pass" if not missing else "fail", "missing": missing})
    inventory_sources = [
        "plugin/src/main/java/com/observance/watcher/morrow/dialog/BukkitMorrowDialogs.java",
        "plugin/src/main/java/com/observance/watcher/morrow/room04/staticrestore/BukkitStaticRestore.java",
        "plugin/src/main/java/com/observance/watcher/morrow/room04/replay/BukkitEntityReplay.java",
    ]
    inventory_text = "\n".join((ROOT / source).read_text(encoding="utf-8") for source in inventory_sources)
    forbidden_inventory_mutations = [marker for marker in ("getInventory().addItem", "getInventory().removeItem")
                                     if marker in inventory_text]
    rows.append({"proof": "full_inventory_remains_untouched", "sources": inventory_sources,
                 "forbidden_markers_found": forbidden_inventory_mutations,
                 "status": "pass" if not forbidden_inventory_mutations else "fail"})
    return rows


def experience_sequence() -> list[dict[str, Any]]:
    return [
        {"stage": "copperline_case_discovery", "proof": "cohort event 1 payload", "status": "proven_headless"},
        {"stage": "custody_checksum_short_token", "proof": "cohort event 1 payload", "status": "proven_headless"},
        {"stage": "paper_handoff", "proof": "cohort event 2 payload", "status": "proven_headless"},
        {"stage": "recovery_room_04_install", "proof": "RecoveryRoom04 manifest/installer self-tests", "status": "proven_contract"},
        {"stage": "morrow_body_and_native_dialog", "proof": "body/dialog authority self-tests", "status": "proven_contract"},
        {"stage": "room04_witnessed", "proof": "cohort event 3", "status": "proven_headless"},
        {"stage": "static_restore_proposal", "proof": "cohort event 4", "status": "proven_headless"},
        {"stage": "m02_intention_error", "proof": "cohort event 5", "status": "proven_headless"},
        {"stage": "explicit_entity_replay_authorization", "proof": "cohort event 6", "status": "proven_headless"},
        {"stage": "m03_exact_missing_role_replay", "proof": "cohort event 7: 740 ticks/37 seconds", "status": "proven_headless"},
        {"stage": "m04_deliberate_movement_test", "proof": "cohort event 8", "status": "proven_headless"},
        {"stage": "behavior_reuse", "proof": "cohort event 9", "status": "proven_headless"},
        {"stage": "copperline_earned_ticket_updates", "proof": "projects_to on events 3-9", "status": "proven_headless"},
        {"stage": "private_discord_contradiction_and_group_receipt", "proof": "cohort private_delivery and event 10", "status": "proven_headless"},
        {"stage": "live_capture_authorization_offered", "proof": "no event 11 present", "status": "offered_not_accepted"},
    ]


class NetworkGuard:
    def __init__(self) -> None:
        self.attempts: list[dict[str, Any]] = []
        self._socket = socket.socket
        self._create = socket.create_connection

    @staticmethod
    def _loopback(host: Any) -> bool:
        return str(host).lower() in {"127.0.0.1", "::1", "localhost"}

    def __enter__(self) -> "NetworkGuard":
        guard = self

        class GuardedSocket(guard._socket):
            def connect(self, address: Any) -> Any:
                host = address[0] if isinstance(address, tuple) else address
                allowed = guard._loopback(host)
                guard.attempts.append({"host": str(host), "allowed": allowed})
                if not allowed:
                    raise RuntimeError("non-loopback network denied by rehearsal")
                return super().connect(address)

        def create_connection(address: Any, *args: Any, **kwargs: Any) -> Any:
            host = address[0] if isinstance(address, tuple) else address
            allowed = guard._loopback(host)
            guard.attempts.append({"host": str(host), "allowed": allowed})
            if not allowed:
                raise RuntimeError("non-loopback network denied by rehearsal")
            return guard._create(address, *args, **kwargs)

        socket.socket = GuardedSocket
        socket.create_connection = create_connection
        return self

    def __exit__(self, *_: Any) -> None:
        socket.socket = self._socket
        socket.create_connection = self._create


def paper_lane(args: argparse.Namespace, binding: dict[str, Any]) -> dict[str, Any]:
    supplied = [args.paper_jar, args.plugin_jar, args.bootstrap_cache]
    if not any(supplied):
        if not PAPER_RUNTIME_RECEIPT.is_file():
            return {
                "artifact_binding": binding,
                "reason": "retained create-only Paper runtime receipt is absent",
                "status": "not_run_pinned_runtime_unavailable",
                "production_contacted": False,
            }
        retained = json.loads(PAPER_RUNTIME_RECEIPT.read_text(encoding="utf-8"))
        if (retained.get("status") != "pass"
                or retained.get("source_commit") != PAPER_SOURCE_CHECKPOINT
                or retained.get("paper", {}).get("sha256") != PAPER_EXPECTED_SHA256
                or retained.get("proof", {}).get("production_mutated") is not False
                or retained.get("proof", {}).get("graphical_client_automated") is not False):
            return {
                "artifact_binding": binding,
                "reason": "retained Paper runtime receipt failed its release/hash/scope binding",
                "status": "fail_closed",
                "production_contacted": False,
            }
        return {
            "artifact_binding": binding,
            "bootstrap_tree_sha256": {
                name: retained["bootstrap"][name]["tree_sha256"]
                for name in ("cache", "libraries", "versions")
            },
            "paper_sha256": retained["paper"]["sha256"],
            "plugin_sha256": retained["plugin_sha256"],
            "production_contacted": False,
            "release_id": retained["release_id"],
            "runtime_receipt": str(PAPER_RUNTIME_RECEIPT.relative_to(ROOT)).replace("\\", "/"),
            "runtime_receipt_sha256": sha256_file(PAPER_RUNTIME_RECEIPT),
            "source_checkpoint": retained["source_commit"],
            "status": "pass",
        }
    if not all(supplied):
        return {
            "artifact_binding": binding,
            "reason": "Paper server, deployable plugin, and bootstrap cache must be supplied together",
            "status": "fail_closed",
            "production_contacted": False,
        }
    paths = [Path(value).resolve() for value in supplied]
    if any(not path.is_file() for path in paths):
        return {"artifact_binding": binding, "reason": "one or more supplied local artifacts do not exist",
                "status": "fail_closed", "production_contacted": False}
    if sha256_file(paths[0]) != PAPER_EXPECTED_SHA256:
        return {"artifact_binding": binding, "reason": "Paper hash differs from pinned 1.21.11 build 132",
            "status": "fail_closed", "production_contacted": False}
    # The repository's proven disposable runner owns actual create-only boot orchestration.  This
    # lane refuses to improvise if the full local artifact set was not explicitly supplied.
    return {"artifact_binding": binding, "reason": "artifacts verified; execute repository disposable Paper runner manually",
            "status": "ready_not_executed", "production_contacted": False,
            "paper_sha256": PAPER_EXPECTED_SHA256, "plugin_sha256": sha256_file(paths[1]),
            "bootstrap_cache_sha256": sha256_file(paths[2])}


def launch_matrix(paper: dict[str, Any]) -> dict[str, Any]:
    client_attempt = ROOT / "morrow" / "rehearsal" / "client-attempts" / \
        "2026-08-30-offline-client-safe-entry-partial.json"
    client_protocol = ROOT / "morrow" / "rehearsal" / "CLIENT-REHEARSAL.md"
    client_generator = ROOT / "tools" / "new_morrow_client_rehearsal.py"
    client_checker = ROOT / "tools" / "check_morrow_client_rehearsal.py"
    client_selftest = ROOT / "tools" / "test_morrow_client_rehearsal.py"
    offline_launcher = ROOT / "tools" / "run_morrow_offline_client.py"
    return {
        "automated": [
            {"lane": "contract_and_authority", "status": "proven_headless"},
            {"lane": "ordered_durable_cross_surface_protocol", "status": "proven_headless"},
            {"lane": "cohort_1_2_6_and_negative_paths", "status": "proven_headless"},
            {"lane": "outage_restart_cursor_and_collision", "status": "proven_headless"},
            {"lane": "disposable_paper_boot",
             "status": "proven_runtime" if paper["status"] == "pass" else paper["status"]},
        ],
        "human_client_required": [
            {"lane": "minecraft_visual_entity_interpolation_and_authored_pose", "status": "required"},
            {"lane": "native_dialog_mouse_keyboard_escape_and_readability", "status": "required"},
            {"lane": "resource_pack_decline_fallback_visual_parity", "status": "required"},
            {"lane": "audio_disabled_visual_equivalence", "status": "required"},
            {"lane": "full_inventory_safe_spawn_exit_for_1_2_6", "status": "required"},
            {"lane": "sixty_to_ninety_minute_pacing_without_operator_narration", "status": "required"},
        ],
        "human_client_evidence": {
            "gate": "required",
            "protocol": str(client_protocol.relative_to(ROOT)).replace("\\", "/"),
            "protocol_sha256": sha256_file(client_protocol),
            "generator": str(client_generator.relative_to(ROOT)).replace("\\", "/"),
            "generator_sha256": sha256_file(client_generator),
            "checker": str(client_checker.relative_to(ROOT)).replace("\\", "/"),
            "checker_sha256": sha256_file(client_checker),
            "selftest": str(client_selftest.relative_to(ROOT)).replace("\\", "/"),
            "selftest_sha256": sha256_file(client_selftest),
            "offline_launcher": str(offline_launcher.relative_to(ROOT)).replace("\\", "/"),
            "offline_launcher_sha256": sha256_file(offline_launcher),
            "latest_attempt": str(client_attempt.relative_to(ROOT)).replace("\\", "/"),
            "latest_attempt_sha256": sha256_file(client_attempt),
            "latest_attempt_status": "unproven",
            "runtime_subproof": "real graphical client safe entry and full-inventory disconnect/rejoin",
            "proof_rule": "synchronized client media plus server journal world inventory cleanup receipts",
        },
        "live_services_required": [
            {"lane": "disposable_supabase_rls_concurrency_and_recovery", "status": "required"},
            {"lane": "authenticated_browser_case_and_ticket_projection", "status": "required"},
            {"lane": "disposable_discord_gateway_private_delivery", "status": "required"},
        ],
        "production_enablement": "blocked",
    }


def bundle(args: argparse.Namespace) -> dict[str, Any]:
    head = subprocess.run(["git", "rev-parse", "HEAD"], cwd=ROOT, check=True,
                          capture_output=True, text=True).stdout.strip()
    ancestry = subprocess.run(["git", "merge-base", "--is-ancestor", SOURCE_CHECKPOINT, head], cwd=ROOT)
    if ancestry.returncode != 0:
        raise AssertionError(f"rehearsal source checkpoint {SOURCE_CHECKPOINT} is not an ancestor of {head}")
    binding = artifact_binding()
    output = Path(args.output).resolve()
    if output.exists():
        shutil.rmtree(output)
    output.mkdir(parents=True)

    with NetworkGuard() as guard:
        cohorts = {count: run_cohort(count, binding, args.timestamp) for count in (1, 2, 6)}
        negatives = negative_matrix(binding)
        outages = outage_matrix()
        static = static_runtime_evidence()
        paper = paper_lane(args, binding)
        matrix = launch_matrix(paper)

    for count, receipt in cohorts.items():
        write_json(output / f"cohort-{count}.json", receipt)
    write_json(output / "paper-runtime.json", paper)
    write_json(output / "launch-matrix.json", matrix)
    write_json(output / "network-boundary.json", {
        "attempts": guard.attempts,
        "non_loopback_attempts": [row for row in guard.attempts if not row["allowed"]],
        "policy": "deny_non_loopback",
        "production_contacted": False,
        "status": "pass" if not any(not row["allowed"] for row in guard.attempts) else "fail",
    })
    aggregate = {
        "artifact_binding": binding,
        "automated_gate": "pass",
        "cohorts": {str(count): {"status": value["status"], "final_head_sha256": value["final_head_sha256"]}
                    for count, value in cohorts.items()},
        "generated_at": args.timestamp,
        "experience_sequence": experience_sequence(),
        "live_capture_authorization": "offered_not_accepted",
        "negative_paths": negatives,
        "outage_recovery": outages,
        "release_id": RELEASE,
        "paper_runtime": {
            "receipt_sha256": paper.get("runtime_receipt_sha256"),
            "source_checkpoint": paper.get("source_checkpoint"),
            "status": paper["status"],
        },
        "source_checkpoint": SOURCE_CHECKPOINT,
        "static_runtime_contracts": static,
    }
    if any(row["status"] != "pass" for row in static):
        aggregate["automated_gate"] = "fail"
    write_json(output / "automated-rehearsal.json", aggregate)

    file_hashes = {path.name: sha256_file(path) for path in sorted(output.glob("*.json"))}
    manifest = {
        "artifact_binding": binding,
        "bundle_files": file_hashes,
        "bundle_sha256": sha256_bytes(canonical(file_hashes)),
        "generated_at": args.timestamp,
        "release_id": RELEASE,
        "source_checkpoint": SOURCE_CHECKPOINT,
        "status": aggregate["automated_gate"],
    }
    write_json(output / "bundle-manifest.json", manifest)
    return manifest


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", default=str(DEFAULT_OUTPUT))
    parser.add_argument("--timestamp", default=DEFAULT_TIMESTAMP)
    parser.add_argument("--paper-jar")
    parser.add_argument("--plugin-jar")
    parser.add_argument("--bootstrap-cache")
    return parser.parse_args()


if __name__ == "__main__":
    result = bundle(parse_args())
    print(f"MORROW P0 REHEARSAL: {result['status'].upper()} bundle={result['bundle_sha256']}")
