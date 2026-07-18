#!/usr/bin/env python3
"""Fail closed if the authored campaign collapses into disconnected case packets."""

from __future__ import annotations

import json
import re
from collections import defaultdict, deque
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
WEB_PATH = ROOT / "campaign/campaign-web.json"
CASE_DIR = ROOT / "campaign/p5-p12"
EVENT_POLICY = ROOT / "dashboard/src/lib/arg-event-policy.ts"
STORY_MAP = ROOT / "campaign/story-interaction-map.json"
CODA_PAGE = ROOT / "dashboard/src/app/community/2011/05/18/archive-closed/page.tsx"
PHASES = [f"P{i}" for i in range(1, 13)]


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit(f"CAMPAIGN WEB: FAIL - {message}")


def load(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def walk_evidence_ids(value: object) -> set[str]:
    found: set[str] = set()
    if isinstance(value, dict):
        item_id = value.get("id")
        if isinstance(item_id, str) and re.fullmatch(r"p(?:[5-9]|1[0-2])\.[a-z]\d+", item_id):
            found.add(item_id)
        for child in value.values():
            found.update(walk_evidence_ids(child))
    elif isinstance(value, list):
        for child in value:
            found.update(walk_evidence_ids(child))
    return found


def reachable(graph: dict[str, set[str]], start: str) -> set[str]:
    seen = {start}
    todo = deque([start])
    while todo:
        current = todo.popleft()
        for target in graph[current]:
            if target not in seen:
                seen.add(target)
                todo.append(target)
    return seen


def main() -> None:
    web = load(WEB_PATH)
    story = load(STORY_MAP)
    policy_text = EVENT_POLICY.read_text(encoding="utf-8")
    event_keys = set(re.findall(r"'((?:p(?:[1-9]|1[0-2]))\.[a-z0-9_]+)'\s*:\s*event\(", policy_text))
    evidence_ids: set[str] = set()
    for case_path in sorted(CASE_DIR.glob("case-p*.json")):
        evidence_ids.update(walk_evidence_ids(load(case_path)))

    require(web["rules"]["closed_puzzle_taxonomy"] is False,
            "campaign web was reduced to a fixed mechanism catalog")
    require(web["rules"]["observation_receipts_gate_correctness"] is False,
            "campaign web reintroduced source-touch correctness gating")
    require(web["rules"]["conclusions_may_be_printed_by_one_source"] is False,
            "campaign web permits direct source-to-restatement conclusions")
    require(len(web["rules"]["exact_ambiguities_remain_open"]) == 3,
            "the exact three ambiguity boundaries are not preserved")

    nodes = web["nodes"]
    node_ids = [row["id"] for row in nodes]
    require(len(node_ids) == len(set(node_ids)), "duplicate graph node id")
    node_by_id = {row["id"]: row for row in nodes}
    require(set(row["phase"] for row in nodes) == set(PHASES), "P1-P12 are not all represented")

    story_phases = {row["phase"]: row for row in story["phase_map"]}
    require(set(story_phases) == set(PHASES), "story map is not exactly P1-P12")
    for node in nodes:
        require(node["surface"] in {
            "minecraft", "copperline", "discord", "media", "file", "cross_surface"
        }, f"{node['id']}: unknown surface")
        require(len(node["meaning"].strip()) >= 35, f"{node['id']}: meaning is not authored")
        if node["kind"] == "event":
            require(node["ref"] in event_keys, f"{node['id']}: event ref {node['ref']} is not executable")
        elif node["kind"] == "evidence":
            require(node["ref"] in evidence_ids, f"{node['id']}: evidence ref {node['ref']} is missing")
        elif node["kind"] == "story_map":
            phase, _, plant = node["ref"].partition(":")
            require(phase in story_phases and plant in story_phases[phase]["plant"],
                    f"{node['id']}: story-map ref is not exact")
        else:
            require(False, f"{node['id']}: unsupported node kind {node['kind']}")

    directed: dict[str, set[str]] = defaultdict(set)
    undirected: dict[str, set[str]] = defaultdict(set)
    incident = defaultdict(int)
    edge_pairs: set[tuple[str, str]] = set()
    for edge in web["edges"]:
        source, target = edge["from"], edge["to"]
        require(source in node_by_id and target in node_by_id, f"edge references missing node {source} -> {target}")
        require(source != target and (source, target) not in edge_pairs, f"duplicate/self edge {source} -> {target}")
        edge_pairs.add((source, target))
        require(PHASES.index(node_by_id[source]["phase"]) <= PHASES.index(node_by_id[target]["phase"]),
                f"edge moves backward in the revelation ladder: {source} -> {target}")
        require(len(edge["why"].strip()) >= 40, f"{source} -> {target}: callback has no authored reason")
        directed[source].add(target)
        undirected[source].add(target)
        undirected[target].add(source)
        incident[source] += 1
        incident[target] += 1

    p7_p8 = next((edge for edge in web["edges"]
                  if edge["from"] == "p7.public-correction"
                  and edge["to"] == "p8.intervention-plan"), None)
    require(p7_p8 is not None and "record-edit pattern" in p7_p8["why"]
            and "cannot become an unrelated closed file" in p7_p8["why"],
            "P7 correction does not constrain the P8 model as a real callback")

    require(all(incident[node_id] > 0 for node_id in node_ids), "one or more authored nodes are isolated")
    connected = reachable(undirected, node_ids[0])
    require(connected == set(node_ids), "campaign graph contains a disconnected case or lore island")

    terminal = "p12.coda"
    for node in nodes:
        if node["phase"] == "P12":
            continue
        require(terminal in reachable(directed, node["id"]),
                f"{node['id']}: plant/action has no directed route to the cross-surface coda")

    threads = web["required_threads"]
    required_thread_ids = {
        "last-company", "ordinary-water", "category-error", "record-copying",
        "keeper-culpability", "current-settlement", "unlit", "copperline",
    }
    require({row["id"] for row in threads} == required_thread_ids,
            "required human/story threads are missing or replaced")
    for thread in threads:
        milestones = thread["path"]
        require(all(node_id in node_by_id for node_id in milestones), f"{thread['id']}: missing milestone")
        phase_numbers = [PHASES.index(node_by_id[node_id]["phase"]) for node_id in milestones]
        require(phase_numbers == sorted(phase_numbers), f"{thread['id']}: milestone order breaks revelation order")
        require(node_by_id[milestones[-1]]["phase"] == "P12", f"{thread['id']}: no P12 payoff/coda")
        require(len(thread["earned_change"].strip()) >= 70, f"{thread['id']}: thread adds nodes but earns no change")

    coda_source = CODA_PAGE.read_text(encoding="utf-8")
    for event_key in [
        "p4.control_reversal_earned", "p5.civic_gallery_recurated",
        "p7.nessa_publicly_cleared", "p8.hold_systems_repaired",
        "p9.company_biographies_restored", "p10.player_copy_proof",
        "p11.averyn_restored_unbound",
    ]:
        require(f"hasCampaignEvent('{event_key}')" in coda_source,
                f"cross-surface coda does not read committed thread event {event_key}")
    for resident in ["Aro", "Wenna", "Coll", "Dob", "Pell"]:
        require(resident in coda_source, f"current-settlement coda omits {resident}")
    require("without naming what the Dark is" in coda_source,
            "coda resolves the Dark or drops the bounded Unlit copy consequence")

    long_callbacks = [
        edge for edge in web["edges"]
        if PHASES.index(node_by_id[edge["to"]]["phase"]) - PHASES.index(node_by_id[edge["from"]]["phase"]) >= 2
    ]
    require(long_callbacks, "no delayed callback crosses more than one phase")
    surfaces = {node["surface"] for node in nodes}
    require({"minecraft", "copperline", "discord", "media", "file"}.issubset(surfaces),
            "the campaign web is not genuinely cross-surface")

    print(
        "CAMPAIGN WEB: PASS - "
        f"{len(nodes)} linked nodes, {len(web['edges'])} authored relationships, "
        f"{len(threads)} required long threads, P1-P12 -> cross-surface coda"
    )


if __name__ == "__main__":
    main()
