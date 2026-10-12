"""Generated evidence files (deterministic: same inputs, same bytes, same MD5).

    python -m morrow.canon.files

Writes dashboard/private-media/files/ (served by gated routes, never public):
  copperline-asset-sale-2014.pdf    Act 4 drop (Lot 14 -> T. Vale)
  6118-checklist.txt                Act 5 drop (Iona's hashes)
  panel_ok.jpg                      Act 6, ~tvale: cropped screenshot whose EXIF thumbnail keeps the original
  copperline-ops.irc.log            Act 7 drop
  mrrw-changelog-2016.txt, crontab.txt, bridge.yml   Act 8 drops
  mossfield_2011-04-20.zip (clean)  Rookery's /notrecovery/  (MD5 goes in the checklist)
  mossfield_2011-04-20.zip (recovered, contaminated) /recovery/  -> recovery-mossfield_2011-04-20.zip
"""

from __future__ import annotations

import hashlib
import io
import json
import struct
import zipfile
from pathlib import Path

from . import irc, later, people, records

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "dashboard" / "private-media" / "files"
FIXED = (2011, 4, 20, 3, 0, 0)


def md5_bytes(b: bytes) -> str:
    return hashlib.md5(b).hexdigest()


# ---------------------------------------------------------------------------
# Backups. The real ones are rebuilt from the Beta world once it exists; until then the
# archives hold a small, honest stand-in so the hashes and the puzzle work end to end.

def backup_zip(contaminated: bool, players: list[str] | None = None) -> bytes:
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, "w", zipfile.ZIP_DEFLATED) as z:
        def add(name, data):
            info = zipfile.ZipInfo("Mossfield/" + name, date_time=FIXED)
            info.external_attr = 0o644 << 16
            z.writestr(info, data)
        add("level.dat.txt", "LevelName: Mossfield\nRandomSeed: 137407650\nVersion: 19132 (McRegion)\nLastPlayed: 2011-04-20 02:30 CDT\n")
        for r in people.RESIDENTS:
            add("players/%s.dat.txt" % r.handle, r.handle + "\n")
        add("region/README.txt", "region files r.-1.-1.mcr .. r.1.1.mcr (stand-in until the Beta world is archived)\n")
        if contaminated:
            add("players/lowpath.dat.txt", "lowpath\n")
            for p in players or []:
                add("players/%s.dat.txt" % p, p + "\n")
            add("mrrw/restore.json", json.dumps({"restored_by": "mrrw", "sessions_open": ["lowpath"], "source": "recovered image"}))
    return buf.getvalue()


# ---------------------------------------------------------------------------
# The asset-sale PDF

def asset_sale_pdf() -> bytes:
    from reportlab.lib.pagesizes import letter
    from reportlab.pdfgen import canvas

    buf = io.BytesIO()
    c = canvas.Canvas(buf, pagesize=letter, invariant=1)
    c.setTitle("Copperline Hosting LLC - Asset Sale, Lot Results")
    c.setAuthor("Lakeshore Liquidators")
    sale = records.ASSET_SALE
    y = 740
    c.setFont("Helvetica-Bold", 14)
    c.drawString(60, y, "LAKESHORE LIQUIDATORS, INC.")
    c.setFont("Helvetica", 10)
    c.drawString(60, y - 16, "Chicago, IL  ·  Business asset dispositions")
    c.setFont("Helvetica-Bold", 12)
    c.drawString(60, y - 50, sale["title"])
    c.setFont("Helvetica", 10)
    c.drawString(60, y - 66, "Sale date: December 9, 2014  ·  Lots sold as-is, where-is. Buyer collects by Dec 16.")
    y -= 104
    c.setFont("Helvetica-Bold", 10)
    for x, h in ((60, "Lot"), (100, "Description"), (400, "Buyer")):
        c.drawString(x, y, h)
    c.line(60, y - 4, 540, y - 4)
    c.setFont("Helvetica", 10)
    for lot in sale["lots"]:
        y -= 20
        c.drawString(60, y, str(lot["lot"]))
        c.drawString(100, y, lot["item"])
        c.drawString(400, y, lot["buyer"])
    c.setFont("Helvetica-Oblique", 8)
    c.drawString(60, 80, "Drives in Lot 14 sold without wipe at buyer's request (signed waiver on file).")
    c.showPage()
    c.save()
    return buf.getvalue()


# ---------------------------------------------------------------------------
# panel_ok.jpg: Theo's 2011 screenshot of the TCAdmin panel, cropped to hide the
# "Start service" confirmation. The JPEG's EXIF thumbnail was never regenerated, so it
# still shows the uncropped original. (A real-world forensic tell, 2011-plausible.)

def _panel_full():
    from PIL import Image, ImageDraw
    img = Image.new("RGB", (800, 500), (232, 236, 239))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 800, 34], fill=(45, 83, 103))
    d.text((12, 10), "TCAdmin 1.0  -  Game Services  -  logged in: tvale", fill=(255, 255, 255))
    d.rectangle([20, 54, 780, 300], outline=(138, 154, 166), fill=(255, 255, 255))
    rows = [("Service", "CHI-07/mrrw"), ("Status", "Stopped (manual, 06:17:22)"), ("Owner", "Copperline Recovery"),
            ("Auto restart", "Off")]
    for i, (k, v) in enumerate(rows):
        d.text((40, 74 + i * 28), k, fill=(60, 60, 60))
        d.text((220, 74 + i * 28), v, fill=(20, 20, 20))
    d.rectangle([20, 320, 780, 470], outline=(170, 40, 40), fill=(255, 244, 244))
    d.text((40, 336), "Confirm: Start service CHI-07/mrrw?", fill=(140, 20, 20))
    d.text((40, 362), "Reason: mid-session, will corrupt if left", fill=(60, 60, 60))
    d.text((40, 388), "Requested by tvale  2011-05-18 06:44:10 CDT", fill=(60, 60, 60))
    d.rectangle([40, 420, 160, 450], fill=(45, 83, 103))
    d.text((70, 428), "Start", fill=(255, 255, 255))
    return img


def _exif_with_thumbnail(thumb_jpeg: bytes) -> bytes:
    """Minimal little-endian TIFF/EXIF: IFD0 (Make, Software, DateTime) + IFD1 (JPEG thumbnail)."""
    entries0 = [(0x010F, 2, b"Dell\x00"), (0x0131, 2, b"Paint.NET v3.5.8\x00"), (0x0132, 2, b"2011:05:18 06:44:31\x00")]
    header = b"II*\x00" + struct.pack("<I", 8)
    n0 = len(entries0)
    ifd0_size = 2 + n0 * 12 + 4
    data_off = 8 + ifd0_size
    blobs, ifd0 = b"", struct.pack("<H", n0)
    for tag, typ, val in entries0:
        if len(val) <= 4:
            ifd0 += struct.pack("<HHI", tag, typ, len(val)) + val.ljust(4, b"\x00")
        else:
            ifd0 += struct.pack("<HHII", tag, typ, len(val), data_off + len(blobs))
            blobs += val
    ifd1_off = data_off + len(blobs)
    ifd0 += struct.pack("<I", ifd1_off)
    ifd1_size = 2 + 3 * 12 + 4
    thumb_off = ifd1_off + ifd1_size
    ifd1 = struct.pack("<H", 3)
    ifd1 += struct.pack("<HHII", 0x0103, 3, 1, 6)              # Compression = JPEG
    ifd1 += struct.pack("<HHII", 0x0201, 4, 1, thumb_off)      # JPEGInterchangeFormat
    ifd1 += struct.pack("<HHII", 0x0202, 4, 1, len(thumb_jpeg))  # JPEGInterchangeFormatLength
    ifd1 += struct.pack("<I", 0)
    return b"Exif\x00\x00" + header + ifd0 + blobs + ifd1 + thumb_jpeg


def panel_jpg() -> bytes:
    full = _panel_full()
    tb = io.BytesIO()
    thumb = full.copy()
    thumb.thumbnail((160, 100))
    thumb.save(tb, "JPEG", quality=80)
    cropped = full.crop((0, 0, 800, 310))  # Theo cut off the confirmation box
    out = io.BytesIO()
    cropped.save(out, "JPEG", quality=88, exif=_exif_with_thumbnail(tb.getvalue()))
    return out.getvalue()


def text_files() -> dict[str, bytes]:
    return {
        "6118-checklist.txt": records.checklist().encode(),
        "copperline-ops.irc.log": irc.render().encode(),
        "mrrw-changelog-2016.txt": ("\n".join(records.THEO_CHANGELOG_2016) + "\n").encode(),
        "crontab.txt": ("\n".join(records.CRONTAB) + "\n").encode(),
        "kit-2019-03-02.eml": (
            "From: Kit Vale <kit.vale@example.net>\nTo: Theo Vale <theo.vale@example.net>\n"
            "Date: Sat, 02 Mar 2019 22:47:13 -0600\nSubject: (no subject)\n\n"
            + records.KIT_EMAIL["body"] + "\n").encode(),
        "macros.txt": "\n".join([
            "Copperline support macros (T. Vale)", "",
            "[ack]      Hi!  Thanks for getting in touch.  I'm on it.",
            "[world]    No worries at all!  Your world will be back by tomorrow.",
            "[billing]  No worries at all, I'll sort the invoice.",
            "[restore]  Iona's looking at it now.  Hang tight.",
            "[close]    Cheers, T", ""]).encode(),
        "bridge.yml": "".join("%s: %s\n" % kv for kv in records.BRIDGE_CONFIG.items()).encode(),
    }


def build(players: list[str] | None = None) -> dict[str, str]:
    OUT.mkdir(parents=True, exist_ok=True)
    clean = backup_zip(False)
    files = {
        "copperline-asset-sale-2014.pdf": asset_sale_pdf(),
        "panel_ok.jpg": panel_jpg(),
        "notrecovery-mossfield_2011-04-20.zip": clean,
        "recovery-mossfield_2011-04-20.zip": backup_zip(True, players),
    }
    files.update(text_files())
    hashes = {}
    for name, data in files.items():
        (OUT / name).write_bytes(data)
        hashes[name] = md5_bytes(data)
    (OUT.parent / "hashes.json").write_text(json.dumps(hashes, indent=1), encoding="utf-8")
    return hashes


if __name__ == "__main__":
    for k, v in build().items():
        print(v, k)
    _ = later  # canon import keeps the dependency explicit
