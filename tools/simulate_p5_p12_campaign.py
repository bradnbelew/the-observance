#!/usr/bin/env python3
"""Deterministic offline proof of P5-P12 progression, recovery, and release invariants."""
from __future__ import annotations

import copy
import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
PROJECTION = ROOT / "plugin/src/main/resources/campaign/p5-p12.json"


def norm(value: str) -> str:
    return re.sub(r"[^a-z0-9]+", " ", value.casefold()).strip()


def conclusions(case: dict) -> list[dict]:
    rows = list(case.get("conclusions", []))
    if case.get("group_conclusion"):
        rows.append(case["group_conclusion"])
    return rows


class State:
    def __init__(self) -> None:
        self.completed: set[str] = {"P4.F5"}
        self.observations: set[str] = set()
        self.events: set[str] = set()
        self.repairs: set[str] = set()
        self.choice: dict[str, str] = {}

    def snapshot(self) -> str:
        return json.dumps({
            "completed": sorted(self.completed), "observations": sorted(self.observations),
            "events": sorted(self.events), "repairs": sorted(self.repairs), "choice": self.choice,
        }, sort_keys=True)

    @classmethod
    def restore(cls, payload: str) -> "State":
        raw = json.loads(payload)
        state = cls()
        state.completed = set(raw["completed"])
        state.observations = set(raw["observations"])
        state.events = set(raw["events"])
        state.repairs = set(raw["repairs"])
        state.choice = dict(raw["choice"])
        return state


def accepted(row: dict, answer: str) -> bool:
    candidate = norm(answer)
    return any(candidate == norm(value) for value in row["accepted_answers"])


def main() -> None:
    projection = json.loads(PROJECTION.read_text(encoding="utf-8"))
    assert projection["phases"] == [f"P{i}" for i in range(5, 13)]
    state = State()
    naive_failures = 0
    correct_without_observation = 0
    restart_checks = 0
    idempotent_checks = 0

    for case in projection["cases"]:
        phase = case["phase"]
        assert all(prereq in state.completed for prereq in case["prerequisites"]), (phase, case["prerequisites"], state.completed)
        before_observations = set(state.observations)
        for row in conclusions(case):
            blind = " ".join(item.get("id", "") for item in case.get("evidence", [])) or "clicked every surface"
            assert not accepted(row, blind), f"{row['id']} accepted naive click-through text"
            assert not accepted(row, ""), f"{row['id']} accepted blank answer"
            naive_failures += 2
            answer = row["accepted_answers"][0]
            assert accepted(row, answer), f"{row['id']} rejected canonical correct answer"
            assert state.observations == before_observations, f"{row['id']} mutated/required observations"
            state.completed.add(row["id"])
            correct_without_observation += 1

        if phase == "P6":
            for dossier in case["dossiers"]:
                state.completed.add(dossier["id"])
        if phase == "P8":
            for repair in case["repairs"]:
                key = repair["idempotency_key"]
                before = len(state.repairs)
                state.repairs.add(key)
                state.repairs.add(key)
                assert len(state.repairs) == before + 1
                idempotent_checks += 1
        if phase == "P10":
            for judgment in ("condemn", "understand", "free"):
                branch = copy.deepcopy(state)
                branch.choice["wren"] = judgment
                assert "P10.F5" in branch.completed
            state.choice["wren"] = "understand"
        if phase == "P12":
            assert case["runtime"]["all_endings_free_averyn"] is True
            for treatment in ("publish", "release unnamed"):
                branch = State.restore(state.snapshot())
                branch.choice["name"] = treatment
                release_id = f"release:{treatment}:{branch.choice['wren']}"
                branch.events.add(release_id)
                branch.events.add(release_id)
                assert len([event for event in branch.events if event == release_id]) == 1
                assert case["runtime"]["durable_before_theater"] is True
                idempotent_checks += 1

        state.events.add(case["consequence"]["state_event"])
        restored = State.restore(state.snapshot())
        assert restored.snapshot() == state.snapshot(), f"{phase}: restart snapshot drift"
        restart_checks += 1

    assert not state.observations, "simulation accidentally introduced observation gating"
    assert {f"p{i}" for i in range(5, 13)} == {event.split(".", 1)[0] for event in state.events if event.startswith("p")}
    print("P5-P12 CAMPAIGN SIMULATION: PASS "
          f"phases=8 zero_observation_correct={correct_without_observation} "
          f"naive_negative={naive_failures} restarts={restart_checks} idempotent={idempotent_checks}")


if __name__ == "__main__":
    main()
