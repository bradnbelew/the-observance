"""Minimal, dependency-free NBT reader/writer (big-endian Java format).

Values are Python objects wrapped in `Tag(type, value)` so a round trip preserves exact
types (a Beta chunk distinguishes byte/short/int fields). Compounds are dicts name -> Tag,
lists are `TagList(elem_type, [Tag...])`.
"""

from __future__ import annotations

import gzip
import io
import struct
import zlib
from dataclasses import dataclass, field

END, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, BYTE_ARRAY, STRING, LIST, COMPOUND, INT_ARRAY, LONG_ARRAY = range(13)


@dataclass
class Tag:
    type: int
    value: object


@dataclass
class TagList:
    elem: int
    items: list = field(default_factory=list)


_FMT = {BYTE: ">b", SHORT: ">h", INT: ">i", LONG: ">q", FLOAT: ">f", DOUBLE: ">d"}


def _read(buf: io.BytesIO, t: int):
    if t in _FMT:
        f = _FMT[t]
        return struct.unpack(f, buf.read(struct.calcsize(f)))[0]
    if t == BYTE_ARRAY:
        (n,) = struct.unpack(">i", buf.read(4))
        return bytearray(buf.read(n))
    if t == STRING:
        (n,) = struct.unpack(">H", buf.read(2))
        return buf.read(n).decode("utf-8")
    if t == LIST:
        elem, n = struct.unpack(">bi", buf.read(5))
        return TagList(elem, [Tag(elem, _read(buf, elem)) for _ in range(n)])
    if t == COMPOUND:
        out = {}
        while True:
            (ct,) = struct.unpack(">b", buf.read(1))
            if ct == END:
                return out
            (n,) = struct.unpack(">H", buf.read(2))
            name = buf.read(n).decode("utf-8")
            out[name] = Tag(ct, _read(buf, ct))
    if t == INT_ARRAY:
        (n,) = struct.unpack(">i", buf.read(4))
        return list(struct.unpack(">%di" % n, buf.read(4 * n)))
    if t == LONG_ARRAY:
        (n,) = struct.unpack(">i", buf.read(4))
        return list(struct.unpack(">%dq" % n, buf.read(8 * n)))
    raise ValueError("bad tag type %d" % t)


def _write(out: io.BytesIO, t: int, v) -> None:
    if t in _FMT:
        out.write(struct.pack(_FMT[t], v))
    elif t == BYTE_ARRAY:
        out.write(struct.pack(">i", len(v)) + bytes(v))
    elif t == STRING:
        b = v.encode("utf-8")
        out.write(struct.pack(">H", len(b)) + b)
    elif t == LIST:
        out.write(struct.pack(">bi", v.elem, len(v.items)))
        for item in v.items:
            _write(out, v.elem, item.value)
    elif t == COMPOUND:
        for name, tag in v.items():
            nb = name.encode("utf-8")
            out.write(struct.pack(">bH", tag.type, len(nb)) + nb)
            _write(out, tag.type, tag.value)
        out.write(b"\x00")
    elif t == INT_ARRAY:
        out.write(struct.pack(">i%di" % len(v), len(v), *v))
    elif t == LONG_ARRAY:
        out.write(struct.pack(">i%dq" % len(v), len(v), *v))
    else:
        raise ValueError("bad tag type %d" % t)


def loads(data: bytes) -> tuple[str, dict]:
    buf = io.BytesIO(data)
    (t,) = struct.unpack(">b", buf.read(1))
    if t != COMPOUND:
        raise ValueError("root is not a compound")
    (n,) = struct.unpack(">H", buf.read(2))
    name = buf.read(n).decode("utf-8")
    return name, _read(buf, COMPOUND)


def dumps(name: str, root: dict) -> bytes:
    out = io.BytesIO()
    nb = name.encode("utf-8")
    out.write(struct.pack(">bH", COMPOUND, len(nb)) + nb)
    _write(out, COMPOUND, root)
    return out.getvalue()


def load_file(path: str) -> tuple[str, dict]:
    raw = open(path, "rb").read()
    try:
        raw = gzip.decompress(raw)
    except OSError:
        try:
            raw = zlib.decompress(raw)
        except zlib.error:
            pass
    return loads(raw)


def save_file(path: str, name: str, root: dict) -> None:
    with open(path, "wb") as f:
        f.write(gzip.compress(dumps(name, root)))


# Convenience constructors
def b(v): return Tag(BYTE, v)
def s(v): return Tag(SHORT, v)
def i(v): return Tag(INT, v)
def st(v): return Tag(STRING, v)
def comp(**kw): return Tag(COMPOUND, dict(kw))
def lst(elem, items): return Tag(LIST, TagList(elem, items))
