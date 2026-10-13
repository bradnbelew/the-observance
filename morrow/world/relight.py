"""Recompute Beta lighting and height maps after code edits.

Beta never relights a chunk on load, so blocks placed by morrow.world tools would sit in the
old light: dark rooms with lit torches, shadows where a tree was cleared. This recomputes
SkyLight, BlockLight and HeightMap for a box of chunks, using Beta's rules (per-block opacity,
light value, 15 levels, falloff of max(1, opacity) per step). The chunks one ring outside the
box are read as a fixed boundary so light still flows in from untouched neighbours; only the
box itself is rewritten.

    python -m morrow.world.relight <world> <cx0> <cz0> <cx1> <cz1>
"""

from __future__ import annotations

import sys

import numpy as np

from morrow.world.mcregion import World

# Light opacity (0-15). Everything not listed is fully opaque.
_CLEAR = [0, 6, 20, 26, 27, 28, 30, 31, 32, 37, 38, 39, 40, 50, 51, 55, 59, 63, 64, 65, 66, 68, 69,
          70, 71, 72, 75, 76, 77, 78, 83, 85, 90, 92, 93, 94]
OPACITY = np.full(256, 15, dtype=np.int16)
OPACITY[_CLEAR] = 0
OPACITY[18] = 1                     # leaves
OPACITY[[8, 9, 79]] = 3             # water, ice
# Light emitted.
EMIT = np.zeros(256, dtype=np.int16)
for bid, level in {10: 15, 11: 15, 50: 14, 51: 15, 62: 13, 74: 9, 76: 7, 89: 15, 90: 11, 91: 15, 94: 9, 39: 1}.items():
    EMIT[bid] = level


def _unpack(nibbles: bytes | bytearray) -> np.ndarray:
    a = np.frombuffer(bytes(nibbles), dtype=np.uint8)
    out = np.empty(a.size * 2, dtype=np.int16)
    out[0::2] = a & 0x0F
    out[1::2] = a >> 4
    return out


def _pack(values: np.ndarray) -> bytearray:
    v = values.astype(np.uint8).ravel()
    return bytearray(((v[0::2] & 0x0F) | ((v[1::2] & 0x0F) << 4)).tobytes())


def _spread(light: np.ndarray, opacity: np.ndarray, fixed: np.ndarray) -> np.ndarray:
    """Flood light outward; each step costs max(1, opacity of the cell entered)."""
    cost = np.maximum(1, opacity)
    for _ in range(15):
        best = light.copy()
        for axis in range(3):
            for shift in (1, -1):
                rolled = np.roll(light, shift, axis=axis)
                edge = [slice(None)] * 3
                edge[axis] = 0 if shift == 1 else -1
                rolled[tuple(edge)] = 0  # no wrap-around
                np.maximum(best, rolled - cost, out=best)
        best[opacity >= 15] = 0
        best = np.where(fixed, light, best)
        np.maximum(best, 0, out=best)
        if np.array_equal(best, light):
            break
        light = best
    return light


def relight(w: World, cx0: int, cz0: int, cx1: int, cz1: int) -> int:
    """Relight chunks cx0..cx1 x cz0..cz1 (inclusive). Returns how many chunks were rewritten."""
    bx0, bz0, bx1, bz1 = cx0 - 1, cz0 - 1, cx1 + 1, cz1 + 1
    nx, nz = (bx1 - bx0 + 1) * 16, (bz1 - bz0 + 1) * 16
    blocks = np.zeros((nx, nz, 128), dtype=np.int16)
    sky = np.zeros_like(blocks)
    blk = np.zeros_like(blocks)
    present = np.zeros((nx, nz), dtype=bool)
    inner = np.zeros((nx, nz), dtype=bool)
    for cx in range(bx0, bx1 + 1):
        for cz in range(bz0, bz1 + 1):
            try:
                lv = w.level(cx * 16, cz * 16)
            except KeyError:
                continue
            xs, zs = slice((cx - bx0) * 16, (cx - bx0) * 16 + 16), slice((cz - bz0) * 16, (cz - bz0) * 16 + 16)
            blocks[xs, zs] = np.frombuffer(bytes(lv["Blocks"].value), dtype=np.uint8).reshape(16, 16, 128)
            sky[xs, zs] = _unpack(lv["SkyLight"].value).reshape(16, 16, 128)
            blk[xs, zs] = _unpack(lv["BlockLight"].value).reshape(16, 16, 128)
            present[xs, zs] = True
            inner[xs, zs] = cx0 <= cx <= cx1 and cz0 <= cz <= cz1
    opacity = OPACITY[blocks]
    opacity[~present] = 15
    fixed = np.broadcast_to(~inner[:, :, None], blocks.shape) & np.broadcast_to(present[:, :, None], blocks.shape)

    # Sky: straight down from the top, losing each cell's opacity, then spread sideways.
    from_top = np.cumsum(opacity[:, :, ::-1], axis=2)[:, :, ::-1]
    direct = np.clip(15 - from_top, 0, 15)
    sky_new = np.where(fixed, sky, direct)
    sky_new = _spread(sky_new, opacity, fixed)
    # Block light: from emitters.
    emit = EMIT[blocks]
    blk_new = np.where(fixed, blk, emit)
    blk_new = _spread(blk_new, np.where(emit > 0, 0, opacity), fixed | False)
    blk_new = np.maximum(blk_new, np.where(fixed, 0, emit))

    rewritten = 0
    for cx in range(cx0, cx1 + 1):
        for cz in range(cz0, cz1 + 1):
            xs, zs = slice((cx - bx0) * 16, (cx - bx0) * 16 + 16), slice((cz - bz0) * 16, (cz - bz0) * 16 + 16)
            if not present[xs, zs].all():
                continue
            lv = w.level(cx * 16, cz * 16)
            lv["SkyLight"].value = _pack(sky_new[xs, zs])
            lv["BlockLight"].value = _pack(blk_new[xs, zs])
            op = opacity[xs, zs]
            solid = op > 0
            top = np.where(solid.any(axis=2), 127 - np.argmax(solid[:, :, ::-1], axis=2) + 1, 0)  # [x][z]
            lv["HeightMap"].value = bytearray(top.T.astype(np.uint8).ravel().tobytes())  # index z*16+x
            w._mark(cx * 16, cz * 16)
            rewritten += 1
    return rewritten


def relight_blocks(w: World, x0: int, z0: int, x1: int, z1: int, margin: int = 16) -> int:
    """Relight every chunk touching a block-coordinate box (plus a margin for light reach)."""
    return relight(w, (min(x0, x1) - margin) >> 4, (min(z0, z1) - margin) >> 4,
                   (max(x0, x1) + margin) >> 4, (max(z0, z1) + margin) >> 4)


if __name__ == "__main__":
    if len(sys.argv) != 6:
        print(__doc__)
        sys.exit(2)
    world = World(sys.argv[1])
    n = relight(world, *map(int, sys.argv[2:6]))
    world.save()
    print("relit %d chunks" % n)
