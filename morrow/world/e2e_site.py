"""End-to-end check of the Copperline mirror, Acts 1-10, against a local test preview.

    python -m morrow.world.e2e_site [http://localhost:3040]

Needs the preview started with MORROW_DEV=1 and MORROW_DESK_FAST=1 (dashboard/scripts/dev-v2.mjs).
Plays each act like a player: files evidence at the Archive Desk, opens drops, logs into TCAdmin,
browses the FTP, submits the case brief. Act 10's console is checked separately against Minecraft.
"""

from __future__ import annotations

import hashlib
import http.cookiejar
import re
import sys
import urllib.parse
import urllib.request

from morrow.canon import ciphers, later

BASE = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:3040"
jar = http.cookiejar.CookieJar()
opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))
FAILS: list[str] = []


def get(path: str, data: dict | None = None) -> tuple[int, str, bytes]:
    body = urllib.parse.urlencode(data).encode() if data is not None else None
    req = urllib.request.Request(BASE + path, data=body)
    try:
        with opener.open(req, timeout=60) as r:
            raw = r.read()
            return r.status, r.geturl(), raw
    except urllib.error.HTTPError as e:
        return e.code, path, e.read()


def text(raw: bytes) -> str:
    return re.sub(r"<!-- -->", "", raw.decode("utf-8", "replace"))


def check(ok: bool, msg: str) -> None:
    print(("  ok   " if ok else "  FAIL ") + msg)
    if not ok:
        FAILS.append(msg)


def act(n: int) -> None:
    get(f"/dev-act?n={n}&back=/")
    print(f"act {n}")


def desk(subject: str, message: str) -> str:
    _, url, raw = get("/submitticket.php", {"name": "e2e", "subject": subject, "message": message})
    return text(raw)


def main() -> int:
    act(1)
    s, _, raw = get("/robots.txt")
    check("/forum-import/" in text(raw), "robots names the forum import")
    get("/dopwreset.php", {"username": "rookery", "answer": "bracken"})
    check("Waived - T.V." in text(get("/clientarea.php?action=invoices")[2]), "security answer opens Rookery's client area")
    check(get("/ftp/")[0] == 503, "FTP is locked by the recovery job in Act 1")

    act(4)
    t = desk("who runs this", "the auction says lot 14 went to T. Vale and CHI-07 is the mossfield node")
    check("it's me. Theo" in t, "unmasking with two tokens gets Theo's confession (T-04a)")
    t = desk("hm", "who bought lot 14?")
    check("It's just an archive" in t, "one token gets the deflection")
    home = text(get("/support/index.php")[2])
    check("copperline-asset-sale-2014.pdf" in home, "Act 4 drop: the auction PDF")
    link = re.search(r'href="(/desk-file/[^"]+)"[^>]*>copperline-asset-sale-2014.pdf', home)
    check(bool(link) and get(link.group(1))[2][:4] == b"%PDF", "the PDF downloads through a signed link")
    check("2011-05-18" in text(get("/~mossfield/logs?day=2011-05-18")[2]), "Act 4: the incident server log is readable")

    act(5)
    t = desk("file request", "could you send moss_0518.ogg please")
    check("Here's patchcord's" in t and "moss_0518.ogg" in t, "file request by exact name (T-05a + attachment)")
    home = text(get("/support/index.php")[2])
    link = re.search(r'href="(/desk-file/[^"]+)"[^>]*>6118-checklist.txt', home)
    checklist = get(link.group(1))[2].decode() if link else ""
    check(ciphers.md5(ciphers.FLOOR_PLAINTEXT) in checklist, "Act 5 drop: Iona's checklist with the floor hash")
    check(get("/~patchcord/")[0] == 401, "patchcord's folder asks for a password")
    req = urllib.request.Request(BASE + "/~patchcord/", headers={"Authorization": "Basic " + __import__("base64").b64encode(b"patchcord:baggage").decode()})
    with opener.open(req, timeout=60) as r:
        check("moss_0518.ogg" in text(r.read()), "'baggage' opens patchcord's mumble folder")
    check(get("/ftp/")[0] == 200 and "incoming" in text(get("/ftp/")[2]), "FTP opens in Act 5")
    check(get("/ftp/incoming/")[0] == 403, "incoming/ is upload-only (no listing)")

    act(6)
    check("06:44:10" not in text(get("/tcadmin/legacy")[2]), "panel log hidden before login")
    get("/tcadmin/legacy/login", {"u": "rookery", "p": "cl-mirror-71e4"})
    panel = text(get("/tcadmin/legacy")[2])
    check("06:44:10" in panel and "mid-session" in panel, "TCAdmin shows Theo's 06:44:10 restart")
    thread = text(get("/forum-import/showthread.php?t=4471")[2])
    check("06:17 CST" in thread, "Morrow's forum rewrite (CST in May) is live")
    check("I stopped the restore process at 06:17 CDT" in text(get("/forum-import/showthread.php?t=4471&view=print")[2]), "print view keeps Iona's original")
    check("macros.txt" in text(get("/~tvale/")[2]), "Theo's staff folder lists his files")
    s, _, jpg = get("/media/tvale/panel_ok.jpg")
    check(s == 200 and jpg[:2] == b"\xff\xd8" and jpg.count(b"\xff\xd8") >= 2, "panel_ok.jpg carries a second (thumbnail) JPEG")
    t = desk("the restart", "the panel log says 06:44:10 tvale started CHI-07/mrrw, mid-session")
    check("Kit was" in t and "asleep" in t, "restart evidence gets the confession (T-06a)")
    check("Brief storage maintenance" in text(get("/status/incident-6118")[2]), "status page shows Morrow's rewrite")

    act(7)
    check("copperline-ops.irc.log" in text(get("/support/index.php")[2]), "Act 7 drop: Iona's IRC log")

    act(8)
    t = desk("where did it learn that", "it keeps saying you can turn it off, its ok. that isn't in any log")
    check("my brother" in t and "kit-2019-03-02.eml" in t, "the ingest question returns Kit's email")
    t = desk("kit", "on 2019-03-02 your brother told you to turn it off")
    check("He was right" in t, "Kit's date + phrase gets T-08a (delay skipped in test mode)")
    check(get("/ftp/incoming/MIC-5-18-B.WAV")[0] in (200, 404), "the mic file path resolves (404 until recorded)")

    act(9)
    md5 = hashlib.md5(get("/ftp/notrecovery/mossfield_2011-04-20.zip")[2]).hexdigest()
    check(md5 in checklist, "the clean backup's MD5 matches Iona's checklist")
    md5b = hashlib.md5(get("/ftp/recovery/mossfield_2011-04-20.zip")[2]).hexdigest()
    check(md5b != md5, "the recovery-folder copy is contaminated (different MD5)")
    profile = text(get("/forum-import/member.php?u=Tester")[2])
    joined = re.search(r"Join Date</th><td>([0-9-]+)", profile)
    check(bool(joined) and joined.group(1).startswith("2010-11"), "the player has a backdated 2010 forum profile")
    _, _, raw = get("/dobrief.php", {"name": "e2e", "restart": "2011-05-18 06:44:10", "lowpath": "2016-08-03",
                                     "kit": "2019-03-02", "clean": md5, "joined": joined.group(1) if joined else ""})
    check("one USB stick" in text(raw), "a correct case brief convinces Theo (T-09a)")
    _, _, raw = get("/dobrief.php", {"name": "e2e", "restart": "no", "lowpath": "", "kit": "", "clean": "", "joined": ""})
    check("Show me." in text(raw), "a weak brief gets T-09b with what's missing")

    act(10)
    panel = text(get("/tcadmin/legacy")[2])
    check("Console" in panel and "retain one image" in panel, "Act 10 console is on the panel")
    _, _, raw = get("/tcadmin/legacy/console", {"command": "HALTMRRW", "key": "wrong", "choice": "clean"})
    check("key rejected" in text(raw), "a wrong key is rejected")
    _, _, raw = get("/tcadmin/legacy/console", {"command": "HALT", "key": ciphers.IONA_TOKEN, "choice": "clean"})
    check("unknown command" in text(raw), "a wrong command is rejected")
    print()
    print("all site checks passed" if not FAILS else "%d FAILURES" % len(FAILS))
    _ = later
    return 1 if FAILS else 0


if __name__ == "__main__":
    sys.exit(main())
