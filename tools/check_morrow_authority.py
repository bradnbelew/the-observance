#!/usr/bin/env python3
"""Fail-closed structural audit for the Morrow reboot authority."""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MORROW = ROOT / "morrow"
PLUGIN_MORROW = ROOT / "plugin" / "src" / "main" / "java" / "com" / "observance" / "watcher" / "morrow"
DASHBOARD_MORROW_ENVELOPE = ROOT / "dashboard" / "src" / "lib" / "morrow-runtime-envelope.ts"
DASHBOARD_MORROW_ROUTE = ROOT / "dashboard" / "src" / "app" / "api" / "runtime" / "minecraft" / "events" / "route.ts"
PLUGIN_BODY_AUTHORITY = PLUGIN_MORROW / "presentation" / "MorrowBodyAuthority.java"
PLUGIN_BODY_RUNTIME = PLUGIN_MORROW / "presentation" / "BukkitMorrowBody.java"
PLUGIN_DIALOG_AUTHORITY = PLUGIN_MORROW / "dialog" / "MorrowDialogAuthority.java"
PLUGIN_DIALOG_RUNTIME = PLUGIN_MORROW / "dialog" / "BukkitMorrowDialogs.java"
PLUGIN_STATIC_RESTORE = PLUGIN_MORROW / "room04" / "staticrestore"
PLUGIN_ENTITY_REPLAY = PLUGIN_MORROW / "room04" / "replay"


def load_json(relative: str):
    path = MORROW / relative
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except Exception as exc:  # noqa: BLE001 - audit must report malformed authority
        raise AssertionError(f"cannot parse {relative}: {exc}") from exc


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def main() -> int:
    errors: list[str] = []

    try:
        states = load_json("contracts/relationship-states.json")
        state_rows = states["states"]
        state_keys = [row["key"] for row in state_rows]
        require(
            state_keys == ["helpful", "curious", "intimate", "possessive", "afraid", "negotiated"],
            "relationship states are missing or out of canonical order",
        )
        require(states["initial_state"] == "helpful", "initial relationship state must be helpful")
        require(state_rows[-1]["next"] is None, "negotiated state must be terminal")
        for index, row in enumerate(state_rows[:-1]):
            require(row["next"] == state_rows[index + 1]["key"], f"broken state transition at {row['key']}")
    except Exception as exc:  # noqa: BLE001
        errors.append(str(exc))

    try:
        replay_authority = (PLUGIN_ENTITY_REPLAY / "EntityReplayAuthority.java").read_text(encoding="utf-8")
        replay_consent = (PLUGIN_ENTITY_REPLAY / "EntityReplayConsentStore.java").read_text(encoding="utf-8")
        replay_clips = (PLUGIN_ENTITY_REPLAY / "EntityReplayClipStore.java").read_text(encoding="utf-8")
        replay_recorder = (PLUGIN_ENTITY_REPLAY / "EntityReplayRecorder.java").read_text(encoding="utf-8")
        replay_bukkit = (PLUGIN_ENTITY_REPLAY / "BukkitEntityReplay.java").read_text(encoding="utf-8")
        ledger = load_json("authority/PUZZLE-LEDGER.json")
        m03 = next(row for row in ledger["investigations"] if row["id"] == "M03")
        m04 = next(row for row in ledger["investigations"] if row["id"] == "M04")
        require(m03["input"] == "A player stands in the missing position and performs the evidenced inventory action during the loop.",
                "canonical M03 concrete input drifted")
        require(m03["required_evidence"] == ["partial avatar loop", "redstone pulse log", "inventory transfer", "captioned voice fragment"],
                "canonical M03 evidence order drifted")
        require(m04["input"] == "Perform any distinctive movement/action sequence inside the marked test area and seal the clip.",
                "canonical M04 concrete input drifted")
        require(m04["failure_behavior"] == "Leaving the boundary cancels and deletes the unsealed clip.",
                "canonical M04 boundary failure drifted")
        for required in (
            "SAMPLE_INTERVAL_TICKS = 2", "MAXIMUM_DURATION_TICKS = 900",
            "MAXIMUM_SAMPLES_PER_PLAYER = 450", "MAXIMUM_TRACKED_PLAYERS = 6",
            "MISSING_ROLE_DURATION_TICKS = 740", "MISSING_ROLE_SAMPLE_COUNT = 370",
            "raw_samples_remote\\\":false", "MISSING_ROLE_COMPLETED", "LIVE_TEST_RECORDED",
            "BEHAVIOR_REUSE_PROVEN",
        ):
            require(required in replay_authority, f"M03/M04 replay authority lacks {required}")
        for source, required in (
            (replay_consent, "consentHash"), (replay_consent, "ATOMIC_MOVE"),
            (replay_clips, "clipHash"), (replay_recorder, "sameConsent"),
            (replay_recorder, "MAXIMUM_TRACKED_PLAYERS"),
        ):
            require(required in source, f"M03/M04 durable local pipeline lacks {required}")
        for required in (
            "BlockDisplay", "TextDisplay", "Interaction", "setTeleportDuration(2)",
            "Provenance.RECORDED", "Provenance.RECONSTRUCTED", "Provenance.LIVE",
            "PlayerQuitEvent", "catchUpFor", "requirePrimaryThread", "morrow_replay_expires",
        ):
            require(required in replay_bukkit, f"M03/M04 Paper loop lacks {required}")
        for forbidden in (
            "setBlockData(", "setType(", "runTaskAsynchronously", "net.minecraft",
            "craftbukkit", "GameProfile", "PlayerInfo",
        ):
            require(forbidden not in replay_bukkit,
                    f"M03/M04 replay crossed its display-only/main-thread boundary via {forbidden}")
    except Exception as exc:  # noqa: BLE001
        errors.append(str(exc))

    try:
        body_authority = PLUGIN_BODY_AUTHORITY.read_text(encoding="utf-8")
        body_runtime = PLUGIN_BODY_RUNTIME.read_text(encoding="utf-8")
        expected_pose_order = ["IDLE", "ATTENDING", "MIRRORING", "RETAINING", "FRACTURED", "NEGOTIATED"]
        declared_poses = re.findall(r'^\s+([A-Z]+)\("[a-z]+",\s*(?:true|false),', body_authority, re.MULTILINE)
        require(declared_poses == expected_pose_order, "Morrow fallback body pose order drifted")
        expected_stage_poses = {
            "HELPFUL": "IDLE",
            "CURIOUS": "ATTENDING",
            "INTIMATE": "MIRRORING",
            "POSSESSIVE": "RETAINING",
            "AFRAID": "FRACTURED",
            "NEGOTIATED": "NEGOTIATED",
        }
        mapped_stage_poses = dict(re.findall(r'MorrowStage[.]([A-Z]+), Pose[.]([A-Z]+)', body_authority))
        require(mapped_stage_poses == expected_stage_poses, "relationship stage/body pose mapping drifted")
        for required in (
            "BlockDisplay", "TextDisplay", "Interaction", "PersistentDataType",
            "cleanupOwned()", "MAXIMUM_LIFETIME_SECONDS", "trackingTick()",
        ):
            require(required in body_runtime or required in body_authority,
                    f"Morrow fallback body lacks {required}")
        for forbidden in ("net.minecraft", "craftbukkit", "FakePlayer", "ServerPlayer"):
            require(forbidden not in body_runtime, f"Morrow fallback body depends on forbidden {forbidden}")
    except Exception as exc:  # noqa: BLE001
        errors.append(str(exc))

    try:
        dialog_authority = PLUGIN_DIALOG_AUTHORITY.read_text(encoding="utf-8")
        dialog_runtime = PLUGIN_DIALOG_RUNTIME.read_text(encoding="utf-8")
        receipt_calls = set(re.findall(
            r'receipt\(\s*(ROOM_WITNESSED|PROPOSAL_AUTHENTICATED|ENTITY_REPLAY_AUTHORIZED),',
            dialog_authority,
        ))
        require(receipt_calls == {"ROOM_WITNESSED", "PROPOSAL_AUTHENTICATED", "ENTITY_REPLAY_AUTHORIZED"},
                "Room 04 dialog receipt authority drifted")
        require('case AUTHORIZE_ENTITY_REPLAY -> has(snapshot, INTENTION_ERROR_PROVEN)' in dialog_authority,
                "Entity Replay dialog lost its proof prerequisite")
        require('\\"world_mutation\\":\\"bounded_static_restore\\"' in dialog_authority
                and '\\"bounded_cells\\":6' in dialog_authority
                and '\\"rollback\\":\\"six_cell_baseline\\"' in dialog_authority,
                "M02 proposal lost its exact bounded mutation/rollback authority")
        require('\\"world_mutation\\":false' in dialog_authority,
                "Entity Replay authorization must still forbid world mutation")
        for required in (
            "Dialog.create", "canCloseWithEscape(true)", "PlayerCustomClickEvent",
            "DialogType.confirmation", "DialogType.multiAction", "MorrowLocalState.CommitResult",
            "PendingClassification", "PlayerJoinEvent", "PlayerQuitEvent",
        ):
            require(required in dialog_runtime, f"Morrow native dialog adapter lacks {required}")
        for forbidden in ("setBlockData(", "setType(", "breakNaturally(", "runTaskAsynchronously"):
            require(forbidden not in dialog_runtime,
                    f"dialog adapter directly owns a forbidden world mutation via {forbidden}")
    except Exception as exc:  # noqa: BLE001
        errors.append(str(exc))

    try:
        static_manifest = (PLUGIN_STATIC_RESTORE / "StaticRestoreManifest.java").read_text(encoding="utf-8")
        static_predicate = (PLUGIN_STATIC_RESTORE / "StaticRestorePredicate.java").read_text(encoding="utf-8")
        static_engine = (PLUGIN_STATIC_RESTORE / "StaticRestoreEngine.java").read_text(encoding="utf-8")
        static_bukkit = (PLUGIN_STATIC_RESTORE / "BukkitStaticRestore.java").read_text(encoding="utf-8")
        ledger = load_json("authority/PUZZLE-LEDGER.json")
        m02 = next(row for row in ledger["investigations"] if row["id"] == "M02")
        require(m02["input"] ==
                "Mark the one unsupported restored block in Minecraft and classify it as authenticated, inferred, conflicting, or unknown.",
                "canonical M02 concrete input drifted")
        require(m02["required_evidence"] ==
                ["current starter room", "archived screenshot", "block manifest", "Finch planning post"],
                "canonical M02 evidence order drifted")
        require('MANIFEST_SHA256 = "870d27b4b21539e496577eff68dc5f26ada71ec321fef9629088e9a8274b28ff"'
                in static_manifest, "M02 exact manifest/content hash drifted")
        candidate_rows = re.findall(r'add\(authored, CandidateId[.](B0[1-6]), .*?Provenance[.]([A-Z]+),',
                                    static_manifest, re.DOTALL)
        require(candidate_rows == [
            ("B01", "AUTHENTICATED"), ("B02", "AUTHENTICATED"),
            ("B03", "AUTHENTICATED"), ("B04", "AUTHENTICATED"),
            ("B05", "AUTHENTICATED"), ("B06", "INFERRED"),
        ], "B06 must remain the sole factual inference error")
        require("CURRENT_ROOM, ARCHIVED_SCREENSHOT, BLOCK_MANIFEST, FINCH_PLANNING_POST" in static_manifest
                and "not placed" in static_manifest,
                "M02 four-source evidence/content authority drifted")
        require("selected == CandidateId.B06 && classification == Provenance.INFERRED" in static_predicate
                and "MorrowDialogAuthority.INTENTION_ERROR_PROVEN" in static_predicate
                and '\\"world_mutation\\":false' in static_predicate,
                "M02 physical predicate no longer exclusively proves B06 as inferred")
        for required in ("applyTransaction", "auditBaseline", "auditComplete", "reset(", "restore("):
            require(required in static_engine, f"M02 transactional engine lacks {required}")
        for required in ("PASS_INTERVAL_TICKS", "spawnParticle", "playSound", "PersistentDataType",
                         "resetAndReplay", "requirePrimaryThread"):
            require(required in static_bukkit, f"M02 Paper theater lacks {required}")
        for forbidden in ("PlayerMoveEvent", "runTaskAsynchronously", "net.minecraft", "craftbukkit",
                          "sendBlockChange"):
            require(forbidden not in static_bukkit, f"P0 item 6 crossed the replay/client boundary via {forbidden}")
    except Exception as exc:  # noqa: BLE001
        errors.append(str(exc))

    try:
        catalog = load_json("contracts/event-catalog.json")
        events = catalog["events"]
        event_keys = [event["key"] for event in events]
        require(len(event_keys) == len(set(event_keys)), "duplicate event key")
        require(len(events) >= 18, "event catalog is too small for the eight-act authority")
        for event in events:
            require(re.fullmatch(r"morrow\.act[0-7]\.[a-z0-9_]+", event["key"]) is not None,
                    f"invalid event key {event['key']}")
            require(event["owner"] in catalog["surfaces"], f"invalid owner for {event['key']}")
            require(event["owner"] in event["projects_to"], f"owner projection missing for {event['key']}")
            require(set(event["projects_to"]) <= set(catalog["surfaces"]),
                    f"invalid projection surface for {event['key']}")
        require({event["act"] for event in events} == set(range(8)), "every act must own at least one event")
    except Exception as exc:  # noqa: BLE001
        errors.append(str(exc))

    try:
        transition_source = (PLUGIN_MORROW / "MorrowTransitionService.java").read_text(encoding="utf-8")
        capability_source = (PLUGIN_MORROW / "MorrowCapability.java").read_text(encoding="utf-8")
        transition_event_keys = set(re.findall(r'"(morrow\.act[0-7]\.[a-z0-9_]+)"', transition_source))
        require(transition_event_keys, "plugin transition service has no canonical event requirements")
        require(
            transition_event_keys <= set(event_keys),
            "plugin transition service references events absent from event-catalog.json: "
            + ", ".join(sorted(transition_event_keys - set(event_keys))),
        )
        java_capabilities = set(re.findall(r'^[ \t]+[A-Z][A-Z0-9_]+\("([a-z0-9_]+)"\)[,;]$', capability_source, re.MULTILINE))
        contract_capabilities = {state["capability"] for state in state_rows}
        require(
            java_capabilities == contract_capabilities,
            "Java capability enum differs from relationship-states.json",
        )

        envelope_source = DASHBOARD_MORROW_ENVELOPE.read_text(encoding="utf-8")
        envelope_event_keys = set(re.findall(r"'(morrow\.act[0-7]\.[a-z0-9_]+)'", envelope_source))
        minecraft_owned_keys = {event["key"] for event in events if event["owner"] == "minecraft"}
        require(
            envelope_event_keys == minecraft_owned_keys,
            "dashboard Minecraft ingest allow-list differs from event-catalog.json",
        )
        paper_authority_source = (PLUGIN_MORROW / "MorrowEventAuthority.java").read_text(encoding="utf-8")
        paper_definitions = {
            match.group(1): (match.group(2), match.group(3))
            for match in re.finditer(
                r'event\("(morrow\.act[0-7]\.[a-z0-9_]+)",\s*"([a-z]+)",\s*(?:null|"(morrow\.act[0-7]\.[a-z0-9_]+)"\))',
                paper_authority_source,
            )
        }
        paper_event_keys = {key for key, (owner, _) in paper_definitions.items() if owner == "minecraft"}
        require(
            paper_event_keys == minecraft_owned_keys,
            "Paper event authority differs from event-catalog.json",
        )
        require(
            {key: owner for key, (owner, _) in paper_definitions.items()}
            == {event["key"]: event["owner"] for event in events},
            "Paper event ownership differs from event-catalog.json",
        )
    except Exception as exc:  # noqa: BLE001
        errors.append(str(exc))

    try:
        ledger = load_json("authority/PUZZLE-LEDGER.json")
        investigations = ledger["investigations"]
        ids = [item["id"] for item in investigations]
        require(ids == [f"M{number:02d}" for number in range(1, 13)], "investigation IDs must be M01-M12")
        grammars: list[str] = []
        for item in investigations:
            require(len(item["required_evidence"]) >= 3, f"{item['id']} has fewer than three evidence sources")
            require(len(item["surfaces"]) >= 2, f"{item['id']} is not cross-surface")
            require(item["accessible_equivalent"].strip(), f"{item['id']} lacks accessibility equivalent")
            require(item["failure_behavior"].strip(), f"{item['id']} lacks recoverable failure behavior")
            require(item["callback"].strip() and item["payoff"].strip(), f"{item['id']} lacks callback/payoff")
            require(len(item["truths"]) >= 2, f"{item['id']} proves too little")
            require("sentence" not in item["input"].lower(), f"{item['id']} appears to require a sentence answer")
            grammars.append(item["mechanics"][0])
        for index in range(len(grammars) - 2):
            require(len(set(grammars[index:index + 3])) > 1,
                    f"three consecutive investigations repeat grammar at {ids[index]}")
    except Exception as exc:  # noqa: BLE001
        errors.append(str(exc))

    try:
        surfaces = load_json("contracts/surface-contracts.json")["surfaces"]
        require(set(surfaces) == {"minecraft", "copperline", "discord", "media", "director"},
                "surface contract set drifted")
        require("physical world state" in surfaces["minecraft"]["owns"], "Minecraft lost physical authority")
        require("custody history" in surfaces["copperline"]["owns"], "Copperline lost custody authority")
    except Exception as exc:  # noqa: BLE001
        errors.append(str(exc))

    required_files = [
        "README.md",
        "authority/CANON.md",
        "authority/PLAYER-JOURNEY.md",
        "architecture/SYSTEM.md",
        "architecture/DATABASE.md",
        "architecture/MINECRAFT-UX.md",
        "architecture/CAPABILITY-MATRIX.md",
        "db/schema-proposal.sql",
    ]
    for relative in required_files:
        if not (MORROW / relative).is_file():
            errors.append(f"missing authority file: {relative}")

    stale_terms = re.compile(r"\b(?:Averyn|Wren|Noland|Keeper|Unlit|Deep Hold)\b", re.IGNORECASE)
    for path in MORROW.rglob("*"):
        if not path.is_file() or path.name == "README.md":
            continue
        if path.suffix.lower() not in {".md", ".json", ".sql"}:
            continue
        match = stale_terms.search(path.read_text(encoding="utf-8"))
        if match:
            errors.append(f"legacy lore token {match.group(0)!r} leaked into {path.relative_to(MORROW)}")

    try:
        schema = (MORROW / "db/schema-proposal.sql").read_text(encoding="utf-8").lower()
        route = DASHBOARD_MORROW_ROUTE.read_text(encoding="utf-8")
        plugin_config = (ROOT / "plugin" / "src" / "main" / "resources" / "config.yml").read_text(encoding="utf-8")
        require("security invoker" in schema, "Minecraft ingest RPC must remain SECURITY INVOKER")
        require("to service_role" in schema, "Minecraft ingest RPC lacks service_role-only grant")
        require("from public, anon, authenticated" in schema, "Minecraft ingest RPC lacks public revocation")
        require("MORROW_MINECRAFT_INGEST_SECRET" in route, "runtime route lacks dedicated ingest secret")
        require("SUPABASE_SERVICE_ROLE_KEY" in route, "runtime route lacks server-only database credential")
        require("NEXT_PUBLIC_SUPABASE_SERVICE" not in route, "service role credential became public")
        require(re.search(r"morrow-reboot:\s*\n\s+enabled: false", plugin_config) is not None,
                "Morrow reboot must ship disabled")
        seed_section = schema.split("insert into morrow_private.event_definitions", 1)[1].split("on conflict", 1)[0]
        seeded = {
            match.group(1): (match.group(2), match.group(3) or None, set(match.group(4).split(",")))
            for match in re.finditer(
                r"\('(morrow[.]act[0-7][.][a-z0-9_]+)',\s*[0-7],\s*'([a-z]+)',\s*'[{]([^}]*)[}]',\s*'[{]([^}]+)[}]'\)",
                seed_section,
            )
        }
        require(set(seeded) == set(event_keys), "database event seed differs from event-catalog.json")
        for event in events:
            owner, prerequisite, projections = seeded[event["key"]]
            require(owner == event["owner"], f"database owner drift for {event['key']}")
            require(projections == set(event["projects_to"]), f"database projections drift for {event['key']}")
            require(prerequisite == paper_definitions[event["key"]][1],
                    f"database/Paper prerequisite drift for {event['key']}")
    except Exception as exc:  # noqa: BLE001
        errors.append(str(exc))

    if errors:
        for error in errors:
            print(f"MORROW AUTHORITY FAIL: {error}", file=sys.stderr)
        return 1

    print("MORROW AUTHORITY: PASS")
    print(f"relationship_states={len(state_rows)} events={len(events)} investigations={len(investigations)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
