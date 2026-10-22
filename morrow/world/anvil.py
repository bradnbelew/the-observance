"""Anvil (.mca, Java 1.18+) region files, read-only: what the upgraded Paper world looks like.

Chunk root (no "Level" wrapper since 1.18):
  sections[]   Y, block_states {palette [{Name, Properties}], data long[]}
               index = y*256 + z*16 + x; entries never span longs (since 1.16);
               bits = max(4, ceil(log2(len(palette)))); a one-entry palette has no data
  block_entities[]   signs, chests, banners ...
Entities (item frames, ...) live in a separate tree, <world>/entities/r.X.Z.mca, chunk root
{Entities: [...]}.
"""

from __future__ import annotations

import gzip
import os
import struct
import zlib

import numpy as np

from . import nbt

SECTOR = 4096


class _Regions:
    def __init__(self, folder: str):
        self.folder = folder
        self.raw: dict[tuple[int, int], bytes | None] = {}

    def chunk(self, cx: int, cz: int) -> dict | None:
        key = (cx >> 5, cz >> 5)
        if key not in self.raw:
            p = os.path.join(self.folder, "r.%d.%d.mca" % key)
            self.raw[key] = open(p, "rb").read() if os.path.exists(p) else None
        raw = self.raw[key]
        if raw is None or len(raw) < 2 * SECTOR:
            return None
        lx, lz = cx & 31, cz & 31
        entry = struct.unpack(">I", raw[4 * (lx + lz * 32): 4 * (lx + lz * 32) + 4])[0]
        off = entry >> 8
        if off == 0:
            return None
        start = off * SECTOR
        length, comp = struct.unpack(">IB", raw[start:start + 5])
        data = raw[start + 5:start + 4 + length]
        if comp == 2:
            data = zlib.decompress(data)
        elif comp == 1:
            data = gzip.decompress(data)
        elif comp != 3:
            return None  # external .mcc or lz4: not used by our worlds
        return nbt.loads(data)[1]


class AnvilWorld:
    def __init__(self, path: str):
        self.path = path
        self.blocks = _Regions(os.path.join(path, "region"))
        self.ents = _Regions(os.path.join(path, "entities"))

    @staticmethod
    def detect(path: str) -> bool:
        reg = os.path.join(path, "region")
        return os.path.isdir(reg) and any(f.endswith(".mca") for f in os.listdir(reg))

    def chunk(self, cx: int, cz: int) -> dict | None:
        return self.blocks.chunk(cx, cz)

    def entities(self, cx: int, cz: int) -> list[dict]:
        root = self.ents.chunk(cx, cz)
        if not root or "Entities" not in root:
            return []
        return [t.value for t in root["Entities"].value.items]


def section_states(sec: dict) -> tuple[list[tuple[str, dict]], np.ndarray] | None:
    """(palette as (name, props), indices shaped [y, z, x]) or None when the section has no blocks."""
    bs = sec.get("block_states")
    if bs is None:
        return None
    bs = bs.value
    pal = []
    for t in bs["palette"].value.items:
        d = t.value
        props = {k: v.value for k, v in d["Properties"].value.items()} if "Properties" in d else {}
        pal.append((d["Name"].value, props))
    if len(pal) == 1 or "data" not in bs:
        return pal, np.zeros((16, 16, 16), np.int32)
    bits = max(4, (len(pal) - 1).bit_length())
    per = 64 // bits
    longs = np.array(bs["data"].value, np.int64).view(np.uint64)
    shifts = (np.arange(per, dtype=np.uint64) * np.uint64(bits))
    idx = ((longs[:, None] >> shifts[None]) & np.uint64((1 << bits) - 1)).reshape(-1)[:4096]
    return pal, idx.astype(np.int32).reshape(16, 16, 16)
