#!/usr/bin/env python3
"""Require every authored P5-P12 evidence record to participate in the campaign's callback web."""

from __future__ import annotations

import json
import re
from collections import defaultdict, deque
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
CASE_DIR = ROOT / "campaign/p5-p12"
WEB = ROOT / "campaign/evidence-relationship-web.json"
PHASES = [f"P{i}" for i in range(5, 13)]


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit(f"EVIDENCE RELATIONSHIP WEB: FAIL - {message}")


def evidence_rows(value: object) -> list[dict]:
    rows: list[dict] = []
    if isinstance(value, dict):
        item_id = value.get("id")
        if isinstance(item_id, str) and re.fullmatch(r"p(?:[5-9]|1[0-2])\.[a-z]\d+", item_id):
            rows.append(value)
        for child in value.values():
            rows.extend(evidence_rows(child))
    elif isinstance(value, list):
        for child in value:
            rows.extend(evidence_rows(child))
    return rows


def reachable(graph: dict[str, set[str]], start: str) -> set[str]:
    seen = {start}
    queue = deque([start])
    while queue:
        for target in graph[queue.popleft()]:
            if target not in seen:
                seen.add(target)
                queue.append(target)
    return seen


def main() -> None:
    records: dict[str, dict] = {}
    for path in sorted(CASE_DIR.glob("case-p*.json")):
        for row in evidence_rows(json.loads(path.read_text(encoding="utf-8"))):
            require(row["id"] not in records, f"duplicate evidence id {row['id']}")
            records[row["id"]] = row

    data = json.loads(WEB.read_text(encoding="utf-8"))
    rules = data["rules"]
    require(rules["all_authored_evidence_is_connected"] is True, "not all evidence must be connected")
    require(rules["source_touch_gates_correctness"] is False, "source-touch gating returned")
    require(rules["closed_mechanism_catalog"] is False, "relationship graph became a puzzle catalog")
    require(rules["every_record_reaches_coda"] is True, "records may terminate without payoff")

    directed: dict[str, set[str]] = defaultdict(set)
    undirected: dict[str, set[str]] = defaultdict(set)
    incident = defaultdict(int)
    pairs: set[tuple[str, str]] = set()
    cross_phase = defaultdict(int)
    for edge in data["relationships"]:
        source, target = edge["from"], edge["to"]
        require(source in records and target in records, f"unknown evidence edge {source} -> {target}")
        require(source != target and (source, target) not in pairs, f"duplicate/self edge {source} -> {target}")
        pairs.add((source, target))
        source_phase, target_phase = source.split(".")[0].upper(), target.split(".")[0].upper()
        require(PHASES.index(source_phase) <= PHASES.index(target_phase),
                f"relationship breaks revelation order: {source} -> {target}")
        require(len(edge["kind"].strip()) >= 4, f"{source} -> {target}: relationship kind is empty")
        require(len(edge["why"].strip()) >= 75, f"{source} -> {target}: relationship reason is not authored")
        directed[source].add(target)
        undirected[source].add(target)
        undirected[target].add(source)
        incident[source] += 1
        incident[target] += 1
        if source_phase != target_phase:
            cross_phase[source_phase] += 1

    require(set(records) == set(incident),
            f"isolated evidence records: {sorted(set(records) - set(incident))}")
    start = next(iter(records))
    require(reachable(undirected, start) == set(records), "evidence web contains a disconnected island")
    terminal = "p12.e07"
    require(terminal in records, "coda evidence is missing")
    for record_id in records:
        if record_id == terminal:
            continue
        require(terminal in reachable(directed, record_id), f"{record_id}: no directed callback/payoff route to coda")
    for phase in PHASES[:-1]:
        require(cross_phase[phase] > 0, f"{phase}: no evidence relationship crosses into a later phase")

    role_counts = defaultdict(int)
    for row in records.values():
        for role in row.get("roles", []):
            role_counts[role] += 1
    require(len(role_counts) >= 10, "evidence roles collapsed despite a connected graph")
    print(
        "EVIDENCE RELATIONSHIP WEB: PASS - "
        f"{len(records)} authored records, {len(pairs)} explicit relationships, "
        f"{len(role_counts)} evidence roles, every record -> P12 coda"
    )


if __name__ == "__main__":
    main()
