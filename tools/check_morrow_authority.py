#!/usr/bin/env python3
"""Fail-closed structural audit for the Morrow reboot authority."""

from __future__ import annotations

import hashlib
import json
import re
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MORROW = ROOT / "morrow"
PLUGIN_MORROW = ROOT / "plugin" / "src" / "main" / "java" / "com" / "observance" / "watcher" / "morrow"
DASHBOARD_MORROW_ENVELOPE = ROOT / "dashboard" / "src" / "lib" / "morrow-runtime-envelope.ts"
DASHBOARD_MORROW_ROUTE = ROOT / "dashboard" / "src" / "app" / "api" / "runtime" / "minecraft" / "events" / "route.ts"
DASHBOARD_COPPERLINE_CASE = ROOT / "dashboard" / "src" / "lib" / "morrow-copperline-case.ts"
DASHBOARD_COPPERLINE_SERVER = ROOT / "dashboard" / "src" / "lib" / "morrow-copperline-server.ts"
DASHBOARD_COPPERLINE_ROUTE = ROOT / "dashboard" / "src" / "app" / "support" / "cases" / "mossfield-recovery" / "page.tsx"
DASHBOARD_COPPERLINE_ACTION = ROOT / "dashboard" / "src" / "app" / "support" / "cases" / "mossfield-recovery" / "actions.ts"
DISCORD_MORROW_DOMAIN = ROOT / "discord" / "src" / "morrow" / "contradiction.ts"
DISCORD_MORROW_REPO = ROOT / "discord" / "src" / "morrow" / "repo.ts"
DISCORD_MORROW_POLICY = ROOT / "discord" / "src" / "morrow" / "projection-policy.ts"
DISCORD_MORROW_WORKER = ROOT / "discord" / "src" / "morrow" / "projection-worker.ts"
DISCORD_MORROW_HANDLER = ROOT / "discord" / "src" / "bot" / "commands" / "morrow.ts"
PLUGIN_BODY_AUTHORITY = PLUGIN_MORROW / "presentation" / "MorrowBodyAuthority.java"
PLUGIN_BODY_RUNTIME = PLUGIN_MORROW / "presentation" / "BukkitMorrowBody.java"
PLUGIN_DIALOG_AUTHORITY = PLUGIN_MORROW / "dialog" / "MorrowDialogAuthority.java"
PLUGIN_DIALOG_RUNTIME = PLUGIN_MORROW / "dialog" / "BukkitMorrowDialogs.java"
PLUGIN_STATIC_RESTORE = PLUGIN_MORROW / "room04" / "staticrestore"
PLUGIN_ENTITY_REPLAY = PLUGIN_MORROW / "room04" / "replay"
MORROW_MIGRATION = ROOT / "supabase" / "migrations" / "20260830200157_morrow_reboot_foundation.sql"
MORROW_DATABASE_RECEIPT = MORROW / "rehearsal" / "database" / "2026-08-30-isolated-supabase.json"
MORROW_BROWSER_RECEIPT = MORROW / "rehearsal" / "browser" / "2026-08-30-authenticated-copperline.json"


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

    try:
        case_model = DASHBOARD_COPPERLINE_CASE.read_text(encoding="utf-8")
        case_server = DASHBOARD_COPPERLINE_SERVER.read_text(encoding="utf-8")
        case_route = DASHBOARD_COPPERLINE_ROUTE.read_text(encoding="utf-8")
        case_action = DASHBOARD_COPPERLINE_ACTION.read_text(encoding="utf-8")
        schema = (MORROW / "db/schema-proposal.sql").read_text(encoding="utf-8")
        require("/support/cases/mossfield-recovery" in case_model,
                "Copperline case lost its exact non-legacy route")
        require("MORROW_CASE_ATTACHMENT_TEXT" in case_model
                and "d7e1aa41f9d4a03ea20bdf079fa71ce7613ad66a8f347ba7f926016109817ffa" in schema,
                "Copperline accessible attachment/checksum authority drifted")
        for event_key in (
            "morrow.act1.room04_witnessed",
            "morrow.act1.static_proposal_authenticated",
            "morrow.act1.intention_error_proven",
            "morrow.act2.missing_role_completed",
            "morrow.act2.live_test_recorded",
            "morrow.act2.behavior_reuse_proven",
        ):
            require(event_key in case_model, f"Copperline projection lacks {event_key}")
        for required in ("import 'server-only'", "auth.getUser()", "morrow_player_projection"):
            require(required in case_server, f"Copperline RLS reader lacks {required}")
        for forbidden in ("SUPABASE_SERVICE_ROLE_KEY", "searchParams", "createAdminClient"):
            require(forbidden not in case_server, f"Copperline browser read boundary leaked {forbidden}")
        for required in ("Accessible attachment metadata", "Synchronized field updates",
                         "Temporary maintenance state", "No linked case found", "Case audit halted"):
            require(required in case_route, f"Copperline working surface lacks {required}")
        for required in ("'use server'", "sameOrigin()", "readMorrowCase()",
                         "morrow_record_copperline_event", "externalMutationsAllowed()"):
            require(required in case_action, f"Copperline mutation boundary lacks {required}")
        for required in ("security definer", "is_linked_player", "owner_surface = 'copperline'",
                         "handoff_token_sha256", "from public, anon, authenticated",
                         "831b4026a5e98b263699ba337d246ad5c2c1f26b3f1d00bdef42775586707096",
                         "69ef307074c7aaf7cc4fff12c5e8b7b2423c51a05dfc7f8bb11d53b059420a6e"):
            require(required in schema, f"Copperline receipt SQL lacks {required}")
        reboot_surface = "\n".join((case_model, case_server, case_route, case_action))
        require(re.search(r"\b(?:Hold|Averyn|Wren|Noland|Keeper|Unlit|Deep Hold)\b", reboot_surface,
                          re.IGNORECASE) is None,
                "legacy lore leaked into the Copperline reboot surface")
    except Exception as exc:  # noqa: BLE001
        errors.append(str(exc))

    try:
        discord_domain = DISCORD_MORROW_DOMAIN.read_text(encoding="utf-8")
        discord_repo = DISCORD_MORROW_REPO.read_text(encoding="utf-8")
        discord_policy = DISCORD_MORROW_POLICY.read_text(encoding="utf-8")
        discord_worker = DISCORD_MORROW_WORKER.read_text(encoding="utf-8")
        discord_handler = DISCORD_MORROW_HANDLER.read_text(encoding="utf-8")
        discord_index = (ROOT / "discord" / "src" / "bot" / "index.ts").read_text(encoding="utf-8")
        discord_register = (ROOT / "discord" / "src" / "bot" / "register.ts").read_text(encoding="utf-8")
        discord_config = (ROOT / "discord" / "src" / "config.ts").read_text(encoding="utf-8")
        discord_package = json.loads((ROOT / "discord" / "package.json").read_text(encoding="utf-8"))
        schema = (MORROW / "db/schema-proposal.sql").read_text(encoding="utf-8")
        event = next(row for row in events
                     if row["key"] == "morrow.act2.private_contradiction_resolved")
        require(event["owner"] == "discord" and event["projects_to"] == ["minecraft", "discord"],
                "Act 2 private contradiction event ownership/projection drifted")
        for required in (
            "MORROW_CONTRADICTION_TTL_MS", "nonceSha256", "resolveInteractionScope",
            "authoredPrivateEvidence", "aggregateLinkedDecisions", "private evidence remains private",
        ):
            require(required.lower() in discord_domain.lower(),
                    f"Morrow Discord domain lacks {required}")
        for required in (
            "morrow_open_discord_contradiction", "morrow_apply_discord_contradiction",
            "morrow_claim_discord_projections", "morrow_complete_discord_projection",
        ):
            require(required in discord_repo, f"Morrow Discord repository lacks {required}")
        for required in ("MessageFlags.Ephemeral", "ButtonBuilder", "StringSelectMenuBuilder",
                         "newContradictionNonce", "resolveInteractionScope", "No prose is graded"):
            require(required.lower() in discord_handler.lower(),
                    f"native Morrow Gateway handler lacks {required}")
        for forbidden in ("ModalBuilder", "fetch(", "SUPABASE_SERVICE_ROLE_KEY", "LLM"):
            require(forbidden.lower() not in discord_handler.lower(),
                    f"Morrow Gateway handler crossed its boundary via {forbidden}")
        require("interaction.isButton()" in discord_index
                and "interaction.isStringSelectMenu()" in discord_index,
                "Discord Gateway does not route Morrow native components")
        require(".setName('morrow')" in discord_register, "native /morrow command is not registered")
        require("MORROW_DISCORD_ENABLED" in discord_config and "morrowEnabled" in discord_config,
                "Morrow Discord runtime lacks a fail-closed feature gate")
        require("chat" not in discord_package.get("dependencies", {})
                and not any(name.startswith("@chat-adapter/")
                            for name in discord_package.get("dependencies", {})),
                "Chat SDK/adapter was added despite the native discord.js architecture")
        for required in (
            "discord_contradiction_flows", "discord_contradiction_sessions",
            "discord_contradiction_votes", "pg_advisory_xact_lock", "skip locked",
            "lease_expires_at", "p_nonce_sha256", "p_purpose",
            "'private_payload', false", "from public, anon, authenticated", "to service_role",
        ):
            require(required.lower() in schema.lower(), f"Morrow Discord SQL lacks {required}")
        require("MORROW_BEHAVIOR_REUSE_EVENT" in discord_policy
                and "MORROW_PRIVATE_CONTRADICTION_EVENT" in discord_policy,
                "Morrow Discord projection policy lost activation/callback ordering")
        require("enforceNonce: true" in discord_worker and "allowedMentions: { parse: [] }" in discord_worker,
                "group receipt delivery lost replay or mention safety")
        reboot_surface = "\n".join((discord_domain, discord_repo, discord_policy,
                                      discord_worker, discord_handler))
        require(re.search(r"\b(?:Averyn|Wren|Noland|Keeper|Unlit|Deep Hold)\b", reboot_surface,
                          re.IGNORECASE) is None,
                "legacy lore leaked into the Morrow Discord surface")
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
        schema_path = MORROW / "db/schema-proposal.sql"
        schema_text = schema_path.read_text(encoding="utf-8")
        schema = schema_text.lower()
        migration_text = MORROW_MIGRATION.read_text(encoding="utf-8")
        database_receipt = json.loads(MORROW_DATABASE_RECEIPT.read_text(encoding="utf-8"))
        require(schema_text.splitlines()[3:] == migration_text.splitlines()[3:],
                "generated migration body differs from rehearsed schema proposal")
        require(database_receipt["artifacts"]["proposal_sha256"]
                == hashlib.sha256(schema_path.read_bytes()).hexdigest(),
                "isolated database receipt proposal hash drifted")
        require(database_receipt["artifacts"]["migration_sha256"]
                == hashlib.sha256(MORROW_MIGRATION.read_bytes()).hexdigest(),
                "isolated database receipt migration hash drifted")
        require(database_receipt["status"] == "pass"
                and database_receipt["project"]["production_contacted"] is False
                and database_receipt["production_enablement"] == "blocked",
                "isolated database receipt overclaims its scope")
        browser_receipt = json.loads(MORROW_BROWSER_RECEIPT.read_text(encoding="utf-8"))
        for artifact in browser_receipt["artifacts"].values():
            artifact_path = ROOT / artifact["path"]
            require(artifact_path.is_file()
                    and artifact["sha256"] == hashlib.sha256(artifact_path.read_bytes()).hexdigest(),
                    f"authenticated browser receipt artifact drifted: {artifact['path']}")
        require(browser_receipt["status"] == "partial_service_transport_and_magic_link_delivery"
                and browser_receipt["ticket_projection"]["automatic_database_projector_proven"] is True
                and browser_receipt["ticket_projection"]["javascript_service_role_transport_live_run"] is False
                and database_receipt["copperline_projection_worker"]["database_rpc_live_rehearsed"] is True
                and database_receipt["copperline_projection_worker"]["raw_payload_leaks"] == 0
                and database_receipt["copperline_projection_worker"]["other_player_receipt_rows"] == 0
                and browser_receipt["production_enablement"] == "blocked",
                "authenticated browser receipt overclaims its scope")
        route = DASHBOARD_MORROW_ROUTE.read_text(encoding="utf-8")
        plugin_config = (ROOT / "plugin" / "src" / "main" / "resources" / "config.yml").read_text(encoding="utf-8")
        require("security invoker" in schema, "Minecraft ingest RPC must remain SECURITY INVOKER")
        require("to service_role" in schema, "Minecraft ingest RPC lacks service_role-only grant")
        require("from public, anon, authenticated" in schema, "Minecraft ingest RPC lacks public revocation")
        for index_name in (
            "morrow_capability_decision_event_fk", "morrow_dialogue_response_player_fk",
            "morrow_dialogue_response_prompt_fk", "morrow_dialogue_response_source_event_fk",
            "morrow_director_action_campaign_fk", "morrow_discord_flow_resolved_event_fk",
            "morrow_discord_session_player_fk", "morrow_discord_session_source_event_fk",
            "morrow_discord_vote_player_fk", "morrow_discord_vote_session_fk",
            "morrow_discord_vote_source_event_fk", "morrow_event_actor_player_fk",
            "morrow_event_definition_fk", "morrow_evidence_receipt_definition_fk",
            "morrow_evidence_receipt_player_fk", "morrow_evidence_receipt_source_event_fk",
            "morrow_media_delivery_event_fk", "morrow_media_delivery_asset_fk",
            "morrow_player_supabase_user_fk", "morrow_relationship_transition_event_fk",
            "morrow_replay_campaign_fk", "morrow_replay_owner_player_fk",
            "morrow_replay_sealed_event_fk", "morrow_restoration_decision_event_fk",
            "morrow_anchor_committed_event_fk", "morrow_anchor_creator_player_fk",
            "morrow_anchor_supersedes_fk",
        ):
            require(f"create index if not exists {index_name}" in schema,
                    f"Morrow advisor covering index missing: {index_name}")
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
