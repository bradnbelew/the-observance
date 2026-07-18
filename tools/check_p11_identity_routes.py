#!/usr/bin/env python3
"""Fail closed if P11's exact identity becomes a spoiler, click quota, or stale reader ladder."""
from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def load(path: str):
    return json.loads((ROOT / path).read_text(encoding="utf-8"))


def require(condition: bool, message: str) -> None:
    if not condition:
        raise SystemExit(f"P11 IDENTITY ROUTES: FAIL - {message}")


def main() -> None:
    contract = load("campaign/p11-identity-routes.json")
    rules = contract["rules"]
    require(rules["routes_are_independent"] is True, "routes are not independent")
    require(rules["routes_may_be_completed_in_any_order"] is True, "route order is gated")
    require(rules["single_source_prints_complete_identity"] is False, "a carrier may print the full identity")
    require(rules["legacy_orin_six_dial_reuse"] is False, "retired Orin dials were reauthorized")
    require(rules["correct_identity_requires_observation_receipts"] is False,
            "correct identity is source-touch gated")

    routes = contract["routes"]
    require(len(routes) == 6, "identity must have exactly six Keeper routes")
    require([row["keeper"] for row in routes] == contract["keeper_order"],
            "route order drifted from named Keeper dossier order")
    require("".join(row["letter"] for row in routes) == "AVERYN",
            "independent letters no longer synthesize AVERYN")
    for field in ("keeper", "evidence_id", "book_id", "letter", "earlier_method",
                  "return_operation", "prerequisite_flag", "completion_flag"):
        require(len({row[field] for row in routes}) == 6, f"route field {field} is not independent")

    books = {row["id"]: row for row in load("arc/v5/minecraft-books.json")["books"]}
    for route in routes:
        book = books.get(route["book_id"])
        require(book is not None, f"missing packaged book {route['book_id']}")
        text = "\n".join(book["pages"])
        require("AVERYN" not in text.upper(), f"{route['book_id']} prints the complete identity")
        for phrase in route["required_book_phrases"]:
            require(phrase.casefold() in text.casefold(),
                    f"{route['book_id']} lacks player-visible route phrase {phrase!r}")

    historical = load("design/ARG-V5-PHYSICAL-PREDICATES.json")
    overlay = load("design/ARG-P11-INDEPENDENT-IDENTITY-OVERLAY.json")
    require(overlay["base_physical_authority"]["bytes_immutable"] is True,
            "overlay does not preserve historical M2 bytes")
    nodes = {row["node_id"]: row for row in overlay["nodes"]}
    for index, route in enumerate(routes, start=2):
        node = nodes[f"AR0{index}"]
        require(set(node["effective_prerequisites"]) == {"v5_ar01_not_kept", route["prerequisite_flag"]},
                f"{node['node_id']} is sequential or lacks its own affidavit prerequisite")
        historical_node = next(row for row in historical["nodes"] if row["node_id"] == node["node_id"])
        require(route["completion_flag"] == historical_node["completion_flag"],
                f"{node['node_id']} completion flag drifted")
        require(historical_node["reward"]["artifact_ids"]
                    == [f"averyn_fragment_{route['letter'].lower()}"],
                f"{node['node_id']} returns the wrong historical custody artifact")
    orin = nodes["AR05"]["effective_predicate"]
    require(orin["kind"] == "affidavit_plus_verified_bearing_and_low_sightline",
            "Orin reverted to the retired dial puzzle")
    require(not re.search(r"six mini-dials|\"dials\"", json.dumps(orin), re.I),
            "Orin still reuses the retired six-dial station")

    plugin = (ROOT / contract["synthesis"]["predicate_owners"][0]).read_text(encoding="utf-8")
    discord = (ROOT / contract["synthesis"]["predicate_owners"][1]).read_text(encoding="utf-8")
    require('equals("AVERYN")' in plugin, "Minecraft exact-artifact predicate is missing")
    require("name !== 'AVERYN'" in discord, "Discord exact-artifact predicate is missing")
    require(contract["synthesis"]["zero_observation_acceptance"] is True,
            "synthesis gained an observation prerequisite")

    print("P11 IDENTITY ROUTES: PASS - six independent packaged routes, no full-name carrier, zero-receipt synthesis")


if __name__ == "__main__":
    main()
