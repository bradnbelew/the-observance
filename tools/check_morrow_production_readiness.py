#!/usr/bin/env python3
"""Validate Morrow production-readiness paperwork stays explicit and fail-closed."""
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
MORROW = ROOT / "morrow"


def load_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def main() -> int:
    try:
        readiness = load_json(MORROW / "rehearsal" / "production-readiness.json")
        media_catalog = load_json(MORROW / "contracts" / "media-catalog.json")
        media_template = load_json(MORROW / "media" / "media-manifest.template.json")
        smoke = load_json(MORROW / "rehearsal" / "website-smoke" / "latest.json")

        require(readiness["status"] == "not_production_ready", "readiness status must remain explicit")
        require(readiness["production_enabled"] is False, "production must not be enabled here")
        require(readiness["first_playable_review_ready"] is True, "first playable review flag drifted")
        require(len(readiness["hard_blockers"]) >= 6, "hard blocker matrix is incomplete")

        blocker_ids = {blocker["id"] for blocker in readiness["hard_blockers"]}
        for required in (
            "website-preview-build",
            "media-final-assets",
            "minecraft-runtime",
            "discord-g10",
            "database-schema",
            "director-controls",
        ):
            require(required in blocker_ids, f"missing blocker {required}")

        for blocker in readiness["hard_blockers"]:
            require(blocker["required_before_release"] is True, f"{blocker['id']} must block release")
            require(blocker["exit_criteria"], f"{blocker['id']} lacks exit criteria")

        require(smoke["build_passed"] is True, "website production build evidence must pass")
        require(smoke["production_mutations_allowed"] is False, "website smoke cannot allow production mutation")
        smoke_paths = {route["path"]: route["status"] for route in smoke["routes"]}
        for required_path in (
            "/game-servers.php",
            "/community/forum",
            "/support/tickets/6118",
            "/recovery/mossfield/console",
            "/api/rehearsal/morrow/readiness",
        ):
            require(smoke_paths.get(required_path) == 200, f"website smoke missing 200 for {required_path}")
        require(any(check["path"] == "/api/rehearsal/morrow/director" and check["actual_status"] == 423 for check in smoke["negative_checks"]), "director negative smoke missing")
        render_paths = {check["path"]: check for check in smoke["browser_render_checks"]}
        for required_path in ("/game-servers.php", "/community/forum", "/support/tickets/6118", "/recovery/mossfield/console"):
            render_check = render_paths.get(required_path)
            require(render_check is not None, f"browser render smoke missing {required_path}")
            require(render_check["has_content"] is True, f"browser render smoke blank for {required_path}")
            require(render_check["next_error_overlay"] is False, f"browser render smoke found Next overlay for {required_path}")
            require(render_check["console_error_count"] == 0, f"browser render smoke found console errors for {required_path}")

        catalog_keys = {asset["key"] for asset in media_catalog["assets"]}
        template_keys = {asset["key"] for asset in media_template["assets"]} | set(media_template["copy_this_shape_for_remaining_assets"])
        require(catalog_keys == template_keys, "media template does not cover catalog keys")
        require(all(asset["release_ready"] is False for asset in media_template["assets"]), "template assets must not be release-ready")
        require((MORROW / "rehearsal" / "FIRST-PLAYABLE-RUNBOOK.md").exists(), "missing first playable runbook")
        require((MORROW / "rehearsal" / "PRODUCTION-READINESS-MATRIX.md").exists(), "missing readiness matrix")
    except Exception as failure:
        print(f"MORROW PRODUCTION READINESS CHECK: FAIL: {failure}", file=sys.stderr)
        return 1

    print("MORROW PRODUCTION READINESS CHECK: PASS status=not_ready first_playable=ready website=build-smoked blockers=6 media=12")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
