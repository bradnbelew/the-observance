#!/usr/bin/env python3
"""Fail closed when a claimed P5-P12 external clue exists only in authored JSON."""
from __future__ import annotations

import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
CAMPAIGN = ROOT / "campaign/p5-p12"
EXTERNAL_TOKENS = ("copperline", "discord", "media", "npc", "archive", "web", "image")


def require(ok: bool, message: str) -> None:
    if not ok:
        raise AssertionError(message)


def authored_surfaces() -> dict[str, str]:
    result: dict[str, str] = {}
    index = json.loads((CAMPAIGN / "campaign.json").read_text(encoding="utf-8"))
    for phase in index["phases"]:
        case = json.loads((CAMPAIGN / phase["file"]).read_text(encoding="utf-8"))
        rows = list(case.get("evidence", []))
        for dossier in case.get("dossiers", []):
            rows.extend(dossier.get("evidence", []))
        for row in rows:
            evidence_id = row["id"]
            require(evidence_id not in result, f"duplicate authored evidence {evidence_id}")
            result[evidence_id] = row["surface"]
    return result


def main() -> None:
    surfaces = authored_surfaces()
    expected = {
        evidence_id for evidence_id, surface in surfaces.items()
        if any(token in surface.casefold() for token in EXTERNAL_TOKENS)
    }
    authority = json.loads((CAMPAIGN / "external-bindings.json").read_text(encoding="utf-8"))
    require(authority["fresh_external_client_receipt"] is False,
            "offline external bindings must not claim a fresh client receipt")
    require("never gates correctness" in authority["rule"],
            "external evidence authority must preserve zero-receipt correctness")

    media = json.loads((CAMPAIGN / "media-custody.json").read_text(encoding="utf-8"))
    media_ids = {row["id"] for row in media["assets"]}
    dialogue = json.loads((ROOT / "arc/v5/npc-dialogue.json").read_text(encoding="utf-8"))
    npcs = {row["id"].replace("-", "_"): row for row in dialogue["townsfolk"]}
    npcs[dialogue["wren"]["id"]] = dialogue["wren"]
    discord_source = (ROOT / "discord/src/v5/evidence-docket.ts").read_text(encoding="utf-8")

    bound: set[str] = set()
    carrier_counts = {"web": 0, "discord": 0, "media": 0, "npc": 0}
    for row in authority["bindings"]:
        evidence_id = row["evidence_id"]
        require(evidence_id in expected, f"external binding names local-only or unknown evidence {evidence_id}")
        require(evidence_id not in bound, f"duplicate external evidence binding {evidence_id}")
        bound.add(evidence_id)
        require(row["carriers"], f"{evidence_id} has no concrete external carrier")
        require(row["delivery_status"].strip(), f"{evidence_id} lacks honest delivery status")
        for carrier in row["carriers"]:
            require(isinstance(carrier, str) and carrier.strip(),
                    f"{evidence_id} carrier must be non-empty text")
            if carrier.startswith("web:"):
                carrier_counts["web"] += 1
                value = carrier.removeprefix("web:")
                path_text, separator, anchor = value.partition("#")
                require(separator == "#" and anchor, f"{evidence_id} web carrier lacks an anchor")
                path = ROOT / path_text
                require(path.is_file(), f"{evidence_id} web carrier is missing: {path_text}")
                source = path.read_text(encoding="utf-8")
                require(f'id="{anchor}"' in source, f"{evidence_id} web anchor is absent: {anchor}")
                require(f'data-evidence-id="{evidence_id}"' in source,
                        f"{evidence_id} web carrier lacks its evidence identity")
            elif carrier.startswith("discord:"):
                carrier_counts["discord"] += 1
                key = carrier.removeprefix("discord:")
                require(f"id: '{key}'" in discord_source,
                        f"{evidence_id} Discord docket carrier is missing: {key}")
            elif carrier.startswith("media:"):
                carrier_counts["media"] += 1
                key = carrier.removeprefix("media:")
                require(key in media_ids, f"{evidence_id} media custody carrier is missing: {key}")
            elif carrier.startswith("npc:"):
                carrier_counts["npc"] += 1
                _, npc_id, state = carrier.split(":", 2)
                npc = npcs.get(npc_id.replace("-", "_"))
                require(npc is not None, f"{evidence_id} NPC carrier is unknown: {npc_id}")
                require(state in npc["lines"] and len(npc["lines"][state]) >= 1,
                        f"{evidence_id} NPC state is missing: {npc_id}:{state}")
            else:
                raise AssertionError(f"{evidence_id} has unsupported external carrier {carrier}")

    require(bound == expected,
            "external evidence carrier coverage drift "
            f"missing={sorted(expected-bound)} extra={sorted(bound-expected)}")
    require(all(count > 0 for count in carrier_counts.values()),
            f"external carrier family is empty: {carrier_counts}")
    print(f"P5-P12 external bindings: PASS ({len(bound)} evidence records; {carrier_counts})")


if __name__ == "__main__":
    main()
