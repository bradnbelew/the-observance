#!/usr/bin/env python3
"""Fail-closed validator for the retained Morrow P0 item 10 rehearsal bundle."""

from __future__ import annotations

import hashlib
import json
import re
import subprocess
import sys
import tempfile
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
RECEIPTS = ROOT / "morrow" / "rehearsal" / "receipts" / "p0-item10-fa1b80b"
SOURCE_CHECKPOINT = "fa1b80b84959dc62012c209c4749c6e31abde8ec"
RELEASE = "morrow.rehearsal.fa1b80b.p0-10.v1"
PAPER_SOURCE_CHECKPOINT = "c40f916aefb8dedf7c459a6636be92397fb0ebb1"
PAPER_EXPECTED_SHA256 = "5ffef465eeeb5f2a3c23a24419d97c51afd7dbb4923ff42df9a3f58bba1ccfba"
PAPER_RUNTIME = ROOT / "morrow" / "rehearsal" / "runtime" / "p0-paper-c40f916"
SEQUENCE = [
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
]


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def validate_client_visual_checkpoint(client: dict[str, Any]) -> dict[str, Any]:
    checkpoint = load(ROOT / client["latest_visual_checkpoint"])
    require(client["visual_checkpoint_status"] == "bounded_visual_checkpoint_pass"
            and checkpoint["status"] == client["visual_checkpoint_status"],
            "exact-window visual checkpoint status drifted")
    require(client["visual_checkpoint_scope"]
            == "joined Room 04 readability plus one native terminal-dialog activation",
            "exact-window visual checkpoint scope drifted")
    require(checkpoint["schema_version"] == "1.0.0-morrow-exact-window-visual-checkpoint",
            "exact-window visual checkpoint schema drifted")
    require(checkpoint["boundary"]["loopback_only"] is True
            and checkpoint["boundary"]["server"] == "127.0.0.1:25592"
            and checkpoint["boundary"]["identity_kind"] == "dummy_offline"
            and checkpoint["boundary"]["account_files_read"] is False
            and checkpoint["boundary"]["production_credentials_loaded"] is False
            and checkpoint["boundary"]["production_contacted"] is False,
            "exact-window visual checkpoint crossed its disposable boundary")
    require(checkpoint["selection"]["exact_match_count"] == 1
            and checkpoint["selection"]["launcher_window_excluded"] is True
            and checkpoint["selection"]["exact_title"]
            == "Minecraft 1.21.11 - Multiplayer (3rd-party Server)"
            and checkpoint["no_blind_input"] is True,
            "exact-window selection or input provenance drifted")
    require(checkpoint["source"]["capture_helper"] == client["capture_fallback"]
            and checkpoint["source"]["capture_helper_sha256"]
            == client["capture_fallback_sha256"],
            "visual checkpoint is not bound to the retained capture fallback")

    artifacts = checkpoint["artifacts"]
    for name in ("launcher_receipt", "server_log"):
        row = artifacts[name]
        require(sha(ROOT / row["path"]) == row["sha256"],
                f"visual checkpoint artifact drifted: {name}")
    captures: dict[str, tuple[dict[str, Any], dict[str, Any]]] = {}
    for name in ("before", "corrected_room", "native_dialog"):
        row = artifacts[name]
        image_path = ROOT / row["image"]
        capture_path = ROOT / row["capture_receipt"]
        require(sha(image_path) == row["image_sha256"],
                f"visual checkpoint image drifted: {name}")
        require(sha(capture_path) == row["capture_receipt_sha256"],
                f"visual checkpoint capture receipt drifted: {name}")
        capture = load(capture_path)
        require(capture["status"] == "captured"
                and capture["input_injected"] is False
                and capture["window"]["title"] == checkpoint["selection"]["exact_title"]
                and capture["window"]["process_image"] == checkpoint["selection"]["process_image"]
                and capture["image"]["sha256"] == row["image_sha256"]
                and capture["image"]["width"] == 1282
                and capture["image"]["height"] == 752
                and capture["image"]["grayscale_entropy"] >= 1.0,
                f"visual checkpoint capture metadata failed: {name}")
        captures[name] = (row, capture)
    require(captures["corrected_room"][1]["window"]["pid"]
            == checkpoint["selection"]["final_process_id"]
            == captures["native_dialog"][1]["window"]["pid"]
            and captures["corrected_room"][1]["window"]["hwnd"]
            == checkpoint["selection"]["final_window_id"]
            == captures["native_dialog"][1]["window"]["hwnd"],
            "corrected room and native dialog were not captured from one exact client window")
    require(captures["before"][0]["image_sha256"]
            != captures["corrected_room"][0]["image_sha256"],
            "before/after visual evidence was reused")

    launcher = load(ROOT / artifacts["launcher_receipt"]["path"])
    require(launcher["schema_version"] == "1.0.0-morrow-offline-client-launch"
            and launcher["server"] == checkpoint["boundary"]["server"]
            and launcher["loopback_only"] is True
            and launcher["identity"]["kind"] == "dummy_offline"
            and launcher["process_id"] == checkpoint["selection"]["final_process_id"]
            and launcher["account_files_read"] is False
            and launcher["production_credentials_loaded"] is False,
            "visual checkpoint launcher provenance drifted")
    server_log = (ROOT / artifacts["server_log"]["path"]).read_text(encoding="utf-8")
    for marker in (
        "Starting Minecraft server on 127.0.0.1:25592",
        "MORROW_RUNTIME_READY",
        "MORROW_PLAYER_SAFE_ENTRY player=MorrowWitness",
        "MORROW_RUNTIME_CLOSED entities_and_tasks=cleaned",
    ):
        require(marker in server_log, f"visual checkpoint server log omitted: {marker}")
    proof = checkpoint["proof_scope"]
    require(proof["overall_human_client_gate"] == "required"
            and proof["production_enablement"] == "blocked"
            and len(proof["not_proven"]) == 6,
            "bounded visual checkpoint overclaimed the overall human-client gate")
    require(checkpoint["findings"]["server_join_observed"] is True
            and checkpoint["findings"]["server_clean_shutdown_observed"] is True
            and checkpoint["findings"]["runtime_closed_without_owned_entity_or_task_leak"] is True,
            "bounded visual checkpoint findings drifted")
    return checkpoint


def validate_paper_runtime() -> dict[str, Any]:
    receipt_path = PAPER_RUNTIME / "paper-runtime-receipt.json"
    receipt = load(receipt_path)
    require(receipt["schema_version"] == "1.0.0-morrow-disposable-paper-runtime",
            "Paper runtime receipt schema drifted")
    require(receipt["status"] == "pass", "actual Paper runtime did not pass")
    require(receipt["source_commit"] == PAPER_SOURCE_CHECKPOINT,
            "actual Paper runtime source binding drifted")
    require(receipt["scope"] == "create-only loopback disposable Paper; never production",
            "actual Paper runtime scope drifted")
    require(receipt["paper"] == {
        "build": 132,
        "selected_source": "D:\\the-observance\\build\\paper-smoke\\paper.jar",
        "sha256": PAPER_EXPECTED_SHA256,
        "version": "1.21.11",
    }, "Paper build selection drifted")
    candidates = receipt["paper_candidates"]
    require(len(candidates) == 18, "Paper candidate inventory count drifted")
    require(all(row["exact_build_132_match"] and row["sha256"] == PAPER_EXPECTED_SHA256
                and row["bytes"] == 54_846_016 for row in candidates),
            "a retained Paper candidate did not match pinned build 132")

    bootstrap = receipt["bootstrap"]
    require(bootstrap["cache"]["file_count"] == 1
            and bootstrap["libraries"]["file_count"] == 114
            and bootstrap["versions"]["file_count"] == 1,
            "copied bootstrap inventory count drifted")
    require(bootstrap["cache"]["tree_sha256"] ==
            "9a50d89744678d4abf38331ae89fa2c507fbec3bf739f1ce8783863fc289f50a",
            "copied Paper cache tree drifted")
    require(bootstrap["libraries"]["tree_sha256"] ==
            "0885507c46c6eb2df137b152d38bfdce18a035507302480570af25e5eeee1d5a",
            "copied Paper libraries tree drifted")
    require(bootstrap["versions"]["tree_sha256"] ==
            "1e9b7e14f276cd183ed3f134f2a1b46652a4d15aff9573ea35501a543c445c4a",
            "copied Paper versions tree drifted")

    require(receipt["runner_sha256"] == sha(ROOT / "tools" / "run_morrow_disposable_paper.py"),
            "actual Paper runner changed after its retained run")
    require(receipt["logs"]["first_sha256"] == sha(PAPER_RUNTIME / "morrow-first-start.log")
            and receipt["logs"]["restart_sha256"] == sha(PAPER_RUNTIME / "morrow-restart.log"),
            "actual Paper log hash drifted")
    attempts = receipt["projection"]["attempts"]
    require([row["response_status"] for row in attempts] == [503, 503, 200],
            "Paper projector outage/recovery sequence drifted")
    require(all(row["signature_valid"]
                and row["campaign_id"] == receipt["campaign_id"]
                and row["release_id"] == receipt["release_id"] for row in attempts),
            "Paper projector signature or release binding failed")
    require(receipt["projection"]["no_restart_redelivery"],
            "Paper restart redelivered an acknowledged event")
    runtime = receipt["runtime"]
    expected_entities = {"body": 2, "entity_replay": 6, "static_restore": 26}
    require(runtime["first_entities"] == expected_entities
            and runtime["restart_entities"] == expected_entities,
            "owned Paper entity counts were not stable across restart")
    require("status=BUILT" in runtime["first_room"]
            and "status=ALREADY_PRESENT" in runtime["restart_room"],
            "Room 04 install/readback lifecycle drifted")
    proof = receipt["proof"]
    expected_true = {
        "bootstrap_copy_hash_preserved", "cursor_prevented_restart_duplicate",
        "entity_counts_stable_across_restart", "exact_paper_match",
        "graceful_cleanup_logged", "listeners_closed", "no_bootstrap_download_or_mutation",
        "projector_outage_recovery", "restart_already_present_audit",
        "room04_built_and_readback_audited",
    }
    require(all(proof[name] is True for name in expected_true), "an actual Paper proof flag is false")
    require(proof["graphical_client_automated"] is False
            and proof["production_mutated"] is False,
            "actual Paper receipt overclaimed scope")
    return receipt


def validate() -> None:
    actual_paper = validate_paper_runtime()
    required = {
        "automated-rehearsal.json", "bundle-manifest.json", "cohort-1.json", "cohort-2.json",
        "cohort-6.json", "launch-matrix.json", "network-boundary.json", "paper-runtime.json",
    }
    require(RECEIPTS.is_dir(), "retained rehearsal receipt directory is missing")
    present = {path.name for path in RECEIPTS.glob("*.json")}
    require(present == required, f"receipt file set drifted: {sorted(present ^ required)}")

    manifest = load(RECEIPTS / "bundle-manifest.json")
    require(manifest["status"] == "pass", "automated bundle is not green")
    require(manifest["source_checkpoint"] == SOURCE_CHECKPOINT, "source checkpoint binding drifted")
    require(manifest["release_id"] == RELEASE, "release binding drifted")
    for name, expected in manifest["bundle_files"].items():
        require(sha(RECEIPTS / name) == expected, f"retained receipt hash mismatch: {name}")
    canonical_hashes = json.dumps(manifest["bundle_files"], sort_keys=True, separators=(",", ":")).encode()
    require(hashlib.sha256(canonical_hashes).hexdigest() == manifest["bundle_sha256"],
            "bundle aggregate hash mismatch")
    artifact = manifest["artifact_binding"]
    for relative, expected in artifact["files"].items():
        require(sha(ROOT / relative) == expected, f"bound artifact changed after rehearsal: {relative}")

    for count in (1, 2, 6):
        cohort = load(RECEIPTS / f"cohort-{count}.json")
        require(cohort["status"] == "pass" and cohort["cohort_size"] == count,
                f"cohort {count} did not pass")
        require(cohort["source_checkpoint"] == SOURCE_CHECKPOINT and cohort["release_id"] == RELEASE,
                f"cohort {count} binding mismatch")
        require(len(cohort["players"]) == count and len(set(cohort["players"])) == count,
                f"cohort {count} identity isolation failed")
        receipts = cohort["event_receipts"]
        require([row["event"] for row in receipts] == SEQUENCE, f"cohort {count} event order drifted")
        require([row["sequence"] for row in receipts] == list(range(1, 11)),
                f"cohort {count} sequence cursor drifted")
        require(len(cohort["restart_after_every_durable_boundary"]) == len(SEQUENCE),
                f"cohort {count} lacks a restart at every durable boundary")
        require(not cohort["live_capture_event_present"]
                and cohort["live_capture_authorization"] == "offered_not_accepted",
                f"cohort {count} silently accepted later capture")
        private = cohort["private_delivery"]
        require(not private["group_receipt_contains_private_payload"]
                and len(private["intended_recipients"]) == count,
                f"cohort {count} private recipient aggregation drifted")
        require(all(row["visible_to"] == [row["player_id"]] for row in private["intended_recipients"]),
                f"cohort {count} private evidence crossed player ownership")

    automated = load(RECEIPTS / "automated-rehearsal.json")
    require(automated["automated_gate"] == "pass", "aggregate automated gate failed")
    sequence = automated["experience_sequence"]
    require(len(sequence) == 15 and sequence[-1]["status"] == "offered_not_accepted",
            "complete vertical-slice experience sequence drifted")
    negative = {row["case"]: row for row in automated["negative_paths"]}
    expected_negative = {
        "reordered", "partial", "wrong_action", "cancel", "decline", "duplicate",
        "altered_collision", "wrong_release", "wrong_campaign", "wrong_player",
        "disconnect_rejoin", "cursor_loss",
    }
    require(set(negative) == expected_negative, "negative path coverage drifted")
    require(all(not row["progressed"] for row in negative.values()), "a negative path progressed")
    require(negative["altered_collision"]["worker_halted"], "collision did not halt")
    require(negative["duplicate"]["sequence_unchanged"], "duplicate changed the cursor")
    require({row["surface"] for row in automated["outage_recovery"]}
            == {"copperline", "discord", "supabase"}, "outage surface coverage drifted")
    require(all(row["status"] == "recovered" and row["ordered_delivery"]
                for row in automated["outage_recovery"]), "outage recovery is not ordered")
    require(all(row["status"] == "pass" for row in automated["static_runtime_contracts"]),
            "a static runtime contract failed")

    network = load(RECEIPTS / "network-boundary.json")
    require(network["status"] == "pass" and not network["non_loopback_attempts"]
            and not network["production_contacted"], "network boundary was crossed")
    paper = load(RECEIPTS / "paper-runtime.json")
    require(paper["status"] == "pass", "retained actual Paper lane is not green")
    require(paper["runtime_receipt_sha256"] == sha(PAPER_RUNTIME / "paper-runtime-receipt.json")
            and paper["paper_sha256"] == PAPER_EXPECTED_SHA256
            and paper["plugin_sha256"] == actual_paper["plugin_sha256"],
            "headless bundle is not bound to the actual Paper receipt")
    matrix = load(RECEIPTS / "launch-matrix.json")
    require(matrix["production_enablement"] == "blocked", "rehearsal opened production enablement")
    require(len(matrix["human_client_required"]) == 6
            and all(row["status"] == "required" for row in matrix["human_client_required"]),
            "client-only evidence was misrepresented as automated proof")
    client = matrix["human_client_evidence"]
    require(client["gate"] == "required" and client["latest_attempt_status"] == "unproven",
            "client evidence gate was silently advanced")
    for key in (
        "protocol", "generator", "checker", "selftest", "offline_launcher",
        "offline_cohort_preparer", "offline_cohort_checker", "offline_cohort_receipt",
        "latest_attempt",
        "latest_capture_retry", "capture_fallback", "latest_visual_checkpoint",
    ):
        path = ROOT / client[key]
        require(path.is_file() and client[f"{key}_sha256"] == sha(path),
                f"client evidence artifact drifted: {key}")
    cohort_result = subprocess.run(
        [sys.executable, str(ROOT / client["offline_cohort_checker"])],
        cwd=ROOT, capture_output=True, text=True,
    )
    require(cohort_result.returncode == 0,
            f"offline cohort preparation receipt failed: {cohort_result.stderr.strip()}")
    attempt = load(ROOT / client["latest_attempt"])
    require(attempt["client_lane_status"] == "unproven"
            and attempt["no_blind_input"] is True
            and attempt["production_contacted"] is False
            and attempt["server"]["clean_shutdown"] is True
            and attempt["graphical_client"]["connected"] is True
            and attempt["server"]["no_suffocation_during_bounded_runs"] is True
            and attempt["server"]["disconnect_rejoin_proven"] is True
            and attempt["server"]["full_main_inventory_items_before_disconnect"] == 2304
            and attempt["server"]["full_main_inventory_items_after_rejoin"] == 2304
            and attempt["server"]["join_to_exact_position"] == [0.5, 80.0, -1.5],
            "failed client attempt was misrepresented")
    capture_retry = load(ROOT / client["latest_capture_retry"])
    require(client["capture_retry_result"]
            == "java_permission_did_not_resolve_windows_capture_interface_error"
            and capture_retry["result"] == client["capture_retry_result"]
            and capture_retry["computer_use"]["java_permission_granted_before_retry"] is True
            and capture_retry["computer_use"]["client_window_exact_match_count"] == 1
            and capture_retry["computer_use"]["launcher_window_excluded"] is True
            and capture_retry["computer_use"]["screen_capture_available"] is False
            and capture_retry["host_capture_compatibility"]["host_current_build"] == 19045
            and capture_retry["host_capture_compatibility"]["documented_api_minimum_build"] == 20348
            and capture_retry["no_blind_input"] is True
            and capture_retry["production_contacted"] is False,
            "post-permission Java capture retry was omitted or overclaimed")
    validate_client_visual_checkpoint(client)
    paper_lane = next(row for row in matrix["automated"] if row["lane"] == "disposable_paper_boot")
    require(paper_lane["status"] == "proven_runtime", "launch matrix omits actual Paper proof")
    database_lane = next(row for row in matrix["live_services_required"]
                         if row["lane"] == "disposable_supabase_rls_concurrency_and_recovery")
    database_receipt_path = ROOT / database_lane["receipt"]
    require(database_lane["status"] == "proven_isolated"
            and database_receipt_path.is_file()
            and database_lane["receipt_sha256"] == sha(database_receipt_path),
            "launch matrix omits or drifts isolated Supabase proof")
    cron_receipt_path = ROOT / database_lane["scheduler_receipt"]
    require(cron_receipt_path.is_file()
            and database_lane["scheduler_receipt_sha256"] == sha(cron_receipt_path),
            "launch matrix omits or drifts database-native scheduler proof")
    database_receipt = load(database_receipt_path)
    require(database_receipt["status"] == "pass"
            and database_receipt["project"]["production_contacted"] is False
            and database_receipt["project"]["final_state"] == "paused"
            and database_receipt["rollback"]["private_schema_restored"] is True
            and database_receipt["rollback"]["public_projection_restored"] is True
            and database_receipt["copperline_projection_worker"]["database_rpc_live_rehearsed"] is True
            and database_receipt["copperline_projection_worker"]["ordered_events_applied"] == 9
            and database_receipt["copperline_projection_worker"]["linked_players_projected"] == 2
            and database_receipt["copperline_projection_worker"]["other_player_receipt_rows"] == 0
            and database_receipt["copperline_projection_worker"]["raw_payload_leaks"] == 0
            and database_receipt["advisors"]["morrow_unindexed_foreign_key_count"] == 0
            and database_receipt["production_enablement"] == "blocked",
            "isolated Supabase receipt overclaims or is incomplete")
    cron_receipt = load(cron_receipt_path)
    require(cron_receipt["status"] == "pass"
            and cron_receipt["project"]["production_contacted"] is False
            and cron_receipt["project"]["final_state"] == "paused"
            and cron_receipt["scheduler"]["schedule_created"] is True
            and cron_receipt["scheduler"]["schedule_removed"] is True
            and cron_receipt["scheduler"]["active_jobs_after_disable"] == 0
            and cron_receipt["automatic_success"]["applied_events"] == 9
            and cron_receipt["automatic_success"]["raw_payload_leaks"] == 0
            and cron_receipt["automatic_failure_and_recovery"]["retry_applied"] is True
            and cron_receipt["production_enablement"] == "blocked",
            "database-native scheduler receipt overclaims or is incomplete")
    browser_lane = next(row for row in matrix["live_services_required"]
                        if row["lane"] == "authenticated_browser_case_and_ticket_projection")
    browser_receipt_path = ROOT / browser_lane["partial_receipt"]
    require(browser_lane["status"] == "required"
            and browser_receipt_path.is_file()
            and browser_lane["partial_receipt_sha256"] == sha(browser_receipt_path),
            "launch matrix omitted or overclaimed partial authenticated-browser evidence")
    browser_receipt = load(browser_receipt_path)
    require(browser_receipt["status"] == "partial_pkce_callback_fix_unverified"
            and browser_receipt["account_enumeration"]["browser_form_exercised"] is True
            and browser_receipt["account_enumeration"]["direct_case_after_unknown_request"] == "withheld"
            and browser_receipt["account_enumeration"]["should_create_user"] is False
            and browser_receipt["account_enumeration"]["synthetic_unknown_auth_users_after_request"] == 0
            and browser_receipt["account_enumeration"]["synthetic_unknown_auth_identities_after_request"] == 0
            and browser_receipt["account_enumeration"]["synthetic_unknown_auth_sessions_after_request"] == 0
            and browser_receipt["account_enumeration"]["synthetic_unknown_refresh_tokens_after_request"] == 0
            and browser_receipt["pkce_callback_rehearsal"]["otp_request_status"] == 200
            and browser_receipt["pkce_callback_rehearsal"]["provider_mail_send_logged"] is True
            and browser_receipt["pkce_callback_rehearsal"]["actual_disposable_inbox_link_received"] is True
            and browser_receipt["pkce_callback_rehearsal"]["provider_verify_status"] == 303
            and browser_receipt["pkce_callback_rehearsal"]["callback_exchange_error_code"]
                == "pkce_code_verifier_not_found"
            and browser_receipt["pkce_callback_rehearsal"]["fixed_request_route_live_callback_retest"] is False
            and browser_receipt["pkce_callback_rehearsal"]["final_owner_only_case_read_from_magic_link"] is False
            and browser_receipt["privacy"]["anonymous_case_material_withheld"] is True
            and browser_receipt["privacy"]["other_player_projection_leaked"] is False
            and browser_receipt["act0_browser_flow"]["wrong_handoff_token_event_rows"] == 0
            and browser_receipt["ticket_projection"]["rendered_update_count"] == 6
            and browser_receipt["ticket_projection"]["automatic_database_projector_proven"] is True
            and browser_receipt["ticket_projection"]["primary_projection_transport"]
                == "database_native_supabase_cron"
            and browser_receipt["ticket_projection"]["javascript_service_role_transport_live_run"] is False
            and browser_receipt["production_enablement"] == "blocked",
            "partial authenticated-browser receipt overclaims or is incomplete")
    discord_lane = next(row for row in matrix["live_services_required"]
                        if row["lane"] == "disposable_discord_gateway_private_delivery")
    discord_receipt_path = ROOT / discord_lane["partial_receipt"]
    require(discord_lane["status"] == "required"
            and discord_receipt_path.is_file()
            and discord_lane["partial_receipt_sha256"] == sha(discord_receipt_path),
            "launch matrix omitted or overclaimed partial Discord Gateway evidence")
    discord_receipt = load(discord_receipt_path)
    require(discord_receipt["status"] == "partial_database_worker_binding"
            and discord_receipt["guild"]["disposable_guild"] is False
            and discord_receipt["guild"]["production_player_channel_contacted"] is False
            and discord_receipt["runtime"]["gateway_message_create_observed"] is True
            and discord_receipt["runtime"]["rest_message_fetch_verified"] is True
            and discord_receipt["runtime"]["enforce_nonce"] is True
            and discord_receipt["runtime"]["allowed_mentions_parse"] == []
            and discord_receipt["runtime"]["mentioned_users"] == 0
            and discord_receipt["runtime"]["mentioned_roles"] == 0
            and discord_receipt["runtime"]["mentioned_everyone"] is False
            and discord_receipt["cleanup"]["temporary_channel_absent"] is True
            and discord_receipt["boundaries"]["production_morrow_enabled"] is False
            and discord_receipt["boundaries"]["production_database_contacted"] is False
            and discord_receipt["boundaries"]["private_player_evidence_sent"] is False
            and discord_receipt["production_enablement"] == "blocked",
            "partial Discord Gateway receipt overclaims or is incomplete")
    for artifact in discord_receipt["artifacts"].values():
        artifact_path = ROOT / artifact["path"]
        require(artifact_path.is_file() and artifact["sha256"] == sha(artifact_path),
                f"Discord Gateway rehearsal artifact drifted: {artifact['path']}")

    # Test the exact checked-in bundle for accidental carryover without writing the retired names here.
    retired = ["hold", "keep" + "er", "aver" + "yn", "wr" + "en", "nol" + "and",
               "deep " + "hold", "un" + "lit"]
    authored = "\n".join(
        path.read_text(encoding="utf-8").lower()
        for path in sorted((ROOT / "morrow" / "rehearsal").rglob("*"))
        if path.is_file() and path.suffix.lower() in {".json", ".log", ".md", ".tsv", ".txt"}
    )
    require(not any(re.search(rf"(?<![a-z]){re.escape(term)}(?![a-z])", authored) for term in retired),
            "retired-canon text leaked into rehearsal artifacts")

    with tempfile.TemporaryDirectory(prefix="morrow-p0-check-") as temporary:
        regenerated = Path(temporary) / "receipts"
        result = subprocess.run(
            [sys.executable, str(ROOT / "tools" / "run_morrow_p0_rehearsal.py"),
             "--output", str(regenerated), "--timestamp", "2026-08-30T00:00:00Z"],
            cwd=ROOT, capture_output=True, text=True,
        )
        require(result.returncode == 0, f"deterministic rehearsal rerun failed: {result.stderr}")
        regenerated_files = {path.name: path.read_bytes() for path in regenerated.glob("*.json")}
        retained_files = {path.name: path.read_bytes() for path in RECEIPTS.glob("*.json")}
        require(regenerated_files == retained_files, "retained receipts are not deterministic")


def main() -> int:
    try:
        validate()
    except Exception as failure:  # noqa: BLE001 - audit emits one concise failing receipt
        print(f"MORROW P0 REHEARSAL CHECK: FAIL: {failure}", file=sys.stderr)
        return 1
    print("MORROW P0 REHEARSAL CHECK: PASS cohorts=1/2/6 boundaries=10 network=loopback-only client=required")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
