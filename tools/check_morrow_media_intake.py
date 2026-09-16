#!/usr/bin/env python3
"""Validate the Morrow media intake manifest is complete and cannot overclaim readiness."""
from __future__ import annotations

import json
import re
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
MORROW = ROOT / "morrow"
HEX_64 = re.compile(r"^[0-9a-f]{64}$")


def load_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def validate_empty_or_hash(value: str, field: str, key: str) -> None:
    require(isinstance(value, str), f"{key}: {field} must be a string")
    if value:
        require(bool(HEX_64.fullmatch(value)), f"{key}: {field} must be a SHA-256 hex digest")


def main() -> int:
    try:
        catalog = load_json(MORROW / "contracts" / "media-catalog.json")
        manifest = load_json(MORROW / "media" / "media-manifest.template.json")

        require(manifest["schema_version"] == "1.1.0-morrow-media-intake-manifest", "unexpected manifest schema")
        require(manifest["status"] == "intake_template_not_release_ready", "template must not claim release readiness")
        policy = manifest["root_policy"]
        require(policy["hash_algorithm"] == "sha256", "media hashes must be SHA-256")
        require(policy["release_ready_requires_all_hashes"] is True, "hash release gate weakened")
        require(policy["release_ready_requires_accessibility_equivalent"] is True, "accessibility release gate weakened")
        require(policy["release_ready_requires_safety_review"] is True, "safety release gate weakened")

        catalog_by_key = {asset["key"]: asset for asset in catalog["assets"]}
        manifest_by_key = {asset["key"]: asset for asset in manifest["assets"]}
        require(set(catalog_by_key) == set(manifest_by_key), "manifest keys must exactly match media catalog")
        require(len(manifest_by_key) == 12, "Morrow requires exactly 12 media intake rows")

        for key, asset in manifest_by_key.items():
            catalog_asset = catalog_by_key[key]
            require(asset["media_type"] == catalog_asset["media_type"], f"{key}: media type drift")
            require(asset["required_observation"] == catalog_asset["required_observation"], f"{key}: observation drift")
            require(asset["work_folder"] == f"morrow/media/work/{key}", f"{key}: work folder drift")
            require(asset["release_ready"] is False, f"{key}: template cannot be release-ready")
            require(len(asset["acceptance_checks"]) >= 3, f"{key}: acceptance checks are too thin")
            validate_empty_or_hash(asset["source_sha256"], "source_sha256", key)
            validate_empty_or_hash(asset["delivery_sha256"], "delivery_sha256", key)
            safety = asset["safety_review"]
            for field in (
                "no_real_private_data",
                "no_real_phone_numbers",
                "no_real_people",
                "no_real_company_impersonation",
            ):
                require(field in safety, f"{key}: missing safety field {field}")
                require(safety[field] is False, f"{key}: template safety field must await review")
            require(
                "real" not in " ".join(asset["acceptance_checks"]).lower()
                or key == "morrow.g11.incident_raw_export",
                f"{key}: acceptance checks should avoid implying real telemetry or people",
            )
    except Exception as failure:
        print(f"MORROW MEDIA INTAKE CHECK: FAIL: {failure}")
        return 1

    print("MORROW MEDIA INTAKE CHECK: PASS assets=12 release_ready=0 safety=pending hashes=sha256")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
