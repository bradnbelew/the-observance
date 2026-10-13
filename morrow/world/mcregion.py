"""McRegion (.mcr, Beta 1.3 - 1.1) region files and Beta chunk block access.

Beta chunk layout (Level compound):
  Blocks     byte[32768]  index = y + z*128 + x*128*16   (x,z in 0..15, y in 0..127)
  Data       byte[16384]  4-bit metadata, same index, low nibble first
  SkyLight / BlockLight   4-bit, same layout
  HeightMap  byte[256]    index = z*16 + x
  TileEntities  list of compounds (Sign, Chest, Music, ...)
"""

from __future__ import annotations

import os
import struct
import time
import zlib
from dataclasses import dataclass

from . import nbt

SECTOR = 4096


def region_path(world: str, cx: int, cz: int) -> str:
    return os.path.join(world, "region", "r.%d.%d.mcr" % (cx >> 5, cz >> 5))


class Region:
    """Reads chunks lazily; rewrites the whole file on save (simple and safe for our sizes)."""

    def __init__(self, path: str):
        self.path = path
        self.chunks: dict[tuple[int, int], dict] = {}
        self.timestamps: dict[tuple[int, int], int] = {}
        self._raw = open(path, "rb").read() if os.path.exists(path) else b"\x00" * (2 * SECTOR)
        self.dirty: set[tuple[int, int]] = set()

    def _offset(self, lx: int, lz: int) -> tuple[int, int]:
        entry = struct.unpack(">I", self._raw[4 * (lx + lz * 32): 4 * (lx + lz * 32) + 4])[0]
        return entry >> 8, entry & 0xFF

    def has(self, lx: int, lz: int) -> bool:
        return (lx, lz) in self.chunks or self._offset(lx, lz)[0] != 0

    def chunk(self, lx: int, lz: int) -> dict | None:
        key = (lx, lz)
        if key in self.chunks:
            return self.chunks[key]
        off, count = self._offset(lx, lz)
        if off == 0:
            return None
        start = off * SECTOR
        length, comp = struct.unpack(">IB", self._raw[start:start + 5])
        data = self._raw[start + 5:start + 4 + length]
        raw = zlib.decompress(data) if comp == 2 else __import__("gzip").decompress(data)
        _, root = nbt.loads(raw)
        self.chunks[key] = root
        ts = struct.unpack(">I", self._raw[SECTOR + 4 * (lx + lz * 32): SECTOR + 4 * (lx + lz * 32) + 4])[0]
        self.timestamps[key] = ts
        return root

    def mark(self, lx: int, lz: int) -> None:
        self.dirty.add((lx, lz))

    def save(self) -> None:
        for lx in range(32):
            for lz in range(32):
                if self.has(lx, lz):
                    self.chunk(lx, lz)
        header = bytearray(2 * SECTOR)
        body = bytearray()
        sector = 2
        for (lx, lz), root in sorted(self.chunks.items()):
            payload = zlib.compress(nbt.dumps("", root))
            blob = struct.pack(">IB", len(payload) + 1, 2) + payload
            blob += b"\x00" * (-len(blob) % SECTOR)
            n = len(blob) // SECTOR
            struct.pack_into(">I", header, 4 * (lx + lz * 32), (sector << 8) | n)
            ts = int(time.time()) if (lx, lz) in self.dirty else self.timestamps.get((lx, lz), int(time.time()))
            struct.pack_into(">I", header, SECTOR + 4 * (lx + lz * 32), ts)
            body += blob
            sector += n
        os.makedirs(os.path.dirname(self.path), exist_ok=True)
        tmp = self.path + ".tmp"
        with open(tmp, "wb") as f:
            f.write(bytes(header) + bytes(body))
        os.replace(tmp, self.path)
        self._raw = bytes(header) + bytes(body)
        self.dirty.clear()


@dataclass
class World:
    path: str

    def __post_init__(self):
        self.regions: dict[str, Region] = {}

    def region_for(self, x: int, z: int) -> Region:
        p = region_path(self.path, x >> 4, z >> 4)
        if p not in self.regions:
            self.regions[p] = Region(p)
        return self.regions[p]

    def level(self, x: int, z: int) -> dict:
        reg = self.region_for(x, z)
        root = reg.chunk((x >> 4) & 31, (z >> 4) & 31)
        if root is None:
            raise KeyError("chunk at block %d,%d has not been generated; walk there in Beta first" % (x, z))
        return root["Level"].value

    def _mark(self, x: int, z: int) -> None:
        self.region_for(x, z).mark((x >> 4) & 31, (z >> 4) & 31)

    @staticmethod
    def _index(x: int, y: int, z: int) -> int:
        return y + (z & 15) * 128 + (x & 15) * 128 * 16

    def get(self, x: int, y: int, z: int) -> tuple[int, int]:
        lv = self.level(x, z)
        i = self._index(x, y, z)
        data = lv["Data"].value[i >> 1]
        return lv["Blocks"].value[i], (data & 0x0F) if i % 2 == 0 else (data >> 4)

    def set(self, x: int, y: int, z: int, block: int, meta: int = 0) -> None:
        lv = self.level(x, z)
        i = self._index(x, y, z)
        lv["Blocks"].value[i] = block
        d = lv["Data"].value
        d[i >> 1] = (d[i >> 1] & 0xF0) | meta if i % 2 == 0 else (d[i >> 1] & 0x0F) | (meta << 4)
        hm = lv["HeightMap"].value
        col = (z & 15) * 16 + (x & 15)
        if block and y + 1 > hm[col]:
            hm[col] = y + 1
        self._mark(x, z)

    def set_tile(self, x: int, y: int, z: int, tile_id: str, **fields) -> None:
        lv = self.level(x, z)
        tiles = lv["TileEntities"].value
        tiles.items = [t for t in tiles.items if not (t.value["x"].value == x and t.value["y"].value == y and t.value["z"].value == z)]
        tiles.items.append(nbt.comp(id=nbt.st(tile_id), x=nbt.i(x), y=nbt.i(y), z=nbt.i(z), **fields))
        if tiles.elem != nbt.COMPOUND:  # Beta writes empty lists typed as BYTE, not END
            tiles.elem = nbt.COMPOUND
        self._mark(x, z)

    def tiles(self, x: int, z: int) -> list[dict]:
        return [t.value for t in self.level(x, z)["TileEntities"].value.items]

    def height(self, x: int, z: int) -> int:
        return self.level(x, z)["HeightMap"].value[(z & 15) * 16 + (x & 15)]

    def generated_chunks(self) -> list[tuple[int, int]]:
        out = []
        rdir = os.path.join(self.path, "region")
        for name in os.listdir(rdir):
            if not name.endswith(".mcr"):
                continue
            _, rx, rz, _ = name.split(".")
            reg = Region(os.path.join(rdir, name))
            for lx in range(32):
                for lz in range(32):
                    if reg.has(lx, lz):
                        out.append((int(rx) * 32 + lx, int(rz) * 32 + lz))
        return out

    def save(self) -> None:
        for reg in self.regions.values():
            if reg.dirty:
                reg.save()


def new_chunk(cx: int, cz: int, ground: int = 63) -> dict:
    """A flat test chunk (stone to ground-1, grass at ground). Used only by self-tests."""
    blocks = bytearray(32768)
    for x in range(16):
        for z in range(16):
            for y in range(ground):
                blocks[y + z * 128 + x * 128 * 16] = 1
            blocks[ground + z * 128 + x * 128 * 16] = 2
    level = {
        "xPos": nbt.i(cx), "zPos": nbt.i(cz), "LastUpdate": nbt.Tag(nbt.LONG, 0), "TerrainPopulated": nbt.b(1),
        "Blocks": nbt.Tag(nbt.BYTE_ARRAY, blocks), "Data": nbt.Tag(nbt.BYTE_ARRAY, bytearray(16384)),
        "SkyLight": nbt.Tag(nbt.BYTE_ARRAY, bytearray(b"\xff" * 16384)), "BlockLight": nbt.Tag(nbt.BYTE_ARRAY, bytearray(16384)),
        "HeightMap": nbt.Tag(nbt.BYTE_ARRAY, bytearray([ground + 1] * 256)),
        "Entities": nbt.lst(nbt.END, []), "TileEntities": nbt.lst(nbt.END, []),
    }
    return {"Level": nbt.Tag(nbt.COMPOUND, level)}
