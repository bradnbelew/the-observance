"""Act 10 across both systems: website console -> server-list ping -> stop code -> mrrw stops -> silence.

    python -m morrow.world.e2e_finale [http://localhost:3040] [rcon_tool.py]

Needs: the local preview (dev-v2.mjs) and the Paper test server, already made ready
(waxed MAINT ON sign + lowpath's session ended). Uses the operator fallback /mrrw stop CODE,
because RCON cannot speak as a player.
"""

from __future__ import annotations

import http.cookiejar
import re
import subprocess
import sys
import time
import urllib.parse
import urllib.request

from morrow.canon import ciphers

BASE = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:3040"
RCON = sys.argv[2] if len(sys.argv) > 2 else None
jar = http.cookiejar.CookieJar()
op = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))


def get(path: str, data: dict | None = None) -> str:
    body = urllib.parse.urlencode(data).encode() if data is not None else None
    with op.open(urllib.request.Request(BASE + path, data=body), timeout=60) as r:
        return r.read().decode("utf-8", "replace").replace("<!-- -->", "")


def rcon(*args: str) -> str:
    return subprocess.run([sys.executable, "-I", RCON, "rcon", *args], capture_output=True, text=True).stdout.strip()


def main() -> int:
    get("/dev-act?n=10&back=/")
    get("/dopwreset.php", {"username": "rookery", "answer": "bracken"})
    get("/tcadmin/legacy/login", {"u": "rookery", "p": "cl-mirror-71e4"})
    page = get("/tcadmin/legacy/console", {"command": "HALTMRRW", "key": ciphers.IONA_TOKEN, "choice": "clean"})
    status = re.search(r"node reports: <code>([^<]*)</code>", page)
    code = re.search(r"<code>([CRW]-[0-9A-F]{4}-[0-9A-F]{4})</code>", page)
    print("console says node reports:", status.group(1) if status else "(none)")
    if not code:
        print("FAIL no stop code issued")
        print(re.sub(r"<[^>]+>", " ", page)[-600:])
        return 1
    print("stop code issued:", code.group(1))
    ticket = re.search(r'href="(/viewticket\.php[^"]+)"', page)
    if ticket:
        t = get(ticket.group(1).replace("&amp;", "&"))
        print("Theo's choice reply present:", "April copy" in t or "holes in it" in t)
    if RCON:
        print("server:", rcon("mrrw", "stop", code.group(1)))
        time.sleep(18)
        print("server:", rcon("mrrw", "status"))
        status_page = get("/status")
        print("status page heartbeat:", "no heartbeat" if "no heartbeat" in status_page else "still beating")
        again = get("/tcadmin/legacy/console", {"command": "HALTMRRW", "key": ciphers.IONA_TOKEN, "choice": "clean"})
        s2 = re.search(r"node reports: <code>([^<]*)</code>", again)
        print("after stop, node reports:", repr(s2.group(1)) if s2 else "(silent)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
