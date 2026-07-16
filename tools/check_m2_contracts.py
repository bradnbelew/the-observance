#!/usr/bin/env python3
"""Static, non-live M2 authority and safety contract check."""
from __future__ import annotations

import csv
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
GEN = ROOT / "design" / "m2" / "generated"
LF_HASH = "16de527496a6c4e3ae0fc093db07b74754be55193059f1c8d3fe9ab0c29a595a"
MIXED_HASH = "37020e754a8048d96e853cc7711f94656b4e66bc183783b9f903947bab585a9b"
SEMANTIC_HASH = "d2eec35f58cf79a30f2255f429cb0d19a5c1e8b5bd7942604b3bef724272cbf6"


def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def load(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit(f"M2 contract check FAILED: {message}")


def main() -> None:
    index = load(GEN / "manifest-index.json")
    digest_lines: list[str] = []
    for item in index["manifests"]:
        actual = sha((GEN / item["path"]).read_bytes())
        require(actual == item["sha256"], f"manifest digest drift: {item['path']}")
        digest_lines.append(f"{item['path']}:{actual}")
    require(sha(("\n".join(digest_lines) + "\n").encode()) == index["manifest_set_sha256"],
            "manifest set digest drift")
    require(index["counts"] == {"arcs": 12, "findings": 68, "observation_sets": 68,
            "artifacts": 21, "protected_choices": 2, "legacy_dispositions": 82}, "manifest counts")

    arcs = load(GEN / "arc-manifest.json")["arcs"]
    require([arc["ordinal"] for arc in arcs] == list(range(1, 13)), "P1-P12 ordering")
    require(all(arc["progression"] == "ordered_arc_free_interior_any_subset" for arc in arcs),
            "any-subset progression")
    findings = load(GEN / "finding-manifest.json")["findings"]
    finding_ids = {item["finding_id"] for item in findings}
    require(len(finding_ids) == 68, "finding IDs must be unique")
    observations = load(GEN / "observation-manifest.json")["observations"]
    require({item["supports_finding"] for item in observations} == finding_ids, "observation coverage")

    choices = load(GEN / "choice-manifest.json")
    require({item["choice_id"] for item in choices["choices"]}
            == {"wren_remembrance", "averyn_name_treatment"}, "exactly two protected choices")
    require(all(not item["automation_may_select"] for item in choices["choices"]), "choice automation forbidden")
    require(choices["universal_outcome"]["required_for_every_valid_path"], "universal Averyn release")
    require(len(choices["deliberately_unresolved"]) == 3, "exactly three ambiguities")

    with (ROOT / "design" / "ARG-V5-NODE-MANIFEST.csv").open(encoding="utf-8-sig", newline="") as handle:
        legacy_source = {row["node_id"]: row["completion_flag"] for row in csv.DictReader(handle)}
    legacy = load(GEN / "legacy-import-map.json")["entries"]
    require(len(legacy) == 82 == len(legacy_source), "all 82 legacy nodes")
    require({row["legacy_node_id"] for row in legacy} == set(legacy_source), "legacy ID coverage")
    for row in legacy:
        require(row["legacy_completion_flag"] == legacy_source[row["legacy_node_id"]], "legacy flag mismatch")
        require(not row["auto_complete_target"], "legacy import may not auto-complete")
        require(row["rollback"]["preserve_import_receipt"] and not row["rollback"]["delete_progress"],
                "rollback preservation")
        require(row["forward_recovery"]["duplicate_reward"] is False, "forward idempotency")
        if row["disposition"] == "retire":
            require(row["tombstone"] and row["executable_handler"] is None, "retirement tombstone")

    predicate = (ROOT / "design" / "ARG-V5-PHYSICAL-PREDICATES.json").read_bytes()
    require(sha(predicate) == LF_HASH and b"\r\n" not in predicate, "checked-out predicate LF bytes")
    semantic = json.dumps(json.loads(predicate), ensure_ascii=False, sort_keys=True,
                          separators=(",", ":")).encode("utf-8")
    require(sha(semantic) == SEMANTIC_HASH, "predicate semantic digest")
    chain = load(ROOT / "design" / "m2" / "predicate-authority-chain.json")
    mixed_lines = set(chain["versions"][0]["lf_only_line_numbers"])
    lines = predicate.splitlines(keepends=True)
    reconstructed = b"".join(line.rstrip(b"\r\n") + (b"\n" if number in mixed_lines else b"\r\n")
                             for number, line in enumerate(lines, 1))
    require(sha(reconstructed) == MIXED_HASH and len(reconstructed) == 138349,
            "exact historical 37020 byte reconstruction")
    for path in [ROOT / "discord" / "supabase" / "seeds" / "v5_investigations.sql",
                 ROOT / "discord" / "supabase" / "apply-all.sql"]:
        text = path.read_text(encoding="utf-8")
        require(LF_HASH in text and MIXED_HASH not in text, f"same-release seed parity: {path.name}")

    surfaces = load(ROOT / "design" / "m2" / "cross-surface-contracts.json")
    require(len(surfaces["surfaces"]) == 7, "seven cross-surface ownership contracts")
    require(surfaces["same_release_receipt"]["production_requires_real_platform_confirmation"],
            "production confirmation rule")

    up = (ROOT / "design" / "m2" / "sql" / "contract-v1.up.sql").read_text(encoding="utf-8")
    rollback = (ROOT / "design" / "m2" / "sql" / "contract-v1.rollback.sql").read_text(encoding="utf-8")
    forward = (ROOT / "design" / "m2" / "sql" / "contract-v1.forward.sql").read_text(encoding="utf-8")
    for term in ["campaign_group_members", "finding_contributors", "protected_choice_commits",
                 "hint_approvals_v2", "media_reveal_receipts_v2", "projection_outbox_v2",
                 "legacy_import_receipts_v2", "release_parity_receipts_v2",
                 "force row level security", "revoke all on table"]:
        require(term in up, f"schema contract missing {term}")
    require(not any(term in rollback.lower() for term in ["drop table", "truncate ", "delete from"]),
            "rollback must retain receipts")
    require("on conflict (idempotency_key) do nothing" in forward, "forward replay idempotency")
    require(not (ROOT / "discord" / "supabase" / "migrations" / "contract-v1.up.sql").exists(),
            "proposal must not masquerade as applied migration")

    validator = (ROOT / "tools" / "validate_m2_supabase.ps1").read_text(encoding="utf-8")
    require('ValidateSet("local")' in validator and "ProductionProjectRef" in validator,
            "Supabase validation harness must remain local-only and production-blocked")
    require('migration", "new"' in validator and 'db", "advisors"' in validator,
            "Supabase validation harness must use CLI scaffolding and both advisor paths")
    for test_name in ["local-baseline.sql", "lifecycle-seed.sql", "assert-forward.sql",
                      "assert-rollback.sql", "assert-final.sql", "contract-v1.test.sql"]:
        require((ROOT / "design" / "m2" / "sql" / "tests" / test_name).exists(),
                f"missing disposable database validation input: {test_name}")
    database_test = (ROOT / "design" / "m2" / "sql" / "tests" / "contract-v1.test.sql").read_text(
        encoding="utf-8")
    for term in ["relforcerowsecurity", "anon cannot read", "service_role",
                 "append-only", "idempotency key", SEMANTIC_HASH]:
        require(term in database_test, f"database test coverage missing {term}")

    ambient = (ROOT / "plugin" / "src" / "main" / "java" / "com" / "observance" / "watcher"
               / "beats" / "AmbientBeatGenerator.java").read_text(encoding="utf-8")
    whisper = (ROOT / "discord" / "src" / "bot" / "commands" / "whisper.ts").read_text(encoding="utf-8")
    require("name_on_wall" not in ambient, "no automatic name_on_wall")
    require("'hint_request'" in whisper and "'pending'" in whisper, "hint request remains pending")
    for forbidden in ["spendWhisper", "recordWhisperEvent", "voice.whisperReply", "'whisper_toll'"]:
        require(forbidden not in whisper, f"automatic solution hint path remains: {forbidden}")

    print("M2 contract check OK — manifests, 82 imports, predicate bytes, schema, surfaces, and automation gates")


if __name__ == "__main__":
    main()
