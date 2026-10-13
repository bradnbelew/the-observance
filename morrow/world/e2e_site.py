"""End-to-end check of the Copperline mirror, Acts 1-10, against a local test preview.

    python -m morrow.world.e2e_site [http://localhost:3040]

Needs the preview started with MORROW_DEV=1 and MORROW_DESK_FAST=1 (dashboard/scripts/dev-v2.mjs).
Plays each act like a player: files evidence at the Archive Desk, opens drops, logs into TCAdmin,
browses the FTP, submits the case brief. Act 10's console is checked separately against Minecraft.

The mrrw bridge is played by a fake plugin started here (127.0.0.1:25591, the URL dev-v2.mjs gives
the preview by default; MORROW_E2E_BRIDGE_PORT changes it). It checks every request's signature
(MORROW_STOP_SECRET, or the dev default), serves /state, and records /event, so the run proves: desk
filings post the right signed events, the act follows the plugin, hint timers scale with the group,
and the Act 10 console only takes the group's USB winner.

Every desk, drop, folder and FTP attachment the site links is actually downloaded (200, non-empty).
Every private-media file the canon bundles reference must be visible to git (not ignored), since
Vercel builds from git. Owner-recorded media that doesn't exist yet (June's shots, closet.jpg,
the recordings, the voicemail, MIC-5-18-B.WAV, Theo's voice notes) is reported as "pending owner
media" instead of failing.
"""

from __future__ import annotations

import base64
import hashlib
import hmac
import html
import http.cookiejar
import json
import os
import re
import subprocess
import sys
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timedelta, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

from morrow.canon import ciphers, later, layout

BASE = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:3040"
ROOT = Path(__file__).resolve().parents[2]
MEDIA = ROOT / "dashboard" / "private-media"
CANON = ROOT / "dashboard" / "src" / "lib" / "canon"
TEMP_PASSWORD = "cl-mirror-71e4"

jar = http.cookiejar.CookieJar()
opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))
FAILS: list[str] = []
PENDING: set[str] = set()
FETCHED: set[str] = set()

# Files in private-media/files/ that the owner records or photographs; everything else there is generated.
OWNER_FILES = {"closet.jpg", "moss_0518.ogg", "theo_voicemail_2011.wav"}
VOICE_EXT = ("ogg", "mp3", "m4a", "wav")
# Voice notes named in dashboard/src/lib/morrow/desk.ts rather than in the canon bundles.
CODE_VOICES = {"T-01", "T-09a", "T-09b", "T-10-booked"}


def get(path: str, data: dict | None = None, headers: dict | None = None, via=None) -> tuple[int, str, bytes]:
    body = urllib.parse.urlencode(data).encode() if data is not None else None
    req = urllib.request.Request(BASE + path, data=body, headers=headers or {})
    try:
        with (via or opener).open(req, timeout=60) as r:
            raw = r.read()
            return r.status, r.geturl(), raw
    except urllib.error.HTTPError as e:
        return e.code, path, e.read()


def basic(user: str, password: str) -> dict:
    return {"Authorization": "Basic " + base64.b64encode(f"{user}:{password}".encode()).decode()}


def text(raw: bytes) -> str:
    return re.sub(r"<!-- -->", "", raw.decode("utf-8", "replace"))


def check(ok: bool, msg: str) -> None:
    print(("  ok   " if ok else "  FAIL ") + msg)
    if not ok:
        FAILS.append(msg)


def act(n: int) -> None:
    get(f"/dev-act?n={n}&back=/")
    print(f"act {n}")


# ---------------------------------------------------------------------------
# private media: what the canon references, what's on disk, what git (and so Vercel) will see

def owner_media(rel: str) -> bool:
    return rel.startswith(("media/", "theo/")) or rel.rsplit("/", 1)[-1] in OWNER_FILES


def on_disk(rel: str) -> Path | None:
    """The file as the site finds it (case-insensitive, like dashboard/src/lib/morrow/serve.ts)."""
    path = MEDIA / rel
    if path.is_file():
        return path
    if path.parent.is_dir():
        for p in path.parent.iterdir():
            if p.name.lower() == path.name.lower() and p.is_file():
                return p
    return None


def voice_paths(vid: str) -> list[str]:
    return [f"theo/{vid}.{ext}" for ext in VOICE_EXT]


def referenced_media() -> dict[str, list[str]]:
    """label -> candidate private-media paths, for everything the canon bundles point at."""
    refs: dict[str, list[str]] = {}
    voices = set(CODE_VOICES)

    def add(rel: str) -> None:
        refs.setdefault(rel, [rel])

    for n in range(1, 11):
        b = json.loads((CANON / f"act-{n:02d}.json").read_text(encoding="utf-8"))
        desk = b["desk"]
        for d in desk["drops"]:
            if d.get("file"):
                add("files/" + d["file"])
        for name, f in desk["files"].items():
            add("files/" + name)
            if f.get("voice"):
                voices.add(f["voice"])
        for r in desk["rules"]:
            if r.get("attach"):
                add("files/" + r["attach"])
            voices.update(v for v in (r.get("voice"), r.get("partial_voice")) if v)
        for m in b["ticket_6118"]["messages"]:
            if m.get("attach"):
                add("files/" + m["attach"])
        for s in b["june_shots"]:
            add("media/june/" + s["filename"])
        for f in b["patchcord_folder"]:
            if f["size"] != "0":
                add("media/patchcord/" + f["file"])
        for f in b["tvale_folder"]:
            add("files/" + f["file"])
        for f in b["tvale_box"]:
            add("files/" + f)
        voices.update(v["voice"] for v in (b.get("usb_choices") or {}).values() if v.get("voice"))
    add("media/ftp/incoming/MIC-5-18-B.WAV")
    add("files/recovery-mossfield_2011-04-20.zip")
    add("files/notrecovery-mossfield_2011-04-20.zip")
    for v in voices:
        refs[f"theo/{v}.*"] = voice_paths(v)
    return refs


def check_media_in_git() -> None:
    print("private media referenced by the canon")
    present: list[Path] = []
    for label, cands in sorted(referenced_media().items()):
        hit = next((p for p in map(on_disk, cands) if p), None)
        if hit:
            present.append(hit)
        elif owner_media(cands[0]):
            PENDING.add(label)
        else:
            check(False, f"{label} is referenced by the canon but missing from private-media (rerun python -m morrow.canon.build?)")
    rels = [p.relative_to(ROOT).as_posix() for p in present]
    run = lambda *args, stdin=None: subprocess.run(["git", *args], input=stdin, capture_output=True, text=True, cwd=ROOT).stdout.split()
    ignored = set(run("check-ignore", "--stdin", stdin="\n".join(rels)))
    tracked = set(run("ls-files", "--", *rels))
    for r in sorted(ignored):
        check(False, f"{r} is git-ignored: Vercel would never get it")
    check(not ignored, f"none of the {len(rels)} referenced media files on disk is git-ignored")
    untracked = [r for r in rels if r not in tracked and r not in ignored]
    if untracked:
        print("  note  not committed yet (commit before deploying): " + ", ".join(untracked))


# ---------------------------------------------------------------------------
# downloads

def fetch(path: str, label: str, cands: list[str] | None = None, headers: dict | None = None) -> bytes:
    """Download a linked attachment: 200 and non-empty, or 404 for owner media not recorded yet."""
    status, _, body = get(path, headers=headers)
    if status == 404 and cands and owner_media(cands[0]) and not any(on_disk(c) for c in cands):
        key = cands[0] if len(cands) == 1 else cands[0].rsplit(".", 1)[0] + ".*"
        PENDING.add(key)
        print(f"  wait {label} (pending owner media: {key})")
        return b""
    check(status == 200 and len(body) > 0, f"{label} downloads ({status}, {len(body)} bytes)")
    return body


def token_payload(token: str) -> dict:
    body = token.split(".")[0]
    return json.loads(base64.urlsafe_b64decode(body + "=" * (-len(body) % 4)))


def fetch_links(html: str, where: str) -> None:
    """Every desk attachment and voice note linked on a page."""
    for href, name in re.findall(r'href="(/desk-file/[^"]+)"[^>]*>([^<]+)<', html):
        if href not in FETCHED:
            FETCHED.add(href)
            fetch(href, f"{where}: attachment {name}", ["files/" + token_payload(href.rsplit("/", 1)[1])["f"]])
    for src in re.findall(r'src="(/desk-voice/[^"]+)"', html):
        if src not in FETCHED:
            FETCHED.add(src)
            vid = token_payload(src.rsplit("/", 1)[1])["v"]
            fetch(src, f"{where}: voice note {vid}", voice_paths(vid))


def desk(subject: str, message: str) -> str:
    _, url, raw = get("/submitticket.php", {"name": "e2e", "subject": subject, "message": message})
    page = text(raw)
    fetch_links(page, f"ticket '{subject}'")
    return page


def desk_home(where: str) -> str:
    page = text(get("/support/index.php")[2])
    fetch_links(page, where)
    return page


def folders() -> dict:
    return json.loads((CANON / "folders.json").read_text(encoding="utf-8"))


def flat(page: str) -> str:
    """Visible text: tags dropped, entities decoded, whitespace collapsed (Theo's double spaces become one)."""
    return re.sub(r"\s+", " ", html.unescape(re.sub(r"<[^>]+>", " ", page)))


# ---------------------------------------------------------------------------
# the fake mrrw bridge (the plugin's side of the contract in dashboard/src/lib/morrow/bridge.ts)

STOP_SECRET = os.environ.get("MORROW_STOP_SECRET", "").strip() or "mrrw-dev-stop-secret"
BRIDGE_PORT = int(os.environ.get("MORROW_E2E_BRIDGE_PORT", "25591"))
SIG_VECTOR = "568f9284666a5990ca2bebb4c800e07a26d3e792e627d51c0e7e1d860433c833"  # test-stop-secret, GET /state, 1700000000, ""


def bridge_sig(secret: str, method: str, target: str, ts: str, body: bytes) -> str:
    msg = f"{method}\n{target}\n{ts}\n".encode() + body
    return hmac.new(secret.encode(), msg, hashlib.sha256).hexdigest()


def default_state() -> dict:
    return {"act": 1, "act_started": None, "players": 0, "ready": False, "stopped": False,
            "usb": {"votes": {"clean": 0, "recovered": 0, "corpus": 0}, "winner": None, "open": False},
            "fingerprint": hmac.new(STOP_SECRET.encode(), b"mrrw-fingerprint", hashlib.sha256).hexdigest()[:8],
            "theo": []}


class FakeBridge:
    state = default_state()
    events: list[dict] = []
    bad: list[str] = []      # requests whose signature didn't verify
    gets = 0
    refuse = False           # answer 401 to everything (a wrong secret)


class BridgeHandler(BaseHTTPRequestHandler):
    def log_message(self, *_args) -> None:
        pass

    def _send(self, code: int, obj: dict) -> None:
        raw = json.dumps(obj).encode()
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(raw)))
        self.end_headers()
        self.wfile.write(raw)

    def _signed(self, body: bytes) -> bool:
        ts, sig = self.headers.get("X-Mrrw-Ts", ""), self.headers.get("X-Mrrw-Sig", "")
        ok = (ts.isdigit() and abs(time.time() - int(ts)) <= 120 and re.fullmatch(r"[0-9a-f]{64}", sig) is not None
              and hmac.compare_digest(sig, bridge_sig(STOP_SECRET, self.command, self.path, ts, body)))
        if not ok:
            FakeBridge.bad.append(f"{self.command} {self.path}")
        return ok and not FakeBridge.refuse

    def do_GET(self) -> None:  # noqa: N802
        if not self._signed(b""):
            return self._send(401, {"error": "signature"})
        if self.path == "/state":
            FakeBridge.gets += 1
            return self._send(200, FakeBridge.state)
        self._send(404, {"error": "not found"})

    def do_POST(self) -> None:  # noqa: N802
        body = self.rfile.read(int(self.headers.get("Content-Length") or 0))
        if not self._signed(body):
            return self._send(401, {"error": "signature"})
        if self.path == "/event":
            FakeBridge.events.append(json.loads(body))
            return self._send(200, {"ok": True, "act": FakeBridge.state["act"]})
        self._send(404, {"error": "not found"})


def start_bridge() -> ThreadingHTTPServer:
    srv = ThreadingHTTPServer(("127.0.0.1", BRIDGE_PORT), BridgeHandler)
    threading.Thread(target=srv.serve_forever, daemon=True).start()
    return srv


def set_bridge(**changes) -> None:
    FakeBridge.state = {**FakeBridge.state, **changes}


def bridge_connected() -> bool:
    """Whether the preview talks to this fake (it was started with MORROW_BRIDGE_URL pointing here)."""
    before = FakeBridge.gets
    get("/support/index.php")
    return FakeBridge.gets > before


def event(ref: str, wait: float = 6.0) -> dict | None:
    """The desk event the site posted for `ref` (it is sent after the response, so give it a moment)."""
    end = time.time() + wait
    while time.time() < end:
        for e in FakeBridge.events:
            if e.get("ref") == ref:
                return e
        time.sleep(0.1)
    return None


def check_event(ref: str, voice: str | None, label: str) -> None:
    e = event(ref)
    check(e is not None and e.get("kind") == "desk" and e.get("voice") == voice and bool(e.get("reply_at"))
          and e.get("subject") is not None,
          f"{label}: signed POST /event ref={ref} voice={voice} reply_at set" + ("" if e else " (nothing arrived)"))


def ago(hours: float) -> str:
    return (datetime.now(timezone(timedelta(hours=-5))) - timedelta(hours=hours)).isoformat(timespec="seconds")


def check_signature_vector() -> None:
    print("bridge signature")
    py = bridge_sig("test-stop-secret", "GET", "/state", "1700000000", b"")
    check(py == SIG_VECTOR, "Python signs the test vector as expected")
    sign_ts = (ROOT / "dashboard" / "src" / "lib" / "morrow" / "bridge-sign.ts").as_uri()
    js = ("import(%r).then(m => console.log(m.bridgeSignature('test-stop-secret', 'GET', '/state', '1700000000', '')))"
          % sign_ts)
    try:
        out = subprocess.run(["node", "--no-warnings", "--input-type=module", "-e", js], capture_output=True, text=True,
                             timeout=60).stdout.strip()
    except (OSError, subprocess.TimeoutExpired) as err:
        out = f"(node failed: {err})"
    check(out == py, f"the site's TypeScript (bridge-sign.ts) signs it identically ({out[:16]}...)")
    # the plugin's vectors (plugin-v2 BridgeLogic): the exact raw target and body bytes are signed
    vectors = [
        ("POST", "/event", '{"kind":"desk","ref":"unmask","reply_at":null,"voice":"T-04a","subject":"Re: who\'s Theo"}',
         "e3cbc7b422424af89abcfeefea80b6b97fc6447260fcf78b8c3103ab78dcaebb"),
        ("GET", "/corpus?heard=hello%20there&persona=rookery&seed=7", "",
         "c36897b35c62fd1830ac5b20ca8e66e551e439e42332f49a1d2c4cd494b6bd4c"),
    ]
    for method, target, body, want in vectors:
        py = bridge_sig("test-stop-secret", method, target, "1700000000", body.encode())
        js = ("import(%r).then(m => console.log(m.bridgeSignature('test-stop-secret', %s, %s, '1700000000', %s)))"
              % (sign_ts, json.dumps(method), json.dumps(target), json.dumps(body)))
        try:
            out = subprocess.run(["node", "--no-warnings", "--input-type=module", "-e", js], capture_output=True,
                                 text=True, timeout=60).stdout.strip()
        except (OSError, subprocess.TimeoutExpired) as err:
            out = f"(node failed: {err})"
        check(py == want and out == want, f"plugin vector {method} {target[:30]}: Python and TypeScript both sign it")
    cli = ROOT / "plugin-v2" / "tools" / "bridge_client.py"
    if cli.is_file():
        try:
            res = subprocess.run([sys.executable, str(cli), "--secret", "test-stop-secret", "sign", "GET", "/state", "1700000000"],
                                 capture_output=True, text=True, timeout=60)
            got = (res.stdout + res.stderr).strip()
        except (OSError, subprocess.TimeoutExpired) as err:
            got = str(err)
        if SIG_VECTOR in got:
            check(True, "the plugin's own bridge_client.py signs the GET /state vector identically")
        else:
            print(f"  note  bridge_client.py sign didn't print the vector (its CLI may differ): {got[:120]}")


def bridge_checks() -> None:
    """The site following the fake plugin: act, act clock, group-scaled hints, folders, the USB winner."""
    print("bridge: the site follows the plugin")
    # a visitor with no act switcher cookie (MORROW_ACT=1 in the preview), on the real act clock
    v = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
    real = {"Cookie": "morrow_dev_clock=real"}
    page = lambda path: flat(text(get(path, headers=real, via=v)[2]))
    hint = lambda n, i: re.sub(r"\s+", " ", later.HINTS[n][i])

    set_bridge(act=1, act_started=ago(20), players=2)
    check("copperline-asset-sale-2014.pdf" not in page("/support/index.php"), "plugin at Act 1: no Act 4 drop on the desk")
    check(get("/~juni_lantern/", via=v)[0] == 404, "plugin at Act 1: June's folder doesn't exist yet (proxy)")
    set_bridge(act=3)
    check(get("/~juni_lantern/", via=v)[0] == 401, "plugin at Act 3: June's folder asks for a password (proxy follows the bridge)")
    set_bridge(act=4)
    desk_page = page("/support/index.php")
    check("copperline-asset-sale-2014.pdf" in desk_page, "plugin at Act 4: the site serves Act 4 (auction drop) with MORROW_ACT=1")
    check(hint(4, 0) in desk_page and hint(4, 1) not in desk_page,
          "2 residents, 20 h into the act: the first hint (24 h x 0.5 = 12 h) is out, the second (36 h) isn't")
    set_bridge(players=5)
    check(hint(4, 0) not in page("/support/index.php"), "5 residents, 20 h in: no hint yet (24 h x 1.0)")
    set_bridge(players=4)
    check(hint(4, 0) in page("/support/index.php"), "4 residents, 20 h in: the first hint is out (24 h x 0.8 = 19.2 h)")
    set_bridge(players=3, act_started=ago(50))
    desk_page = page("/support/index.php")
    check(hint(4, 1) in desk_page and hint(4, 2) not in desk_page, "3 residents, 50 h in: two hints (72 h x 0.65 = 46.8 h)")

    print("bridge: Act 10 console takes the group's USB winner only")
    set_bridge(act=10, act_started=ago(1), players=3)
    get("/tcadmin/legacy/login", {"u": "rookery", "p": TEMP_PASSWORD}, via=v)
    raw_panel = text(get("/tcadmin/legacy", via=v)[2])
    panel = flat(raw_panel)
    undecided = later.USB_VOTE["undecided"].replace("  ", " ")
    check("decided by the group" in panel and undecided in panel and 'name="choice"' not in raw_panel,
          "no winner yet: the console shows the vote and 'The group hasn't decided'")
    form = {"command": ciphers.HALT_COMMAND, "key": ciphers.IONA_TOKEN, "choice": "clean"}
    out = flat(text(get("/tcadmin/legacy/console", form, via=v)[2]))
    check(undecided in out and not re.search(r"[CRW]-[0-9A-F]{4}-[0-9A-F]{4}", out), "no winner yet: the console refuses the stop")
    set_bridge(usb={"votes": {"clean": 1, "recovered": 2, "corpus": 0}, "winner": "recovered", "open": True})
    panel = flat(text(get("/tcadmin/legacy", via=v)[2]))
    rec = later.USB_VOTE["options"]["recovered"]["label"]
    check(f"Retain: {rec}" in panel and "2 votes" in panel, "a winner: the console shows the tally and 'Retain: The finished town'")
    status, url, raw = get("/tcadmin/legacy/console", form, via=v)
    out = flat(text(raw))
    passed = undecided not in out and "choose what to retain" not in out
    check(passed, "with a winner the console accepts the stop request (the form's 'clean' is ignored)")
    ticket = re.search(r'href="(/viewticket\.php[^"]+)"', text(raw))
    if ticket:  # the local Paper server was ready, so a code was issued: the stick holds the winner
        reply = flat(text(get(html.unescape(ticket.group(1)), via=v)[2]))
        check("finished town" in reply, "the stop ticket keeps the group's choice (recovered), not the form's")
    else:
        print("  note  node not ready / unreachable, so no code was issued (the readiness check is e2e_finale's)")

    print("bridge: fallback")
    FakeBridge.refuse = True
    time.sleep(0.2)
    check(get("/~juni_lantern/", via=v)[0] == 404, "a refused signature means no bridge: back to MORROW_ACT=1")
    FakeBridge.refuse = False
    bad = [b for b in FakeBridge.bad if b]
    check(len(FakeBridge.events) > 0 and len(bad) == 0, f"every request the site sent verified ({len(FakeBridge.events)} events, {len(bad)} bad)")
    set_bridge(**default_state())


def main() -> int:
    check_media_in_git()
    check_signature_vector()
    try:
        srv = start_bridge()
    except OSError as err:
        check(False, f"fake bridge could not listen on 127.0.0.1:{BRIDGE_PORT} ({err})")
        srv = None
    time.sleep(3.5)  # the site remembers a missing bridge for 3 s
    linked = srv is not None and bridge_connected()
    check(linked, "the preview talks to the fake bridge (start it with dashboard/scripts/dev-v2.mjs: "
                  f"MORROW_BRIDGE_URL=http://127.0.0.1:{BRIDGE_PORT})")

    act(1)
    s, _, raw = get("/robots.txt")
    check("/forum-import/" in text(raw), "robots names the forum import")
    check("/forum-import/index.php" in text(get("/community/index.php")[2]), "the Community page points at the forum import (same-night start)")
    stranger = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
    check(TEMP_PASSWORD not in text(get("/pwreset.php?step=done", via=stranger)[2]), "pwreset.php?step=done alone does not show the new password")
    get("/dopwreset.php", {"username": "rookery"}, via=stranger)
    _, _, raw = get("/dopwreset.php", {"username": "rookery", "answer": "wrong"}, via=stranger)
    check(TEMP_PASSWORD not in text(raw), "a wrong security answer does not show the new password")
    _, _, raw = get("/dopwreset.php", {"username": "rookery", "answer": "bracken"})
    check(TEMP_PASSWORD in text(raw), "the right security answer shows the new password")
    check("Waived - T.V." in text(get("/clientarea.php?action=invoices")[2]), "security answer opens Rookery's client area")
    check(get("/ftp/")[0] == 503, "FTP is locked by the recovery job in Act 1")
    ticket = text(get("/clientarea.php?action=viewticket")[2])
    check("theo_voicemail_2011.wav" in ticket, "ticket 6118 carries Rookery's voicemail attachment")
    fetch_links(ticket, "ticket 6118")

    home = text(get("/~mossfield/")[2])
    check("server.properties" not in home, "Act 1: Rook's page doesn't list server.properties yet")
    thread = text(get("/forum-import/showthread.php?t=3724")[2])
    check("[QUOTE" not in thread and "Originally Posted by" in thread and "vb-quote" in thread,
          "forum [QUOTE=name] renders as a vBulletin quote box, not raw BBCode")

    act(2)
    home = text(get("/~mossfield/")[2])
    check('href="/~mossfield/server.properties"' in home, "Act 2: Rook's page lists server.properties (mossfield_files)")
    props = text(get("/~mossfield/server.properties")[2])
    check("level-seed=bracken" in props, "server.properties is served with level-seed=bracken")
    key = text(get("/~mossfield/mapkey.txt")[2])
    tx, ty, tz = layout.ANCHORS["june_tower"]
    check("the tower (wip)" in key and f"{tx} {ty} {tz}" in key, "Rook's mapkey.txt lists the tower (at the layout's june_tower)")
    check("markers.json" not in text(get("/~mossfield/dynmap/")[2]), "no anachronistic Dynmap markers.json")

    act(3)
    tx, ty, tz = layout.ANCHORS["june_tower"]
    lb = text(get(f"/~mossfield/logblock?x={tx}&y={ty}&z={tz}&since=2011-05-18&until=2011-05-18")[2])
    check(f"<td>{ty + 22}</td>" in lb and f"<td>{ty + 23}</td>" in lb,
          f"LogBlock at the tower's map-key position reaches the lamp and roof rows (y {ty + 22}, {ty + 23})")
    june = folders()["juni_lantern"]["password"]
    listing = text(get("/~juni_lantern/", headers=basic("juni_lantern", june))[2])
    for name in sorted(set(re.findall(r'href="/media/june/([^"]+)"', listing))):
        fetch(f"/media/june/{name}", f"june's {name}", [f"media/june/{name}"], headers=basic("juni_lantern", june))
    t = flat(desk("hm", "it'll be back by tomorrow, the glass on the tower is weird"))
    check("That's not anyone" not in t, "'tomorrow' + tower details doesn't count as finding Morrow's name")
    t = flat(desk("logblock", "a player called morrow placed glass on june's tower at 04:11:04"))
    check("That's not anyone" in t, "finding Morrow in LogBlock gets Theo's deflection (found_morrow)")
    if linked:
        check_event("found_morrow", None, "Act 3 found_morrow")

    act(4)
    t = desk("who runs this", "the auction says lot 14 went to T. Vale and CHI-07 is the mossfield node")
    check("it's me. Theo" in flat(t), "unmasking with two tokens gets Theo's confession (T-04a)")
    if linked:
        check_event("unmask", "T-04a", "Act 4 unmask")
    t = desk("hm", "who bought lot 14?")
    check("It's just an archive" in t, "one token gets the deflection")
    home = desk_home("Act 4 desk")
    check("copperline-asset-sale-2014.pdf" in home, "Act 4 drop: the auction PDF")
    link = re.search(r'href="(/desk-file/[^"]+)"[^>]*>copperline-asset-sale-2014.pdf', home)
    check(bool(link) and get(link.group(1))[2][:4] == b"%PDF", "the PDF downloads through a signed link")
    check("2011-05-18" in text(get("/~mossfield/logs?day=2011-05-18")[2]), "Act 4: the incident server log is readable")

    act(5)
    t = desk("file request", "could you send moss_0518.ogg please")
    check("Here's patchcord's" in flat(t) and "moss_0518.ogg" in t, "file request by exact name (T-05a + attachment)")
    if linked:
        check_event("moss_0518", "T-05a", "Act 5 moss_0518 file request")
    home = desk_home("Act 5 desk")
    check("/~tvale/" not in home, "the desk doesn't point at Theo's folder before Act 6")
    link = re.search(r'href="(/desk-file/[^"]+)"[^>]*>6118-checklist.txt', home)
    checklist = get(link.group(1))[2].decode() if link else ""
    check(ciphers.md5(ciphers.FLOOR_PLAINTEXT) in checklist, "Act 5 drop: Iona's checklist with the floor hash")
    check(get("/~patchcord/")[0] == 401, "patchcord's folder asks for a password")
    patch = basic("patchcord", "baggage")
    mumble = text(get("/~patchcord/", headers=patch)[2])
    check("moss_0518.ogg" in mumble, "'baggage' opens patchcord's mumble folder")
    for name in sorted(set(re.findall(r'href="/media/patchcord/([^"]+)"', mumble))):
        fetch(f"/media/patchcord/{name}", f"patchcord's {name}", [f"media/patchcord/{name}"], headers=patch)
    check(get("/ftp/")[0] == 200 and "incoming" in text(get("/ftp/")[2]), "FTP opens in Act 5")
    check(get("/ftp/incoming/")[0] == 403, "incoming/ is upload-only (no listing)")

    act(6)
    check("06:44:10" not in text(get("/tcadmin/legacy")[2]), "panel log hidden before login")
    get("/tcadmin/legacy/login", {"u": "rookery", "p": TEMP_PASSWORD})
    panel = text(get("/tcadmin/legacy")[2])
    check("06:44:10" in panel and "mid-session" in panel, "TCAdmin shows Theo's 06:44:10 restart")
    thread = text(get("/forum-import/showthread.php?t=4471")[2])
    check("06:17 CST" in thread, "Morrow's forum rewrite (CST in May) is live")
    check("I stopped the restore process at 06:17 CDT" in text(get("/forum-import/showthread.php?t=4471&view=print")[2]), "print view keeps Iona's original")
    check('href="/~tvale/"' in desk_home("Act 6 desk"), "the desk's staff profile links Theo's folder from Act 6")
    tvale = text(get("/~tvale/")[2])
    check("macros.txt" in tvale, "Theo's staff folder lists his files")
    for name in sorted(set(re.findall(r'href="/media/tvale/([^"]+)"', tvale))):
        fetch(f"/media/tvale/{name}", f"~tvale/{name}", [f"files/{name}"])
    s, _, jpg = get("/media/tvale/panel_ok.jpg")
    check(s == 200 and jpg[:2] == b"\xff\xd8" and jpg.count(b"\xff\xd8") >= 2, "panel_ok.jpg carries a second (thumbnail) JPEG")
    t = desk("the restart", "the panel log says 06:44:10 tvale started CHI-07/mrrw, mid-session")
    check("Kit was" in t and "asleep" in t, "restart evidence gets the confession (T-06a)")
    if linked:
        check_event("restart", "T-06a", "Act 6 restart")
    check("Brief storage maintenance" in text(get("/status/incident-6118")[2]), "status page shows Morrow's rewrite")

    act(7)
    home = desk_home("Act 7 desk")
    check("copperline-ops.irc.log" in home, "Act 7 drop: Iona's IRC log")
    t = flat(desk("found this", f"it was in the chest: {ciphers.IONA_TOKEN}"))
    check("That's Iona's key" in t, "filing Iona's key gets Theo's answer (key)")
    if linked:
        check_event("key", None, "Act 7 key")

    act(8)
    t = desk("where did it learn that", "it keeps saying you can turn it off, its ok. that isn't in any log")
    check("my brother" in t and "kit-2019-03-02.eml" in t, "the ingest question returns Kit's email")
    t = desk("kit", "on 2019-03-02 your brother told you to turn it off")
    check("He was right" in t, "Kit's date + phrase gets T-08a (delay skipped in test mode)")
    if linked:
        check_event("kit", "T-08a", "Act 8 kit")
    fetch("/ftp/incoming/MIC-5-18-B.WAV", "FTP incoming/MIC-5-18-B.WAV", ["media/ftp/incoming/MIC-5-18-B.WAV"])
    s = get("/ftp/incoming/mic-5-18-b.wav")[0]
    check(s in (200, 404) and (s == 200) == bool(on_disk("media/ftp/incoming/MIC-5-18-B.WAV")), "the mic file name matches case-insensitively")
    check("closet.jpg" in desk_home("Act 8 desk"), "Act 8 drop: Theo's closet photo")
    check("box/" in text(get("/~tvale/")[2]), "Theo's folder now lists box/")
    check(get("/~tvale/box/")[0] == 401, "box/ asks for a password")
    box = basic("tvale", "chi07-2010")
    listing = text(get("/~tvale/box/", headers=box)[2])
    check("mrrw-changelog-2016.txt" in listing, "'chi07-2010' (sticky note + founding year) opens Theo's box")
    for name in sorted(set(re.findall(r'href="/media/tvalebox/([^"]+)"', listing))):
        fetch(f"/media/tvalebox/{name}", f"~tvale/box/{name}", [f"files/{name}"], headers=box)

    act(9)
    clean = fetch("/ftp/notrecovery/mossfield_2011-04-20.zip", "FTP notrecovery/ zip")
    md5 = hashlib.md5(clean).hexdigest()
    check(md5 in checklist, "the clean backup's MD5 matches Iona's checklist")
    md5b = hashlib.md5(fetch("/ftp/recovery/mossfield_2011-04-20.zip", "FTP recovery/ zip")).hexdigest()
    check(md5b != md5, "the recovery-folder copy is contaminated (different MD5)")
    rng = urllib.request.Request(BASE + "/ftp/recovery/mossfield_2011-04-20.zip", headers={"Range": "bytes=0-1"})
    try:
        with opener.open(rng, timeout=60) as r:
            check(r.status == 206 and r.read() == b"PK", "FTP downloads honour byte ranges")
    except urllib.error.HTTPError as e:
        check(False, f"FTP downloads honour byte ranges ({e.code})")
    profile = text(get("/forum-import/member.php?u=Tester")[2])
    joined = re.search(r"Join Date</th><td>([0-9-]+)", profile)
    check(bool(joined) and joined.group(1).startswith("2010-11"), "the player has a backdated 2010 forum profile")
    _, _, raw = get("/dobrief.php", {"name": "e2e", "restart": "2011-05-18 06:44:10", "lowpath": "2016-08-03",
                                     "kit": "2019-03-02", "clean": md5, "joined": joined.group(1) if joined else ""})
    check("one USB stick" in text(raw), "a correct case brief convinces Theo (T-09a)")
    fetch_links(text(raw), "case brief")
    if linked:
        check_event("brief", "T-09a", "Act 9 accepted brief")
    _, _, raw = get("/dobrief.php", {"name": "e2e", "restart": "no", "lowpath": "", "kit": "", "clean": "", "joined": ""})
    check("Show me." in text(raw), "a weak brief gets T-09b with what's missing")
    fetch_links(text(raw), "weak brief")
    if linked:
        check_event("brief_missing", "T-09b", "Act 9 weak brief")
    form = flat(text(get("/support/brief.php")[2]))
    check(all(f["label"] in form for f in later.CASE_BRIEF_FIELDS), "the brief form shows every canon field label")
    check(all(f'name="{f["id"]}"' in text(get("/support/brief.php")[2]) for f in later.CASE_BRIEF_FIELDS),
          "the brief form posts the canon field ids")
    _, _, raw = get("/dobrief.php", {"name": "e2e", "restart": "06:44:10", "lowpath": "2016-08-03", "kit": "2019-03-02",
                                     "clean": "Don't let it finish anything.", "joined": "1999-01-01"})
    check("one USB stick" in text(raw), "the clean-copy field accepts rook.txt's line instead of the MD5")
    _, _, raw = get("/dobrief.php", {"name": "e2e", "restart": "06:44:10", "lowpath": "2016-08-03", "kit": "",
                                     "clean": "", "joined": ""})
    miss = flat(text(raw))
    labels = {f["id"]: f["label"] for f in later.CASE_BRIEF_FIELDS}
    check(all(labels[k] in miss for k in ("kit", "clean", "joined")) and labels["restart"] not in miss.split("couldn't match:")[-1],
          "a weak brief names the missing fields by their labels")

    act(10)
    panel = text(get("/tcadmin/legacy")[2])
    check("Console" in panel and "retain one image" in panel, "Act 10 console is on the panel")
    saturdays = re.findall(r'<option value="(\d{4}-\d{2}-\d{2})">', panel)
    check(bool(saturdays), "the maintenance window form offers Saturdays")
    _, _, raw = get("/tcadmin/legacy/book", {"day": "2026-10-14", "hour": "20"})
    check("only be booked on an upcoming Saturday" in text(raw), "a non-Saturday window is refused")
    if saturdays:
        _, _, raw = get("/tcadmin/legacy/book", {"day": saturdays[0], "hour": "20"})
        check("Saturday then" in text(raw), "a Saturday window is booked (T-10-booked)")
        fetch_links(text(raw), "booking")
        if linked:
            check_event("booking", "T-10-booked", "Act 10 booking")
    _, _, raw = get("/tcadmin/legacy/console", {"command": "HALTMRRW", "key": "wrong", "choice": "clean"})
    check("key rejected" in text(raw), "a wrong key is rejected")
    _, _, raw = get("/tcadmin/legacy/console", {"command": "HALT", "key": ciphers.IONA_TOKEN, "choice": "clean"})
    check("unknown command" in text(raw), "a wrong command is rejected")
    s, _, raw = get("/status/heartbeat.json")
    check(s == 200 and "heartbeat" in json.loads(raw), "heartbeat.json answers (stopped state is checked by e2e_finale)")
    if linked:
        bridge_checks()
    print()
    if PENDING:
        print("pending owner media (expected until recorded/uploaded, not a failure):")
        for p in sorted(PENDING):
            print("  - private-media/" + p)
        print()
    print("all site checks passed" if not FAILS else "%d FAILURES" % len(FAILS))
    _ = later
    return 1 if FAILS else 0


if __name__ == "__main__":
    sys.exit(main())
