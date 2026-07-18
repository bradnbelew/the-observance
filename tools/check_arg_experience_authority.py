#!/usr/bin/env python3
"""Fail closed on lore-scavenger/puzzle-hunt structures masquerading as ARG experience.

This is a structural rejection gate, not a creativity score. Human cold play and Brad approval remain
separate. The vocabulary of player actions stays open-ended; no mechanism enum is defined here.
"""
from __future__ import annotations

import json
import re
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
AUTHORITY = ROOT / "design/handoff/ARG-EXPERIENCE-AUTHORITY.json"
REDESIGN = ROOT / "campaign/arg-experience-redesign.json"
CHOREOGRAPHY = ROOT / "campaign/arg-state-choreography.json"
STORY_AUTHORITY = ROOT / "design/handoff/STORY-EXPANSION-ARG-INTEGRATION.json"
STORY_MAP = ROOT / "campaign/story-interaction-map.json"
DEPENDENCIES = ROOT / "campaign/story-dependency-map.json"
GRAMMAR = ROOT / "campaign/campaign-grammar-audit.json"
FEASIBILITY = ROOT / "campaign/functional-feasibility-matrix.json"
INPUTS = ROOT / "campaign/platform-input-feasibility-matrix.json"
PACK = ROOT / "campaign/p5-p12"
EARLY_INPUTS = ROOT / "campaign/p1-p4/input-contracts.json"
PAPER_RECEIPTS = (
    ROOT / "design/handoff/P4-P5-STRUCTURED-ANSWER-PAPER-RECEIPT-2026-07-17.json",
    ROOT / "design/handoff/WHOLE-CAMPAIGN-DISPOSABLE-PAPER-PASS-2026-07-18.json",
    ROOT / "design/handoff/UNLIT-COPY-PROOF-DISPOSABLE-PAPER-PASS-2026-07-18.json",
)


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def load(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def folded(value: str) -> str:
    return " ".join(re.findall(r"[A-Z0-9]+", value.upper()))


def walk_strings(value: Any):
    if isinstance(value, str):
        yield value
    elif isinstance(value, list):
        for item in value:
            yield from walk_strings(item)
    elif isinstance(value, dict):
        for key, item in value.items():
            yield key
            yield from walk_strings(item)


def active_evidence_and_answers() -> tuple[list[str], list[tuple[str, str]]]:
    evidence: list[str] = []
    answers: list[tuple[str, str]] = []
    p4 = load(ROOT / "design/m3/P4-VNEXT-CASE-BRIEF.json")
    evidence.extend(row.get("content_job", "") for row in p4.get("evidence_graph", []))
    for conclusion in p4.get("findings", []):
        answers.extend((conclusion["id"], answer)
                       for answer in conclusion.get("accepted_normalized_answers", []))
    for path in sorted(PACK.glob("case-p*.json")):
        case = load(path)
        rows = list(case.get("evidence", []))
        for dossier in case.get("dossiers", []):
            rows.extend(dossier.get("evidence", []))
        evidence.extend(row.get("content", "") for row in rows)
        conclusions = list(case.get("conclusions", []))
        if case.get("group_conclusion"):
            conclusions.append(case["group_conclusion"])
        for conclusion in conclusions:
            answers.extend((conclusion.get("id", case["phase"]), answer)
                           for answer in conclusion.get("accepted_answers", []))
    return evidence, answers


def guardrail_violations(candidate: dict[str, Any]) -> list[str]:
    """Pure negative-contract oracle used by the routed gate and mutation self-test."""
    violations: list[str] = []
    rules = {
        "conclusion_printed_by_one_source": candidate["conclusion_printed_by_one_source"] is False,
        "single_surface_bounded_case": candidate["minimum_case_surface_count"] >= 2,
        "click_or_receipt_gated_correctness": candidate["correctness_requires_receipt"] is False,
        "interaction_free_story": candidate["minimum_player_action_count"] >= 1,
        "no_player_caused_response": candidate["minimum_world_response_count"] >= 1,
        "repeated_answer_box_grammar": candidate["answer_box_dominates_adjacent_cases"] is False,
        "optional_major_side_story": candidate["major_story_optional"] is False,
        "absent_named_player_gate": candidate["named_player_eligibility"] is False,
        "unsafe_or_unbounded_personalized_copy": candidate["unsafe_personalized_copy"] is False,
        "lectern_text_input": candidate["lectern_text_input"] is False,
        "decorative_fake_form": candidate["decorative_fake_form"] is False,
        "required_unrestricted_chat_parser": candidate["required_chat_parser"] is False,
        "input_absent_from_pinned_platform": candidate["input_absent_from_pinned_platform"] is False,
        "book_sign_or_dialog_overflow": candidate["client_input_overflow"] is False,
        "needlessly_opaque_decoded_plaintext": candidate["decoded_plaintext_needlessly_opaque"] is False,
        "source_click_prerequisite": candidate["source_click_prerequisite"] is False,
        "hidden_long_canonical_phrase": candidate["hidden_long_canonical_phrase"] is False,
    }
    for name, passed in rules.items():
        if not passed:
            violations.append(name)
    return violations


def main() -> None:
    authority = load(AUTHORITY)
    require(authority["status"] == "binding_research_based_experience_authority",
            "ARG experience authority is not binding")
    require(authority["brad_approval"] is None
            and authority["p4_vnext_experiential_proof"] == "rejected"
            and authority["whole_campaign_experiential_readiness"] == "not_proven"
            and authority["launch_readiness"] == "not_proven",
            "Brad rejection or readiness boundary drift")
    require(authority["campaign_level_grammar"] == "arg",
            "ARG is no longer the campaign-level grammar")
    classifications = authority["classifications"]
    require(set(classifications) == {"lore_scavenger", "escape_room_or_puzzle_hunt", "arg"},
            "experience distinctions drifted")
    require(all(source.get("url", "").startswith("https://") and source.get("applied_findings")
                for source in authority["research_sources"]),
            "research source lacks URL or applied findings")
    require(len({source["id"] for source in authority["research_sources"]})
                == len(authority["research_sources"]), "duplicate research source")
    runtime = authority["runtime_invariants"]
    require(runtime["observation_receipts_gate_correctness"] is False
            and runtime["source_possession_gates_correctness"] is False
            and runtime["local_primary"] is True and runtime["any_subset"] is True
            and runtime["public_real_world_travel_phone_or_personal_data_required"] is False,
            "ARG runtime/safety invariants weakened")
    answer_shape = authority["answer_shape_contract"]
    require(answer_shape["exact_match_allowed_only_when_evidence_yields_exact_value"] is True
            and answer_shape["interpretive_conclusion_hidden_canonical_sentence"] is False
            and answer_shape["natural_paraphrase_and_word_order_coverage_required"] is True
            and answer_shape["partial_contradictory_irrelevant_accounts_fail"] is True
            and answer_shape["llm_judge_required"] is False
            and answer_shape["observation_or_possession_gate"] is False,
            "campaign answer-shape contract weakened")
    relationship_web = authority["relationship_web_contract"]
    require(relationship_web["isolated_case_sections_or_clue_stations_forbidden"] is True
            and relationship_web["artifact_local_function_is_sufficient"] is False
            and relationship_web["backward_or_forward_connection_required"] is True
            and relationship_web["callbacks_and_references_may_cross_cases_and_surfaces"] is True
            and relationship_web["ordinary_texture_may_gain_later_significance"] is True
            and relationship_web["major_threads_require_multiple_independent_in_doors"] is True
            and "quota" in relationship_web["machine_review"],
            "campaign relationship web weakened or converted into isolated sections/a brittle quota")

    story_authority = load(STORY_AUTHORITY)
    require(story_authority["status"] == "binding_offline_story_authority_not_human_approved"
            and story_authority["brad_approval"] is None
            and story_authority["p4_current_candidate"] == "rejected_technical_proof_only"
            and story_authority["whole_campaign_experiential_readiness"] == "not_proven"
            and story_authority["new_brad_server_authorized"] is False,
            "story expansion authority or human gate drift")
    require(story_authority["campaign_order"] == [f"P{i}" for i in range(1, 13)],
            "story authority campaign order drift")
    require(story_authority["duration"]["playtest_authoritative"] is True
            and story_authority["duration"]["padding_forbidden"] is True,
            "story duration became a quota")
    require(len(story_authority["human_layers"]) == 5,
            "five human story layers are not preserved")
    threads = story_authority["required_parallel_threads"]
    require(threads and all(row["major"] is True and row["required"] is True
                            for row in threads.values()),
            "a major parallel story became optional")
    require(threads["unlit_parallel_act"]["seven_houses_required"] is True
            and threads["unlit_parallel_act"]["base_synthesis_required"] is True
            and threads["unlit_parallel_act"]["one_visit"] is False,
            "Unlit required parallel-act contract weakened")
    functional = story_authority["functional_invariants"]
    require(functional["paper_version"] == "1.21.11"
            and functional["correctness_requires_observation_receipts"] is False
            and functional["correctness_requires_source_possession"] is False
            and functional["correctness_requires_npc_interaction"] is False
            and functional["absent_player_gate"] is False
            and functional["optional_major_story"] is False
            and functional["closed_mechanism_taxonomy"] is False,
            "story runtime/correctness invariant weakened")
    require(len(story_authority["exact_ambiguities"]) == 3,
            "story authority no longer preserves exactly three ambiguities")
    language = story_authority["plain_language_standard"]
    require(language["modern_easy_english"] is True
            and language["decoded_plaintext_needlessly_opaque"] is False
            and language["automated_readability_is_quota"] is False
            and language["human_voice_review_required"] is True,
            "plain-language or human review standard weakened")
    cipher = story_authority["cipher_puzzle_standard"]
    require(cipher["substantial_across_campaign"] is True
            and cipher["illustrative_families_not_catalog"] is True
            and cipher["in_world_reason_required"] is True
            and cipher["earned_key_or_fair_key_trail_required"] is True
            and cipher["later_consequence_required"] is True
            and cipher["traditional_answer_submissions_may_dominate"] is False,
            "cipher/puzzle depth or open-endedness weakened")
    input_rules = story_authority["platform_input_rules"]
    require(input_rules["lectern_free_text_input"] is False
            and input_rules["required_unrestricted_chat_parser"] is False
            and input_rules["fake_decorative_form"] is False
            and input_rules["dialog_api_experimental"] is True
            and input_rules["dialog_response_server_validation"] is True
            and input_rules["stable_command_fallback"] is True,
            "real-platform input boundary weakened")
    for relative in story_authority["offline_required_files"]:
        require((ROOT / relative).is_file(), f"missing story authority artifact: {relative}")

    redesign = load(REDESIGN)
    require(redesign["status"] == "offline_experiential_redesign_authority_not_human_approved"
            and redesign["closed_mechanism_taxonomy"] is False
            and redesign["answer_or_action_requires_observation_receipts"] is False
            and redesign["brad_approval"] is None,
            "redesign authority or human gate drift")
    cases = redesign["cases"]
    require([case["phase"] for case in cases] == [f"P{i}" for i in range(1, 13)],
            "ARG redesign must cover exact ordered P1-P12")
    required = set(authority["case_required_fields"])
    all_events: set[str] = set()
    action_signatures: set[tuple[str, ...]] = set()
    for case in cases:
        phase = case["phase"]
        require(case["experience_classification"] == "ARG", f"{phase}: not classified as ARG")
        require(required.issubset(case), f"{phase}: incomplete ARG case brief: {sorted(required-set(case))}")
        require(case["competing_hypotheses"], f"{phase}: no competing hypotheses")
        fragments = case["distributed_fragments"]
        require(fragments and len({row["surface"] for row in fragments}) > 1,
                f"{phase}: single-surface bounded case")
        actions = case["player_initiated_actions"]
        require(actions, f"{phase}: interaction-free case")
        verbs = tuple(row["verb"].strip().casefold() for row in actions)
        require(any(verb not in {"answer", "submit", "type", "file"} for verb in verbs),
                f"{phase}: answer-box verbs dominate the case")
        require(verbs not in action_signatures,
                f"{phase}: repeats an adjacent case's complete player-action signature")
        action_signatures.add(verbs)
        for action in actions:
            require(action.get("action") and action.get("response_event")
                    and action.get("receipt_gate") is False,
                    f"{phase}: action is not consequential or reintroduces receipt gating")
            all_events.add(action["response_event"])
        reactions = case["authored_reactivity"]
        require(reactions, f"{phase}: world never answers player action")
        reaction_events = set()
        for reaction in reactions:
            require(reaction.get("automation") in {"A0", "A1"},
                    f"{phase}: unapproved automation in authored reactivity")
            require(reaction.get("exact_trigger") and reaction.get("surfaces")
                    and reaction.get("response") and reaction.get("idempotent") is True
                    and reaction.get("catch_up"), f"{phase}: incomplete authored reaction")
            reaction_events.add(reaction["event"])
        require(reaction_events.issubset(all_events),
                f"{phase}: reaction has no player-initiated event")
        require(case["direct_source_restatement_core"] is False,
                f"{phase}: direct source-restatement loop admitted")
        require(case["single_surface_bounded_case"] is False,
                f"{phase}: single-surface case admitted")
        require(case["conclusion_printed_verbatim"] is False,
                f"{phase}: source prints the conclusion")
        require(case["interaction_free"] is False and case["player_caused_world_response"] is True,
                f"{phase}: no player-caused world response")
        require("domin" in case["answer_input_role"].casefold()
                or "no final answer box" in case["answer_input_role"].casefold(),
                f"{phase}: answer-input minority role is not explicit")
        require(case["novelty_against_adjacent"], f"{phase}: no qualitative novelty comparison")

    choreography = load(CHOREOGRAPHY)
    allowed_choreography_statuses = {
        "offline_authored_not_deployed",
        "local_file_runtime_and_deployable_supabase_discord_adapter_implemented_external_staging_unverified",
    }
    require(choreography["status"] in allowed_choreography_statuses
            and choreography["state_model"]["closed_mechanism_enum"] is False
            and choreography["state_model"]["observation_receipts_gate_actions_or_answers"] is False
            and choreography["state_model"]["source_possession_gate_actions_or_answers"] is False,
            "state choreography became a closed or receipt-gated runtime")
    if choreography["status"] != "offline_authored_not_deployed":
        runtime = choreography.get("projection_runtime", {})
        require(runtime.get("canonical_commit_before_projection") is True
                and runtime.get("external_staging_receipt") is None
                and runtime.get("unknown_event_behavior", "").startswith("fail closed")
                and runtime.get("worker_timing", {}).get("maximum_attempts") == 8
                and runtime.get("proof") == "design/handoff/CROSS-SURFACE-EVENT-OUTBOX-RECEIPT-2026-07-18.json",
                "implemented projection runtime lacks fail-closed, bounded, or honest staging evidence")
    require(choreography["privacy_and_safety"]["public_real_world_travel_required"] is False
            and choreography["privacy_and_safety"]["real_phone_required"] is False
            and choreography["privacy_and_safety"]["personal_data_required"] is False,
            "unsafe real-world ARG requirement introduced")
    phase_rows = choreography["phase_events"]
    require([row["phase"] for row in phase_rows] == [f"P{i}" for i in range(1, 13)],
            "choreography does not cover P1-P12")
    choreographed = {event for row in phase_rows for event in row["events"]}
    require(all_events == choreographed,
            f"action/choreography event mismatch: actions-only={sorted(all_events-choreographed)}, choreography-only={sorted(choreographed-all_events)}")

    story_map = load(STORY_MAP)
    require(story_map["status"] == "offline_authored_not_implemented_or_human_approved"
            and story_map["brad_approval"] is None,
            "story map claims implementation or human approval")
    require(story_map["ambiguity_boundary"]["count"] == 3
            and len(story_map["ambiguity_boundary"]["open_only"]) == 3,
            "story map ambiguity boundary drift")
    require([row["phase"] for row in story_map["phase_map"]] == [f"P{i}" for i in range(1, 13)],
            "story/interaction map must cover exact ordered P1-P12")
    phase_required = {"plant", "incident", "action", "response", "intersection", "reversal", "payoff", "coda"}
    for row in story_map["phase_map"]:
        require(phase_required.issubset(row) and all(row[key] for key in phase_required),
                f"{row.get('phase')}: incomplete story/interaction beat")
    keeper_fields = set(story_authority["keeper_humanization_required_fields"])
    require(set(story_map["keepers"]) == {"Vaun", "Mara", "Sella", "Orin", "Brann", "Iss"},
            "six Keeper people drifted")
    for keeper, row in story_map["keepers"].items():
        require(keeper_fields.issubset(row),
                f"{keeper}: humanization fields missing: {sorted(keeper_fields-set(row))}")
    unlit = story_map["unlit_expeditions"]
    require(len(unlit) == 8 and len({row["house"] for row in unlit}) == 8
            and {row["house"] for row in unlit} >= {"Lamp", "Cairn", "Coop", "Well", "Watch", "Warm", "Threshold", "Base mirror"},
            "seven Unlit houses plus base synthesis are not intact")
    require(story_map["major_threads_required"] is True
            and story_map["major_threads_optional"] is False
            and story_map["named_player_eligibility"] is False
            and story_map["observation_receipt_correctness_gate"] is False,
            "story map reintroduced optional content, attendance, or receipts")

    dependencies = load(DEPENDENCIES)
    require(dependencies["locked_truth"] and dependencies["newly_approved_flesh"]
            and dependencies["provisional_creative_candidates"]
            and dependencies["implementation_prerequisites"],
            "story dependency classes are incomplete")
    require(all(row.get("source") for row in dependencies["locked_truth"]),
            "locked truth lacks source authority")
    require(all(row.get("status") and row.get("may_change_without_canon_change") is True
                for row in dependencies["provisional_creative_candidates"]),
            "provisional candidate is presented as locked canon")

    grammar = load(GRAMMAR)
    require(grammar["closed_mechanism_catalog"] is False and grammar["mechanic_quotas"] is False,
            "grammar audit became a finite taxonomy or quota")
    qualitative = grammar["qualitative_checks"]
    require(qualitative["each_case_has_exposition_interaction_challenge"] is True
            and qualitative["each_case_has_player_initiated_action"] is True
            and qualitative["each_case_has_authored_world_response"] is True
            and qualitative["conclusions_printed_verbatim"] is False
            and qualitative["answer_input_dominates_any_adjacent_pair"] is False
            and qualitative["major_threads_optional"] is False
            and qualitative["human_review_still_required"] is True,
            "qualitative campaign grammar weakened")
    require([row["phase"] for row in grammar["case_rhythm"]] == [f"P{i}" for i in range(1, 13)],
            "campaign grammar does not audit ordered P1-P12")

    feasibility = load(FEASIBILITY)
    feasibility_fields = {"id", "phase", "beat", "paper_primitive", "surface_owner", "state_transition",
                          "outage", "accessibility", "replay_catch_up", "security_risk", "automated_tests", "human_gap"}
    require(feasibility["entries"] and all(feasibility_fields.issubset(row) and row["automated_tests"]
                                           and row["human_gap"] for row in feasibility["entries"]),
            "functional feasibility row is incomplete")
    require(feasibility["external_deployment_receipts_claimed"] is False,
            "feasibility fabricates external deployment proof")
    if feasibility["paper_runtime_receipts_claimed"] is True:
        receipt_scope = feasibility.get("paper_runtime_receipt_scope", "").casefold()
        require(all(path.is_file() for path in PAPER_RECEIPTS)
                and "p4" in receipt_scope and "deep hold" in receipt_scope
                and "bounded unlit copy" in receipt_scope and "human" in receipt_scope,
                "Paper runtime claim lacks exact scoped receipts")
    else:
        require(feasibility["paper_runtime_receipts_claimed"] is False,
                "paper_runtime_receipts_claimed must be boolean")

    inputs = load(INPUTS)
    input_fields = {"id", "used_by", "platform_version", "visible_trigger_affordance", "primitive",
                    "data_shape_normalization", "predicate_owner", "concurrency_idempotency_restart",
                    "feedback", "accessibility_fallback", "security_privacy_risk", "tests", "proof"}
    require(inputs["records"] and all(input_fields.issubset(row) and row["tests"] and row["proof"]
                                      for row in inputs["records"]),
            "platform input feasibility row is incomplete")
    by_id = {row["id"]: row for row in inputs["records"]}
    require("DialogResponseView" in by_id["MC_DIALOG_SHORT_TEXT"]["primitive"]
            and "server" in by_id["MC_DIALOG_SHORT_TEXT"]["data_shape_normalization"].casefold()
            and "namespaced Brigadier" in by_id["MC_DIALOG_SHORT_TEXT"]["accessibility_fallback"],
            "Paper Dialog lacks server validation or stable command fallback")
    require("not a text field" in by_id["MC_LECTERN_READ_ONLY"]["predicate_owner"].casefold()
            or "none for text entry" in by_id["MC_LECTERN_READ_ONLY"]["predicate_owner"].casefold(),
            "lectern is presented as text input")
    require("real HTML form" in by_id["WEB_COPPERLINE_FORM"]["primitive"]
            and "server" in by_id["WEB_COPPERLINE_FORM"]["primitive"].casefold(),
            "Copperline input is a decorative fake form")
    require("P8 causal intervention plan" in by_id["WEB_COPPERLINE_FORM"]["used_by"]
            and "canonical component ids" in by_id["WEB_COPPERLINE_FORM"]["data_shape_normalization"]
            and "raw player prose is not stored" in by_id["WEB_COPPERLINE_FORM"]["data_shape_normalization"],
            "P8 Copperline plan lacks real form ownership, canonical idempotency, or text-retention safety")
    p9_page = (ROOT / "dashboard/src/app/community/archive/ash-camp/page.tsx").read_text(encoding="utf-8")
    p9_form = (ROOT / "dashboard/src/app/community/archive/ash-camp/LeakWindowForm.tsx").read_text(encoding="utf-8")
    p9_action = (ROOT / "dashboard/src/app/community/archive/ash-camp/actions.ts").read_text(encoding="utf-8")
    discord_register = (ROOT / "discord/src/bot/register.ts").read_text(encoding="utf-8")
    require("Copies from separate clocks" in p9_page
            and "<form action={action}" in p9_form
            and "p9.leak_window_proven" in p9_action
            and "observation_receipts: 0" in p9_action
            and "file-leak-window" not in discord_register,
            "P9 private-window finding is not real provenance work or retains a Discord answer menu")
    p9_predicate = (ROOT / "plugin/src/main/java/com/observance/watcher/v5runtime/P9CampPredicate.java").read_text(
        encoding="utf-8")
    p9_input_by_id = {row["id"]: row for row in load(PACK / "input-contracts.json")["contracts"]}
    p9_coordinator = (ROOT / "plugin/src/main/java/com/observance/watcher/v5runtime/V5RuntimeCoordinator.java").read_text(
        encoding="utf-8")
    p9_finding_command = (ROOT / "plugin/src/main/java/com/observance/watcher/command/CampaignFindingCommand.java").read_text(
        encoding="utf-8")
    require(all("/obsfinding" in p9_input_by_id[key]["platform"]
                and p9_input_by_id[key]["zero_observation_acceptance"] is True
                for key in ("P9.F2", "P9.F4", "P9.F6"))
            and "class P9CampPredicate" in p9_predicate
            and "validPeople" in p9_predicate and "validWindow" in p9_predicate
            and "submitP9CampPeople" in p9_coordinator and "submitP9LeakWindow" in p9_coordinator
            and 'case "p9-people"' in p9_finding_command and 'case "p9-window"' in p9_finding_command
            and 'people.addProperty("observation_receipts", 0)' in p9_coordinator
            and 'leak.addProperty("observation_receipts", 0)' in p9_coordinator
            and 'snapshot.isComplete("v5_a02_stations")' not in p9_coordinator
            and 'snapshot.isComplete("v5_case_c07_complete")' not in p9_coordinator,
            "P9 local path still auto-certifies biographies/version chronology from station completion or lacks the shared zero-observation predicate")
    p10_predicate = (ROOT / "plugin/src/main/java/com/observance/watcher/v5runtime/P10WrenTransmissionPredicate.java").read_text(
        encoding="utf-8")
    p11_predicate = (ROOT / "plugin/src/main/java/com/observance/watcher/v5runtime/P11AverynIdentityPredicate.java").read_text(
        encoding="utf-8")
    require(all("/obsfinding p10-wren" in p9_input_by_id[key]["platform"]
                and p9_input_by_id[key]["zero_observation_acceptance"] is True
                for key in ("P10.F2", "P10.F3"))
            and "/obsfinding p11-name" in p9_input_by_id["P11.F8"]["platform"]
            and p9_input_by_id["P11.F8"]["zero_observation_acceptance"] is True
            and "class P10WrenTransmissionPredicate" in p10_predicate
            and "source clicks" in p10_predicate and "NPC contact" in p10_predicate
            and "class P11AverynIdentityPredicate" in p11_predicate
            and "submitP10WrenTransmission" in p9_coordinator
            and "submitP11AverynIdentity" in p9_coordinator
            and 'case "p10-wren"' in p9_finding_command
            and 'case "p11-name"' in p9_finding_command
            and 'finding.addProperty("observation_receipts", 0)' in p9_coordinator
            and 'finding.addProperty("npc_contact_gate", false)' in p9_coordinator
            and 'identity.addProperty("observation_receipts", 0)' in p9_coordinator
            and 'identity.addProperty("affidavit_possession_gate", false)' in p9_coordinator
            and 'snapshot.isComplete("v5_wr03_confession")' not in p9_coordinator
            and 'snapshot.isComplete("v5_case_c09_complete")' not in p9_coordinator,
            "P10/P11 local knowledge paths are source-gated, auto-certified, or lack shared bounded predicates")
    require("never parse free chat" in by_id["DISCORD_INVESTIGATION_MODAL"]["data_shape_normalization"],
            "Discord input requires unrestricted chat parsing")
    modal = by_id["DISCORD_INVESTIGATION_MODAL"]
    require(modal["used_by"] == ["P7.F6 public Nessa correction"]
            and "showModal" in modal["primitive"]
            and "MODAL_SUBMIT" in modal["primitive"]
            and "observance:p7:nessa-correction:v1" in modal["primitive"]
            and "/investigate file-nessa" in modal["accessibility_fallback"]
            and "private guild" in modal["proof"].casefold()
            and "remains open" in modal["proof"].casefold(),
            "active Discord modal lacks exact primitive, fallback, or honest client gap")
    discord_handler = (ROOT / "discord/src/bot/commands/investigate.ts").read_text(encoding="utf-8")
    require("contact-wren" in discord_register
            and "confront-wren" not in discord_register
            and "observance:p10:wren-transmission:v1" in discord_handler
            and "validWrenTransmission" in discord_handler
            and "WREN_TRANSMISSION_CANONICAL_PAYLOAD" in discord_handler,
            "P10 remains a visible answer-choice menu or lacks a canonical character-response modal")
    wren_modal = by_id["DISCORD_CHARACTER_RESPONSE_MODAL"]
    require(wren_modal["used_by"] == ["P10.F2 transmission finding", "P10.F3 motive boundary"]
            and "showModal" in wren_modal["primitive"]
            and "observance:p10:wren-transmission:v1" in wren_modal["primitive"]
            and "raw player prose is not stored" in wren_modal["data_shape_normalization"]
            and "private guild" in wren_modal["proof"].casefold()
            and "remain open" in wren_modal["proof"].casefold(),
            "P10 character-response input lacks exact primitive, retention boundary, or honest client gap")
    p3_field = by_id["DISCORD_SEMANTIC_FIELD_ACTION"]
    require(p3_field["used_by"] == ["P3.DISPATCH open resident disagreement"]
            and "ChatInputCommandInteraction" in p3_field["primitive"]
            and "raw player prose is not stored" in p3_field["data_shape_normalization"]
            and "private guild" in p3_field["proof"].casefold()
            and "remains open" in p3_field["proof"].casefold(),
            "P3 field-note input lacks real primitive, retention boundary, or honest client gap")
    coordinator = (ROOT / "plugin/src/main/java/com/observance/watcher/v5runtime/V5RuntimeCoordinator.java").read_text(encoding="utf-8")
    require("restore-name" in discord_register
            and "identify-averyn" not in discord_register
            and "P11_UNBOUND_EVENT" in coordinator
            and 'snapshot.isComplete("v5_rp02_configured")' in coordinator
            and "mirrorP11UnboundAsync" in coordinator
            and '"minecraft:p11:averyn-relationship-unbound"' in coordinator,
            "P11 retains a relationship answer menu or lacks the local physical unbound transition")
    aliases = (ROOT / "plugin/src/main/java/com/observance/watcher/v5runtime/ArgEventPrerequisiteAliases.java").read_text(encoding="utf-8")
    remote_cache = (ROOT / "plugin/src/main/java/com/observance/watcher/v5runtime/V5RemoteStateCache.java").read_text(encoding="utf-8")
    for legacy, event in {
        "v5_case_c02_complete": "p5.civic_gallery_recurated",
        "v5_case_c03_complete": "p6.six_responsibilities_acknowledged",
        "v5_case_c04_complete": "p7.nessa_publicly_cleared",
        "v5_case_c05_complete": "p8.intervention_plan_accepted",
        "v5_case_c06_complete": "p8.hold_systems_repaired",
        "v5_case_c07_complete": "p9.leak_window_proven",
        "v5_case_c08_complete": "p10.wren_remembrance_committed",
        "v5_case_c09_complete": "p11.averyn_restored_unbound",
    }.items():
        require(f'Map.entry("{legacy}", "{event}")' in aliases,
                f"missing one-way story prerequisite alias {legacy} -> {event}")
    require("v5_case_c10_complete" not in aliases
            and "ArgEventPrerequisiteAliases.storyEventFor(flag)" in remote_cache
            and "ArgEventPrerequisiteAliases.resolvedAliases(flags)" in remote_cache,
            "cross-surface prerequisite bridge bypasses physical C10 or is not wired into Paper")
    require('P8_UNLIT_EVENT = "p8.unlit_house_synthesis_completed"' in coordinator
            and all(flag in coordinator for flag in (
                "v5_bi01_lamp", "v5_bi02_cairn", "v5_bi03_coop", "v5_bi04_well",
                "v5_bi05_watch", "v5_bi06_warm", "v5_bi07_threshold",
            ))
            and "P8_UNLIT_HOUSE_PROOFS.stream().allMatch(snapshot::isComplete)" in coordinator
            and "snapshot.isComplete(P8_UNLIT_EVENT)" in coordinator
            and "mirrorP8UnlitAsync" in coordinator,
            "seven required Unlit houses/base are not independently committed before P8 repair")
    early_input_contracts = load(EARLY_INPUTS)["contracts"]
    require([row["id"] for row in early_input_contracts] == [
        "P1.SERVICE", "P1.CUSTODY", "P2.PACKAGE", "P2.HANDOFF",
        "P3.DISPATCH", "P4.RESTORE", "P4.TEST", "P4.CONCLUSION"],
        "P1-P4 real input contract is incomplete or reordered")
    require(all(row["zero_observation_acceptance"] is True for row in early_input_contracts)
            and all(not (row["interpretive"] and row["runtime_exact_phrase"]) for row in early_input_contracts),
            "P1-P4 input contract reintroduced receipt gating or an interpretive magic phrase")
    input_contracts = load(PACK / "input-contracts.json")["contracts"]
    input_contract_by_id = {row["id"]: row for row in input_contracts}
    require("protected public-curation controls" in input_contract_by_id["P5.F1"]["platform"]
            and "Discord" not in input_contract_by_id["P5.F1"]["platform"]
            and "six profession-specific" in input_contract_by_id["P6.F7"]["platform"]
            and "Discord modal" not in input_contract_by_id["P6.F7"]["platform"],
            "P5/P6 input authority still advertises an unimplemented answer form instead of physical work")
    p5_runtime = (ROOT / "plugin/src/main/java/com/observance/watcher/v5runtime/P5CurationRuntime.java").read_text(
        encoding="utf-8")
    finding_command = (ROOT / "plugin/src/main/java/com/observance/watcher/command/CampaignFindingCommand.java").read_text(
        encoding="utf-8")
    minecraft_bindings = load(PACK / "minecraft-bindings.json")
    p5_controls = {row["id"]: row for row in minecraft_bindings["runtime_owned_controls"]}
    require("p5_civic_records_counter" in p5_controls
            and p5_controls["p5_civic_records_counter"]["interaction_order"] == "any"
            and p5_controls["p5_civic_records_counter"]["source_touch_prerequisite"] is False
            and "P5CurationRuntime" in p5_runtime
            and "SERVICE_SELECTED" in p5_runtime and "PENALTY_SELECTED" in p5_runtime
            and "CHRONOLOGY_EVENT" in p5_runtime and "CURATION_EVENT" in p5_runtime
            and "CHISELED_BOOKSHELF" in p5_runtime and "WAXED_COPPER_GRATE" in p5_runtime,
            "P5 curation is still only disposable-slice scaffolding or lacks a real occupied main-world control")
    p6_contract = input_contract_by_id["P6.F7"]
    p6_predicate = (ROOT / "plugin/src/main/java/com/observance/watcher/v5runtime/P6ResponsibilityPredicate.java").read_text(
        encoding="utf-8")
    p6_proof_block = coordinator[coordinator.index("P6_PROFESSIONAL_PROOFS.stream().allMatch"):
                                 coordinator.index("snapshot = progress.snapshot();",
                                                   coordinator.index("P6_PROFESSIONAL_PROOFS.stream().allMatch"))]
    require("/obsfinding p6-recovery" in p6_contract["platform"]
            and p6_contract["zero_observation_acceptance"] is True
            and "cannot certify the conclusion" in p6_contract["acceptance_owner"]
            and "class P6ResponsibilityPredicate" in p6_predicate
            and "observation, affidavit possession, or per-room completion is inspected" in p6_predicate
            and "submitP6ResponsibilityMatrix" in coordinator
            and 'responsibility.addProperty("observation_receipts", 0)' in coordinator
            and 'responsibility.addProperty("affidavit_possession_gate", false)' in coordinator
            and 'case "p6-recovery"' in finding_command
            and "P6_RESPONSIBILITY_EVENT" not in p6_proof_block,
            "P6 responsibility still auto-completes from six source interactions or lacks a real zero-observation recovery predicate")
    p8_contracts = [input_contract_by_id[key] for key in ("P8.F3", "P8.F4", "P8.F5")]
    p8_predicate = (ROOT / "plugin/src/main/java/com/observance/watcher/v5runtime/P8InterventionPlanPredicate.java").read_text(
        encoding="utf-8")
    coordinator = (ROOT / "plugin/src/main/java/com/observance/watcher/v5runtime/V5RuntimeCoordinator.java").read_text(
        encoding="utf-8")
    plugin_yml = (ROOT / "plugin/src/main/resources/plugin.yml").read_text(encoding="utf-8")
    require(all("/obsfinding" in row["platform"] and row["runtime_exact_phrase"] is False
                and row["zero_observation_acceptance"] is True for row in p8_contracts)
            and "observancefinding:" in plugin_yml
            and 'split("\\\\|", -1)' in finding_command
            and "return List.of();" in finding_command
            and "MAX_FIELD_LENGTH = 180" in p8_predicate
            and "submitP8InterventionPlan" in coordinator
            and 'plan.addProperty("observation_receipts", 0)' in coordinator,
            "P8 Minecraft outage path is not a real bounded command sharing the zero-receipt meaning predicate")
    require("if (P8_UNLIT_HOUSE_PROOFS.stream().allMatch(snapshot::isComplete)) {" in coordinator,
            "seven-house synthesis still waits on the separate causal-plan input")
    v5_seed = (ROOT / "discord/supabase/seeds/v5_investigations.sql").read_text(encoding="utf-8")
    require("v5-a10-private-window" in v5_seed
            and "inside access sender unresolved" in v5_seed
            and "v5-a10-sabotage" not in v5_seed
            and "wren leaked the plan" not in v5_seed.casefold()
            and "wren is the only remaining source" not in v5_seed.casefold(),
            "P9 legacy compatibility path still reveals Wren before P10 proves attribution")
    seventh_retirement = (ROOT / "discord/supabase/migrations/0019_retire_superseded_seventh_runtime.sql").read_text(encoding="utf-8")
    seventh_rollback = (ROOT / "discord/supabase/rollbacks/0019_retire_superseded_seventh_runtime.rollback.sql").read_text(encoding="utf-8")
    apply_all = (ROOT / "discord/supabase/apply-all.sql").read_text(encoding="utf-8")
    retired_seventh_keys = {
        "seventh-shrine", "seventh-unwriting", "seventh-cause", "seventh-choice", "seventh-name",
    }
    require("iss signed averyn out" not in v5_seed.casefold()
            and "subject averyn" not in v5_seed.casefold(),
            "P6 compatibility path names Averyn before the P11 identity restoration")
    require(all(f"'{key}'" in seventh_retirement and f"'{key}'" in seventh_rollback
                for key in retired_seventh_keys)
            and "set active = false" in seventh_retirement.casefold()
            and "set active = true" in seventh_rollback.casefold(),
            "stale literal-Seventh runtime is not retired by an exact reversible migration")
    require("('v5-ar08-averyn'" in v5_seed and "('RP06'" in v5_seed,
            "Averyn identity or physical universal release was lost while retiring stale runtime")
    require(apply_all.index("-- FILE: discord/supabase/migrations/0019_retire_superseded_seventh_runtime.sql")
            > apply_all.index("-- FILE: discord/supabase/seeds/v5_investigations.sql"),
            "literal-Seventh retirement must run after all legacy and V5 seeds")
    if choreography["status"] != "offline_authored_not_deployed":
        projection = by_id.get("DISCORD_EVENT_PROJECTION", {})
        require("enforce_nonce=true" in projection.get("primitive", "")
                and "never echoed" in projection.get("data_shape_normalization", "")
                and "external" in projection.get("proof", "").casefold()
                and "unverified" in projection.get("proof", "").casefold(),
                "implemented Discord projection lacks idempotency, payload isolation, or honest staging gap")
    require(by_id["MC_SIGN_SHORT_INPUT"]["used_by"] == []
            and by_id["MC_WRITABLE_BOOK_INPUT"]["used_by"] == [],
            "unproven sign/book input is active")
    forbidden_assumptions = set(inputs["forbidden_assumptions"])
    require({"a lectern accepts free text", "a styled object is a web form",
             "ordinary chat is a required parser", "media submits an answer",
             "client overflow is acceptable", "source interaction receipts gate a correct answer"}.issubset(forbidden_assumptions),
            "platform input fail-closed assumptions are incomplete")
    require("an interpretive conclusion requires a hidden long canonical sentence" in forbidden_assumptions,
            "hidden canonical prose assumption is not forbidden")

    safe_copy = story_authority["safe_player_copy"]
    baseline_guardrails = {
        "conclusion_printed_by_one_source": any(case["conclusion_printed_verbatim"] for case in cases),
        "minimum_case_surface_count": min(len({row["surface"] for row in case["distributed_fragments"]}) for case in cases),
        "correctness_requires_receipt": redesign["answer_or_action_requires_observation_receipts"],
        "minimum_player_action_count": min(len(case["player_initiated_actions"]) for case in cases),
        "minimum_world_response_count": min(len(case["authored_reactivity"]) for case in cases),
        "answer_box_dominates_adjacent_cases": qualitative["answer_input_dominates_any_adjacent_pair"],
        "major_story_optional": story_map["major_threads_optional"],
        "named_player_eligibility": story_map["named_player_eligibility"],
        "unsafe_personalized_copy": not (safe_copy["registered_surface_only"]
                                           and safe_copy["allowlisted_tokens_only"]
                                           and safe_copy["bounded_pattern"]
                                           and safe_copy["deterministic_alteration"]
                                           and len(safe_copy["forbidden_inputs"]) >= 8),
        "lectern_text_input": input_rules["lectern_free_text_input"],
        "decorative_fake_form": input_rules["fake_decorative_form"],
        "required_chat_parser": input_rules["required_unrestricted_chat_parser"],
        "input_absent_from_pinned_platform": False,
        "client_input_overflow": False,
        "decoded_plaintext_needlessly_opaque": language["decoded_plaintext_needlessly_opaque"],
        "source_click_prerequisite": functional["correctness_requires_observation_receipts"],
        "hidden_long_canonical_phrase": answer_shape["interpretive_conclusion_hidden_canonical_sentence"],
    }
    require(not guardrail_violations(baseline_guardrails),
            f"offline ARG negative contract violated: {guardrail_violations(baseline_guardrails)}")

    evidence, answers = active_evidence_and_answers()
    folded_evidence = [folded(value) for value in evidence if value]
    for answer_id, answer in answers:
        normalized = folded(answer)
        if len(normalized) < 16:
            continue
        require(not any(normalized in source for source in folded_evidence),
                f"{answer_id}: accepted conclusion is printed verbatim in one active source")

    corpus = "\n".join(walk_strings([redesign, story_authority, story_map, grammar, inputs])).casefold()
    for forbidden in ("mechanism_type", "puzzle_type", "click every source", "receipt required"):
        require(forbidden not in corpus, f"closed/stale ARG design token present: {forbidden}")
    print("ARG EXPERIENCE AUTHORITY: PASS - story-first P1-P12 map, required parallel spines, real platform inputs, layered puzzle depth, zero receipt gating, and exact authored choreography")


if __name__ == "__main__":
    main()
