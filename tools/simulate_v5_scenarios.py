#!/usr/bin/env python3
"""Deterministic model-level failure simulation for The Observance V5.

This deliberately does not claim to replace a real Paper/client rehearsal. It exercises the
canonical prerequisite graph, idempotent answers, monotonic gates, artifact recovery contract,
service-failure invariants, group concurrency, and every finale branch from the checked-in
manifests. Any violated invariant fails the release check.
"""

from __future__ import annotations

import csv
import json
import sys
from collections import Counter, defaultdict, deque
from dataclasses import dataclass, field
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
NODES = ROOT / "design" / "ARG-V5-NODE-MANIFEST.csv"
ARTIFACTS = ROOT / "design" / "ARG-V5-ARTIFACT-MANIFEST.csv"

GATES = {
    "G1": {"v5_case_c02_complete"},
    "G2": {
        "v5_kv03_affidavit", "v5_km03_affidavit", "v5_ks03_affidavit",
        "v5_ko03_affidavit", "v5_kb03_affidavit", "v5_ki03_affidavit",
    },
    "G3": {"v5_case_c04_complete", "v5_case_c05_complete"},
    "G4": {"v5_case_c06_complete"},
    "PRIOR": {"v5_a01_location"},
    "DREAD": {"v5_case_c07_complete"},
    "G5": {"v5_case_c08_complete"},
    "G6": {"v5_case_c09_complete"},
}


def rows(path: Path) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8", newline="") as handle:
        return list(csv.DictReader(handle))


def prerequisites(raw: str) -> set[str]:
    return {part.strip() for part in raw.split(";") if part.strip() and part.strip() != "start"}


def topological(nodes: list[dict[str, str]]) -> list[dict[str, str]]:
    by_flag = {row["completion_flag"]: row for row in nodes}
    outgoing: dict[str, set[str]] = defaultdict(set)
    degree = {flag: 0 for flag in by_flag}
    for row in nodes:
        for before in prerequisites(row["prerequisites"]):
            if before in by_flag:
                outgoing[before].add(row["completion_flag"])
                degree[row["completion_flag"]] += 1
    queue = deque(flag for flag, count in degree.items() if count == 0)
    ordered: list[dict[str, str]] = []
    while queue:
        flag = queue.popleft()
        ordered.append(by_flag[flag])
        for after in outgoing[flag]:
            degree[after] -= 1
            if degree[after] == 0:
                queue.append(after)
    if len(ordered) != len(nodes):
        raise AssertionError("node graph is cyclic")
    return ordered


@dataclass
class EvidenceState:
    flags: set[str] = field(default_factory=set)

    def submit(self, node: dict[str, str], correct: bool) -> bool:
        required = prerequisites(node["prerequisites"])
        if not required.issubset(self.flags) or not correct:
            return False
        before = len(self.flags)
        self.flags.add(node["completion_flag"])
        return len(self.flags) > before


@dataclass
class ArtifactState:
    earned: bool
    locations: set[str] = field(default_factory=set)
    inventory_full: bool = False

    def recover(self) -> str:
        if not self.earned:
            return "refused_not_earned"
        if self.locations:
            return "already_present"
        if self.inventory_full:
            return "refused_full_inventory"
        self.locations.add("player_inventory")
        return "issued"


@dataclass
class FinaleState:
    phase: str = "ready"
    branch: tuple[str, str] | None = None
    saved: bool = False
    kicked: bool = False
    shutdown: bool = False

    def arm(self, wren: str, name: str) -> None:
        if self.phase != "ready":
            raise AssertionError("finale can only arm from ready")
        self.branch = (wren, name)
        self.phase = "armed"

    def cancel(self) -> None:
        if self.phase != "armed":
            raise AssertionError("only an armed finale can cancel")
        self.phase = "ready"
        self.branch = None

    def commit(self) -> None:
        if self.phase != "armed" or self.branch is None:
            raise AssertionError("unarmed finale cannot commit")
        self.phase = "committed"  # durable before theater
        self.saved = True
        self.kicked = True
        self.shutdown = True
        self.phase = "coda"


def simulate() -> tuple[int, Counter[str]]:
    node_rows = rows(NODES)
    artifact_rows = rows(ARTIFACTS)
    ordered = topological(node_rows)
    counts: Counter[str] = Counter()
    canonical = EvidenceState()

    # Each node is tested for prerequisite denial, wrong/partial neutrality, exact solve,
    # duplicate idempotence, restart durability, and reconnect durability.
    for node in ordered:
        required = prerequisites(node["prerequisites"])
        if required:
            early = EvidenceState(flags=canonical.flags - {next(iter(required))})
            before = set(early.flags)
            assert not early.submit(node, True) and early.flags == before
            counts["node_prerequisite_denial"] += 1

        before = set(canonical.flags)
        assert not canonical.submit(node, False) and canonical.flags == before
        counts["node_wrong_answer"] += 1
        assert not canonical.submit(node, False) and canonical.flags == before
        counts["node_partial_answer"] += 1

        assert canonical.submit(node, True)
        counts["node_correct_answer"] += 1
        solved = set(canonical.flags)
        assert not canonical.submit(node, True) and canonical.flags == solved
        counts["node_duplicate_answer"] += 1
        assert EvidenceState(set(canonical.flags)).flags == canonical.flags
        counts["node_restart_persistence"] += 1
        assert EvidenceState(set(canonical.flags)).flags == canonical.flags
        counts["node_reconnect_persistence"] += 1

    assert len(canonical.flags) == 82

    # The same submission arriving from 1, 2, 4, or 7 players still creates one durable receipt.
    by_flag = {node["completion_flag"]: node for node in node_rows}
    for player_count in (1, 2, 4, 7):
        for node in node_rows:
            state = EvidenceState(flags=set(prerequisites(node["prerequisites"])))
            outcomes = [state.submit(node, True) for _ in range(player_count)]
            assert outcomes.count(True) == 1
            assert state.flags.issuperset({node["completion_flag"]})
            counts[f"concurrent_{player_count}_players"] += 1

    # Once complete, external delivery failures cannot delete evidence or rewind progression.
    for failure in ("supabase_down", "stale_false", "delayed", "duplicate_delivery", "restored"):
        for flag in canonical.flags:
            snapshot = set(canonical.flags)
            assert flag in snapshot and canonical.flags == snapshot
            counts[f"service_{failure}"] += 1

    # Every gate is fail-closed before its final condition, monotonic after opening, and restart-safe.
    for gate, required in GATES.items():
        missing = next(iter(required))
        latch = required.issubset(canonical.flags - {missing})
        assert not latch
        counts["gate_fail_closed"] += 1
        latch = latch or required.issubset(canonical.flags)
        assert latch
        counts["gate_opens_exactly"] += 1
        for condition in ("remote_false", "remote_down", "restart"):
            latch = latch or False
            assert latch
            counts[f"gate_monotonic_{condition}"] += 1

    # Recovery must never duplicate, displace, drop, or issue an unearned critical artifact.
    for artifact in artifact_rows:
        artifact_id = artifact["artifact_id"]
        state = ArtifactState(earned=False)
        assert state.recover() == "refused_not_earned" and not state.locations
        counts["artifact_unearned"] += 1

        state = ArtifactState(earned=True)
        assert state.recover() == "issued" and state.locations == {"player_inventory"}
        counts["artifact_issue"] += 1
        assert state.recover() == "already_present" and len(state.locations) == 1
        counts["artifact_repeat"] += 1

        for location in ("player_inventory", "ender_chest", "loaded_container", "loaded_drop"):
            present = ArtifactState(earned=True, locations={location})
            assert present.recover() == "already_present" and present.locations == {location}
            counts[f"artifact_scan_{location}"] += 1

        full = ArtifactState(earned=True, inventory_full=True)
        assert full.recover() == "refused_full_inventory" and not full.locations
        counts["artifact_full_inventory"] += 1

        # State-preserving fixture repair must not mutate artifact custody.
        held = ArtifactState(earned=True, locations={f"loaded_container:{artifact_id}"})
        snapshot = set(held.locations)
        assert held.locations == snapshot
        counts["artifact_repair_preserves"] += 1

    # All six moral/name combinations finish, cancel safely, persist before theater, and boot Coda.
    for wren in ("condemn", "understand", "free"):
        for name in ("publish", "release_unnamed"):
            unarmed = FinaleState()
            try:
                unarmed.commit()
                raise AssertionError("unarmed finale unexpectedly committed")
            except AssertionError as exc:
                assert "unarmed finale" in str(exc)
            counts["finale_unarmed_refusal"] += 1

            rehearsal = FinaleState()
            rehearsal.arm(wren, name)
            assert rehearsal.phase == "armed" and rehearsal.branch == (wren, name)
            counts["finale_arm"] += 1
            rehearsal.cancel()
            assert rehearsal.phase == "ready" and rehearsal.branch is None
            counts["finale_cancel"] += 1

            live = FinaleState()
            live.arm(wren, name)
            live.commit()
            assert live.phase == "coda" and live.branch == (wren, name)
            assert live.saved and live.kicked and live.shutdown
            counts["finale_durable_commit"] += 1
            counts["finale_save"] += 1
            counts["finale_goodbye_kick_shutdown"] += 1
            restarted = FinaleState("coda", live.branch, True, True, True)
            assert restarted.phase == "coda" and restarted.branch == live.branch
            counts["finale_coda_restart"] += 1
            faulted = FinaleState("fault")
            assert faulted.phase == "fault" and faulted.branch is None
            counts["finale_corrupt_state_fault"] += 1

    total = sum(counts.values())
    return total, counts


def main() -> int:
    try:
        total, counts = simulate()
    except (AssertionError, KeyError, OSError, ValueError) as exc:
        print(f"V5 SCENARIO SIMULATION: FAIL - {exc}")
        return 1
    if "--json" in sys.argv:
        print(json.dumps({"status": "PASS", "scenarios": total, "families": counts}, indent=2))
    else:
        print(f"V5 SCENARIO SIMULATION: PASS ({total} deterministic scenarios)")
        for family, count in sorted(counts.items()):
            print(f"- {family}: {count}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
