#!/usr/bin/env python3
"""Fail closed when a canonical ARG event has policy/projection text but no real mutation owner."""

from __future__ import annotations

import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "campaign/campaign-action-owners.json"
POLICY = ROOT / "dashboard/src/lib/arg-event-policy.ts"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit(f"CAMPAIGN ACTION OWNERS: FAIL - {message}")


def main() -> None:
    data = json.loads(MANIFEST.read_text(encoding="utf-8"))
    policy = POLICY.read_text(encoding="utf-8")
    policy_events = set(re.findall(r"'((?:p(?:[1-9]|1[0-2]))\.[a-z0-9_]+)'\s*:\s*event\(", policy))

    rules = data["rules"]
    require(rules["closed_mechanism_catalog"] is False, "manifest became a closed puzzle taxonomy")
    require(rules["projection_is_not_mutation"] is True, "projection text may be mistaken for an action owner")
    require(rules["observation_receipts_gate_correctness"] is False, "source-touch gating was reintroduced")
    require(rules["every_event_has_real_owner"] is True, "real ownership is no longer required")
    require(rules["physical_truth_is_local_primary"] is True, "physical truth is no longer local-primary")

    rows = data["events"]
    ids = [row["event"] for row in rows]
    require(len(ids) == len(set(ids)), "duplicate event owner row")
    require(set(ids) == policy_events,
            f"owner/policy mismatch: missing={sorted(policy_events - set(ids))}, extra={sorted(set(ids) - policy_events)}")

    source_surfaces: dict[str, set[str]] = {}
    for line in policy.splitlines():
        match = re.search(
            r"'((?:p(?:[1-9]|1[0-2]))\.[a-z0-9_]+)'\s*:\s*event\(" 
            r"'P\d+',\s*\[[^\]]*\],\s*\[([^\]]+)\]",
            line,
        )
        if match:
            source_surfaces[match.group(1)] = set(re.findall(r"'([a-z]+)'", match.group(2)))
    require(set(source_surfaces) == policy_events, "could not parse every policy source-surface list")

    allowed_surfaces = {"minecraft", "copperline", "discord", "dashboard", "media", "npc"}
    for row in rows:
        event = row["event"]
        surface = row["owner_surface"]
        require(surface in allowed_surfaces, f"{event}: unknown owner surface {surface}")
        require(surface in source_surfaces[event], f"{event}: owner surface is not authorized by policy")
        require(row["zero_observation_correctness"] is True, f"{event}: correctness depends on observation receipts")
        require(len(row["trigger"].strip()) >= 35, f"{event}: trigger is not player-legible")
        require(len(row["recovery"].strip()) >= 35, f"{event}: recovery is not authored")
        require(len(row["mutation_tokens"]) >= 3, f"{event}: mutation proof is too weak")
        source = ROOT / row["source"]
        require(source.is_file(), f"{event}: owner source is missing: {row['source']}")
        require("generated" not in source.parts and "projection" not in source.name.lower(),
                f"{event}: generated/projection code cannot own mutation")
        text = source.read_text(encoding="utf-8")
        for token in row["mutation_tokens"]:
            require(token in text, f"{event}: mutation token absent from {row['source']}: {token}")

    minecraft_events = [row for row in rows if row["owner_surface"] == "minecraft"]
    require(len(minecraft_events) >= 20, "physical/local-primary campaign ownership was unexpectedly reduced")
    print(
        "CAMPAIGN ACTION OWNERS: PASS - "
        f"{len(rows)} canonical events have real mutation owners; "
        f"{len(minecraft_events)} are local-primary Minecraft actions"
    )


if __name__ == "__main__":
    main()
