#!/usr/bin/env python3
"""Build the versioned M2 logical and migration manifests from approved authorities.

This generator is deliberately deterministic. It reads only committed design authorities and writes
JSON under design/m2/generated. It never contacts or mutates Supabase or another live service.
"""

from __future__ import annotations

import csv
import hashlib
import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "design" / "m2" / "generated"
EVIDENCE_PATH = ROOT / "design" / "handoff" / "PHASE-2-EVIDENCE-ARCHITECTURE.md"
DISPOSITION_PATH = ROOT / "design" / "handoff" / "PHASE-2-LEGACY-NODE-DISPOSITION.md"
NODE_PATH = ROOT / "design" / "ARG-V5-NODE-MANIFEST.csv"
ARTIFACT_PATH = ROOT / "design" / "ARG-V5-ARTIFACT-MANIFEST.csv"

VERSION = "2.0.0-m2"
AUTHORITY = "PHASE-2-EVIDENCE-ARCHITECTURE.md"


def normalized_sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes().replace(b"\r\n", b"\n")).hexdigest()


def write_json(name: str, payload: object) -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    path = OUT / name
    path.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8", newline="\n")


def parse_arcs(text: str) -> list[dict[str, object]]:
    arcs: list[dict[str, object]] = []
    headings = list(re.finditer(r"^## (P(?P<number>\d+)) — (?P<title>.+)$", text, re.MULTILINE))
    for index, heading in enumerate(headings):
        start = heading.end()
        end = headings[index + 1].start() if index + 1 < len(headings) else text.find("## Pacing", start)
        section = text[start:end if end >= 0 else None]
        fields: dict[str, str] = {}
        for row in re.finditer(r"^\| (?P<key>[^|]+?) \| (?P<value>.*?) \|$", section, re.MULTILINE):
            fields[row.group("key").strip()] = row.group("value").strip().replace("**", "")
        number = int(heading.group("number"))
        arcs.append({
            "arc_id": f"P{number}",
            "ordinal": number,
            "title": heading.group("title").strip(),
            "duration_contract": fields.get("Duration / group split", ""),
            "core_revelation": fields.get("Core revelation", ""),
            "required_prior_knowledge": fields.get("Required prior knowledge", ""),
            "investigation_identity": fields.get("Distinct investigation identity", ""),
            "submission_surface": fields.get("Submission surface", ""),
            "emotional_function": fields.get("Emotional function", ""),
            "progression": "ordered_arc_free_interior_any_subset",
        })
    return arcs


def expand_finding(label: str, body: str) -> list[tuple[str, str, str]]:
    label = " ".join(label.split()).rstrip(".")
    range_match = re.fullmatch(r"(P11)\.F2[–-]F7 — Six independent letters", label)
    if range_match:
        names = [(2, "Vaun A provenance"), (3, "Mara V provenance"),
                 (4, "Sella E provenance"), (5, "Orin R provenance"),
                 (6, "Brann Y provenance"), (7, "Iss N provenance")]
        return [(f"P11.F{number}", title, body.strip()) for number, title in names]
    match = re.fullmatch(r"(?P<id>P\d+\.(?:F\d+|[VMSOBI])) — (?P<title>.+)", label)
    if not match:
        raise ValueError(f"Unrecognized Phase 2 finding label: {label}")
    return [(match.group("id"), match.group("title").rstrip("."), body.strip())]


def parse_findings(text: str) -> list[dict[str, object]]:
    pattern = re.compile(
        r"^\d+\. \*\*(?P<label>P\d+\.[^*]+?)\*\* (?P<body>.*?)"
        r"(?=^\d+\. \*\*|^Hints/recovery:|^## P\d+|^## Pacing)",
        re.MULTILINE | re.DOTALL,
    )
    findings: list[dict[str, object]] = []
    for match in pattern.finditer(text):
        body = " ".join(match.group("body").split())
        for finding_id, title, expanded_body in expand_finding(match.group("label"), body):
            arc_id = finding_id.split(".", 1)[0]
            findings.append({
                "finding_id": finding_id,
                "arc_id": arc_id,
                "title": title,
                "evidence_contract": expanded_body,
                "observation_ids": [f"{finding_id}.OBS"],
                "acceptance": "all_declared_observation_sources_and_provenance_required",
                "attendance_is_not_a_predicate": True,
                "elapsed_time_is_not_a_predicate": True,
            })
    return findings


def observation_manifest(findings: list[dict[str, object]]) -> list[dict[str, object]]:
    observations: list[dict[str, object]] = []
    for finding in findings:
        contract = str(finding["evidence_contract"])
        first_sentence = contract.split(". ", 1)[0].rstrip(".") + "."
        observations.append({
            "observation_id": f"{finding['finding_id']}.OBS",
            "arc_id": finding["arc_id"],
            "supports_finding": finding["finding_id"],
            "kind": "source_bundle",
            "source_summary": first_sentence,
            "custody": "protected_original_or_exact_versioned_replay",
            "minimum_independent_sources": 2,
            "replayable": True,
            "spoiler_boundary": finding["arc_id"],
        })
    return observations


def extract_finding_ids(value: str) -> list[str]:
    result: list[str] = []
    for match in re.finditer(r"P(\d+)\.F(\d+)[–-]F?(\d+)", value):
        arc, first, last = map(int, match.groups())
        result.extend(f"P{arc}.F{number}" for number in range(first, last + 1))
    scrubbed = re.sub(r"P\d+\.F\d+[–-]F?\d+", "", value)
    result.extend(re.findall(r"P\d+\.(?:F\d+|[VMSOBI])", scrubbed))
    return list(dict.fromkeys(result))


def parse_dispositions(node_flags: dict[str, str], finding_ids: set[str]) -> list[dict[str, object]]:
    text = DISPOSITION_PATH.read_text(encoding="utf-8")
    rows: list[dict[str, object]] = []
    for line in text.splitlines():
        if not re.match(r"^\| (?:LS|LC|KV|KM|KS|KO|KB|KI|CW|BI|HS|A|WR|AR|RP)\d{2} \|", line):
            continue
        cells = [cell.strip() for cell in line.strip().strip("|").split("|")]
        old_id, disposition, replacement, reason = cells
        targets = extract_finding_ids(replacement)
        unknown = set(targets) - finding_ids
        if unknown:
            raise ValueError(f"{old_id} maps to unknown findings {sorted(unknown)}")
        if old_id not in node_flags:
            raise ValueError(f"Disposition has no legacy completion flag: {old_id}")
        mode = {"Reuse": "alias_receipt", "Map/merge": "merge_evidence", "Retire": "tombstone"}[disposition]
        rows.append({
            "legacy_node_id": old_id,
            "legacy_completion_flag": node_flags[old_id],
            "disposition": disposition.lower().replace("/", "_"),
            "import_mode": mode,
            "target_finding_ids": targets,
            "legacy_receipt_import": "preserve_as_provenance_only",
            "auto_complete_target": False,
            "promotion_rule": "re-evaluate_all_target_finding_propositions_against_imported_sources",
            "alias_readback": disposition != "Retire",
            "executable_handler": None if disposition == "Retire" else "resolved_by_versioned_binding",
            "tombstone": disposition == "Retire",
            "reason": reason,
            "rollback": {
                "restore": "frozen_v5_binding_and_completion_flag_readback",
                "preserve_import_receipt": True,
                "delete_progress": False,
            },
            "forward_recovery": {
                "method": "replay_idempotent_import_from_legacy_flag_and_receipt",
                "requires_source_provenance": True,
                "duplicate_reward": False,
            },
        })
    if len(rows) != 82 or len({row["legacy_node_id"] for row in rows}) != 82:
        raise ValueError(f"Expected exactly 82 legacy dispositions, found {len(rows)}")
    return rows


def artifact_manifest(dispositions: dict[str, dict[str, object]]) -> list[dict[str, object]]:
    artifacts: list[dict[str, object]] = []
    with ARTIFACT_PATH.open(encoding="utf-8", newline="") as handle:
        for row in csv.DictReader(handle):
            mapping = dispositions[row["earned_node"]]
            artifacts.append({
                "artifact_id": row["artifact_id"],
                "legacy_source_node": row["earned_node"],
                "earned_from_findings": mapping["target_finding_ids"],
                "display_name": row["display_name"],
                "physical_material_is_flesh": True,
                "legacy_material": row["material"],
                "stable_identity_key": row["pdc_key"],
                "stable_identity_value": row["pdc_value"],
                "consumer": row["consumer"],
                "authority": "minecraft_local_primary",
                "provenance_required": True,
                "duplicate_scan_before_reissue": True,
                "recovery_contract": row["recovery_contract"],
            })
    return artifacts


def choices_manifest() -> dict[str, object]:
    return {
        "schema_version": VERSION,
        "choices": [
            {
                "choice_id": "wren_remembrance",
                "arc_id": "P10",
                "options": ["condemn", "understand", "free"],
                "facts_fixed_before_choice": True,
                "protected_surface": True,
                "immutable_after_commit": True,
                "automation_may_select": False,
                "affects": "remembrance_only",
            },
            {
                "choice_id": "averyn_name_treatment",
                "arc_id": "P12",
                "options": ["publish", "deliberately_unfile"],
                "plain_language_states_universal_release": True,
                "protected_surface": True,
                "immutable_after_commit": True,
                "automation_may_select": False,
                "commit_gate": "player_confirmation_plus_operator_arm",
                "affects": "coda_wording_only",
            },
        ],
        "universal_outcome": {
            "outcome_id": "record_closed_averyn_freed",
            "required_for_every_valid_path": True,
            "branch_matrix_forbidden": True,
        },
        "deliberately_unresolved": [
            "end_voice_metaphysical_continuity",
            "dark_ultimate_nature",
            "wren_post_closure_autonomy",
        ],
    }


def main() -> None:
    evidence_text = EVIDENCE_PATH.read_text(encoding="utf-8")
    arcs = parse_arcs(evidence_text)
    findings = parse_findings(evidence_text)
    finding_ids = {str(row["finding_id"]) for row in findings}
    with NODE_PATH.open(encoding="utf-8", newline="") as handle:
        node_flags = {row["node_id"]: row["completion_flag"] for row in csv.DictReader(handle)}
    legacy = parse_dispositions(node_flags, finding_ids)
    legacy_by_id = {str(row["legacy_node_id"]): row for row in legacy}
    common = {
        "schema_version": VERSION,
        "authority": AUTHORITY,
        "authority_sha256_lf": normalized_sha(EVIDENCE_PATH),
    }
    write_json("arc-manifest.json", {**common, "arcs": arcs})
    write_json("finding-manifest.json", {**common, "findings": findings})
    write_json("observation-manifest.json", {**common, "observations": observation_manifest(findings)})
    write_json("artifact-manifest.json", {**common, "artifacts": artifact_manifest(legacy_by_id)})
    write_json("choice-manifest.json", {**common, **choices_manifest()})
    write_json("legacy-import-map.json", {
        "schema_version": VERSION,
        "authority": "PHASE-2-LEGACY-NODE-DISPOSITION.md",
        "authority_sha256_lf": normalized_sha(DISPOSITION_PATH),
        "policy": {
            "legacy_count_is_not_a_design_quota": True,
            "default_import_is_provenance_not_completion": True,
            "retirement_is_tombstone_not_delete": True,
            "rollback_preserves_all_import_receipts": True,
        },
        "entries": legacy,
    })
    manifest_names = [
        "arc-manifest.json", "finding-manifest.json", "observation-manifest.json",
        "artifact-manifest.json", "choice-manifest.json", "legacy-import-map.json",
    ]
    file_hashes = {name: hashlib.sha256((OUT / name).read_bytes()).hexdigest() for name in manifest_names}
    set_receipt = "".join(f"{name}:{file_hashes[name]}\n" for name in manifest_names).encode("utf-8")
    write_json("manifest-index.json", {
        "schema_version": VERSION,
        "manifests": [{"path": name, "sha256": file_hashes[name]} for name in manifest_names],
        "manifest_set_sha256": hashlib.sha256(set_receipt).hexdigest(),
        "counts": {
            "arcs": len(arcs), "findings": len(findings), "observation_sets": len(findings),
            "artifacts": len(artifact_manifest(legacy_by_id)), "protected_choices": 2,
            "legacy_dispositions": len(legacy),
        },
        "invariants": {
            "ordered_arcs": 12,
            "legacy_node_quota": False,
            "any_subset_progress": True,
            "universal_averyn_release": True,
            "deliberately_unresolved_ambiguities": 3,
        },
    })
    print(f"M2 contracts generated: {len(arcs)} arcs, {len(findings)} findings, {len(legacy)} legacy rows")


if __name__ == "__main__":
    main()
