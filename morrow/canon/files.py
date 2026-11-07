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
        add("region/README.txt", "region/ not included in this export (over the 50 MB ticket attachment limit).\n"
            "full copy: CHI-07:/srv/backup/mossfield/2011-04-20/region (6 files, r.-2.-1 .. r.0.0)\n")
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
# panel_ok.jpg: Theo's 2011 screenshot of the TCAdmin panel. He pasted Print Screen into
# Paint.NET 3.5.8 and saved it at 06:44:31 (with a small EXIF thumbnail), then cropped off the
# "Start service" confirmation and saved again at 06:46. The old metadata came across, thumbnail
# and all, so the thumbnail still shows the uncropped original. Auto restart reads On: Iona only
# turned it off at 11:12.

def _panel_full():
    """The uncropped Print Screen: the panel on the left, and behind it, the Mumble window Theo had
    open at 06:44 (rookery and patchcord in the channel with him: the lead to MIC-5-18-B.WAV). Type is
    large so the EXIF thumbnail (320 wide, 0.4x) stays readable."""
    from PIL import Image, ImageDraw, ImageFont
    font = lambda n: ImageFont.load_default(size=n)
    img = Image.new("RGB", (800, 500), (232, 236, 239))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 800, 34], fill=(45, 83, 103))
    d.text((12, 8), "TCAdmin 1.0  -  Game Services  -  logged in: tvale", fill=(255, 255, 255), font=font(17))
    d.rectangle([20, 54, 560, 300], outline=(138, 154, 166), fill=(255, 255, 255))
    rows = [("Service", "CHI-07/mrrw"), ("Status", "Stopped (manual, 06:17:22)"), ("Owner", "Copperline Recovery"),
            ("Auto restart", "On")]
    for i, (k, v) in enumerate(rows):
        d.text((40, 72 + i * 44), k, fill=(60, 60, 60), font=font(20))
        d.text((220, 72 + i * 44), v, fill=(20, 20, 20), font=font(20))
    # the Mumble window, behind and to the right, mostly in the band Theo cropped off
    d.rectangle([580, 54, 790, 300], outline=(120, 120, 120), fill=(246, 246, 246))
    d.rectangle([580, 54, 790, 80], fill=(90, 90, 90))
    d.text((588, 58), "Mumble 1.2.3", fill=(255, 255, 255), font=font(17))
    d.rectangle([430, 312, 790, 494], outline=(120, 120, 120), fill=(246, 246, 246))
    d.text((442, 318), "mossfield", fill=(30, 30, 30), font=font(30))
    for i, name in enumerate(["rookery", "patchcord (idle)", "tvale"]):
        d.ellipse([446, 362 + i * 40, 462, 378 + i * 40], fill=(60, 140, 60) if "idle" not in name else (170, 170, 170))
        d.text((472, 354 + i * 40), name, fill=(30, 30, 30), font=font(30))
    d.text((690, 462), "06:44", fill=(90, 90, 90), font=font(26))
    # the confirmation Theo cut off, large enough to read in the thumbnail
    d.rectangle([20, 312, 420, 494], outline=(170, 40, 40), fill=(255, 244, 244))
    d.text((32, 318), "Start service", fill=(140, 20, 20), font=font(44))
    d.text((32, 366), "CHI-07/mrrw ?", fill=(140, 20, 20), font=font(40))
    d.text((32, 412), "mid-session, will", fill=(60, 60, 60), font=font(28))
    d.text((32, 440), "corrupt if left", fill=(60, 60, 60), font=font(28))
    d.rectangle([300, 420, 408, 480], fill=(45, 83, 103))
    d.text((318, 432), "Start", fill=(255, 255, 255), font=font(30))
    d.text((300, 318), "tvale 06:44:10", fill=(60, 60, 60), font=font(17))
    return img


def _exif_with_thumbnail(thumb_jpeg: bytes) -> bytes:
    """Minimal little-endian TIFF/EXIF: IFD0 (Software, DateTime) + IFD1 (JPEG thumbnail). A screenshot
    has no camera, so no Make or Model."""
    entries0 = [(0x0131, 2, b"Paint.NET v3.5.8\x00"), (0x0132, 2, b"2011:05:18 06:44:31\x00")]
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
    thumb.thumbnail((320, 200))
    thumb.save(tb, "JPEG", quality=90)
    cropped = full.crop((0, 0, 800, 306))  # Theo cut off the confirmation box (and the Mumble window with it)
    out = io.BytesIO()
    cropped.save(out, "JPEG", quality=88, exif=_exif_with_thumbnail(tb.getvalue()))
    return out.getvalue()


def text_files() -> dict[str, bytes]:
    return {
        "6118-checklist.txt": records.checklist().encode(),
        # Theo's Act 7 copy has 10 June cut out (the clean backup); the missing day is Act 9's drop
        "copperline-ops.irc.log": irc.render(skip=("2011-06-10",)).encode(),
        "copperline-ops.irc.2011-06-10.log": irc.render(only=("2011-06-10",)).encode(),
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
        "heartbeat.log.2011-05": records.heartbeat_log("2011-05").encode(),
        "heartbeat.log.2019-03": records.heartbeat_log("2019-03").encode(),
        # patch's index of the Mumble night, sent to June with the file; the desk hands it over while the
        # recording itself is not made yet, so the floor can be solved without it
        "moss_0518.txt": MOSS_0518_NOTES.encode(),
    }


MOSS_0518_NOTES = """moss_0518.ogg - mumble, 18 may, from 11:58 my time (05:58 chicago). 21 min, mostly nothing.
where stuff is, for june (you asked):

00:00 start. me, rook, june, cairn. kit's asleep. theo not on.
00:20 cairn: its doing his morning loop. farm, storehouse, farm. hes stood in the storehouse watching himself walk past
00:38 june: her tower has a glass roof now. she didnt put one on
00:46 rook: it read her plans post and guessed
01:05 theres a second rook at the cake. actual rook is at spawn
01:20 rook reads the spawn rules out loud, all six, "b4" and everything. says he wrote them for bracken
02:30 me: it typed as theo earlier. "no worries at all", word for word
03:00 iona joins. shes stopping it. everyone off in 15
03:15 rook: theo said leave it. iona: theos not watching it, i am
03:24 rook asks how he stops it himself if it happens again. iona wont say it on here because im recording. "ill message you"
03:42 does theo know how to stop it. "theo knows how to start it"
04:02 rook: hes going to write it down somewhere it cant edit. in wool, in his cellar. back and forth like finch's rail, and shifted by the rules so it cant read it. iona: thats actually smart
04:30 to 11:15 nothing. seven minutes of my footsteps
11:30 june laughs at my typing (dont)
13:10 cairn again. theres another him at the farm
16:30 iona: everyone off. june's taking pictures first
16:50 "dont move, any of you"
19:22 iona: done, its down
19:30 cairn: the other him stopped in the field. then gone
19:57 "thats it. going to sleep. nobody touch anything"
20:40 me saving it. this file

add the minutes to 05:58 for chicago time if you need it to line up with anything.
- patch
"""


def build(players: list[str] | None = None) -> dict[str, str]:
    OUT.mkdir(parents=True, exist_ok=True)
    clean = backup_zip(False)
    files = {
        "copperline-asset-sale-2014.pdf": asset_sale_pdf(),
        "panel_ok.jpg": panel_jpg(),
        "notrecovery-mossfield_2011-04-20.zip": clean,
        "recovery-mossfield_2011-04-20.zip": backup_zip(True, players),
        # Rook left it BESIDE the zip in September 2011: inside, it would break the MD5 Iona logged in June
        "rook.txt": later.ROOK_TXT.encode(),
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
