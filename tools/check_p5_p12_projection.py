#!/usr/bin/env python3
"""Require byte parity for Paper, dashboard, and Discord campaign projections."""
from __future__ import annotations

import hashlib
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "campaign/p5-p12/projection-manifest.json"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def main() -> None:
    data = json.loads(MANIFEST.read_text(encoding="utf-8"))
    require(data["phase_count"] == 8 and data["production_deployed"] is False,
            "campaign projection breadth/deployment boundary drift")
    payloads = []
    for relative in data["targets"]:
        path = ROOT / relative
        require(path.is_file(), f"missing campaign projection: {relative}")
        payload = path.read_bytes()
        require(len(payload) == data["projection_bytes"], f"projection byte count drift: {relative}")
        require(hashlib.sha256(payload).hexdigest() == data["projection_sha256"],
                f"projection hash drift: {relative}")
        parsed = json.loads(payload)
        require(parsed["phases"] == [f"P{i}" for i in range(5, 13)],
                f"projection phase order drift: {relative}")
        require(parsed["observation_receipts_gate_answers"] is False,
                f"projection observation gate drift: {relative}")
        payloads.append(payload)
    require(len(set(payloads)) == 1, "surface projections are not byte-identical")
    binding_source = ROOT / data["minecraft_binding_source"]
    binding_target = ROOT / data["minecraft_binding_target"]
    require(binding_source.read_bytes() == binding_target.read_bytes(),
            "packaged Minecraft binding differs from canonical source")
    require(hashlib.sha256(binding_target.read_bytes()).hexdigest() == data["minecraft_binding_sha256"],
            "packaged Minecraft binding hash drift")
    print(f"P5-P12 projection: PASS (8 phases, 3 byte-identical surfaces, {data['projection_sha256']})")


if __name__ == "__main__":
    main()
